#------------------------------------------------------------------------------
# $Id$
# $Source$
#------------------------------------------------------------------------------

doc:
	mkdir -p html homepage
	xsltproc \
          --param chunk.section.depth 0 \
          --param toc.section.depth 1 \
          -o html/ \
          $(XSLDIR)/html/chunk.xsl xml/book.xml
	xsltproc -o homepage/index.html \
          $(XSLDIR)/html/docbook.xsl xml/homepage.xml
