## makefile automates the build and deployment for lein projects

# type of project, currently one of: clojure, python
PROJ_TYPE=		clojure
# make modules to add functionality to a build
PROJ_MODULES=		appassem release

# use clojure 10
UBER_JAR_PROFS +=	with-profile +1.10
APP_INST_DIR =		$(HOME)/opt/app/cisql

include ./zenbuild/main.mk

.PHONY:	test
test:
	$(LEIN) test

.PHONY:	repl
repl:
	$(LEIN) with-profile +ciderrepl run --cider 32345 --config 'gui=false'
