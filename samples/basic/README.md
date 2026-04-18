# TotalSchema Sample Project

This directory contains a sample project for TotalSchema. 

It demonstrates using totalschema with H2: both the managed
target database and totalschema system databases are stored
in H2 databases. Tables are created and changed via SQL and
Groovy change files: for this to work, **the H2 JDBC driver 
and the Groovy JAR must be manually installed to totalschema 
installation directory**.

**totalschema/changes** directory contains a set of change files:
these can be automatically applied via totalschema.

**totalschema.yml** is a configuration file of totalschema.

To try this out, simply add H2 DB JDBC driver and Groovy all release
JARs to your totalschema installation and then run the command:

```
totalschema apply -e DEV --password FOOBAR
```

