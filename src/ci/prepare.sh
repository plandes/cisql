#!/bin/sh

lein -version
mkdir -p target
( cd target ; git clone http://github.com/plandes/clj-zenbuild )
