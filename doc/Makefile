#------------------------------------------------------------------------------
# $Id$
# $Source$
#------------------------------------------------------------------------------

doc:
	mkdir -p html homepage
	xsltproc \
          --param admon.graphics 0 \
          --param admon.graphics.path '""' \
          --param chunk.section.depth 0 \
          --param html.stylesheet '"docbook.css"' \
          --param navig.showtitles 0 \
          --param refentry.generate.name 0 \
          --param refentry.generate.title 1 \
          --param toc.section.depth 0 \
          -o html/ \
          $(XSLDIR)/html/chunk.xsl xml/book.xml
        #cp images/*.png css/docbook.css html
	cp css/docbook.css html
	xsltproc -o homepage/index.html \
          $(XSLDIR)/html/docbook.xsl xml/homepage.xml
