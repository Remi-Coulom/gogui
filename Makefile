#------------------------------------------------------------------------------
# $Id$
# $Source$
#------------------------------------------------------------------------------

VERSION=0.2.1.x

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
	mkdir -p build/gogui
	javac -O -deprecation -sourcepath . -source 1.4 -d build/gogui @files.txt
	mkdir -p build/gogui/doc
	cp -R doc/html/* build/gogui/doc
	mkdir -p build/gogui/images
	for i in $(IMAGES); do cp $$i build/gogui/$$i; done 
	jar cmf manifest-addition.txt gogui.jar -C build/gogui .

# Run with 'jdb -classpath build_dbg -sourcepath src GoGui'
debug: version
	mkdir -p build/gogui_debug
	javac -g -deprecation -sourcepath . -source 1.4 -d build/gogui_debug @files.txt
	mkdir -p build/gogui_debug/doc
	mkdir -p build/gogui_debug/images
	for i in $(IMAGES); do cp $$i build/gogui_debug/$$i; done 
	cp -R doc/html/* build/gogui_debug/doc

version:
	sed 's/m_version = \".*\"/m_version = \"$(VERSION)\"/' <src/gui/Version.java >src/gui/.Version.java.new
	mv src/gui/.Version.java.new src/gui/Version.java

.PHONY: gmptogtp

gmptogtp:
	mkdir -p build/gmptogtp
	javac -O -deprecation -sourcepath . -source 1.4 -d build/gmptogtp @files-gmptogtp.txt
	jar cmf manifest-addition.txt gmptogtp.jar -C build/gmptogtp .

.PHONY: srcdoc clean doc changelog tags

clean:
	-rm -r build

doc:
	echo "$(VERSION)" >doc/xml/version.xml
	$(MAKE) -C doc

docsrc:
	javadoc -sourcepath src -d docsrc -source 1.4 $(PACKAGES)

changelog:
	cvs2cl.pl

tags:
	etags `find . -name "*.java"`
