# totalschema-release

This module is responsible for building the final distributable archives (`.zip` and `.tar.gz`) for totalschema.

## Purpose

The `totalschema-release` module packages:
- The main `totalschema-cli` executable JAR
- All optional extensions from `totalschema-extensions` module
  - `totalschema-groovy-extensions`
- All database integrations from `totalschema-database-integrations` module
  - `totalschema-gcp-bigquery`
- User documentation
- Shell scripts for running totalschema
- LICENSE file

## Build

To build the release archives:

```bash
mvn clean package
```

This will generate:
- `target/totalschema-{version}.tar.gz`
- `target/totalschema-{version}.zip`

## Distribution Structure

The generated archives contain:
```
totalschema-{version}/
├── LICENSE
├── README.md
├── bin/
│   ├── totalschema.sh   (Unix/Linux/Mac)
│   └── totalschema.bat  (Windows)
└── lib/
    ├── totalschema-cli-{version}.jar
    ├── totalschema-groovy-extensions-{version}.jar
    ├── totalschema-gcp-bigquery-{version}.jar
    ├── [all dependency JARs]
    └── jdbc/
        └── lib-jdbc-README.md
```

## Adding New Modules

When new extension or database integration modules are added to the project, they should be added as dependencies in this module's `pom.xml` to ensure they are included in the final distribution.

