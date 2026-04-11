# totalschema-connector-shell

Connector module that executes **local shell scripts** as OS processes. It is part of the
`totalschema-connectors` group and ships as a built-in connector type alongside the JDBC and SSH
connectors.

---

## Connector type

```
type: shell
```

---

## Configuration

### Minimal configuration

```yaml
connectors:
  deploy:          # connector name – must match the <connector> segment of your script files
    type: shell
```

### Overriding the interpreter (`start.command`)

By default the connector auto-detects the right interpreter from the script's file extension and
the host OS (see [Interpreter selection](#interpreter-selection) below).  
To override this for all scripts in a connector, set `start.command` as a comma-separated token
list:

```yaml
connectors:
  pwsh_deploy:
    type: shell
    start:
      command: pwsh,-ExecutionPolicy,Bypass,-File   # use pwsh for every file regardless of extension
```

```yaml
connectors:
  wine_batch:
    type: shell
    start:
      command: wine,cmd.exe,/c   # run .bat files on Linux via Wine
```

`start.command` takes the highest priority and bypasses all extension and OS detection.

---

## Script file naming

Script files must follow the standard TotalSchema convention, with the `<connector>` segment
matching the connector name declared in `totalschema.yml`:

```
<order>.<description>[.<env>].<type>.<connector>.<ext>
```

Examples for a connector named `deploy`:

```
0001.create_schema.apply.deploy.sh
0002.seed_data.DEV.apply.deploy.sql
0010.cleanup.revert.deploy.sh
```

---

## Interpreter selection

When `start.command` is **not** set, the factory applies the following rules **in priority order**:

| Extension | Host OS | `pwsh` on PATH | Interpreter |
|---|---|---|---|
| `.sh` | any | any | `sh <script>` |
| `.ps1` | any | ✓ | `pwsh -ExecutionPolicy Bypass -File <script>` |
| `.ps1` | Windows | ✗ | `powershell.exe -ExecutionPolicy Bypass -File <script>` |
| `.ps1` | non-Windows | ✗ | ❌ `IllegalStateException` |
| `.bat` / `.cmd` | Windows | any | `cmd.exe /c <script>` |
| `.bat` / `.cmd` | non-Windows | any | ❌ `IllegalStateException` |
| other | non-Windows | any | `sh <script>` |
| other | Windows | any | `cmd.exe /c <script>` |

Extension matching is **case-insensitive** (`.SH`, `.PS1`, `.BAT`, `.CMD` are all recognised).

`sh` is invoked as `sh <path>` (no `-c`), so scripts do **not** require the execute bit to be set.

---

## Replacing the factory (SPI)

The entire interpreter-selection logic is encapsulated in `ShellScriptRunnerFactory`. You can
replace it by providing a custom implementation and registering it via Java `ServiceLoader`:

1. Implement `io.github.totalschema.connector.shell.spi.ShellScriptRunnerFactory`.
2. Add a file `META-INF/services/io.github.totalschema.connector.shell.spi.ShellScriptRunnerFactory`
   containing the fully-qualified class name of your implementation.
3. Place the JAR in `user_libs/`.

If no custom factory is registered, `DefaultShellScriptRunnerFactory` is used as the built-in
fallback.

