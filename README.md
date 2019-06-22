# SQL Command Line Interface

[![Travis CI Build Status][travis-badge]][travis-link]

This program provides a command line interface to interacting with relational
database managements systems (RDMBs) and currently includes SQLite, MySQL and
PostgreSQL out of the box.  Additional JDBC drivers can easily be added in a
(somewhat incomplete) plugin system.

Features include:

* Multiple GUI tabulation frames for query results.
* Use any JDBC driver to connect almost any database.
* Evaluate Clojure code as input directly SQL queries with JVM in memory
  objects that come directly from the `java.sql.ResultSet`.
* JDBC drivers are configured in the command event loop application and
  downloaded and integrated without having to restart.
* Persist result sets as a `.csv` file.
* Emacs interaction with the [ciSQL] library.
* Primitive variable setting (controls GUI interface, logging, etc).
* Distribution is a one Java Jar file with all dependencies.


<!-- markdown-toc start - Don't edit this section. Run M-x markdown-toc-refresh-toc -->
## Table of Contents

- [Obtaining](#obtaining)
- [Usage](#usage)
    - [Queries and Directives](#queries-and-directives)
    - [Variables](#variables)
    - [Graphical Results](#graphical-results)
    - [Evaluation](#evaluation)
- [Database Access](#database-access)
    - [Connecting to a Database](#connecting-to-a-database)
    - [Installing new JDBC Drivers](#installing-new-jdbc-drivers)
        - [SQLite](#sqlite)
        - [Apache Drill](#apache-drill)
    - [Querying the database](#querying-the-database)
- [Emacs Integration](#emacs-integration)
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
(C) Paul Landes 2015 - 2019
 1 >
```


### Queries and Directives

Each line of input is either is a part or whole SQL query, or it is a
`directive`.  A `directive` includes commands meant for the program itself.
There are some directives that take queries as (usually optional) input like
the `export` directive.  You can get a list of directives and how to use them
using the `help` directive.

Every SQL query given is stored even after the results are retrieved from the
database and displayed.  This query is referred to as the *last query* and is
used in many directives like the `export` directive as mentioned.

**Important**: Those queries entered without an ending line separator
(i.e. `;`) used with a directive on a new line are not recorded as the *last
query*.

For example, the following code prints the `coder` column of the table in one
line:
```sql
select * from coders
eval (fn [r _] (println (map #(get % "coder") r)))
```

However, if you had used just (notice the ending line separator (`;`)):
```sql
select * from coders;
```

**then** you used the `eval` directive:
```clojure
eval (fn [r _] (println (map #(get % "coder") r)))
```
it would produce the same output because it uses the select used on the single
line.  In this way, you can *up arrow* as many times as you want without having
to produce different SQL.  For the `eval` directive, this is very useful as you
*debug* your directive.

To get a list of directives, use the `help` directive.


### Variables

The program keeps track of variables across sessions, which is persisted in
using the Java persistence framework.  You can set a variable with the `set`
directive.  For example, to set the prompt:
```sql
set prompt 'darkstar %1$s> '
```

**Important**: To add white space you can use quote (single quote `'`) for
verbatim values.

Certain variables are booleans like `gui`, which tells the program to show
results in a GUI frame.  These variables should be toggled with the `tg`
directive.

To list all variables, use the `sh` directive, which also takes a variable name
as a parameter if you want to see just one.


### Graphical Results

As mentioned in the [section on variables](#variables), use `tg gui` to switch
between using a GUI based frame to report results and a text based table in the
same command line window.

By default each query replaces the results of the last.  However, you can
create multiple windows to compare results by using the `orph` directive.  This
*orphans* the window from any further result reporting.  The directive takes a
`label` argument, which is used in the frame title.


### Evaluation

You can "pipe" the results of queries to your own custom Clojure code.  The
easiest way to do this is using the `eval` directive as demonstrated in the
[queries and directives](#queries-and-directives) section.  In addition, the
`load` directive reads a Clojure file and uses the defined function to process
the query results.  Like the `eval` directive, this function takes the
following parameters:
* **rows**: a lazy sequence of maps, each map is a key/value set of column
  name to value.
* **header**: a list of string column names.


#### Loading a File

This example adds the string `Mrs` to each row for the `coder` column:
```clojure
(ns temporary
  (:require [clojure.tools.logging :as log]))

(defn- process-query [rows header]
  (log/debugf "header: %s" (vec header))
  (let [col-name "coder"]
   (->> rows
        (map (fn [row]
               (assoc row col-name (str "Mrs " (get row col-name)))))
        (array-map :header header :rows)
        (array-map :display))))
```

The following happens based on the output of this function:
* **nil**: nothing happens
* **a map with a :display key**: the `:header` and `:rows:` keys are used to
  display the results just like any query
* anything else: the value is printed

Observe that you can write your functions to `printlin` anything just like any
Clojure program and the output goes to the interactive window.

**Important**: The function to evaluate must be the last symbolic expression in
the file.


#### Evaluation Directive

Another more comprehensive example, which renames the `firstName` column to `name`:

```clojure
eval (fn [r h] {:display {:header ["name"] :rows (map #(array-map "name" (get % "firstName")) r)}})
```


### Bad State

If for any reason the program gets in a bad state, you can reset data to their
defaults.  The directives used for this are:

* **purgedrv** remove all JDBC driver configuration.
* **resetenv** reset all variables back to their initial values.
* **vaporize** clear all application data.

Of course you want to exercises extreme caution with these as they are very
destructive.


## Database Access

Since the program is written in a Java Virtual Machine language any JDBC driver
can be used.


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

For example, to connect to an *SQLite* database, use the following:
```sql
 1 > conn sqlite --database awards.sqlite
spec: loading dependencies for [[org.xerial/sqlite-jdbc "3.8.11.2"]]
configured jdbc:sqlite:awards.sqlite
```

To connect to a *mySql* database:
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


## Emacs Integration

If you're an Emacs user, the [ciSQL] library is available, which integrates
with the [Emacs SQL system](#https://www.emacswiki.org/emacs/SqlMode).


## Documentation

This program is written in Clojure.  See the API
[documentation](https://plandes.github.io/cisql/codox/index.html).

If you integrate this program with you own, please let me know.  I'm interested
in knowing how others use it.


## Changelog

An extensive changelog is available [here](CHANGELOG.md).


## License

Copyright © 2017-2019 Paul Landes

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
[ciSQL]: https://github.com/plandes/icsql
[travis-link]: https://travis-ci.org/plandes/cisql
[travis-badge]: https://travis-ci.org/plandes/cisql.svg?branch=master
