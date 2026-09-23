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

<xsl:template match="/modules">

    <xsl:for-each select="module">
        <xsl:choose>
            <xsl:when test="./selected=''">
                <div class="nav-item selected">
                    <span class="avatar"><xsl:value-of select="substring(./name,1,1)"/></span>
                    <xsl:call-template name="linkage"/>
                </div>
            </xsl:when>
            <xsl:otherwise>
                <div class="nav-item">
                    <span class="avatar"><xsl:value-of select="substring(./name,1,1)"/></span>
                    <xsl:call-template name="linkage"/>
                </div>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:for-each>

</xsl:template>
</xsl:stylesheet>
