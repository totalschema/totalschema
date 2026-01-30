#!/bin/bash

# Get the directory where the script is located
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BASE_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

# Build JDBC classpath from multiple sources
JDBC_CLASSPATH=""

# 1. Check for dedicated JDBC driver directory (lib/jdbc/)
if [ -d "$BASE_DIR/lib/jdbc" ]; then
    JDBC_CLASSPATH="$BASE_DIR/lib/jdbc/*"
fi

# 2. Check for environment variable (can extend or override)
if [ -n "$TOTALSCHEMA_JDBC_DRIVERS" ]; then
    if [ -n "$JDBC_CLASSPATH" ]; then
        JDBC_CLASSPATH="$JDBC_CLASSPATH:$TOTALSCHEMA_JDBC_DRIVERS"
    else
        JDBC_CLASSPATH="$TOTALSCHEMA_JDBC_DRIVERS"
    fi
fi

# Build final classpath
if [ -n "$JDBC_CLASSPATH" ]; then
    CLASSPATH="$BASE_DIR/lib/*:$JDBC_CLASSPATH"
else
    CLASSPATH="$BASE_DIR/lib/*"
fi

# Run the application
java -cp "$CLASSPATH" io.github.totalschema.cli.Main "$@"

