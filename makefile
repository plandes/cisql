## makefile automates the build and deployment for lein projects

# type of project, currently one of: clojure, python
PROJ_TYPE=		clojure
# make modules to add functionality to a build
PROJ_MODULES=		appassem release
# cheat install
APP_INST_DIR =		$(HOME)/opt/app/cisql/cisql.jar

include ./zenbuild/main.mk

.PHONY:	test
test:
	$(LEIN) test

.PHONY:	repl
repl:
	$(LEIN) with-profile +ciderrepl run --cider 32345 --config 'gui=false'
