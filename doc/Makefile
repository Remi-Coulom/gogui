#------------------------------------------------------------------------------
# $Id$
# $Source$
#------------------------------------------------------------------------------

doc:
	mkdir -p html homepage
	xsltproc -o html/ $(XSLDIR)/html/chunk.xsl xml/book.xml
	xsltproc -o homepage/index.html $(XSLDIR)/html/docbook.xsl xml/homepage.xml
