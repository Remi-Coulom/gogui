DOCS= \
  analyze.html \
  compatibility.html \
  index.html \
  interrupt.html

ICONS= \
  icons/NewBoard.png \
  org/javalobby/icons/20x20png/Computer.png \
  org/javalobby/icons/20x20png/Delete.png \
  org/javalobby/icons/20x20png/Enter.png \
  org/javalobby/icons/20x20png/Left.png \
  org/javalobby/icons/20x20png/Gearwheel.png \
  org/javalobby/icons/20x20png/Home.png \
  org/javalobby/icons/20x20png/New.png \
  org/javalobby/icons/20x20png/Open.png \
  org/javalobby/icons/20x20png/Right.png \
  org/javalobby/icons/20x20png/Save.png \
  org/javalobby/icons/20x20png/Stop.png \
  org/javalobby/icons/20x20png/VCRBack.png \
  org/javalobby/icons/20x20png/VCRBegin.png \
  org/javalobby/icons/20x20png/VCRFastForward.png \
  org/javalobby/icons/20x20png/VCRForward.png \
  org/javalobby/icons/20x20png/VCREnd.png \
  org/javalobby/icons/20x20png/VCRRewind.png

PACKAGES= \
  gmp \
  go \
  gtp \
  gui \
  sgf \
  utils

release:
	test -d build || mkdir build
	javac -O -deprecation -sourcepath . -source 1.4 -d build @files.txt
	test -d build/doc || mkdir build/doc
	for d in $(DOCS); do cp doc/$$d build/doc/$$d; done 
	test -d build/icons || mkdir -p build/icons
	test -d build/org/javalobby/icons/20x20png \
	|| mkdir -p build/org/javalobby/icons/20x20png
	for i in $(ICONS); do cp $$i build/$$i; done 
	jar cmf manifest-addition.txt gogui.jar -C build .

# Run with 'jdb -classpath build_dbg -sourcepath src GoGui'
debug:
	test -d build_dbg || mkdir build_dbg
	javac -g -deprecation -sourcepath . -source 1.4 -d build_dbg @files.txt
	test -d build_dbg/doc || mkdir build_dbg/doc
	test -d build_dbg/icons || mkdir -p build_dbg/icons
	test -d build_dbg/org/javalobby/icons/20x20png \
	|| mkdir -p build_dbg/org/javalobby/icons/20x20png
	for i in $(ICONS); do cp $$i build_dbg/$$i; done 
	for d in $(DOCS); do cp doc/$$d build_dbg/doc/$$d; done 

.PHONY: gmptogtp

gmptogtp:
	test -d build-gmptogtp || mkdir build-gmptogtp
	javac -O -deprecation -sourcepath . -source 1.4 -d build-gmptogtp @files-gmptogtp.txt
	jar cmf manifest-addition.txt gmptogtp.jar -C build-gmptogtp .

.PHONY: srcdoc clean changelog tags

clean:
	-rm -r build build_dbg

docsrc:
	javadoc -sourcepath src -d docsrc -source 1.4 $(PACKAGES)

changelog:
	cvs2cl.pl

tags:
	etags `find . -name "*.java"`
