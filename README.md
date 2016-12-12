# SQL CLI Interface

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


## Documentation

Additional [documentation](https://plandes.github.io/cisql/codox/index.html).


## Usage

```sql
$ java -jar target/cisql-0.0.10-standalone.jar --subprotocol sqlite --database awards.sqlite --config 'linesep=;,gui=false'
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
 1 > help
Clojure Interactive SQL (cisql) v0.0.10
(C) Paul Landes 2015 - 2017
commands:
cf <variable value>  configure (set) a 'variable' to 'value' (ie 'tg gui')
sh [variable]        show 'variable', or show them all if not given
tg <variable>        toggle a boolean variable
shtab [table]        show table metdata or all if no table given
orph [label]         orphan (spawn new next) window in GUI mode
cfcat <catalog>      configure (set) the database (like 'use <db name>')
export <filename>    export the last query as a CSV file

variables:
gui:                whether or not to use a graphical window to display result sets
linesep:            tell where to end a query and then send
prompt:             a format string for the promp
errorlong:          if true provide more error information
loglev:             log level of the program (error, warn, info (default), debug, trace)
 1 > orph awards
 1 > select * from award limit 20;
20 row(s) affected (0.037s)
 1 > export /d/awards.csv
20 row(s) affected (0.014s)
```

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
