# totalschema-connector-shell — AI Agent Guide

## Purpose

Provides the built-in `shell` connector type, which executes local shell scripts as OS processes.
The module is self-contained: it has no runtime dependencies beyond `totalschema-core`,
`totalschema-connector-common`, and SLF4J.

---

## Package structure

```
io.github.totalschema.connector.shell
├── ShellScriptConnector               – Connector implementation (public)
├── ShellScriptConnectorComponentFactory – IoC factory binding "shell" type (public)
└── impl/                              – All package-private implementation detail
│   ├── DefaultShellScriptRunnerFactory  – Factory: OS+extension → runner selection
│   ├── RuntimeInformation               – Interface: isWindows(), isPwshAvailable()
│   ├── DefaultRuntimeInformation        – Real impl: reads os.name and PATH
│   ├── GenericShellScriptRunner         – Base runner: stores prefix, builds command
│   ├── ShScriptRunner                   – Prefix: ["sh"]
│   ├── CmdExeScriptRunner               – Prefix: ["cmd.exe", "/c"]
│   ├── PwshScriptRunner                 – Prefix: ["pwsh", "-ExecutionPolicy", "Bypass", "-File"]
│   └── WindowsPowerShellScriptRunner    – Prefix: ["powershell.exe", "-ExecutionPolicy", "Bypass", "-File"]
└── spi/
    ├── ShellScriptRunner                – TerminalSession<List<String>> (public interface)
    └── ShellScriptRunnerFactory         – SPI: getInstance() / getRunner() (public interface)
```

---

## Architecture

### Execution flow

```
ShellScriptConnector.execute(changeFile, context)
  └─ ShellScriptRunnerFactory.getRunner(name, config, fileName)   ← SPI call
       └─ DefaultShellScriptRunnerFactory.getRunner(...)          ← built-in impl
            ├─ start.command set?  → new GenericShellScriptRunner(name, configuredTokens)
            ├─ .sh extension       → new ShScriptRunner(name)
            ├─ .ps1 extension      → PwshScriptRunner | WindowsPowerShellScriptRunner | throw
            ├─ .bat/.cmd extension → CmdExeScriptRunner | throw
            └─ other extension     → ShScriptRunner (Unix) | CmdExeScriptRunner (Windows)
  └─ runner.execute(List.of(absolutePath))    ← runs OS process via ExternalProcessTerminalSession
  └─ runner.close()                           ← try-with-resources; always called
```

### Runner class hierarchy

```
ExternalProcessTerminalSession  (totalschema-connector-common)
  └─ GenericShellScriptRunner   (stores prefix, implements buildActualCommand)
       ├─ ShScriptRunner              hard-codes ["sh"]
       ├─ CmdExeScriptRunner          hard-codes ["cmd.exe", "/c"]
       ├─ PwshScriptRunner            hard-codes ["pwsh", "-ExecutionPolicy", "Bypass", "-File"]
       └─ WindowsPowerShellScriptRunner  hard-codes ["powershell.exe", ...]
```

`GenericShellScriptRunner` is **not abstract** and is also instantiated directly by
`DefaultShellScriptRunnerFactory` for the `start.command` case.

### OS detection (`RuntimeInformation`)

`RuntimeInformation` is a package-private interface with two methods:

| Method | Default implementation |
|---|---|
| `isWindows()` | `System.getProperty("os.name").toLowerCase().startsWith("windows")` |
| `isPwshAvailable()` | Walks `PATH` entries, checks `pwsh` / `pwsh.exe` is executable |

The interface exists solely to make `DefaultShellScriptRunnerFactory` testable without `PowerMock`
or system-property manipulation. The factory receives a `RuntimeInformation` via a
**package-visible constructor**; production code always calls the public no-arg constructor.

---

## Interpreter selection (exact priority order)

1. `start.command` configured → `GenericShellScriptRunner` (bypasses everything else)
2. `.sh` extension (any OS) → `ShScriptRunner`
3. `.ps1` extension:
   - `pwsh` on PATH → `PwshScriptRunner`
   - Windows, no `pwsh` → `WindowsPowerShellScriptRunner`
   - non-Windows, no `pwsh` → **`IllegalStateException`**
4. `.bat` or `.cmd` extension:
   - Windows → `CmdExeScriptRunner`
   - non-Windows → **`IllegalStateException`**
5. Other extension:
   - non-Windows → `ShScriptRunner`
   - Windows → `CmdExeScriptRunner`

Extension matching is case-insensitive.

---

## Key invariants

- A new runner is created **per script execution** — `ShellScriptConnector` holds no long-lived
  runner. The runner is always closed via try-with-resources even if `execute()` throws.
- `ShellScriptConnector` ignores the `CommandContext`; it only needs the change file path.
- `start.command` is parsed by `Configuration.getList()` which splits on commas and trims
  surrounding whitespace from each token.
- `.bat` / `.cmd` / `.ps1` without an appropriate OS/PATH combination are **hard errors**, not
  silent fallbacks. Use `start.command` to override (e.g. `wine,cmd.exe,/c` on Linux).

---

## Testing patterns

### `ShellScriptConnectorTest`

Injects a **mock `ShellScriptRunnerFactory`** via the three-argument constructor.
No OS process is ever spawned.

```java
ShellScriptRunnerFactory mockFactory = createMock(ShellScriptRunnerFactory.class);
ShellScriptRunner mockRunner  = createMock(ShellScriptRunner.class);
ShellScriptConnector connector = new ShellScriptConnector("myshell", config, mockFactory);

expect(mockFactory.getRunner("myshell", config, "0001.setup.apply.myshell.sh"))
        .andReturn(mockRunner);
mockRunner.execute(List.of("/abs/path/0001.setup.apply.myshell.sh"));
mockRunner.close();
replay(mockFactory, mockRunner);

connector.execute(applyFile, context);
verify(mockFactory, mockRunner);
```

### `DefaultShellScriptRunnerFactoryTest`

Injects a **mock `RuntimeInformation`** via the package-visible constructor.
Helper factories (one line each) cover the four OS×pwsh combinations:

```java
private static DefaultShellScriptRunnerFactory factory(boolean windows, boolean pwshAvailable) {
    RuntimeInformation ri = createMock(RuntimeInformation.class);
    expect(ri.isWindows()).andReturn(windows).anyTimes();
    expect(ri.isPwshAvailable()).andReturn(pwshAvailable).anyTimes();
    replay(ri);
    return new DefaultShellScriptRunnerFactory(ri);
}
```

The selected prefix is read back via the package-visible `GenericShellScriptRunner.getCommandPrefix()`
accessor — no reflection required:

```java
List<String> prefix = ((GenericShellScriptRunner) runner).getCommandPrefix();
assertEquals(prefix, List.of("sh"));
```

Exception paths are covered with `@Test(expectedExceptions = IllegalStateException.class,
expectedExceptionsMessageRegExp = "...")`.

---

## Extension point — custom `ShellScriptRunnerFactory`

To replace the entire interpreter-selection logic:

1. Implement `io.github.totalschema.connector.shell.spi.ShellScriptRunnerFactory`.
2. Register via `META-INF/services/io.github.totalschema.connector.shell.spi.ShellScriptRunnerFactory`.
3. Place the JAR in `user_libs/`.

`ShellScriptRunnerFactory.getInstance()` returns the first registered implementation via
`ServiceLoader`, or falls back to `DefaultShellScriptRunnerFactory` if none is found.

---

## Common pitfalls

| Pitfall | Fix |
|---|---|
| `.bat` / `.cmd` script on Linux throws at runtime | Set `start.command: wine,cmd.exe,/c` or run on Windows |
| `.ps1` on Linux without `pwsh` installed throws | Install PowerShell Core, or set `start.command` |
| Script is not found | Verify the `<connector>` segment in the file name matches the connector name in `totalschema.yml` |
| Script needs env vars | Use `start.command: env,MY_VAR=value,sh` or set vars in the outer shell before running TotalSchema |
| Custom factory not picked up | Check the `META-INF/services` file name matches the fully-qualified interface name exactly |

