# TotalSchema CLI Usage Guide

This guide covers the command-line interface (CLI) distribution of TotalSchema, which is the recommended approach for operations teams, standalone deployments, and CI/CD pipeline integration.

## Table of Contents

- [Installation](#installation)
- [Adding JDBC Drivers](#adding-jdbc-drivers)
- [Basic Usage](#basic-usage)
- [CLI Command Reference](#cli-command-reference)
- [Working with Encrypted Secrets](#working-with-encrypted-secrets)
- [CI/CD Integration](#cicd-integration)
- [Examples](#examples)

---

## Installation

Download and extract the pre-built CLI distribution for a complete, ready-to-use TotalSchema installation.

### Download the Distribution

```bash
# Download the latest release
wget https://github.com/totalschema/totalschema/releases/download/v1.0/totalschema-1.0-SNAPSHOT.tar.gz

# Extract
tar -xzf totalschema-1.0-SNAPSHOT.tar.gz
cd totalschema-1.0-SNAPSHOT
```

Or on Windows, download the `.zip` file and extract it.

### Directory Structure

```
totalschema-1.0-SNAPSHOT/
├── bin/
│   ├── totalschema.sh      # Linux/macOS launcher
│   └── totalschema.bat     # Windows launcher
├── lib/
│   ├── totalschema-cli-1.0-SNAPSHOT.jar
│   ├── totalschema-core-1.0-SNAPSHOT.jar
│   └── [other dependencies...]
└── lib/jdbc/               # Place JDBC drivers here
    └── README.md
```

---

## Adding JDBC Drivers

TotalSchema requires JDBC drivers for your databases. Add them to the `lib/jdbc/` directory:

```bash
# Example: PostgreSQL
cp postgresql-42.7.3.jar totalschema-1.0-SNAPSHOT/lib/jdbc/

# Example: Oracle
cp ojdbc11.jar totalschema-1.0-SNAPSHOT/lib/jdbc/

# Example: MySQL
cp mysql-connector-j-8.3.0.jar totalschema-1.0-SNAPSHOT/lib/jdbc/
```

Multiple JDBC drivers can coexist in the same directory.

### Alternative: Using Environment Variable

Alternatively, use the `TOTALSCHEMA_JDBC_DRIVERS` environment variable:

```bash
# Linux/macOS
export TOTALSCHEMA_JDBC_DRIVERS="/path/to/postgresql.jar:/path/to/oracle.jar"

# Windows
set TOTALSCHEMA_JDBC_DRIVERS=C:\drivers\postgresql.jar;C:\drivers\oracle.jar
```

---

## Basic Usage

### Linux/macOS

```bash
cd totalschema-1.0-SNAPSHOT

# Apply changes to DEV environment
bin/totalschema.sh apply -e DEV

# Preview changes before applying
bin/totalschema.sh apply -e DEV --dry-run

# Show pending changes
bin/totalschema.sh show pending-apply -e DEV

# Display applied state
bin/totalschema.sh state display -e DEV

# Validate changes
bin/totalschema.sh validate -e DEV

# List environments
bin/totalschema.sh environments list

# List variables for an environment
bin/totalschema.sh variables list -e PROD
```

### Windows

```bat
cd totalschema-1.0-SNAPSHOT

REM Apply changes
bin\totalschema.bat apply -e DEV

REM Preview changes before applying
bin\totalschema.bat apply -e DEV --dry-run

REM Show pending changes
bin\totalschema.bat show pending-apply -e DEV

REM Display state
bin\totalschema.bat state display -e DEV

REM Validate changes
bin\totalschema.bat validate -e DEV
```

### Using Java Directly

```bash
# Alternative: Run using Java directly
java -jar totalschema-cli-1.0-SNAPSHOT-shaded.jar apply -e DEV
```

---

## CLI Command Reference

### General Syntax

```bash
bin/totalschema.sh <command> [options]
```

### Apply and Revert Commands

Execute or preview database changes:

```bash
# Apply all pending changes
bin/totalschema.sh apply -e <environment>

# Preview what would be applied (dry-run)
bin/totalschema.sh apply -e <environment> --dry-run

# Apply with filter (only matching paths)
bin/totalschema.sh apply -e DEV -f "1.X/1.0.0/*"

# Preview apply with filter
bin/totalschema.sh apply -e DEV -f "1.X/1.0.0/*" --dry-run

# Revert applied changes
bin/totalschema.sh revert -e <environment>

# Preview what would be reverted (dry-run)
bin/totalschema.sh revert -e <environment> --dry-run

# Revert with filter
bin/totalschema.sh revert -e PROD -f "1.X/1.0.0/*"

# Preview revert with filter
bin/totalschema.sh revert -e DEV -f "2.X/*" --dry-run
```

### Show Commands

Display pending or applicable changes without executing:

```bash
# Show pending apply changes
bin/totalschema.sh show pending-apply -e DEV

# Show pending apply changes with filter
bin/totalschema.sh show pending-apply -e DEV -f "2.X/*"

# Show applicable revert operations
bin/totalschema.sh show applicable-revert -e DEV

# Show applicable reverts with filter
bin/totalschema.sh show applicable-revert -e DEV -f "1.X/*"
```

### State Commands

View the current deployment state:

```bash
# Display current state (all applied changes)
bin/totalschema.sh state display -e <environment>

# Display state with filter
bin/totalschema.sh state display -e DEV -f "1.X/*"
```

### Validation Commands

Validate that applied changes haven't been modified:

```bash
# Validate applied changes against current files
bin/totalschema.sh validate -e <environment>

# Validate with filter
bin/totalschema.sh validate -e DEV -f "1.X/*"
```

### Environment Commands

List available environments:

```bash
# List all configured environments
bin/totalschema.sh environments list
```

### Variable Commands

View variable configuration:

```bash
# List all variables for an environment
bin/totalschema.sh variables list -e <environment>

# Examples
bin/totalschema.sh variables list -e DEV
bin/totalschema.sh variables list -e PROD
```

### Secret Management Commands

Encrypt and manage secrets:

```bash
# Encrypt a string value
bin/totalschema.sh secrets encrypt-string --clearTextValue <value> --password <password>
bin/totalschema.sh secrets encrypt-string --clearTextValue <value> --passwordFile <path-to-password-file>

# Encrypt a file
bin/totalschema.sh secrets encrypt-file --clearTextFile <input-file> --encryptedFile <output-file> --password <password>
bin/totalschema.sh secrets encrypt-file --clearTextFile <input-file> --encryptedFile <output-file> --passwordFile <path-to-password-file>
```

---

## Working with Encrypted Secrets

### Encrypting Secrets

```bash
# Encrypt a password or API key
bin/totalschema.sh secrets encrypt-string \
  --clearTextValue "MySecretPassword123" \
  --password "EncryptionKey456"

# Output: !SECRET;1.0;0F0E0000000898660FA93EB1FAA500000010A230E4F7CCC4BCB1808C5C35F643861B0000001001D4A16E28AD33D31C8671753E926495

# Encrypt using password file
bin/totalschema.sh secrets encrypt-string \
  --clearTextValue "MySecretPassword123" \
  --passwordFile /secure/encryption.key

# Encrypt a file (e.g., SSH private key)
bin/totalschema.sh secrets encrypt-file \
  --clearTextFile secrets.txt \
  --encryptedFile secrets.txt.secret \
  --password "EncryptionKey456"
```

### Using Encrypted Secrets at Runtime

When executing changes with encrypted secrets, provide the decryption password:

```bash
# Using password file (recommended)
bin/totalschema.sh apply -e PROD --passwordFile /secure/encryption.key

# Using inline password
bin/totalschema.sh apply -e PROD --password "EncryptionKey456"
```
# Works with any command that needs secret access
bin/totalschema.sh validate -e PROD --passwordFile /secure/encryption.key
bin/totalschema.sh state display -e PROD --password "EncryptionKey456"
```
---

## Examples

### Example 1: Standard Deployment Workflow

```bash
# 1. Check what changes are pending
bin/totalschema.sh show pending-apply -e DEV

# 2. Review current state
bin/totalschema.sh state display -e DEV

# 3. Preview what would be applied (dry-run)
bin/totalschema.sh apply -e DEV --dry-run

# 4. Apply pending changes
bin/totalschema.sh apply -e DEV

# 5. Validate everything is correct
bin/totalschema.sh validate -e DEV
```

### Example 2: Filtered Deployment

Deploy only changes from a specific version:

```bash
# Show pending changes for version 1.0.0 only
bin/totalschema.sh show pending-apply -e QA -f "1.X/1.0.0/*"

# Preview before applying
bin/totalschema.sh apply -e QA -f "1.X/1.0.0/*" --dry-run

# Apply only those changes
bin/totalschema.sh apply -e QA -f "1.X/1.0.0/*"
```

### Example 3: Rollback Scenario

```bash
# Show what would be reverted
bin/totalschema.sh show applicable-revert -e DEV

# Preview the rollback (dry-run)
bin/totalschema.sh revert -e DEV --dry-run

# Execute the rollback
bin/totalschema.sh revert -e DEV

# Verify state after rollback
bin/totalschema.sh state display -e DEV
```

### Example 4: Multi-Environment Deployment

```bash
# Deploy to DEV
bin/totalschema.sh apply -e DEV
bin/totalschema.sh validate -e DEV

# Deploy to QA
bin/totalschema.sh apply -e QA
bin/totalschema.sh validate -e QA

# Deploy to PROD (with secrets)
bin/totalschema.sh apply -e PROD --passwordFile /secure/prod.key
bin/totalschema.sh validate -e PROD --passwordFile /secure/prod.key
```

### Example 5: Inspecting Configuration

```bash
# List all available environments
bin/totalschema.sh environments list

# Check variables for production
bin/totalschema.sh variables list -e PROD

# Show all change files (without applying)
bin/totalschema.sh execute show all -e PROD
```

### Example 6: Using with Custom JDBC Drivers

```bash
# Set JDBC driver path
export TOTALSCHEMA_JDBC_DRIVERS="/opt/drivers/postgresql-42.7.3.jar:/opt/drivers/ojdbc11.jar"

# Run deployment
bin/totalschema.sh apply -e DEV
```

---

## Advantages of CLI Distribution

✅ **No build tool required** - Works standalone without Maven or Gradle  
✅ **Simple installation** - Just download, extract, and run  
✅ **Operations team friendly** - Designed for non-developers  
✅ **Easy CI/CD integration** - Works with any pipeline system  
✅ **Full control over JDBC drivers** - Manage driver versions independently  
✅ **Portable** - Move between environments easily  
✅ **No code compilation** - Ready to use immediately  

---

## Use Cases

The CLI distribution is ideal for:

- **Production deployments** by operations teams
- **CI/CD pipeline integration** (Jenkins, GitLab CI, GitHub Actions)
- **Manual database migrations** by DBAs
- **Scheduled automated deployments** via cron or schedulers
- **Docker container deployments**
- **Environments without Java build tools**
- **Quick testing and prototyping**

---

## Troubleshooting

### Command Not Found

**Problem**: `bash: bin/totalschema.sh: No such file or directory`

**Solution**: Ensure you're in the correct directory and the file has execute permissions:

```bash
cd totalschema-1.0-SNAPSHOT
chmod +x bin/totalschema.sh
```

### JDBC Driver Not Found

**Problem**: `No suitable driver found for jdbc:...`

**Solution**: 
1. Verify JDBC driver is in `lib/jdbc/` directory
2. Or set `TOTALSCHEMA_JDBC_DRIVERS` environment variable
3. Check the driver JAR is not corrupted

### Permission Denied

**Problem**: `Permission denied` when running scripts

**Solution**:

```bash
# Linux/macOS: Make scripts executable
chmod +x bin/totalschema.sh

# Windows: Run as administrator or check file permissions
```

### Java Not Found

**Problem**: `java: command not found`

**Solution**: Install Java 11 or later:

```bash
# Check Java version
java -version

# Install Java (Ubuntu/Debian)
sudo apt-get install openjdk-11-jre

# Install Java (macOS with Homebrew)
brew install openjdk@11
```

---

## Related Documentation

- [Main README](../../README.md) - Complete TotalSchema documentation
- [Configuration Guide](../../README.md#getting-started) - Setting up totalschema.yml
- [Change Script Naming](../../README.md#change-script-file-naming-convention) - File naming rules
- [Secret Management](../../README.md#secret-management) - Encryption details

---

## Support

- **Issues**: https://github.com/totalschema/totalschema/issues
- **Documentation**: https://github.com/totalschema/totalschema
- **License**: GNU Affero General Public License v3.0

