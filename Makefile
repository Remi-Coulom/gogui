#------------------------------------------------------------------------------
# $Id$
# $Source$
#------------------------------------------------------------------------------

VERSION=0.1.x

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

release: version
	mkdir -p build
	javac -O -deprecation -sourcepath . -source 1.4 -d build @files.txt
	mkdir -p build/doc
	cp -R doc/html/* build/doc
	test -d build/icons || mkdir -p build/icons
	mkdir -p build/org/javalobby/icons/20x20png
	for i in $(ICONS); do cp $$i build/$$i; done 
	jar cmf manifest-addition.txt gogui.jar -C build .

# Run with 'jdb -classpath build_dbg -sourcepath src GoGui'
debug: version
	mkdir -p build_dbg
	javac -g -deprecation -sourcepath . -source 1.4 -d build_dbg @files.txt
	mkdir -p build_dbg/doc
	mkdir -p build_dbg/icons
	mkdir -p build_dbg/org/javalobby/icons/20x20png
	for i in $(ICONS); do cp $$i build_dbg/$$i; done 
	cp -R doc/html/* build_dbg/doc

version:
	sed 's/m_version = \".*\"/m_version = \"$(VERSION)\"/' <src/gui/Version.java >src/gui/.Version.java.new
	mv src/gui/.Version.java.new src/gui/Version.java

.PHONY: gmptogtp

gmptogtp:
	test -d build-gmptogtp || mkdir build-gmptogtp
	javac -O -deprecation -sourcepath . -source 1.4 -d build-gmptogtp @files-gmptogtp.txt
	jar cmf manifest-addition.txt gmptogtp.jar -C build-gmptogtp .

.PHONY: srcdoc clean doc changelog tags

clean:
	-rm -r build build_dbg

doc:
	echo "$(VERSION)" >doc/xml/version.xml
	$(MAKE) -C doc

docsrc:
	javadoc -sourcepath src -d docsrc -source 1.4 $(PACKAGES)

changelog:
	cvs2cl.pl

tags:
	etags `find . -name "*.java"`
