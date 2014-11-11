ORPixip
=======

Pixip Proof Of Concept

To set up this project:

Database:
=========

Pre-requesite:
MySQL Installed and running

mysqladmin --user=root --password=cpr create PixipDB
mysql --user=root --password=cpr

mysql> create user 'openrate'@'localhost' identified by 'openrate';
mysql> grant all privileges on PixipDB.* to 'openrate'@'localhost';
mysql> grant execute on *.* to 'openrate'@'localhost';
mysql> grant create routine on PixipDB.* to 'openrate'@'localhost';
mysql> exit

mysql --user=openrate --password=openrate PixipDB < Pixip-nnn.sql (replace "nnn"
by the highest version number you find).


OpenRate Core:
==============

Download the OpenRate project from GitHub. Clean and build. Ensure that it is
loaded into your Maven repository.

cd <your-choice-of-directory>
git clone git@github.com:isparkes/OpenRate.git
mvn install

Pixip Project:
==============

Download the Pixip project from GitHub (you already have if you are reading 
this).

cd <your-choice-of-directory>
git clone git@github.com:isparkes/Pixip.git
mvn install


Running for debug:
==================

You should be able to run the Pixip project from any IDE. We use Netbeans. Load
up the project in your IDE and you should be able to run, build and debug as
with any other Maven project.


Running in outside of the IDE:
==============================

The startup script launches the OpenRate application.


