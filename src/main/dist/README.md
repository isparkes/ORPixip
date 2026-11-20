ORPixip
=======

Revenue assurance for a mobile operator, built on
[OpenRate](https://github.com/isparkes/OpenRate). It rates the operator's
traffic a second time, independently of the billing system, and reports every
record where the two answers disagree.

This is the packaged distribution: the application, the OpenRate framework,
every dependency, the configuration and both database dumps. No Maven and no
source checkout needed — a JDK 17 and Docker are enough.

Quick start
-----------

```bash
bin/db.sh        # start MySQL and load both schemas
bin/run.sh       # start the framework - runs until stopped
```

`run.sh` occupies the terminal. From a second one, in the same directory:

```bash
bin/db.sh status # what the rating found
bin/feed.sh      # offer every record up for rating again
bin/stop.sh      # shut down cleanly
```

What it finds
-------------

```
+------------+---------+-------+---------+-------------+
| SOURCE     | RECORDS | RATED | DIFFERS | RATED_VALUE |
+------------+---------+-------+---------+-------------+
| billing    |    1311 |  1311 |       3 |     1574.44 |
| probe call |    1021 |  1021 |      78 |      673.69 |
| probe sms  |      94 |    94 |       4 |      120.31 |
| probe ftp  |      11 |    11 |       0 |        0.29 |
| probe http |    2342 |  2342 |      22 |      134.52 |
+------------+---------+-------+---------+-------------+
```

`DIFFERS` is the point of the exercise: records where OpenRate's charge is
more than 8 cents away from the billing system's on a voice call, or 5 cents
on anything else. They are written with `#DIFF` on the end of their rating
trail, and `bin/db.sh status` lists the billing ones.

The database
------------

Two schemas on one MySQL server:

| Schema | Holds |
|---|---|
| `PixipDB` | The rating reference data — normalisation rules, destinations, time models, price models |
| `mtn_poc` | The operator's own data — the records to rate, and the result tables |

`bin/db.sh` runs MySQL in a Docker container on port **3307** and loads both
from the dumps in `db/`. Port 3307 rather than 3306 so it does not collide
with a MySQL of your own.

| Command | Does |
|---|---|
| `bin/db.sh` | Start MySQL and load both schemas |
| `bin/db.sh status` | Say whether it is up, and show what the rating found |
| `bin/db.sh shell` | Open a mysql prompt on `mtn_poc` |
| `bin/db.sh stop` | Stop the database, keeping its data |
| `bin/db.sh reset` | Throw it away and start over |

To use a MySQL you already run instead:

```bash
export OPENRATE_DB_MODE=local
export OPENRATE_DB_USER=root OPENRATE_DB_PASS=yourpassword
bin/db.sh load
```

The connection details are in `properties/Pixip.properties.xml`, so a server
on another host or port needs that file changed to match. The application
connects as `openrate`/`openrate`, which `bin/db.sh load` creates.

Feeding it
----------

Nothing is fed in from outside: the records are already in the database, and
a flag on each one says whether it has been rated. The input adapters poll for
records where that flag is unset, so `bin/feed.sh` clears it:

```bash
bin/feed.sh           # all five sources
bin/feed.sh billing   # just the billing CDRs
bin/feed.sh call      # just the probe's call records (also sms, ftp, http)
```

Layout
------

| Directory | Contents |
|---|---|
| `bin/` | The scripts above |
| `properties/` | `Pixip.properties.xml` and `logPixip.xml` |
| `dist/` | This application |
| `ORlib/` | The OpenRate framework |
| `lib/` | Third party dependencies |
| `db/` | The schema dumps, sample traffic and stored procedures |
| `log/` | Framework, pipeline and error logs |
| `Data/Pixip/` | Per-record processing dumps from the `Dump` module |

Environment
-----------

| Variable | Default | Does |
|---|---|---|
| `JAVA_HOME` | first `java` on the PATH | Which JDK to run (17 or later) |
| `JAVA_OPTS` | — | Extra JVM options, e.g. `-Xmx2g` |
| `OPENRATE_ECI_PORT` | `8086` | Admin console port |
| `OPENRATE_DB_MODE` | `docker` | `docker` or `local` |
| `OPENRATE_DB_PORT` | `3307` | MySQL port |
| `OPENRATE_DB_USER` | `root` | Administrative MySQL user |
| `OPENRATE_DB_PASS` | `cpr` | Its password |

The admin console
-----------------

```bash
telnet localhost 8086
```

`Help` lists the commands. `CacheFactory:Reload=true` reloads the rating
reference data without restarting, which is how you try a price change on a
running system.

> The console is unauthenticated. It is fine on a development machine; do not
> expose the port on a shared or public network.

License
-------

Licensed under the Apache License, Version 2.0. See `LICENSE`.

Copyright 2006-2026 Ian Sparkes.
