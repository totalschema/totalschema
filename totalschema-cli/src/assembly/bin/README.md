# TotalSchema Run Scripts

This directory contains platform-specific run scripts for TotalSchema CLI.

## Linux/macOS

```bash
./bin/totalschema.sh [COMMAND] [OPTIONS]
```

## Windows

``.bat
bin\totalschema.bat [COMMAND] [OPTIONS]
```

## Available Commands

Run the script without arguments to see all available commands:

```bash
./bin/totalschema.sh
```

Example commands:
- `apply` - Apply pending changes to database
- `revert` - Revert applied changes from database
- `show` - Show pending or applicable changes
- `environments` - Environments related subcommands
- `variables` - Variables related subcommands
- `state` - State management commands
- `secrets` - Secret management subcommands
- `validate` - Validate workspace files against state

