#!/bin/bash

# totalschema: tool for managing database versioning and schema changes with ease.
# Copyright (C) 2025-2026 totalschema development team
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU Affero General Public License as
# published by the Free Software Foundation, either version 3 of the
# License, or (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU Affero General Public License for more details.
#
# You should have received a copy of the GNU Affero General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.

# Get the directory where the script is located
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BASE_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

# Check if Java is available
if ! command -v java &> /dev/null; then
    echo "ERROR: Java is not found in PATH" >&2
    echo "" >&2
    echo "TotalSchema requires Java 11 or higher to run." >&2
    echo "Please install Java and ensure it is available in your PATH." >&2
    echo "" >&2
    echo "To install Java:" >&2
    echo "  - macOS: brew install openjdk@11" >&2
    echo "  - Ubuntu/Debian: sudo apt-get install openjdk-11-jdk" >&2
    echo "  - Red Hat/CentOS: sudo yum install java-11-openjdk" >&2
    echo "" >&2
    echo "Or download from: https://adoptium.net/" >&2
    exit 1
fi

# Build classpath starting with bundled libraries
CLASSPATH="$BASE_DIR/lib/*"

# Add user_libs directory if it exists
if [ -d "$BASE_DIR/user_libs" ]; then
    CLASSPATH="$CLASSPATH:$BASE_DIR/user_libs/*"
fi

# Add TOTALSCHEMA_USER_LIBS environment variable if set
if [ -n "$TOTALSCHEMA_USER_LIBS" ]; then
    CLASSPATH="$CLASSPATH:$TOTALSCHEMA_USER_LIBS"
fi

# Run the application
java -cp "$CLASSPATH" io.github.totalschema.cli.Main "$@"

