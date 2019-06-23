# Change Log
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/)
and this project adheres to [Semantic Versioning](http://semver.org/).


## [Unreleased]

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


[Unreleased]: https://github.com/plandes/cisql/compare/v0.0.13...HEAD
[0.0.12]: https://github.com/plandes/clj-mkproj/compare/v0.0.12...v0.0.13
[0.0.11]: https://github.com/plandes/clj-mkproj/compare/v0.0.10...v0.0.11
