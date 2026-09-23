<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:output method="html"/>

<xsl:template name="linkage">
<xsl:element name="a">
   <xsl:attribute name="href">
      <xsl:value-of select="./link" />
   </xsl:attribute>
   <xsl:attribute name="title">
      <xsl:value-of select="./description" />
   </xsl:attribute>
   <xsl:value-of select="./name"/>
</xsl:element>
</xsl:template>

<xsl:template match="/tabs">

    <div class="tab-row">
    <xsl:for-each select="tab">
        <xsl:choose>
            <xsl:when test="./selected=''">
                <span class="tab-link selected"><xsl:call-template name="linkage"/></span>
            </xsl:when>
            <xsl:otherwise>
                <span class="tab-link"><xsl:call-template name="linkage"/></span>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:for-each>
    </div>

</xsl:template>
</xsl:stylesheet>
