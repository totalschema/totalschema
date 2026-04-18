UPDATE phone_book SET first_name = REGEXP_SUBSTR(TRIM(name), '(\w+)\W+.*');
