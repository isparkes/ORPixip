ORPixip
=======

<br><br>You might find this file easier to read in a text editor!!!<br><br>

Pixip Proof Of Concept.

This project is a proof of concept for rating for revenue assurance purposes.
It takes 5 sources of information and applies rating to them:
 - An input billing table (mtn_poc.mtn_billing_cdr), taken from the billing
   system and rated according to the values in the table. Results are placed in a 
   dedicated output table (mtn_poc.or_billing_result), joined to the input
   table on mtn_cdr_id
 - An input table for probe call records (mtn_poc.mxass_or_cdr_call), rated to
   an output table (mtn_poc.or_xmass_call_result), joined on RESULT_ID
 - An input table for probe SMS records (mtn_poc.mxass_or_cdr_sms), rated to
   an output table (mtn_poc.or_xmass_sms_result), joined on RESULT_ID
 - An input table for probe FTP records (mtn_poc.mxass_or_cdr_ftp), rated to
   an output table (mtn_poc.or_xmass_ftp_result), joined on RESULT_ID
 - An input table for probe HTTP records (mtn_poc.mxass_or_cdr_http), rated to
   an output table (mtn_poc.or_xmass_http_result), joined on RESULT_ID

The output tables are loaded by a stored procedure, which implements a simple
"upsert" (insert if not present, update if already present) procedure.

Records are taken for rating if a key field in the input table has an expected
value:
 - For input table mtn_billing_cdr, records are taken in FIELD3 is null
 - For input tables mxass_or_cdr_call, mxass_or_cdr_sms, mxass_or_cdr_ftp and
   mxass_or_cdr_http records are taken if OR_RATE_CONTROL is 0

Pre-requesites:
  MySQL Installed and running
  Dump files have been uncompressed
  Maven is installed


To set up this project:

Configuration Database:
=======================

The system uses two databases: 

 - A configuration database "PixipDB" which holds the static reference data 
for rating.
 - A database for holding the input 

mysqladmin --user=root --password=cpr create PixipDB

mysql --user=root --password=cpr

mysql> create user 'openrate'@'localhost' identified by 'openrate';

mysql> grant all privileges on PixipDB.* to 'openrate'@'localhost';

mysql> grant execute on *.* to 'openrate'@'localhost';

mysql> grant create routine on PixipDB.* to 'openrate'@'localhost';

mysql> exit

Then load the data

mysql --user=openrate --password=openrate PixipDB < Pixip-nnn.sql (replace "nnn"
by the highest version number you find).


CDR Database (local development):
=================================
mysqladmin --user=root --password=cpr create mtn_poc

mysql --user=root --password=cpr

mysql> grant all privileges on mtn_poc.* to 'openrate'@'localhost';

Then load the data

mysql --user=openrate --password=openrate mtn_poc < mtn_poc.sql (replace "nnn"
by the highest version number you find).


Compile OpenRate Core:
======================

Download the OpenRate project from GitHub. Clean and build. Ensure that it is
loaded into your Maven repository.

cd <your-choice-of-directory>
git clone git@github.com:isparkes/OpenRate.git
mvn install


Compile Pixip Project:
======================

Download the Pixip project from GitHub (you already have if you are reading 
this).

cd <your-choice-of-directory>
git clone git@github.com:isparkes/Pixip.git
mvn clean install


Running:
=======

The easiest way to run through Maven. From the command line, type

   mvn exec:java


Running for debug:
==================

You should be able to run the Pixip project from any IDE. We use Netbeans. Load
up the project in your IDE and you should be able to run, build and debug as
with any other Maven project.


Running in real life:
=====================

The startup script launches the OpenRate application. There is a startup
script (bin/startup.sh) and a shutdown script (bin/shutdown.sh) in the 
distribution directory.

