#------------------------------------------------------------------------------
# $Id$
# $Source$
#------------------------------------------------------------------------------

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
  gmptogtp \
  go \
  gtp \
  gtpdummy \
  gtpregress \
  gtpserver \
  gui \
  netgtp \
  sgf \
  sgftotex \
  twogtp \
  utils \
  version

DOC= \
  doc/xml/analyze.xml \
  doc/xml/book.xml \
  doc/xml/bugs.xml \
  doc/xml/compatibility.xml \
  doc/xml/gpl.xml \
  doc/xml/gtpshell.xml \
  doc/xml/interrupt.xml \
  doc/xml/invocation.xml \
  doc/xml/news.xml \
  doc/xml/programs.xml \
  doc/xml/readme.xml \
  doc/xml/reference-gmptogtp.xml \
  doc/xml/reference-gogui.xml \
  doc/xml/reference-gtpregress.xml \
  doc/xml/reference-gtpserver.xml \
  doc/xml/reference-netgtp.xml \
  doc/xml/reference-twogtp.xml \
  doc/xml/tools.xml \
  doc/xml/version.xml

JAR= \
  gogui.jar \
  gtpdummy.jar \
  gtpregress.jar \
  gtpserver.jar \
  gmptogtp.jar \
  netgtp.jar \
  sgftotex.jar \
  twogtp.jar

JAVAOPT=-deprecation -sourcepath . -source 1.4

all: $(JAR)

gogui.jar: build/gogui/doc/index.html $(patsubst %, build/gogui/%, $(IMAGES)) $(shell cat build/files-gogui.txt)

gmptogtp.jar: $(shell cat build/files-gmptogtp.txt)

gtpdummy.jar: $(shell cat build/files-gtpdummy.txt)

gtpserver.jar: $(shell cat build/files-gtpserver.txt)

netgtp.jar: $(shell cat build/files-netgtp.txt)

gtpregress.jar: $(shell cat build/files-gtpregress.txt)

sgftotex.jar: $(shell cat build/files-sgftotex.txt)

twogtp.jar: $(shell cat build/files-twogtp.txt)

build/gogui/doc/index.html: doc/html/index.html
	mkdir -p build/gogui/doc
	cp -R doc/html/* build/gogui/doc

build/gogui/images/%.png: images/%.png
	mkdir -p build/gogui/images
	cp $< $@

%.jar: build/files-%.txt
	mkdir -p build/$*
	javac -O $(JAVAOPT) -d build/$* @build/files-$*.txt
	jar cmf build/manifest-$*.txt $*.jar -C build/$* .

src/version/Version.java: build/version.txt
	sed 's/m_version = \".*\"/m_version = \"$(shell cat build/version.txt)\"/' <src/version/Version.java >src/version/.Version.java.new
	mv src/version/.Version.java.new src/version/Version.java

doc/html/index.html: $(DOC) build/version.txt
	cp build/version.txt doc/xml/version.xml
	$(MAKE) -C doc

.PHONY: clean dist docsrc gogui_debug tags

clean:
	-rm -rf $(patsubst %.jar, build/%, $(JAR)) $(JAR)

dist: all docsrc
	-rm -rf $(patsubst %.jar, build/%, $(JAR))

docsrc:
	javadoc -sourcepath src -d docsrc -source 1.4 $(PACKAGES)

# Run with 'jdb -classpath build_dbg -sourcepath src GoGui'
gogui_debug: doc/html/index.html $(IMAGES) $(shell cat build/files-gogui.txt)
	mkdir -p build/gogui_debug
	javac -g $(JAVAOPT) -d build/gogui_debug @build/files-gogui.txt
	mkdir -p build/gogui_debug/doc
	cp -R doc/html/* build/gogui_debug/doc
	mkdir -p build/gogui_debug/images
	for i in $(IMAGES); do cp $$i build/gogui_debug/$$i; done 

tags:
	etags `find . -name "*.java"`
