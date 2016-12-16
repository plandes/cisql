## makefile automates the build and deployment for lein projects

APP_SCR_NAME=	cisql
#DIST_UJAR_NAME=	$(ANRRES).jar

# location of the http://github.com/plandes/clj-zenbuild cloned directory
ZBHOME=		../clj-zenbuild

all:		distuber

include $(ZBHOME)/src/mk/compile.mk
include $(ZBHOME)/src/mk/dist.mk
