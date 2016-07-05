USER=		plandes
PROJ=		cisql
POM=		pom.xml
TARG=		target
DOC_DIR=	doc

all:		package

.PHONEY:
package:	$(TARG)

.PHONEY:
deploy:		clean
	lein deploy clojars

# https://github.com/weavejester/codox/wiki/Deploying-to-GitHub-Pages
$(DOC_DIR):
	rm -rf $(DOC_DIR) && mkdir $(DOC_DIR)
	git clone https://github.com/$(USER)/$(PROJ).git $(DOC_DIR)
	git update-ref -d refs/heads/gh-pages 
	git push --mirror
	( cd $(DOC_DIR) ; \
	  git symbolic-ref HEAD refs/heads/gh-pages ; \
	  rm .git/index ; \
	  git clean -fdx )
	lein codox

.PHONEY:
pushdoc:	$(DOC_DIR)
	( cd $(DOC_DIR) ; \
	  git add . ; \
	  git commit -am "new doc push" ; \
	  git push -u origin gh-pages )

$(TARG):
	lein jar

$(POM):		project.clj
	lein pom

clean:
	rm -fr dev-resources $(POM)* $(DOC_DIR) $(TARG)
	rmdir test 2>/dev/null || true
	rmdir resources 2>/dev/null || true
