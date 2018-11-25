## makefile automates the build and deployment for lein projects

# type of project, currently one of: clojure, python
PROJ_TYPE=		clojure
# make modules to add functionality to a build
PROJ_MODULES=		appassem release

# make build dependencies
_ :=	$(shell [ ! -d .git ] && git init ; [ ! -d zenbuild ] && \
	  git submodule add https://github.com/plandes/zenbuild && make gitinit )

include ./zenbuild/main.mk

.PHONY:	test
test:
	$(LEIN) test

.PHONY:	repl
repl:
	$(LEIN) with-profile +ciderrepl run --cider 32345 --config 'gui=false'
