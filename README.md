ORPixip
=======

Revenue assurance for a mobile operator, built on
[OpenRate](https://github.com/isparkes/OpenRate). It rates the operator's
traffic a second time, independently of the billing system, and reports every
record where the two answers disagree.

Five sources are rated in parallel: the billing system's own call records, and
four streams of probe records (call, SMS, FTP, HTTP) captured on the network.
Each rated record is written back next to the original, with the charge
OpenRate arrived at and the trail it followed to get there, so the two can be
compared.

This is a decommissioned proof of concept for an African mobile operator, with
its real reference data and a sample of its real traffic. It is the
database-in, database-out counterpart to
[OpenRateWeblinksInterconnect](https://github.com/isparkes/OpenRateWeblinksInterconnect):
no files anywhere, five pipelines instead of two.

Requires OpenRate 1.5.2.6 or later, JDK 17, and Docker (for the database, see
below).

Quick start
-----------

```bash
./demo.sh
```

That brings up MySQL, loads both schemas, builds the application, starts the
framework, rates all 4779 records, shows what it found, and shuts down again.
It takes about two minutes.

If OpenRate itself is not installed yet, `demo.sh` stops and tells you how.
OpenRate is not published to Maven Central, so the framework has to be built
locally first — once, before any application will resolve it:

```bash
git clone https://github.com/isparkes/OpenRate.git
cd OpenRate
./bootstrap.sh
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

`DIFFERS` is the point of the exercise. A record counts as differing when
OpenRate's charge is more than 8 cents away from the billing system's on a
voice call, or 5 cents on anything else — the tolerances are in
`CompareCharge.java`. Those records are written with `#DIFF` on the end of
their rating trail, and `./db.sh status` lists the billing ones:

```
+------+---------+---------+--------+----------+---------------------------------------------+
| CDR  | SERVICE | SECONDS | BILLED | OPENRATE | RATING_TRAIL                                |
+------+---------+---------+--------+----------+---------------------------------------------+
| 1307 | VOICE   |      64 |   2.25 |     1.95 | PMB,NAL,OFFPEAK,PM_1_5,PMB,NAL,VALUE,PM_0_9 |
| 1323 | VOICE   |      64 |   1.65 |      1.8 | PMB,NAL,VALUE,PM_0_9,PMB,NAL,PEAK,PM_1_8    |
| 1352 | VOICE   |      64 |   2.25 |     1.95 | PMB,NAL,OFFPEAK,PM_1_5,PMB,NAL,VALUE,PM_0_9 |
+------+---------+---------+--------+----------+---------------------------------------------+
```

All three are 64 second calls that crossed a time band boundary, so they were
split into two charge packets and rated partly at one price and partly at
another. The billing system handled the split differently. That is exactly the
kind of thing this application exists to surface.

The database
------------

Two schemas on one MySQL server:

| Schema | Holds |
|---|---|
| `PixipDB` | The rating reference data — normalisation rules, destinations, time models, price models |
| `mtn_poc` | The operator's own data — the records to rate, and the result tables |

`db.sh` runs MySQL in a Docker container on port **3307** and loads both, so
there is nothing to install beyond Docker. Port 3307 rather than 3306 so it
does not collide with a MySQL of your own, or with the other OpenRate
examples.

| Command | Does |
|---|---|
| `./db.sh` | Start MySQL and load both schemas |
| `./db.sh status` | Say whether it is up, and show what the rating found |
| `./db.sh shell` | Open a mysql prompt on `mtn_poc` |
| `./db.sh stop` | Stop the database, keeping its data |
| `./db.sh reset` | Throw it away and start over |

To use a MySQL you already run instead:

```bash
export OPENRATE_DB_MODE=local
export OPENRATE_DB_USER=root OPENRATE_DB_PASS=yourpassword
./db.sh load
```

The connection details are in `src/main/resources/Pixip.properties.xml`, so a
server on another host or port needs that file changed to match. The
application connects as `openrate`/`openrate`, which `db.sh load` creates.

Running it yourself
-------------------

`demo.sh` just chains the other scripts together. To drive it by hand:

| Script | Does |
|---|---|
| `./build.sh` | Build the application |
| `./run.sh` | Start the framework in the foreground; runs until stopped |
| `./feed.sh` | Offer every record up for rating again |
| `./feed.sh call` | Just the probe's call records (also `billing`, `sms`, `ftp`, `http`) |
| `./stop.sh` | Shut it down cleanly |

OpenRate runs here as a **daemon**: it starts, polls the five input tables for
records that have not been rated yet, rates what it finds, and keeps running
until told to stop. So `run.sh` occupies a terminal, and you drive it from a
second one:

```bash
./db.sh                  # once
./run.sh                 # terminal 1 - wait for "Running..."
./db.sh status           # terminal 2 - what it found
./feed.sh                # terminal 2 - rate it all again
./stop.sh                # terminal 2
```

Nothing is fed in from outside. Each input table carries the rating state of
its own records, and `feed.sh` resets it:

| State | `mtn_billing_cdr.FIELD3` | `xmass_or_cdr_*.OR_RATE_CONTROL` |
|---|---|---|
| Waiting to be rated | `null` | `0` |
| Claimed by a pipeline | `'1'` | `1` |
| Rated, not yet committed | `'2'` | `2` |
| Done | `'3'` | `3` |
| Rolled back | `'-1'` | `-1` |

The input adapters poll for the first of those, so clearing the flag is what
gives the framework work to do.

What comes out
--------------

Each rated record goes back to the database through an upsert stored
procedure, into a result table joined to the input on its id:

| Input table | Result table |
|---|---|
| `mtn_billing_cdr` | `or_billing_result` |
| `xmass_or_cdr_call` | `or_xmass_call_result` |
| `xmass_or_cdr_sms` | `or_xmass_sms_result` |
| `xmass_or_cdr_ftp` | `or_xmass_ftp_result` |
| `xmass_or_cdr_http` | `or_xmass_http_result` |

Each result row carries the charge (`OR_RATED_AMOUNT`) and the rating trail
(`OR_MESSAGE`) — the tariff, zone, time band and price model used for each
charge packet, with `#DIFF` appended if the comparison failed.

The `Dump` module also writes a full per-record, per-module trace to
`Data/Pixip/*.dump`. They are large — tens of megabytes a run — but they are
the best way to see what actually happened to one record:

```bash
grep -A40 'mtn_cdr_id = 1307' Data/Pixip/Billing_*.dump | less
```

How it works
------------

Five pipelines, all defined in `src/main/resources/Pixip.properties.xml`.
`PixipPipe` rates the billing records; the four `XMASS*Pipe` pipelines do the
same for the probe records, differing only in their input table and one extra
module. The chain is:

| Module | Role |
|---|---|
| `PixipBillingDBInputAdapter` | Claims unrated rows and maps them into a `PixipRecord` |
| `Normalisation` | Normalises the dialled number (regex match) |
| `CustomerTariffLookup` | Finds the caller's tariff and balance type from their MSISDN |
| `ChargePacketCreation` | Creates the charge packets, so the call can be split |
| `TimeLookup` | Splits the call across time bands — peak, off peak, value |
| `ZoneLookup` | Finds the destination zone by longest prefix match |
| `PriceLookup` | Picks the price group for that zone, time band and service |
| `SetupDA1XMASS` | (probe pipelines only) Sets up the dedicated account balance |
| `PreRatingBalanceHandling` | Reduces the rateable amount for the distributor package |
| `Rating` | Rates each charge packet with RUM based rating |
| `RateRounding` | Rolls the impacts up and rounds |
| `PostPaidVATDropDown` | Takes VAT out again for postpaid customers |
| `CalculateOriginalCharge` | Reconstructs what the source system charged, main account plus any dedicated account movement |
| `CompareCharge` | Flags the record if the two differ beyond tolerance |
| `Dump` | Writes the per-record trace |
| `PixipDBOutputAdapter` | Upserts the result, appending `#DIFF` if flagged |

All the reference data is in `PixipDB`, loaded into in-memory caches at
startup and reloadable from the admin console while the framework runs.

The admin console
-----------------

OpenRate exposes a control interface on port 8086:

```bash
telnet localhost 8086
```

`Help` lists the available commands. `stop.sh` uses this to issue:

```
Framework:shutdown=true
```

> The console is unauthenticated. It is fine on a development machine; do not
> expose the port on a shared or public network.

Building a distribution
-----------------------

```bash
mvn clean package
```

also produces `target/ORPixip-1.0.0-bin.zip`: a self-contained distribution
that runs anywhere with a JDK 17 and Docker, with no Maven and no source
checkout.

```
unzip ORPixip-1.0.0-bin.zip -d pixip
cd pixip
bin/db.sh && bin/run.sh
```

It carries its own `README.md`, the framework and dependency jars, the
configuration, both database dumps, and `bin/db.sh`, `bin/run.sh`,
`bin/feed.sh`, `bin/stop.sh`. Those distribution scripts launch Java directly;
the scripts in the project root drive Maven, so each set only works in its own
context. The distribution sources live in `src/main/bin` and `src/main/dist`.

Where to go next
----------------

Change a price and watch the discrepancy count move. `PRICE_MODEL` in
`PixipDB` holds the rates:

```bash
./db.sh shell
mysql> use PixipDB;
mysql> update PRICE_MODEL set FACTOR = FACTOR * 1.1 where PRICE_MODEL = 'PM_1_8';
```

The reference data is cached in memory, so tell the framework to reload it
rather than restarting:

```bash
echo 'CacheFactory:Reload=true' | nc localhost 8086
```

then `./feed.sh billing` and `./db.sh status`. The rated values move, and
records that agreed with the billing system before now show up as `#DIFF`.

Other examples: [OpenRateSimple](https://github.com/isparkes/OpenRateSimple)
(the simplest one, no database),
[OpenRateWeblinksInterconnect](https://github.com/isparkes/OpenRateWeblinksInterconnect)
(interconnect rating and billing),
[ORTyfon](https://github.com/isparkes/ORTyfon) (fixed line and VoIP rating
with fraud detection),
[ORSeaSatCom](https://github.com/isparkes/ORSeaSatCom) (GSM and satellite
retail rating).

License
-------

Licensed under the Apache License, Version 2.0. See [LICENSE](LICENSE).

Copyright 2006-2026 Ian Sparkes.
