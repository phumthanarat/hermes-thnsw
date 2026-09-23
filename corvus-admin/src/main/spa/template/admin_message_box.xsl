<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:output method="html"/>

<xsl:template match="/message">

  <a name="message"/>
  <xsl:choose>
    <xsl:when test="./description">
      <div class="message-bar has-message">
        <span><b>Message:</b>&#160;<xsl:value-of select="./description"/></span>
        <a href="#">Top</a>
      </div>
      <script>document.location='#message';</script>
    </xsl:when>
    <xsl:otherwise>
      <div class="message-bar">
        <span>Ready</span>
        <a href="#">Top</a>
      </div>
    </xsl:otherwise>
  </xsl:choose>

</xsl:template>

</xsl:stylesheet>
