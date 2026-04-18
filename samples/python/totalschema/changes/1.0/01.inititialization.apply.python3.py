"""
Sample TotalSchema Python connector script.

Demonstrates that plain Python scripts are executed as change files.
This script runs once (apply) and prints deployment metadata to stdout,
which TotalSchema captures and logs at INFO level.

It also imports ``get_greeting`` from ``greeting_lib`` — a user-provided helper
library made importable via the ``modulesDirectory`` connector option — to show
how shared Python modules can be used across change scripts.
"""

import sys
import platform
import datetime
import getpass

from totalschema.sdk import Variable
from greeting_lib.greeting import get_greeting

username = getpass.getuser()
print(get_greeting(username))

print("=" * 60)
print("TotalSchema – Python connector sample")
print("=" * 60)
print(f"Executed at : {datetime.datetime.now().isoformat()}")
print(f"Python      : {sys.version}")
print(f"Platform    : {platform.platform()}")
print("=" * 60)

foo = Variable.get('foo')
bar = Variable.get('bar')

print(f"Variable foo : {foo}")
print(f"Variable bar : {bar}")
