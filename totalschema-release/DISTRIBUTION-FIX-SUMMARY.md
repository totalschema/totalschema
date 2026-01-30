# Distribution Build Configuration Fix Summary

## Problem
The `totalschema-release` distribution was bundling unwanted dependencies:
- **groovy-all** - Should be provided by the user, not bundled
- **junit** and test dependencies - Test frameworks should never be in production distributions
- **h2** - Provided-scope dependency that shouldn't be distributed

## Solution Applied

### 1. Updated `pom.xml` Dependencies
Added explicit exclusions in the `pom.xml` to prevent transitive dependencies from being included:

```xml
<dependencies>
    <!-- Core CLI dependency -->
    <dependency>
        <groupId>io.github.totalschema</groupId>
        <artifactId>totalschema-cli</artifactId>
        <version>${project.version}</version>
        <exclusions>
            <!-- Exclude provided-scope dependencies from CLI -->
            <exclusion>
                <groupId>com.h2database</groupId>
                <artifactId>h2</artifactId>
            </exclusion>
            <exclusion>
                <groupId>io.github.totalschema</groupId>
                <artifactId>totalschema-groovy-extensions</artifactId>
            </exclusion>
            <exclusion>
                <groupId>org.codehaus.groovy</groupId>
                <artifactId>groovy-all</artifactId>
            </exclusion>
        </exclusions>
    </dependency>

    <!-- Extensions -->
    <dependency>
        <groupId>io.github.totalschema</groupId>
        <artifactId>totalschema-groovy-extensions</artifactId>
        <version>${project.version}</version>
        <exclusions>
            <!-- groovy-all should be provided by the user -->
            <exclusion>
                <groupId>org.codehaus.groovy</groupId>
                <artifactId>groovy-all</artifactId>
            </exclusion>
        </exclusions>
    </dependency>

    <!-- Database Integrations -->
    <dependency>
        <groupId>io.github.totalschema</groupId>
        <artifactId>totalschema-gcp-bigquery</artifactId>
        <version>${project.version}</version>
    </dependency>
</dependencies>
```

**Key Points:**
- Excluded `h2`, `groovy-all`, and circular `totalschema-groovy-extensions` from CLI dependency
- Excluded `groovy-all` from the groovy-extensions dependency
- These dependencies are already marked as `provided` scope in their respective modules

### 2. Updated `distribution.xml` Assembly Descriptor
Changed the dependency scope from `runtime` to `compile` and added explicit excludes:

```xml
<dependencySet>
    <outputDirectory>/lib</outputDirectory>
    <useProjectArtifact>false</useProjectArtifact>
    <unpack>false</unpack>
    <scope>compile</scope>  <!-- Changed from 'runtime' to 'compile' -->
    <excludes>
        <!-- Exclude test dependencies -->
        <exclude>*:*:*:*:test</exclude>
        <!-- Exclude provided dependencies that should be supplied by user -->
        <exclude>org.codehaus.groovy:groovy-all</exclude>
        <exclude>com.h2database:h2</exclude>
        <!-- Exclude test frameworks -->
        <exclude>junit:junit</exclude>
        <exclude>org.junit.jupiter:*</exclude>
        <exclude>org.mockito:*</exclude>
        <exclude>org.hamcrest:*</exclude>
    </excludes>
</dependencySet>
```

**Key Points:**
- Changed scope from `runtime` to `compile` - more precise control over what gets included
- Added explicit exclusions for test dependencies (junit, mockito, hamcrest)
- Added exclusions for provided-scope dependencies (groovy-all, h2)

## Verification

The distribution now correctly includes:

### ✅ Included JAR Files
- `commons-codec-1.17.1.jar`
- `commons-csv-1.12.0.jar`
- `commons-io-2.17.0.jar`
- `commons-lang3-3.14.0.jar`
- `commons-text-1.12.0.jar`
- `jsch-0.1.55.jar`
- `logback-classic-1.5.15.jar`
- `logback-core-1.5.15.jar`
- `picocli-4.1.4.jar`
- `slf4j-api-2.0.16.jar`
- `snakeyaml-2.2.jar`
- `totalschema-cli-1.0-SNAPSHOT.jar`
- `totalschema-core-1.0-SNAPSHOT.jar`
- `totalschema-gcp-bigquery-1.0-SNAPSHOT.jar` ✓ (database integration)
- `totalschema-groovy-extensions-1.0-SNAPSHOT.jar` ✓ (extension)

### ❌ Correctly Excluded
- ❌ `groovy-all.jar` - User must provide
- ❌ `h2.jar` - Provided scope
- ❌ `junit.jar` - Test dependency
- ❌ `mockito.jar` - Test dependency
- ❌ `hamcrest.jar` - Test dependency

## Build Command
```bash
cd totalschema
mvn clean package -DskipTests -pl totalschema-release -am
```

## Result
The distribution archives (`totalschema-1.0-SNAPSHOT.zip` and `totalschema-1.0-SNAPSHOT.tar.gz`) now contain:
- Core CLI with all runtime dependencies
- Optional extension JARs (groovy-extensions, gcp-bigquery)
- No test dependencies
- No provided-scope dependencies that users should supply themselves

## Why This Matters

1. **Smaller Distribution Size** - Excludes unnecessary test and provided dependencies
2. **Correct Dependencies** - Users can supply their own groovy-all version
3. **Clean Separation** - Test dependencies don't leak into production distributions
4. **Better User Experience** - Clear documentation about what needs to be provided vs what's included
