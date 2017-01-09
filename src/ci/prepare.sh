#!/bin/sh

sudo apt-get -yq update
sudo apt-get -yq install git
mkdir -p target
( cd target ; git clone http://github.com/plandes/clj-zenbuild )
