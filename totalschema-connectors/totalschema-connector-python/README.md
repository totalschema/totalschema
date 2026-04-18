# totalschema-connector-python

Connector module that executes **local Python scripts** as OS processes. Each change file is a
`.py` script invoked by the configured Python executable. Standard output and standard error are
captured and logged line-by-line at `INFO` level; a non-zero exit code fails the deployment.

---

## Connector type

```
type: python
```

---

## Quick start

```yaml
connectors:
  myetl:
    type: python
```

```python
# changes/1.X/1.0.0/0001.seed_reference_data.apply.myetl.py
import datetime
print(f"Running ETL pipeline at {datetime.datetime.now().isoformat()}")
```

---

## Configuration reference

### All options

```yaml
connectors:
  myetl:
    type: python

    # ── Interpreter ─────────────────────────────────────────────────────────
    executable: python3              # optional — see "Executable" below

    # ── Working directory ───────────────────────────────────────────────────
    workingDirectory: /path/to/dir  # optional — see "Working directory" below

    # ── Isolation ───────────────────────────────────────────────────────────
    copyToTempDir: true             # optional, default: false

    # ── Local module imports ─────────────────────────────────────────────────
    modulesDirectory: scripts/lib   # optional — see "Local Python modules" below

    # ── One-time initialisation ──────────────────────────────────────────────
    initFiles:                      # optional — files written before initCommands run
      requirements.txt: |
        pandas==2.0.0
        requests==2.31.0
    initCommands:                   # optional — commands run once before the first script
      - pip install -r requirements.txt

    # ── TotalSchema SDK ──────────────────────────────────────────────────────
    sdk:
      enabled: true                 # optional, default: false
      variables:                    # name → value map exposed to Variable.get()
        db_password: ${dbPassword}
        env_name: ${dbName}

    # ── Environment variables ────────────────────────────────────────────────
    environmentVariables:           # optional — merged into the child process environment
      MY_API_KEY: secret123
      EXTRA_PATH: /opt/mylibs
```

---

### `executable`

| Default (*nix) | Default (Windows) |
|---|---|
| `python3` | `python` |

Overrides the Python interpreter for every script in this connector. Useful when multiple Python
versions are installed:

```yaml
executable: python3.12
```

The OS default is detected once at startup via the `os.name` system property.

---

### `workingDirectory`

By default the process working directory is the **directory that contains the change file**,
which means `__file__` and relative paths resolve naturally next to the script.

Set `workingDirectory` to override this for all scripts in the connector:

```yaml
workingDirectory: /opt/myapp/etl
```

The value is used as-is (absolute path) or resolved relative to the JVM's current working
directory (typically the workspace root).

---

### `copyToTempDir`

When `true`, a fresh temporary directory is created for every script execution:

1. The script is **copied** into the temp dir.
2. The temp dir becomes the working directory (unless `workingDirectory` is explicitly set).
3. The temp dir is **always deleted** after the script finishes, even on failure.

This is useful when scripts must not leave artifacts in the source tree, or when a clean working
directory is required for each run.

```yaml
copyToTempDir: true
```

---

### `modulesDirectory`

Points to a directory of user-provided Python modules that scripts can import. The resolved
absolute path is **prepended to `PYTHONPATH`** before the process starts.

```yaml
modulesDirectory: scripts/lib     # relative to CWD (workspace root), or absolute
```

**When to use `modulesDirectory`:** This option is most valuable when a **shared library** needs
to be importable by scripts spread across multiple version directories. Because the connector
resolves the path once and injects it into every script's environment, you never need to replicate
the library or update individual scripts when the library moves.

```
my-project/
├── totalschema.yml
├── shared_lib/              ← modulesDirectory points here
│   └── db_utils.py
└── changes/
    ├── 1.X/
    │   └── 1.0.0/
    │       └── 0001.seed.apply.myetl.py   ← can do: from db_utils import ...
    └── 2.X/
        └── 2.0.0/
            └── 0001.migrate.apply.myetl.py ← same import works here too
```

**Modules inside the changes tree:** Helper modules can also live *inside* the changes directory
structure. Add them to `.totalschemaignore` so TotalSchema's file discovery skips them, then set
`modulesDirectory` to their parent:

```
changes/
├── .totalschemaignore        ← contains: my_lib/
├── my_lib/
│   └── helpers.py
└── 1.X/
    └── 1.0.0/
        └── 0001.run.apply.myetl.py
```

```yaml
# totalschema.yml
connectors:
  myetl:
    type: python
    modulesDirectory: totalschema/changes   # parent of my_lib/
```

```
# changes/.totalschemaignore
my_lib/
```

If `environmentVariables.PYTHONPATH` is also set, `modulesDirectory` is prepended to it:

```
PYTHONPATH = <resolved modulesDirectory> : <existing PYTHONPATH value>
```

---

### `initFiles`

A map of `filename → content` written to the **working directory** once, before
`initCommands` run and before the first script executes. Files are written UTF-8.

```yaml
initFiles:
  requirements.txt: |
    pandas==2.0.0
    requests==2.31.0
  config.ini: |
    [database]
    host = localhost
```

Combine with `initCommands` to install dependencies on first run:

```yaml
initFiles:
  requirements.txt: |
    pandas==2.0.0
initCommands:
  - pip install -r requirements.txt
```

---

### `initCommands`

A list of commands executed **once**, before the first script in a deployment run. Each entry is
a full command string that is split on whitespace into an argv list.

```yaml
initCommands:
  - pip install -r requirements.txt
  - echo "Connector initialised"
```

Commands run in the order they are declared, in the same working directory as the scripts.

---

### `sdk`

When `sdk.enabled: true`, the connector generates a `totalschema` Python package in the working
directory before each script and removes it immediately afterwards. The package exposes a
`Variable.get()` API backed by the `sdk.variables` map.

```yaml
sdk:
  enabled: true
  variables:
    db_password: ${dbPassword}   # TotalSchema variable substitution applies
    batch_size: "1000"
```

```python
from totalschema.sdk import Variable

password = Variable.get("db_password")
batch    = int(Variable.get("batch_size"))
```

`Variable.get(name)` raises `KeyError` if `name` is not in the map.

Values are **base64-encoded** inside the generated source file, so multiline strings, embedded
quotes, backslashes, and Unicode all round-trip correctly without any escaping.

The generated `totalschema/` directory is an implementation detail. It is never present on disk
outside of the actual script execution window.

---

### `environmentVariables`

Extra environment variables merged into the child process environment. The parent process
environment is inherited first; these values are overlaid on top.

```yaml
environmentVariables:
  MY_API_KEY: secret123
  PYTHONPATH: /opt/shared_libs     # also see modulesDirectory
  TMPDIR: /fast_ssd/tmp
```

---

## Script file naming

Script files must follow the standard TotalSchema convention, with the `<connector>` segment
matching the connector name declared in `totalschema.yml`:

```
<order>.<description>[.<env>].<type>.<connector>.<ext>
```

Examples for a connector named `myetl`:

```
0001.create_reference_data.apply.myetl.py
0002.daily_aggregation.DEV.apply_always.myetl.py
0010.cleanup_temp_tables.revert.myetl.py
```

The extension is always `.py`.

---

## Non-convention files in the changes tree

Python helper modules, `requirements.txt`, `README.md`, and any other non-change files inside the
changes directory must be excluded from discovery, or TotalSchema will throw
`File name is illegal`. Use a `.totalschemaignore` file at the changes root (or in the relevant
sub-directory) to tell TotalSchema which files and directories to skip.

This makes it perfectly valid to keep helper modules *inside* the changes directory tree — they
are simply invisible to the change-file scanner:

```
changes/
├── .totalschemaignore        ← excludes my_lib/ from discovery
├── my_lib/
│   ├── __init__.py
│   └── helpers.py
└── 1.X/
    └── 1.0.0/
        └── 0001.run.apply.myetl.py   ← imports from my_lib work via modulesDirectory
```

```
# changes/.totalschemaignore
my_lib/
```

```yaml
connectors:
  myetl:
    type: python
    modulesDirectory: totalschema/changes   # parent of my_lib/
```

Files and directories whose name starts with `.` (dot-files) are always excluded automatically
— no entry needed for `.totalschemaignore` itself, `.git/`, `.DS_Store`, etc.

For the full `.totalschemaignore` pattern syntax, see the
[`.totalschemaignore` section in the main README](../../README.md#ignoring-files-and-directories-totalschemaignore).

---

## Full example

### `totalschema.yml`

```yaml
connectors:
  pipeline:
    type: python
    modulesDirectory: etl/lib       # helper modules live outside the changes tree
    initFiles:
      requirements.txt: |
        pandas==2.0.0
        boto3==1.34.0
    initCommands:
      - pip install -r requirements.txt
    sdk:
      enabled: true
      variables:
        db_host: ${dbHost}
        db_password: ${dbPassword}
    environmentVariables:
      AWS_REGION: eu-west-1
```

### Change script

```python
# changes/2.X/2.0.0/0001.load_products.apply.pipeline.py
import pandas as pd
from totalschema.sdk import Variable
from db_utils import get_connection   # imported from etl/lib/db_utils.py

conn = get_connection(
    host=Variable.get("db_host"),
    password=Variable.get("db_password"),
)

df = pd.read_csv("products.csv")
df.to_sql("products", conn, if_exists="replace", index=False)
print(f"Loaded {len(df)} rows into products table")
```

### `changes/.totalschemaignore`

```
# Exclude the helper library directory from change-file discovery
```

> The `etl/lib/` directory lives outside `changes/`, so no ignore entry is needed for it.

---

## Key classes (for developers)

| Class | Responsibility |
|---|---|
| `PythonConnector` | Orchestrates the full execution flow; reads all configuration options |
| `DefaultPythonProcessRunner` | Launches OS processes via `ProcessBuilder`; merges environment variables |
| `TotalSchemaSdkGenerator` | Generates and cleans up the `totalschema/` Python package |
| `OperatingSystem` | Constants `IS_WINDOWS` / `IS_MAC` / `IS_LINUX` for OS detection |
| `PythonConnectorComponentFactory` | IoC factory; qualifier = `"python"` |

