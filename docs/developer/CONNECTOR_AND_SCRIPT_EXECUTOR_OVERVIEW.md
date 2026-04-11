# Connector & Script Executor — Concept Overview

## Connector

A **Connector** is the execution layer that takes a **change script file** and deploys it against
a specific target system. Every change filename contains a connector name
(`0001.create_users.DEV.apply.mydb.sql` → `mydb`) that must match a key under `connectors:` in
`totalschema.yml`.

### Four Built-in Types

| Type | Module | What it does |
|---|---|---|
| `jdbc` | `totalschema-core` | Runs scripts against any JDBC database. Delegates actual execution to a `ScriptExecutor` looked up by file extension. |
| `ssh-script` | `totalschema-connector-ssh` | SCPs the whole script to a remote host then executes it as one shell session (stateful: `cd`, variables, functions all persist within a script). |
| `ssh-commands` | `totalschema-connector-ssh` | Opens a fresh SSH `exec` channel for **each line** — no shared shell state between lines. |
| `shell` | `totalschema-connector-shell` | Runs a script on the **local** machine. Auto-selects interpreter (`sh`, `pwsh`, `cmd.exe`) from file extension or the `start.command` config. |

### Key Classes

- `Connector` (abstract) — `execute(ChangeFile, CommandContext)`
- `AbstractConnectorComponentFactory` — factory base; subclasses only implement
  `createConnector(name, config)`
- `DefaultConnectorManager` — reads `connectors.<name>` config, merges env overrides, calls
  `context.get(Connector.class, type, name, config)` via the IoC container

### Plugin Model

Register a custom connector by:

1. Extending `AbstractConnectorComponentFactory` and setting a `getQualifier()` string (e.g. `"ftp"`).
2. Implementing `createConnector(String name, Configuration config)` to return your connector instance.
3. Declaring the factory in `META-INF/services/io.github.totalschema.spi.factory.ComponentFactory`.

No changes to `totalschema-core` needed.

---

## Script Executor

A **ScriptExecutor** is only relevant to the **JDBC connector**. Because `JdbcConnector` is
agnostic to script language, it delegates actual execution by looking up a `ScriptExecutor` keyed
by the **file extension**:

```java
// JdbcConnector.execute() — simplified
JdbcDatabase jdbcDatabase = context.get(JdbcDatabase.class, null, name, connectorConfiguration);

CommandContext executorContext = new CommandContext(context);
executorContext.setValue(JdbcDatabase.class, jdbcDatabase);

ScriptExecutor executor = executorContext.get(ScriptExecutor.class, extension, connectorConfiguration);
executor.execute(fileContent, executorContext);
```

`JdbcConnector` acquires the `JdbcDatabase` and places it in a child `CommandContext` before
the executor is invoked. Executors retrieve it with `context.get(JdbcDatabase.class)` — they do
not create or own the connection themselves.

Variable substitution (`${varName}` replacement) lives in `SqlScriptExecutor`, controlled by the
`variableSubstitution.extensions` key in the connector configuration. Other executors are unaffected.

### Two Built-in Executors

| Extension | Class | Module |
|---|---|---|
| `.sql` | `SqlScriptExecutor` | `totalschema-core` |
| `.groovy` | `GroovyScriptExecutor` | `totalschema-groovy-extensions` |

`SqlScriptExecutor` optionally substitutes variables, splits the file on `;`, and executes each
statement via JDBC.
`GroovyScriptExecutor` injects a `groovy.sql.Sql` object and the `Configuration` so scripts can
use them directly.

### Plugin Model

1. Implement `ScriptExecutor`. Retrieve `JdbcDatabase` from the `Context` inside `execute()` —
   it is placed there by `JdbcConnector` before invocation.
2. Extend `AbstractScriptExecutorComponentFactory` (in `io.github.totalschema.spi.script`) and
   call `super("<extension>")` in the constructor (e.g. `super("python")` for `.py` files).
3. Override `createExecutor(Configuration)` to return your executor instance.
4. Declare the factory in `META-INF/services/io.github.totalschema.spi.factory.ComponentFactory`.

```java
public final class PythonScriptExecutorComponentFactory
        extends AbstractScriptExecutorComponentFactory {

    public PythonScriptExecutorComponentFactory() {
        super("python");
    }

    @Override
    protected ScriptExecutor createExecutor(Configuration configuration) {
        return new PythonScriptExecutor(configuration);
    }
}
```

The IoC container automatically calls `close()` on all `Closeable` executors in reverse creation
order when the engine closes — no manual event subscription needed.

---

## Relationship Between the Two

```
JdbcConnector  ──(file extension)──►  ScriptExecutor
     │
 Other connector types (ssh-script, ssh-commands, shell)
     │
 Execute the script file directly — no ScriptExecutor involved
```

Script executors are exclusively a concern of the JDBC connector. SSH and shell connectors handle
the script content themselves (uploading/running the file as-is).

---

## Related Documentation

- [`CONNECTOR_ARCHITECTURE.md`](CONNECTOR_ARCHITECTURE.md) — full connector architecture,
  built-in types, execution flow, and custom connector guide
- [`SCRIPT_EXECUTOR_SUBSYSTEM.md`](SCRIPT_EXECUTOR_SUBSYSTEM.md) — detailed guide on adding
  script executors
- [`IOC_CONTAINER_ARCHITECTURE.md`](IOC_CONTAINER_ARCHITECTURE.md) — how the IoC container
  resolves factories by type and qualifier

