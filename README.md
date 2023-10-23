# SQL Command Line Interface

[![Travis CI Build Status][travis-badge]][travis-link]

This program provides a command line interface to interacting with relational
database managements systems (RDMBs) and currently includes SQLite, MySQL and
PostgreSQL out of the box.  Additional JDBC drivers can easily be added on the
command line.

Features include:

* Multiple [GUI tabulation](#graphical-results) frames for query results.
* Use any [JDBC driver](#database-access) to connect almost any database.
* [Evaluate](#evaluation) of Clojure code as input directly SQL queries with JVM
  in memory objects that come directly from the `java.sql.ResultSet`.
* [Macros](#macros) that save you typing commands and queries over and over.
* JDBC drivers are [configured](#installing-new-jdbc-drivers) in the command
  event loop application and downloaded and integrated without having to
  restart.
* [Export](#queries-and-directives) result sets as a `.csv` file.
* Emacs interaction with the [ciSQL] library.
* Configuration via command line [persistent variables](#variables) to controls
  GUI interface, logging, etc.
* Distribution is a [stand alone Java jar file] with all dependencies.
* Data base [meta data](#database-meta-data) access as results and to Clojure
  programs.

<!-- markdown-toc start - Don't edit this section. Run M-x markdown-toc-refresh-toc -->
## Table of Contents

- [Obtaining](#obtaining)
- [Usage](#usage)
    - [Online Help](#online-help)
    - [Queries and Directives](#queries-and-directives)
        - [Multi-line Queries](#multi-line-queries)
        - [Send Verbatim](#send-verbatim)
        - [Row Count](#row-count)
        - [Print Directive](#print-directive)
    - [Variables](#variables)
        - [Boolean Variables](#boolean-variables)
        - [Built-in and User Variables](#built-in-and-user-variables)
        - [Variable Substitution](#variable-substitution)
    - [Macros](#macros)
    - [Graphical Results](#graphical-results)
    - [Database Meta Data](#database-meta-data)
    - [Evaluation](#evaluation)
        - [Loading a File](#loading-a-file)
        - [Evaluation Directive](#evaluation-directive)
        - [Program API Access](#program-api-access)
        - [Plugins](#plugins)
    - [Run SQL Offline](#run-sql-offline)
    - [Start Up Execution Resource File](#start-up-execution-resource-file)
    - [Bad State](#bad-state)
    - [Command Line Help](#command-line-help)
- [Database Access](#database-access)
    - [Connecting to a Database](#connecting-to-a-database)
    - [Installing new JDBC Drivers](#installing-new-jdbc-drivers)
        - [SQLite](#sqlite)
        - [Apache Drill](#apache-drill)
    - [Querying the Database](#querying-the-database)
    - [Removing JDBC Drivers](#removing-jdbc-drivers)
- [Emacs Integration](#emacs-integration)
- [Documentation](#documentation)
    - [API Documentation](#api-documentation)
- [Known Issues](#known-issues)
- [Changelog](#changelog)
- [License](#license)

<!-- markdown-toc end -->


## Obtaining

The latest release binaries are available as a [stand alone Java jar file].


## Usage

The program is a command line console application that provides a prompt in a
command event loop.  Each text typed followed by **return** is interpreted as
SQL to be sent to the database or a `directive` (see [queries-and-directives]).

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

If you're in a hurry, you can skip to the section
[connecting](#database-access) to the database.


### Online Help

Typing `help` at the command line gives a list of
[directives](#queries-and-directives) and [variables](#variables).  To get help
additional help for directives, type `help <directive name>`.


### Queries and Directives

Each line of input is either is a part or whole SQL query, or it is a
`directive`.  A `directive` includes commands meant for the command event loop
of the program.  There are some directives that take queries as (usually
optional) input like the `export` directive.  You can get a list of directives
and how to use them using the `help` directive.


#### Multi-line Queries

Every SQL query given is stored even after the results are retrieved from the
database and displayed.  In this way you can build queries that contain several
lines. This multi-lined query is referred to as the *last query* and is used in
many directives like the `export` directive as mentioned.

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

To purge any previous query use the `clear` directive.  To get a list of
directives, use the `help`.


#### Send Verbatim

The `send` directive is used to send SQL directly to the database and bypasses
all directive special.  For example, some databases have the `set` reserved
word, which is a directive in ciSQL.  To use `set` but `send` in front as such:
```sql
send SET :HVL = CURRENT PATH;
```

Note that this directive does not support multi-line queries. so you *must* add
the line separator (usually `;`) and add no new lines.


#### Row Count

The special variable `rowcount` determines how many rows are returned from all
queries, **which includes meta data**.  The default is not value so all results
come back.  Note that this proceeds any `limit` or `top N` constraints on your
query.

It's important to remember to unset this when you're finished so the program
doesn't give unexpected truncated results when constraining the query or
getting database metadata.  For this reason, the program treats this as more of
a temporary usage and it is up to the user to reset the variable to the *none*
value, which can be accomplished as such:
```sql
set rowcount
```

#### Print Directive

The `print` directive prints a message and utilizes [variable
substitution](#variable-substitution) like any other query or directive.
This directive is useful for instances when executing [offline
SQL](#run-sql-offline).


### Variables

The program keeps track of variables across sessions, which is persisted in
using the Java persistence framework.  You can set a variable with the `set`
directive.  For example, to set the prompt:
```sql
set prompt 'darkstar %1$s> '
```

**Important**: To add white space you can use quote (single quote `'`) for
verbatim values.

Some built in variables can be set to a *none* value like `catalog`, which
means to not set a connection's catalog.  To set a variable to the *none* value
simply omit the value, such as:
```sql
set catalog
```

You can also remove one or more variables with the `rm` directive such as:
```sql
rm v1 v2
```


#### Boolean Variables

Certain variables are booleans like `gui`, which tells the program to show
results in a GUI frame.  These variables should be toggled with the `tg`
directive.

To list all variables, use the `sh` directive, which also takes a variable name
as a parameter if you want to see just one.


#### Built-in and User Variables

There are two kinds of variables:

* built ins: these are variables that come predefined and control the behavior
  of the program
* user: these are variables the user can add.

The special variable `strict` controls whether only built in variables can be
set and be default is off to disallow user variables.  This is to void
misspelling variables that are often modified.

When you turn this off (use `set strict false`), user variables can be added
using the `set` and `tg` directives.

User defined variables can be unset/removed with the `rm` directive.


#### Variable Substitution

Any series of characters and numbers that are preceded by `@@` are substituted
by that variable's value.  For example:
```sql
 1 > set mytable items
mytable: null -> items
 1 > print 'my table is @@mytable'
my table is items
 1 > select * from @@mytable;
executing: select * from items
731 row(s) affected (0.019s)
```


### Macros

A crude macro system is available with the `do` directive, which takes a list
of [user variable](#built-in-and-user-variables) names as input.  For each
variable name given, it invokes the contents of the value of the variable as if
it were given on the command event loop of the program.  For example:
```sql
set sela 'select * from annotations limit 5;'
set selc 'select * from coders;'
set con 'conn sqlite -d annotated-corpus.db'
do con sela selc
```
First connects to an SQLite database, and executes two `select` statements.


### Graphical Results

As mentioned in the [section on variables](#variables), use `tg gui` to switch
between using a GUI based frame to report results and a text based table in the
command event loop of the program.  This setting produces a graphical results
that are much easier to view and handles large result sets efficiently.  An
example of a GUI results set frame follows.

![GUI Results](https://plandes.github.io/cisql/img/results.png)

By default each query replaces the results of the last.  However, you can
create multiple windows to compare results by using the `orph` directive.  This
*orphans* the window from any further result reporting.  The directive takes a
`label` argument, which is used in the frame title.

When the variable `headless` is set to `false` then a separate application window
starts when results are available for graphical display.


### Database Meta Data

The `shtab` displays all table meta data in the database or the meta data for a
table if the table name is given.  However, meta data is also available to
directives that take SQL result set output by using a special syntax:
```sql
select @meta;
```

A table is specified before the `metadata` keyword to get a table meta data
result set.:
```sql
select @some_table.meta;
```

Note that the query terminator (`;` in this case) still needs to be present.


### Evaluation

You can "pipe" the results of queries to your own custom Clojure code.  The
easiest way to do this is using the `eval` directive as demonstrated in the
[queries and directives](#queries-and-directives) section.


#### Loading a File

In addition, the `load` directive reads a Clojure file and uses the defined
function to process the query results.  Like the `eval` directive, this
function takes the following parameters:
* **rows**: a lazy sequence of maps, each map is a key/value set of column
  name to value.
* **header**: a list of string column names.

However, functions defined in the loaded file can omit these arguments.  If
only one is given the **rows** are passed as the singleton argument.  If no
arguments are defined in the function the query is not invoked and a query need
not be given.

The `load` directive takes two optional arguments: the file to load and
function to call in the file.  The file defaults to `cisql.clj` and the
function to call defaults to the last function in the evaluated file.

This example adds the string `Mrs` to each row for the `coder` column (say this
is in a file called `fix-columns.clj`:
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

To run from the program, invoke:
```sql
select * from coders;
load fix-columns.clj
```

The following happens based on the output of this function:
* **nil**: nothing happens
* **a map with a :display key**: the `:header` and `:rows:` keys are used to
  display the results just like any query
* anything else: the value is printed

You can write your functions to `printlin` anything just like any Clojure
program and the output goes to the interactive window.  Direct access to a
running SQL prompt with an [Emacs Cider session](#emacs-integration) is also
available.  You can also clone this repo, add your own Clojure files and
[Cider] debug your code.  Currently this isn't possible with loaded files since
they're read and evaluated.


#### Evaluation Directive

Another more comprehensive example, which renames the `firstName` column to `name`:

```clojure
eval (fn [r h] {:display {:header ["name"] :rows (map #(array-map "name" (get % "firstName")) r)}})
```


#### Program API Access

Note that you also have access to the program API.  For example, to access the
variables, use the `zensols.cisql.conf` name space.  This example uses sets and
prints a variable with the names of columns and added to file `table-meta.clj`:
```clojure
(ns some-ns
  (:require [clojure.tools.logging :as log]
            [zensols.cisql.conf :as conf]))

(defn- set-table-names [rows _]
  (->> rows
       (map #(get % "TABLE_NAME"))
       vec
       (conf/set-config :table-names))
  "set table-names variable")

(defn- read-table-names [$ _]
  (format "%d tables" (count (conf/config :table-names))))
```

To run:
```sql
select from @@metadata;
load table-meta.clj set-table-names
sh table-names
load table-meta.clj read-table-names
```


#### Plugins

Say you write a [Clojure load file](#loading-a-file) you use and want to save
typing.  You can write a *plugin* that creates a new directive so you don't
have to use the `load` directive each time.

The file needs the following:
* **namespace**: the file needs the standard namespace definition.  The
  namesapce itself can be anything, but shouldn't be any exiting ciSQL
  namespace.
* **dependencies variable**: this is optional unless your plugin depends on
  other jars to be loaded; this variable should be a map with the following
  keys:
  * **coordinates**: a sequence of the maven repository coordinates, for example:
  `{:coordinates [[us.fatehi/schemacrawler "15.06.01"]]}`
  * **repositories**: an optional sequence of repositories in the format `{name
    url}`
* **directives**: This is the definition of the directive itself.  This is a
  map with the following keys:
  * **arg-count**: either a number indicating the number of arguments or a
    regular expression charater (i.e. `*`, `+`, etc) representing the number
    of arguments.
  * **usage**: a usage string giving a human readable description of the
    arguments it takes.
  * **desc**: a human readable description of documentation for the directive.
  * **fn**: a function taking the query information map and arguments (see
    examples).

See the [webcrawler](src/plugins/schema-crawler.clj) plugin for an example.  In
addition you can see the [built-in-directives
function](src/clojure/zensols/cisql/directive.clj) for more examples.

Use the `plugin` with the file or directory.  If the given path is a directory,
all files in that directory are loaded and assumed to be Clojure source files.
For this reason every file in the specified directory must follow the plugin
format given in this section.  Otherwise the plugin registration will fail.


### Run SQL Offline

You can execute a series of queries and/or directives as if they were given
directly on the command line from a file.  The `run` directive takes a one or
more files that are executed on the command line.


### Start Up Execution Resource File

The program looks for and executes the contents of `~/.cisql` if it exists just
as if the `run` directive was used (see the `run`
[directive](#run-sql-offline)).


### Bad State

If for any reason the program gets in a bad state, you can reset data to their
defaults.  The directives used for this are:

* **purgedrv** remove all JDBC driver configuration.
* **resetenv** reset all variables back to their initial values.
* **vaporize** clear all application data.

Of course you want to exercises extreme caution with these as they are very
destructive.


### Command Line Help

The command line usage for convenience is given here:

```sql
usage: cisql [options]

Clojure Interactive SQL (cisql) v0.0.18 
(C) Paul Landes 2015 - 2019

Connect to a database
  -n, --name <name>                        JDBC driver name (ex: mysql)
  -l, --level <log level>       INFO       Log level to set in the Log4J2 system.
  -u, --user <string>                      login name
  -p, --password <string>                  login password
  -h, --host <string>           localhost  database host name
  -d, --database <string>                  database name
      --port <number>                      database port
  -c, --config <k1=v1>[,k2=v2]             set session configuration
```


## Database Access

Since the program is written in a Java Virtual Machine language any JDBC driver
can be used.


### Connecting to a Database

The connection usage is the same in the event loop and on the command line.  In
the event loop you can use the `conn` directive:

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
*configuration* settings for popular databases and are configured in the
[driver resource](resources/driver.csv).  The system uses the [maven
repository] system and will automatically download and use the new driver
without having to exit and restart the program.

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

Note that you can add multiple maven coordinates separated with a comma and no
space, which is useful when you need to add license files.  In the case of
license files that have no maven coordinates, you'll have to install the them
as maven files yourself directly.  You can do this using the [maven install
plugin] yourself or use the [install-maven-file](src/sh/install-maven-file)
PERL script, which provides slightly nicer syntax and help.


#### SQLite

```sql
1> newdrv -n sqlite -c org.sqlite.JDBC -u jdbc:sqlite:%5$s -d org.xerial/sqlite-jdbc/3.25.2
```


#### Apache Drill

The following installs the direct drill bit JDBC driver:

```sql
1> newdrv -n drilldir -u jdbc:drill:drillbit=%3$s:%4$s -p 31010 -c org.apache.drill.jdbc.Driver -d org.apache.drill.exec/drill-jdbc/1.10.0
```


### Querying the Database

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

The last command produces the GUI results window given in the [graphical result
set](#graphical-results) section.


```sql
 1 > orph awards
 1 > select * from award limit 20;
20 row(s) affected (0.037s)
 1 > export /d/awards.csv
20 row(s) affected (0.014s)
```
The last command creates a new `.csv` spreadsheet file as shown:

![Spreadsheet.csv](https://plandes.github.io/cisql/img/spreadsheet-export.png)


### Removing JDBC Drivers

Use the `removedrv` to remove a JDBC driver.  Note this only removes the entry
in the configuration and not the driver jar in the maven repository on disk.


## Emacs Integration

If you're an Emacs user, the [ciSQL] library is available, which integrates
with the [Emacs SQL system](#https://www.emacswiki.org/emacs/SqlMode).

In addition, you can start a [Cider] REPL using the `repl` directive while the
program is running and evaluating SQL statements and directives.  This provides
an even deeper way of integrating data base access with Clojure.  See the
[evaluation](#evaluation) section for other ways of integration.


## Documentation

The command event loop of the program provides a `man` directive to go to
documentation about a specific directive or variable, which is this page.  For
example:

```sql
man conn
```

Starts a web browser that goes to this section.


### API Documentation

This program is written in Clojure.  See the API
[documentation](https://plandes.github.io/cisql/codox/index.html).

If you integrate this program with you own, please let me know.  I'm interested
in knowing how others use it.


## Known Issues

The default version of the SQLite driver does not work under macOS M1 chips.
To fix this, use the following command to install a version that works:
```sql
removedrv sqlite
newdrv -n sqlite -c org.sqlite.JDBC -u jdbc:sqlite:%5$s -d org.xerial/sqlite-jdbc/3.41.2.1
```

There have been reports of the Clojure dependency resolver not downloading jars
correctly.  In that case, use the following command to install this jar:
```bash
mvn org.apache.maven.plugins:maven-dependency-plugin:2.10:get -Dartifact=org.xerial:sqlite-jdbc:3.41.2.1
```


## Changelog

An extensive changelog is available [here](CHANGELOG.md).


## License

Copyright © 2017-2021 Paul Landes

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

[maven repository]: https://mvnrepository.com
[maven install plugin]: https://maven.apache.org/guides/mini/guide-3rd-party-jars-local.html
[stand alone Java jar file]: https://github.com/plandes/cisql/releases/latest
[Cider]: https://github.com/clojure-emacs/cider
