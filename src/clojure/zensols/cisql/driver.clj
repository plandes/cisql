(ns ^{:doc "This package manages DB specs.  It also class loads and optionally
downloads the JDBC drivers."
      :author "Paul Landes"}
    zensols.cisql.driver
  (:require [zensols.actioncli.dynamic :refer (defa-) :as dyn])
  (:require [zensols.cisql.pref :as pref]))
