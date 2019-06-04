# SQL Command Line Interface

[![Travis CI Build Status][travis-badge]][travis-link]

This program provides a command line interface to interacting with relational
database managements systems (RDMBs) and currently includes SQLite, MySQL and
PostgreSQL out of the box.  Additional JDBC drivers can easily be added in a
(somewhat incomplete) plugin system.

Features include:

* A GUI tabulation presentation, which is handy for very large result sets.
* Use any JDBC driver to connect almost any database.
* JDBC drivers are configured in the command event loop application and
  downloaded and integrated without having to restart.
* Persist result sets as a `.csv` file.
* Emacs interaction with the [cisql] library.
* Primitive variable setting (controls GUI interface, logging, etc).
* Distribution is a one Java Jar file with all dependencies.


<!-- markdown-toc start - Don't edit this section. Run M-x markdown-toc-refresh-toc -->
## Table of Contents

- [Obtaining](#obtaining)
- [Usage](#usage)
    - [Connecting to a Database](#connecting-to-a-database)
    - [Installing new JDBC Drivers](#installing-new-jdbc-drivers)
        - [SQLite](#sqlite)
        - [Apache Drill](#apache-drill)
    - [Querying the database](#querying-the-database)
    - [Command Line Usage](#command-line-usage)
- [Documentation](#documentation)
- [Changelog](#changelog)
- [License](#license)

<!-- markdown-toc end -->


## Obtaining

The latest release binaries are
available [here](https://github.com/plandes/cisql/releases/latest).


## Usage

You can specify command line arguments to connect to a database or you can
connect (and re-connect) while in the command event loop of the program.

The command line usage is given with the with `--help` option
(see [command line usage](#command-line-usage)).  You
can [connect](#connecting-to-a-database) in the application and not provide any
connection arguments to start the application as well.

An example of how to connect to an SQLlite database from the command line follows:
```bash
$ java -jar target/cisql.jar -n sqlite -d path/to/awards.sqlite
Clojure Interactive SQL (cisql) v0.0.10
(C) Paul Landes 2015 - 2017
 1 >
```


### Connecting to a Database

The connection usage is the same in the event loop and on the command line:

```sql
 1 > conn help
usage: conn <help|driver [options]>
Connect to a database
  -u, --user <string>                 login name
  -p, --password <string>             login password
  -h, --host <string>      localhost  database host name
  -d, --database <string>             database name
      --port <number>                 database port
```

To connect to an *SQLite* database, use the following:
```sql
 1 > conn sqlite --database awards.sqlite
spec: loading dependencies for [[org.xerial/sqlite-jdbc "3.8.11.2"]]
configured jdbc:sqlite:awards.sqlite
```

Connect to a *mySql* database:
```sql
 1 > conn postgres -u puser -p pass -d puser -h 192.168.99.100
spec: loading dependencies for [[postgresql/postgresql "9.1-901-1.jdbc4"]]
configured jdbc:postgresql://puser:pass@localhost:5432/puser
```


### Installing new JDBC Drivers

The tool itself comes with no JDBC drivers.  However it does have JDBC
*configuration* settings for popular databases and
are
[configured in a resource file](https://github.com/plandes/cisql/blob/master/resources/driver.csv) in
the program.  The system uses the [maven repository](https://mvnrepository.com)
system and will automatically download and use the new driver without having to
exit and restart the program.

To configure and install a new JDBC driver (in this example to read comma
delimited CSV files):
```sql
 1 > newdrv -n csv -c org.relique.jdbc.csv.CsvDriver -d net.sourceforge.csvjdbc/csvjdbc/1.0.28 -u jdbc:relique:csv:%5$s
spec: loading driver: csv
spec: loading dependencies for [[net.sourceforge.csvjdbc/csvjdbc "1.0.28"]]
spec: added driver: csv

 1 > conn csv -d /Users/paul/stats-dir
spec: loading dependencies for [[net.sourceforge.csvjdbc/csvjdbc "1.0.28"]]
configured jdbc:relique:csv:/Users/paul/stats-dir

 1 > select count(*) from stat-file;
db-access: executing: select count(*) from stat-file

| COUNT(*) |
|----------|
|       41 |
```


#### SQLite

```sql
1> newdrv -n sqlite -c org.sqlite.JDBC -u jdbc:sqlite:%5$s -d org.xerial/sqlite-jdbc/3.25.2
```


#### Apache Drill

The following installs the direct drill bit JDBC driver:

```sql
1> newdrv -n drilldir -u jdbc:drill:drillbit=%3$s:%4$s -p 31010 -c org.apache.drill.jdbc.Driver -d org.apache.drill.exec/drill-jdbc/1.10.0
```


### Querying the database

These examples show how to list tables in the database, a particular table and
a select from that table.

```sql
 1 > shtab

| TABLE_CAT | TABLE_SCHEM |      TABLE_NAME | TABLE_TYPE | REMARKS | TYPE_CAT | TYPE_SCHEM | TYPE_NAME | SELF_REFERENCING_COL_NAME | REF_GENERATION |
|-----------+-------------+-----------------+------------+---------+----------+------------+-----------+---------------------------+----------------|
|           |             |           award |      TABLE |         |          |            |           |                           |                |
|           |             |         cyclist |      TABLE |         |          |            |           |                           |                |
|           |             | sqlite_sequence |      TABLE |         |          |            |           |                           |                |
|           |             |             won |      TABLE |         |          |            |           |                           |                |
 1 > shtab cyclist

| TABLE_CAT | TABLE_SCHEM | TABLE_NAME | COLUMN_NAME | DATA_TYPE | TYPE_NAME | COLUMN_SIZE | BUFFER_LENGTH | DECIMAL_DIGITS | NUM_PREC_RADIX | NULLABLE | REMARKS | COLUMN_DEF | SQL_DATA_TYPE | SQL_DATETIME_SUB | CHAR_OCTET_LENGTH | ORDINAL_POSITION | IS_NULLABLE | SCOPE_CATLOG | SCOPE_SCHEMA | SCOPE_TABLE | SOURCE_DATA_TYPE |
|-----------+-------------+------------+-------------+-----------+-----------+-------------+---------------+----------------+----------------+----------+---------+------------+---------------+------------------+-------------------+------------------+-------------+--------------+--------------+-------------+------------------|
|           |             |    cyclist |          id |         4 |   INTEGER |  2000000000 |    2000000000 |             10 |             10 |        0 |         |            |             0 |                0 |        2000000000 |                0 |          NO |              |              |             |                  |
|           |             |    cyclist |        name |        12 |      TEXT |  2000000000 |    2000000000 |             10 |             10 |        0 |         |            |             0 |                0 |        2000000000 |                1 |          NO |              |              |             |                  |
|           |             |    cyclist |     is_male |         4 |   INTEGER |  2000000000 |    2000000000 |             10 |             10 |        0 |         |            |             0 |                0 |        2000000000 |                2 |          NO |              |              |             |                  |
 1 > select * from award limit 5;

|  id |                       name | level |
|-----+----------------------------+-------|
| 615 |             Re-unification |     5 |
| 616 |             World traveler |     5 |
| 617 | Celebrity guest appearance |     5 |
| 618 |                  New Rider |     4 |
| 619 |            Most Consistent |     4 |
 1 > tg gui
gui: false -> true
1 > select * from award limit 10;
10 row(s) affected (0.828s)
```

The last command produces the following GUI results window:

![GUI Results](https://plandes.github.io/cisql/img/results.png)

```sql
 1 > orph awards
 1 > select * from award limit 20;
20 row(s) affected (0.037s)
 1 > export /d/awards.csv
20 row(s) affected (0.014s)
```
The last command creates a new `.csv` spreadsheet file shown below:

![Spreadsheet .csv](https://plandes.github.io/cisql/img/spreadsheet-export.png)


### Command Line Usage

The command line usage is given below for convenience.
```sql
usage: cisql [options]

Clojure Interactive SQL (cisql) v0.0.12
(C) Paul Landes 2015 - 2017

Start an interactive session
  -l, --level <log level>       INFO       Log level to set in the Log4J2 system.
  -n, --name <product>                     DB implementation name
  -u, --user <string>                      login name
  -p, --password <string>                  login password
  -h, --host <string>           localhost  database host name
  -d, --database <string>                  database name
      --port <number>                      database port
  -c, --config <k1=v1>[,k2=v2]             set session configuration
      --repl <number>                      the port bind for the repl server
```

## Documentation

API [documentation](https://plandes.github.io/cisql/codox/index.html).


## Changelog

An extensive changelog is available [here](CHANGELOG.md).


## License

Copyright © 2017-2018 Paul Landes

Permission is hereby granted, free of charge, to any person obtaining a copy of
this software and associated documentation files (the "Software"), to deal in
the Software without restriction, including without limitation the rights to
use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
of the Software, and to permit persons to whom the Software is furnished to do
so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.


<!-- links -->
[cisql]: https://github.com/plandes/cisql
[travis-link]: https://travis-ci.org/plandes/cisql
[travis-badge]: https://travis-ci.org/plandes/cisql.svg?branch=master
