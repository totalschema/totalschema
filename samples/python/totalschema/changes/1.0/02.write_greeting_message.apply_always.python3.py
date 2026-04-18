"""
Sample TotalSchema Python connector script — apply_always variant.

This script is re-executed on every deployment run (apply_always type).
It writes a timestamped message to a local audit log file next to the script,
illustrating how Python scripts can perform lightweight side-effects such as
file I/O or calling external APIs.

It also imports a helper from ``my_sample_py_lib`` to show how user-provided
local modules can be used via the ``modulesDirectory`` connector option.
"""

import datetime
import getpass
import pathlib

from greeting_lib.greeting import get_greeting

greeting_file = pathlib.Path(__file__).parent / "greeting_message.log"


username = getpass.getuser()
message = get_greeting(username)

with open(greeting_file, "a", encoding="utf-8") as f:
    f.write(message)

print(f"Greeting message written to: {greeting_file}")
print(message.strip())

