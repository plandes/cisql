# SQL Command Line Interface [![Travis CI Build Status][travis-badge]][travis-link]

  [travis-link]: https://travis-ci.org/plandes/cisql
  [travis-badge]: https://travis-ci.org/plandes/cisql.svg?branch=master

This program provides a command line interface to interacting with relational
database managements systems (RDMBs) and currently includes SQLite, MySQL and
PostgreSQL out of the box.  Additional JDBC drivers can easily be added in a
(somewhat incomplete) plugin system.

Features include:

* A GUI tabulation presentation, which is handy for very large result sets.
* Emacs interaction via the standard `sql.el` library.
* Primitive variable setting (controls GUI interface, logging, etc).
* Persist result sets as a `.csv` file.
* Distribution is a one Java Jar file with all dependencies.


## Obtaining

The latest release binaries are
available [here](https://github.com/plandes/cisql/releases/latest).


## Documentation

Additional [documentation](https://plandes.github.io/cisql/codox/index.html).


## Usage

```sql
$ java -jar target/cisql.jar -n sqlite -d path/to/awards.sqlite --config 'linesep=;,gui=false'
Clojure Interactive SQL (cisql) v0.0.10
(C) Paul Landes 2015 - 2017
type 'help' to see a list of commands
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


### Connecting to a Database

You can specify command line arguments to connect to a database or you can
connect (and re-connect) while in the command event loop of the program.
The connection usage is the same in the event loop and on the command line:

```sql
1 > connect help
Start an interactive session
  -n, --name <product>                DB implementation name
  -u, --user <string>                 login name
  -p, --password <string>             login password
  -h, --host <string>      localhost  database host name
  -d, --database <string>             database name
      --port <number>                 database port
```

To connect to an *SQLite* database, use the following:
```sql
 1 > connect --name sqlite --database awards.sqlite
spec: loading dependencies for [[org.xerial/sqlite-jdbc "3.8.11.2"]]
configured jdbc:sqlite:awards.sqlite
```

Connect to a *mySql* database:
```sql
 1 > connect -n postgres -u puser -p pass -d puser -h 192.168.99.100
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

 1 > connect -n csv -d /Users/paul/stats-dir
spec: loading dependencies for [[net.sourceforge.csvjdbc/csvjdbc "1.0.28"]]
configured jdbc:relique:csv:/Users/paul/stats-dir

 1 > select count(*) from stat-file;
db-access: executing: select count(*) from stat-file

| COUNT(*) |
|----------|
|       41 |
```


## Changelog

An extensive changelog is available [here](CHANGELOG.md).


## License

Copyright © 2017 Paul Landes

Apache License version 2.0

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

[http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
