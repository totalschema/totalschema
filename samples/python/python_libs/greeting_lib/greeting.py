"""
greeting.py — simple helper module demonstrating a user-provided library
that TotalSchema Python scripts can import via the ``modulesDirectory``
connector option.

This module lives in ``samples/python/python_libs/greeting_lib/`` — outside
the changes tree — and is made importable by pointing ``modulesDirectory`` at
``samples/python/python_libs`` in ``totalschema.yml``.
"""


def get_greeting(user_name: str) -> str:
    """Return a greeting string for the given user.

    Args:
        user_name: the OS login name of the user running the deployment
                   (typically obtained via ``getpass.getuser()``).

    Returns:
        A human-readable greeting string.
    """
    return f"[greeting_lib] Hello, '{user_name}' – running via TotalSchema!"
