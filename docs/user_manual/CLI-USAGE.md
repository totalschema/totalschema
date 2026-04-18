# TotalSchema CLI Reference

Quick reference for the TotalSchema command-line interface. For concepts, configuration, and
connector documentation see **[README.md](README.md)**.

## Table of Contents

- [Setup](#setup)
- [General Syntax](#general-syntax)
- [Commands](#commands)
- [Common Options](#common-options)
- [Examples](#examples)
- [Troubleshooting](#troubleshooting)

---

## Setup

1. Extract the distribution archive.
2. Copy your JDBC driver(s) into `user_libs/`:
   ```bash
   cp postgresql-42.7.3.jar totalschema-<version>/user_libs/
   ```
3. Run any command from the distribution root:
   ```bash
   bin/totalschema.sh apply -e DEV
   ```

Java 11 or later is required. Set `TOTALSCHEMA_USER_LIBS` to override the default `user_libs/`
location:
```bash
export TOTALSCHEMA_USER_LIBS="/opt/drivers/postgresql.jar:/opt/groovy/groovy-all.jar"
```

---

## General Syntax

```
bin/totalschema.sh <command> [subcommand] [options]
bin\totalschema.bat <command> [subcommand] [options]
```

---

## Commands

### `apply` — deploy pending changes

```bash
bin/totalschema.sh apply -e <env>                      # apply all pending
bin/totalschema.sh apply -e <env> --dry-run            # preview without executing
bin/totalschema.sh apply -e <env> -f "<glob>"          # apply matching paths only
bin/totalschema.sh apply -e <env> -f "<glob>" --dry-run
```

### `revert` — roll back applied changes

```bash
bin/totalschema.sh revert -e <env>                     # revert all applicable
bin/totalschema.sh revert -e <env> --dry-run
bin/totalschema.sh revert -e <env> -f "<glob>"
bin/totalschema.sh revert -e <env> -f "<glob>" --dry-run
```

### `show` — inspect pending work without executing

```bash
bin/totalschema.sh show pending-apply -e <env>         # changes not yet applied
bin/totalschema.sh show pending-apply -e <env> -f "<glob>"
bin/totalschema.sh show applicable-revert -e <env>     # changes that can be reverted
bin/totalschema.sh show applicable-revert -e <env> -f "<glob>"
```

### `state` — inspect recorded state

```bash
bin/totalschema.sh state display -e <env>              # list all applied changes
bin/totalschema.sh state display -e <env> -f "<glob>"
```

### `validate` — detect modified applied scripts

```bash
bin/totalschema.sh validate -e <env>                   # validate all applied scripts
bin/totalschema.sh validate -e <env> -f "<glob>"
```

### `environments` — list configured environments

```bash
bin/totalschema.sh environments list
```

### `variables` — inspect resolved variables

```bash
bin/totalschema.sh variables list -e <env>
```

### `secrets` — encrypt sensitive values

```bash
# Encrypt a string value
bin/totalschema.sh secrets encrypt-string \
  --clearTextValue <value> \
  --password <key>
  # or --passwordFile <path>

# Encrypt a file (e.g. SSH private key)
bin/totalschema.sh secrets encrypt-file \
  --clearTextFile <input> \
  --encryptedFile <output> \
  --password <key>
  # or --passwordFile <path>
```

---

## Common Options

| Option | Description |
|---|---|
| `-e <env>` | Target environment (required for most commands) |
| `-w <path>` | Workspace directory containing `totalschema.yml` (default: current dir) |
| `-f "<glob>"` | Filter — only process matching change script paths |
| `--dry-run` | Preview what would happen without making any changes |
| `--password <key>` | Decryption password for encrypted secrets |
| `--passwordFile <path>` | Read decryption password from file (recommended over `--password`) |

---

## Examples

### Standard deployment workflow

```bash
bin/totalschema.sh show pending-apply -e DEV     # 1. check what is pending
bin/totalschema.sh apply -e DEV --dry-run         # 2. preview
bin/totalschema.sh apply -e DEV                   # 3. apply
bin/totalschema.sh validate -e DEV                # 4. verify
```

### Deploy a specific version only

```bash
bin/totalschema.sh apply -e QA -f "1.X/1.2.0/*" --dry-run
bin/totalschema.sh apply -e QA -f "1.X/1.2.0/*"
```

### Roll back

```bash
bin/totalschema.sh show applicable-revert -e DEV  # see what can be reverted
bin/totalschema.sh revert -e DEV --dry-run
bin/totalschema.sh revert -e DEV
bin/totalschema.sh state display -e DEV            # confirm state
```

### Production deployment with encrypted secrets

```bash
bin/totalschema.sh apply    -e PROD --passwordFile /secure/prod.key
bin/totalschema.sh validate -e PROD --passwordFile /secure/prod.key
```

### Encrypt a new secret

```bash
bin/totalschema.sh secrets encrypt-string \
  --clearTextValue "s3cr3tP@ss" \
  --passwordFile /secure/master.key
# Copy the !SECRET;1.0;... output into totalschema.yml wrapped in ${secret:...}
```

---

## Troubleshooting

| Symptom | Fix |
|---|---|
| `No such file or directory` | Run from inside the distribution root; `chmod +x bin/totalschema.sh` |
| `No suitable driver found` | Add the JDBC driver JAR to `user_libs/` |
| `Permission denied` | `chmod +x bin/totalschema.sh` |
| `java: command not found` | Install Java 11+: `sudo apt-get install openjdk-17-jre` |
| Lock not released after crash | `DELETE FROM totalschema_lock WHERE lock_id = 'totalschema_main_lock';` |
