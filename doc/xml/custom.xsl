<?xml version='1.0'?> 
<xsl:stylesheet
 xmlns:xsl="http://www.w3.org/1999/XSL/Transform"  version="1.0"> 

<xsl:import href="chunk.xsl"/> 

<xsl:template name="body.attributes">
</xsl:template>

<!-- Don't use header for abstract -->
<xsl:template match="abstract" mode="titlepage.mode">
  <div class="{name(.)}">
    <xsl:call-template name="anchor"/>
    <xsl:apply-templates mode="titlepage.mode"/>
  </div>
</xsl:template>

</xsl:stylesheet>
