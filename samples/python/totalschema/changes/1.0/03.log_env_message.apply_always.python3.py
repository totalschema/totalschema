"""
Sample TotalSchema Python connector script — environment variable access.

This script is re-executed on every deployment run (apply_always type).
It reads the MY_MESSAGE environment variable injected by the connector's
``environmentVariables`` configuration and prints it to stdout.

It also imports ``get_greeting`` from ``greeting_lib`` — a user-provided helper
library made importable via the ``modulesDirectory`` connector option — to show
how shared Python modules can be used across change scripts.

Demonstrates how environment variables configured in ``totalschema.yml``
are made available to Python scripts at runtime.
"""

import os
import getpass

from greeting_lib.greeting import get_greeting

username = getpass.getuser()
print(get_greeting(username))

message = os.environ.get("MY_MESSAGE", "<MY_MESSAGE not set>")
print(f"MY_MESSAGE = {message}")
