# TotalSchema Maven Plugin Usage Guide

This guide covers using TotalSchema as a Maven plugin, which is the recommended approach for Java/Maven projects that need to integrate database change management into their build lifecycle.

## Table of Contents

- [Overview](#overview)
- [Prerequisites](#prerequisites)
- [Basic Configuration](#basic-configuration)
- [Maven Goals](#maven-goals)
- [Usage Examples](#usage-examples)
- [Lifecycle Integration](#lifecycle-integration)
- [Advanced Configuration](#advanced-configuration)
- [Working with Encrypted Secrets](#working-with-encrypted-secrets)
- [Multi-Module Projects](#multi-module-projects)
- [Maven Profiles](#maven-profiles)
- [CI/CD Integration](#cicd-integration)
- [Examples](#examples)
- [Troubleshooting](#troubleshooting)

---

## Overview

The TotalSchema Maven plugin allows you to:
- Integrate database change management into your Maven build lifecycle
- Manage JDBC drivers as Maven dependencies
- Execute changes automatically during build phases
- Use Maven properties and profiles for configuration
- Leverage Maven's dependency management

---

## Prerequisites

- Java 11 or later
- Maven 3.6 or later
- TotalSchema configuration file (`totalschema.yml`)
- Change scripts in the `changes/` directory

---

## Basic Configuration

Add the TotalSchema Maven plugin to your `pom.xml`:

```xml
<project>
    <build>
        <plugins>
            <plugin>
                <groupId>io.github.totalschema</groupId>
                <artifactId>totalschema-maven-plugin</artifactId>
                <version>1.0-SNAPSHOT</version>
                
                <!-- Add JDBC drivers as plugin dependencies -->
                <dependencies>
                    <!-- TotalSchema Core (Required) -->
                    <dependency>
                        <groupId>io.github.totalschema</groupId>
                        <artifactId>totalschema-core</artifactId>
                        <version>1.0-SNAPSHOT</version>
                    </dependency>
                    
                    <!-- Your JDBC Drivers -->
                    <dependency>
                        <groupId>org.postgresql</groupId>
                        <artifactId>postgresql</artifactId>
                        <version>42.7.3</version>
                    </dependency>
                    <dependency>
                        <groupId>com.oracle.database.jdbc</groupId>
                        <artifactId>ojdbc11</artifactId>
                        <version>23.3.0.23.09</version>
                    </dependency>
                    <!-- Add other drivers as needed -->
                </dependencies>
                
                <configuration>
                    <arguments>
                        <argument>apply</argument>
                        <argument>-e</argument>
                        <argument>${environment}</argument>
                    </arguments>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

---

## Maven Goals

The TotalSchema Maven plugin provides the following goals:

### `totalschema:run`

Execute TotalSchema with the configured arguments.

```bash
mvn totalschema:run
```

### `totalschema:invoke`

Alternative goal name (alias for `run`).

```bash
mvn totalschema:invoke
```

---

## Usage Examples

### Apply Changes

```bash
# Apply changes to DEV environment
mvn totalschema:run -Denvironment=DEV

# Or with full argument override
mvn totalschema:run -Darguments="execute,apply,-e,DEV"
```

### Show Pending Changes

```bash
mvn totalschema:run -Darguments="execute,show,pending,-e,DEV"
```

### Validate Changes

```bash
mvn totalschema:run -Darguments="validate,-e,DEV"
```

### Display State

```bash
mvn totalschema:run -Darguments="state,display,-e,PROD"
```

### List Environments

```bash
mvn totalschema:run -Darguments="environments,list"
```

### List Variables

```bash
mvn totalschema:run -Darguments="variables,list,-e,DEV"
```

### Execute Reverts

```bash
mvn totalschema:run -Darguments="execute,revert,-e,DEV"
```

### With Encrypted Secrets

```bash
mvn totalschema:run -Denvironment=PROD -Darguments="execute,apply,-e,PROD,--passwordFile,/secure/key"
```

---

## Lifecycle Integration

Bind TotalSchema execution to Maven build phases for automatic execution:

### Apply During Install Phase

```xml
<plugin>
    <groupId>io.github.totalschema</groupId>
    <artifactId>totalschema-maven-plugin</artifactId>
    <version>1.0-SNAPSHOT</version>
    
    <dependencies>
        <!-- Dependencies as shown in Basic Configuration -->
    </dependencies>
    
    <executions>
        <!-- Apply changes during install phase -->
        <execution>
            <id>apply-changes</id>
            <phase>install</phase>
            <goals>
                <goal>run</goal>
            </goals>
            <configuration>
                <arguments>
                    <argument>apply</argument>
                    <argument>-e</argument>
                    <argument>${environment}</argument>
                </arguments>
            </configuration>
        </execution>
    </executions>
</plugin>
```

Then run:

```bash
mvn clean install -Denvironment=DEV
```

### Validate During Verify Phase

```xml
<execution>
    <id>validate-changes</id>
    <phase>verify</phase>
    <goals>
        <goal>run</goal>
    </goals>
    <configuration>
        <arguments>
            <argument>validate</argument>
            <argument>-e</argument>
            <argument>${environment}</argument>
        </arguments>
    </configuration>
</execution>
```

### Multiple Executions

Execute multiple TotalSchema operations in different phases:

```xml
<plugin>
    <groupId>io.github.totalschema</groupId>
    <artifactId>totalschema-maven-plugin</artifactId>
    <version>1.0-SNAPSHOT</version>
    
    <dependencies>
        <!-- Dependencies as shown in Basic Configuration -->
    </dependencies>
    
    <executions>
        <!-- Show pending changes during validate phase -->
        <execution>
            <id>show-pending</id>
            <phase>validate</phase>
            <goals>
                <goal>run</goal>
            </goals>
            <configuration>
                <arguments>
                    <argument>show</argument>
                    <argument>pending-apply</argument>
                    <argument>-e</argument>
                    <argument>${environment}</argument>
                </arguments>
            </configuration>
        </execution>
        
        <!-- Apply changes during install phase -->
        <execution>
            <id>apply-changes</id>
            <phase>install</phase>
            <goals>
                <goal>run</goal>
            </goals>
            <configuration>
                <arguments>
                    <argument>apply</argument>
                    <argument>-e</argument>
                    <argument>${environment}</argument>
                </arguments>
            </configuration>
        </execution>
        
        <!-- Validate changes during verify phase -->
        <execution>
            <id>validate-changes</id>
            <phase>verify</phase>
            <goals>
                <goal>run</goal>
            </goals>
            <configuration>
                <arguments>
                    <argument>validate</argument>
                    <argument>-e</argument>
                    <argument>${environment}</argument>
                </arguments>
            </configuration>
        </execution>
    </executions>
</plugin>
```

Then run:

```bash
# Runs all three executions
mvn clean install -Denvironment=DEV
```

---

## Advanced Configuration

### Using Maven Properties

Define environment in properties:

```xml
<properties>
    <totalschema.environment>DEV</totalschema.environment>
</properties>

<plugin>
    <groupId>io.github.totalschema</groupId>
    <artifactId>totalschema-maven-plugin</artifactId>
    <version>1.0-SNAPSHOT</version>
    <configuration>
        <arguments>
            <argument>apply</argument>
            <argument>-e</argument>
            <argument>${totalschema.environment}</argument>
        </arguments>
    </configuration>
</plugin>
```

### Custom Configuration File Location

```xml
<configuration>
    <configFile>${project.basedir}/config/totalschema.yml</configFile>
    <arguments>
        <argument>apply</argument>
        <argument>apply</argument>
        <argument>-e</argument>
        <argument>${environment}</argument>
    </arguments>
</configuration>
```

### Skip Execution

```xml
<properties>
    <totalschema.skip>false</totalschema.skip>
</properties>

<plugin>
    <configuration>
        <skip>${totalschema.skip}</skip>
        <!-- ... other configuration ... -->
    </configuration>
</plugin>
```

Then skip with:

```bash
mvn install -Dtotalschema.skip=true
```

---

## Working with Encrypted Secrets

### Using Password File

```xml
<configuration>
    <arguments>
        <argument>execute</argument>
        <argument>apply</argument>
        <argument>-e</argument>
        <argument>${environment}</argument>
        <argument>--passwordFile</argument>
        <argument>${totalschema.password.file}</argument>
    </arguments>
</configuration>
```

Run with:

```bash
mvn totalschema:run -Denvironment=PROD -Dtotalschema.password.file=/secure/encryption.key
```

### Using Inline Password (Less Secure)

```bash
mvn totalschema:run -Darguments="execute,apply,-e,PROD,--password,MySecretPassword"
```

### Encrypting Secrets with Maven

```bash
# Encrypt a string
mvn totalschema:run -Darguments="secret,encrypt-string,--clearTextValue,MyPassword,--passwordFile,/secure/key"

# Encrypt a file
mvn totalschema:run -Darguments="secret,encrypt-file,--clearTextFile,secrets.txt,--encryptedFile,secrets.txt.secret,--passwordFile,/secure/key"
```

---

## Multi-Module Projects

### Parent POM Configuration

Define the plugin in the parent POM with `<pluginManagement>`:

```xml
<!-- Parent POM -->
<project>
    <build>
        <pluginManagement>
            <plugins>
                <plugin>
                    <groupId>io.github.totalschema</groupId>
                    <artifactId>totalschema-maven-plugin</artifactId>
                    <version>1.0-SNAPSHOT</version>
                    <dependencies>
                        <dependency>
                            <groupId>io.github.totalschema</groupId>
                            <artifactId>totalschema-core</artifactId>
                            <version>1.0-SNAPSHOT</version>
                        </dependency>
                        <dependency>
                            <groupId>org.postgresql</groupId>
                            <artifactId>postgresql</artifactId>
                            <version>42.7.3</version>
                        </dependency>
                    </dependencies>
                </plugin>
            </plugins>
        </pluginManagement>
    </build>
</project>
```

### Child Module Configuration

Use the plugin in specific modules:

```xml
<!-- Child Module POM -->
<project>
    <parent>
        <groupId>com.example</groupId>
        <artifactId>my-parent</artifactId>
        <version>1.0.0</version>
    </parent>
    
    <artifactId>database-module</artifactId>
    
    <build>
        <plugins>
            <plugin>
                <groupId>io.github.totalschema</groupId>
                <artifactId>totalschema-maven-plugin</artifactId>
                <executions>
                    <execution>
                        <id>apply-changes</id>
                        <phase>install</phase>
                        <goals>
                            <goal>run</goal>
                        </goals>
                        <configuration>
                            <arguments>
                                <argument>apply</argument>
                                <argument>-e</argument>
                                <argument>${environment}</argument>
                            </arguments>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

---

## Maven Profiles

Use Maven profiles for environment-specific configuration:

```xml
<profiles>
    <!-- Development Profile -->
    <profile>
        <id>dev</id>
        <activation>
            <activeByDefault>true</activeByDefault>
        </activation>
        <properties>
            <environment>DEV</environment>
        </properties>
    </profile>
    
    <!-- QA Profile -->
    <profile>
        <id>qa</id>
        <properties>
            <environment>QA</environment>
        </properties>
    </profile>
    
    <!-- Production Profile -->
    <profile>
        <id>prod</id>
        <properties>
            <environment>PROD</environment>
            <totalschema.password.file>/secure/prod-encryption.key</totalschema.password.file>
        </properties>
    </profile>
</profiles>

<build>
    <plugins>
        <plugin>
            <groupId>io.github.totalschema</groupId>
            <artifactId>totalschema-maven-plugin</artifactId>
            <version>1.0-SNAPSHOT</version>
            <executions>
                <execution>
                    <id>apply-changes</id>
                    <phase>install</phase>
                    <goals>
                        <goal>run</goal>
                    </goals>
                    <configuration>
                        <arguments>
                            <argument>apply</argument>
                            <argument>-e</argument>
                            <argument>${environment}</argument>
                            <argument>--passwordFile</argument>
                            <argument>${totalschema.password.file}</argument>
                        </arguments>
                    </configuration>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

Usage:

```bash
# Deploy to DEV (default profile)
mvn clean install

# Deploy to QA
mvn clean install -Pqa

# Deploy to PROD
mvn clean install -Pprod
```

---

## CI/CD Integration

### GitHub Actions with Maven

```yaml
name: Build and Deploy Database

on:
  push:
    branches: [main, develop]

jobs:
  deploy:
    runs-on: ubuntu-latest
    
    steps:
      - uses: actions/checkout@v2
      
      - name: Set up JDK 11
        uses: actions/setup-java@v2
        with:
          java-version: '11'
          distribution: 'adopt'
      
      - name: Cache Maven packages
        uses: actions/cache@v2
        with:
          path: ~/.m2
          key: ${{ runner.os }}-m2-${{ hashFiles('**/pom.xml') }}
          restore-keys: ${{ runner.os }}-m2
      
      - name: Create password file
        run: |
          echo "${{ secrets.TOTALSCHEMA_ENCRYPTION_KEY }}" > /tmp/encryption.key
          chmod 600 /tmp/encryption.key
      
      - name: Build and deploy changes
        run: |
          mvn clean install -Denvironment=PROD -Dtotalschema.password.file=/tmp/encryption.key
      
      - name: Cleanup
        if: always()
        run: rm -f /tmp/encryption.key
```

### Jenkins Pipeline

```groovy
pipeline {
    agent any
    
    tools {
        maven 'Maven 3.8'
        jdk 'JDK 11'
    }
    
    stages {
        stage('Build') {
            steps {
                sh 'mvn clean compile'
            }
        }
        
        stage('Deploy Database Changes') {
            steps {
                withCredentials([string(credentialsId: 'totalschema-encryption-key', variable: 'ENC_KEY')]) {
                    sh '''
                        echo "$ENC_KEY" > /tmp/encryption.key
                        chmod 600 /tmp/encryption.key
                        mvn totalschema:run -Denvironment=PROD -Dtotalschema.password.file=/tmp/encryption.key
                        rm -f /tmp/encryption.key
                    '''
                }
            }
        }
        
        stage('Package') {
            steps {
                sh 'mvn package -DskipTests'
            }
        }
    }
}
```

### GitLab CI with Maven

```yaml
variables:
  MAVEN_OPTS: "-Dmaven.repo.local=$CI_PROJECT_DIR/.m2/repository"

cache:
  paths:
    - .m2/repository

stages:
  - build
  - deploy

build:
  stage: build
  image: maven:3.8-jdk-11
  script:
    - mvn clean compile

deploy-database:
  stage: deploy
  image: maven:3.8-jdk-11
  script:
    - echo "$TOTALSCHEMA_ENCRYPTION_KEY" > /tmp/encryption.key
    - chmod 600 /tmp/encryption.key
    - mvn totalschema:run -Denvironment=PROD -Dtotalschema.password.file=/tmp/encryption.key
    - rm -f /tmp/encryption.key
  only:
    - main
```

---

## Examples

### Example 1: Simple Development Workflow

```bash
# Show what will be applied
mvn totalschema:run -Darguments="execute,show,pending,-e,DEV"

# Apply changes
mvn totalschema:run -Denvironment=DEV

# Validate
mvn totalschema:run -Darguments="validate,-e,DEV"
```

### Example 2: Integrated Build

**pom.xml:**

```xml
<plugin>
    <groupId>io.github.totalschema</groupId>
    <artifactId>totalschema-maven-plugin</artifactId>
    <version>1.0-SNAPSHOT</version>
    <dependencies>
        <dependency>
            <groupId>io.github.totalschema</groupId>
            <artifactId>totalschema-core</artifactId>
            <version>1.0-SNAPSHOT</version>
        </dependency>
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <version>2.2.224</version>
        </dependency>
    </dependencies>
    <executions>
        <execution>
            <id>apply-db-changes</id>
            <phase>pre-integration-test</phase>
            <goals>
                <goal>run</goal>
            </goals>
            <configuration>
                <arguments>
                    <argument>apply</argument>
                    <argument>-e</argument>
                    <argument>TEST</argument>
                </arguments>
            </configuration>
        </execution>
    </executions>
</plugin>
```

**Run integration tests with database setup:**

```bash
mvn clean verify
```

### Example 3: Environment-Specific Deployment

```bash
# Deploy to DEV
mvn clean install -Denvironment=DEV

# Deploy to QA
mvn clean install -Denvironment=QA

# Deploy to PROD with secrets
mvn clean install -Denvironment=PROD -Dtotalschema.password.file=/secure/prod.key
```

### Example 4: Filter Specific Changes

```bash
# Apply only version 1.0.0 changes
mvn totalschema:run -Darguments="execute,apply,-e,DEV,-f,1.X/1.0.0/*"
```

### Example 5: Multi-Module Project

**Project structure:**

```
my-project/
├── pom.xml (parent)
├── application/
│   └── pom.xml
└── database/
    ├── pom.xml
    ├── totalschema.yml
    └── changes/
```

**Parent pom.xml:**

```xml
<modules>
    <module>database</module>
    <module>application</module>
</modules>
```

**database/pom.xml:**

```xml
<build>
    <plugins>
        <plugin>
            <groupId>io.github.totalschema</groupId>
            <artifactId>totalschema-maven-plugin</artifactId>
            <executions>
                <execution>
                    <id>apply-changes</id>
                    <phase>install</phase>
                    <goals>
                        <goal>run</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

**Run from parent:**

```bash
mvn clean install -Denvironment=DEV
```

---

## Troubleshooting

### Plugin Not Found

**Problem:** `Plugin 'io.github.totalschema:totalschema-maven-plugin' not found`

**Solution:**
- Verify the plugin coordinates (groupId, artifactId, version)
- Check that the plugin is available in your Maven repository
- Run `mvn clean` and try again

### JDBC Driver Not Found

**Problem:** `No suitable driver found for jdbc:...`

**Solution:**
- Ensure JDBC driver dependency is added to the plugin's `<dependencies>` section
- Verify the driver version is correct
- Check that the driver supports your database version

### Wrong Goal Name

**Problem:** `Unknown lifecycle phase` or `Unknown goal`

**Solution:**
- Use `totalschema:run` or `totalschema:invoke` (not `totalschema:execute`)
- Check goal spelling

### Arguments Not Passed Correctly

**Problem:** Arguments not recognized

**Solution:**
- Use comma-separated values in `-Darguments`: 
  ```bash
  -Darguments="apply,-e,DEV"
  ```
- Not spaces: `-Darguments="apply -e DEV"` ❌

### Environment Variable Not Resolved

**Problem:** `${environment}` appears literally in output

**Solution:**
- Define the property:
  ```xml
  <properties>
      <environment>DEV</environment>
  </properties>
  ```
- Or pass via command line: `-Denvironment=DEV`

### Changes Not Applied During Build

**Problem:** Maven build completes but changes aren't applied

**Solution:**
- Verify the execution phase is correct
- Check that the environment is set
- Run with `-X` for debug output:
  ```bash
  mvn clean install -X -Denvironment=DEV
  ```

---

## Advantages of Maven Plugin

✅ **Integrated into Maven build lifecycle** - Automatic execution during build  
✅ **JDBC drivers managed via Maven dependencies** - Consistent dependency management  
✅ **Consistent with Java project tooling** - Familiar Maven commands  
✅ **Automatic execution during build phases** - No separate deployment step  
✅ **Inherits Maven project properties and profiles** - Reuse existing configuration  
✅ **Works with Maven release plugin** - Integrated versioning  
✅ **Multi-module support** - Manage databases across multiple modules  

---

## Use Cases

The Maven plugin is ideal for:

- **Java application development** with database changes
- **CI/CD builds** for Java/Maven projects
- **Automated testing** with database setup
- **Application versioning** aligned with database versioning
- **Multi-module Maven projects** with database components
- **Integration testing** requiring database state
- **Development workflows** using Maven tooling

---

## Related Documentation

- [Main README](../README.md) - Complete TotalSchema documentation
- [CLI Usage Guide](totalschema-cli/CLI-USAGE.md) - Command-line interface documentation
- [Configuration Guide](../README.md#getting-started) - Setting up totalschema.yml
- [Change Script Naming](../README.md#change-script-file-naming-convention) - File naming rules

---

## Support

- **Issues**: https://github.com/totalschema/totalschema/issues
- **Documentation**: https://github.com/totalschema/totalschema
- **License**: GNU Affero General Public License v3.0

