#------------------------------------------------------------------------------
# $Id$
# $Source$
#------------------------------------------------------------------------------

VERSION=0.2.x

IMAGES= \
  images/back.png \
  images/button_cancel.png \
  images/filenew.png \
  images/fileopen.png \
  images/filesave2.png \
  images/forward.png \
  images/gear.png \
  images/gohome.png \
  images/next.png \
  images/openterm.png \
  images/player_back.png \
  images/player_end.png \
  images/player_fwd.png \
  images/player_next.png \
  images/player_rew.png \
  images/player_start.png \
  images/stop.png \
  images/wood.png

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
	mkdir -p build/images
	mkdir -p build/org/javalobby/icons/20x20png
	for i in $(IMAGES); do cp $$i build/$$i; done 
	jar cmf manifest-addition.txt gogui.jar -C build .

# Run with 'jdb -classpath build_dbg -sourcepath src GoGui'
debug: version
	mkdir -p build_dbg
	javac -g -deprecation -sourcepath . -source 1.4 -d build_dbg @files.txt
	mkdir -p build_dbg/doc
	mkdir -p build_dbg/images
	mkdir -p build_dbg/org/javalobby/icons/20x20png
	for i in $(IMAGES); do cp $$i build_dbg/$$i; done 
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
