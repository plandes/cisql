# Change Log
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/)
and this project adheres to [Semantic Versioning](http://semver.org/).


## [Unreleased]


## [0.0.22] - 2021-06-05
### Changed
- Bug fix using GUI results on a fresh install.


## [0.0.21] - 2021-03-04
### Changed
- Add built in variables for adding *fudge* window space to allow extra pixels
  since every OS approximates the window real estate slightly differently.


## [0.0.20] - 2021-01-14
### Changed
- nRepl bump for newer version of cider integration.


## [0.0.19] - 2020-11-09
### Added
- Add back command line help in docs.
### Changed
- Fixed eval bug.


## [0.0.18] - 2019-07-02
### Added
- Add `rowcount` variable, which limits number of returned rows.
- Add `run` directive, which executes a file as if given on the command line.
- Add `print` directive, which is useful when used with the `run` directive.


## [0.0.17] - 2019-06-28
### Added
- Send directive to allow verbatim SQL to the database.
- Allow variable number of arguments functions defined in loaded files.
- Directive plugin system, which enables users to write their own directives.
### Changed
- Allow any function to access result sets in database access library
  `db_access.clj`.


## [0.0.16] - 2019-06-23
### Added
- Add the `clear` directive to clear the query.
### Changed
- Changed variable `guiwin` to `headless`.
- Fixed GUI window/headless popup ignore `headless` variable.


## [0.0.15] - 2019-06-23
### Added
- Adding CHANGELOG.md and lots of doc.
- Connection in session.
- Moving to lein-git-version 1.2.7.
- Interrupt long blocking queries.
- Handle multiple maven coordinates for driver dependencies.
- Clojure evaluation of result set data (`load` and `eval` directives).
- Basic macros (`do` directive).
- Online help (`man` directive).

### Changed
- Refactor parsing/processing.
- Move to instaparse for directive parsing.
- URL centric JDBC connection configuration.
- Better variable handling and add `strict`.
- Upgrade to Cider 0.21.0 and nREPL.
- Upgrade to Clojure 1.9.


## [0.0.12] -2017-01-27
### Added
- Add dynamic JDBC download and classloading
- Adding Travis build

### Changed
- Command event loop configuration changes.


## [0.0.11] - 2016-12-16
### Added
- First real release
- Retrofit to zensols libraries (ie tabres) and build
- README Documentation

### Changed
- Build


[Unreleased]: https://github.com/plandes/cisql/compare/v0.0.22...HEAD
[0.0.22]: https://github.com/plandes/cisql/compare/v0.0.21...v0.0.22
[0.0.21]: https://github.com/plandes/cisql/compare/v0.0.20...v0.0.21
[0.0.20]: https://github.com/plandes/cisql/compare/v0.0.19...v0.0.20
[0.0.19]: https://github.com/plandes/cisql/compare/v0.0.18...v0.0.19
[0.0.18]: https://github.com/plandes/cisql/compare/v0.0.17...v0.0.18
[0.0.17]: https://github.com/plandes/cisql/compare/v0.0.16...v0.0.17
[0.0.16]: https://github.com/plandes/cisql/compare/v0.0.15...v0.0.16
[0.0.15]: https://github.com/plandes/clj-mkproj/compare/v0.0.12...v0.0.15
[0.0.12]: https://github.com/plandes/clj-mkproj/compare/v0.0.12...v0.0.13
[0.0.11]: https://github.com/plandes/clj-mkproj/compare/v0.0.10...v0.0.11
