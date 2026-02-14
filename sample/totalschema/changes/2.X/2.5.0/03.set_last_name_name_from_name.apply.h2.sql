UPDATE phone_book SET last_name = REGEXP_SUBSTR(TRIM(name), '\w+\W+(.*)');
