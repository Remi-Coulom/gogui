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
  go \
  gtp \
  gtpserver \
  gui \
  sgf \
  utils \
  version

DOC= \
  doc/xml/analyze.xml \
  doc/xml/book.xml \
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
  doc/xml/reference-gtpserver.xml \
  doc/xml/reference-netgtp.xml \
  doc/xml/reference-regression.xml \
  doc/xml/reference-twogtp.xml \
  doc/xml/tools.xml

JAR= \
  gogui.jar \
  gtpserver.jar \
  gmptogtp.jar \
  netgtp.jar \
  regression.jar \
  sgftotex.jar \
  twogtp.jar

JAVAOPT=-deprecation -sourcepath . -source 1.4

all: $(JAR)

gogui.jar: build/gogui/doc/index.html $(patsubst %, build/gogui/%, $(IMAGES)) $(shell cat build/files-gogui.txt)

gmptogtp.jar: $(shell cat build/files-gmptogtp.txt)

gtpserver.jar: $(shell cat build/files-gtpnet.txt)

netgtp.jar: $(shell cat build/files-netgtp.txt)

regression.jar: $(shell cat build/files-regression.txt)

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

.PHONY: changelog clean gogui_debug srcdoc tags

# Run with 'jdb -classpath build_dbg -sourcepath src GoGui'
gogui_debug: doc/html/index.html $(IMAGES) $(shell cat build/files-gogui.txt)
	mkdir -p build/gogui_debug
	javac -g $(JAVAOPT) -d build/gogui_debug @build/files-gogui.txt
	mkdir -p build/gogui_debug/doc
	cp -R doc/html/* build/gogui_debug/doc
	mkdir -p build/gogui_debug/images
	for i in $(IMAGES); do cp $$i build/gogui_debug/$$i; done 

src/version/Version.java: build/version.txt
	sed 's/m_version = \".*\"/m_version = \"$(shell cat build/version.txt)\"/' <src/version/Version.java >src/version/.Version.java.new
	mv src/version/.Version.java.new src/version/Version.java

clean:
	-rm -rf $(patsubst %.jar, build/%, $(JAR)) $(JAR)

doc/html/index.html: $(DOC) build/version.txt
	cp build/version.txt doc/xml/version.xml
	$(MAKE) -C doc

docsrc:
	javadoc -sourcepath src -d docsrc -source 1.4 $(PACKAGES)

tags:
	etags `find . -name "*.java"`
