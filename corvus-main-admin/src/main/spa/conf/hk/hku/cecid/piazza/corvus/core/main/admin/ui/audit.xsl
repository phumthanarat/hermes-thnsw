<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:output method="html"/>

<xsl:template match="/audit">
	<br/>
	<form method="get" action="audit" class="readonly-form">
	<table border="0" cellpadding="2" cellspacing="2" width="100%">
	  <tr>
	    <td>User <input type="text" name="user" size="12"><xsl:attribute name="value"><xsl:value-of select="filter/user" /></xsl:attribute></input></td>
	    <td>Contains <input type="text" name="text" size="18" placeholder="action, page, detail"><xsl:attribute name="value"><xsl:value-of select="filter/text" /></xsl:attribute></input></td>
	    <td>From <input type="date" name="from"><xsl:attribute name="value"><xsl:value-of select="filter/from" /></xsl:attribute></input></td>
	    <td>To <input type="date" name="to"><xsl:attribute name="value"><xsl:value-of select="filter/to" /></xsl:attribute></input></td>
	    <td style="white-space:nowrap;">
	      <input type="submit" value="Search" />
	      <a style="margin-left:10px;"><xsl:attribute name="href">audit_export?x=1<xsl:value-of select="query" /></xsl:attribute>Export CSV</a>
	    </td>
	  </tr>
	</table>
	</form>
	<p style="color:var(--text-soft); font-size:12px;">
	  <xsl:value-of select="total" /> entries<xsl:if test="keep_days!='0'">; kept for <xsl:value-of select="keep_days" /> days</xsl:if>.
	</p>
	<table border="0" cellpadding="2" cellspacing="2" width="100%">
	  <tr><th>Time</th><th>User</th><th>From</th><th>Action</th><th>Target</th><th>Outcome</th><th>Detail</th></tr>
	  <xsl:for-each select="entry">
	    <tr style="font-size:12px;">
	      <td style="white-space:nowrap;"><xsl:value-of select="time" /></td>
	      <td><xsl:value-of select="user" /></td>
	      <td><xsl:value-of select="ip" /></td>
	      <td><xsl:value-of select="action" /></td>
	      <td style="max-width:220px; overflow-wrap:anywhere;"><xsl:value-of select="target" /></td>
	      <td>
	        <xsl:choose>
	          <xsl:when test="outcome='ok'"><span class="badge badge-success">ok</span></xsl:when>
	          <xsl:when test="outcome='denied'"><span class="badge badge-warning">denied</span></xsl:when>
	          <xsl:otherwise><span class="badge badge-danger"><xsl:value-of select="outcome" /></span></xsl:otherwise>
	        </xsl:choose>
	      </td>
	      <td style="max-width:320px; overflow-wrap:anywhere;"><xsl:value-of select="detail" /></td>
	    </tr>
	  </xsl:for-each>
	</table>
	<div class="page-links">
	  <xsl:if test="number(offset) &gt; 0">
	    <a><xsl:attribute name="href">audit?offset=<xsl:value-of select="number(offset) - number(page_size)" /><xsl:value-of select="query" /></xsl:attribute>&#8249; Newer</a>
	  </xsl:if>
	  <xsl:if test="number(offset) + number(page_size) &lt; number(total)">
	    <a style="margin-left:16px;"><xsl:attribute name="href">audit?offset=<xsl:value-of select="number(offset) + number(page_size)" /><xsl:value-of select="query" /></xsl:attribute>Older &#8250;</a>
	  </xsl:if>
	</div>
</xsl:template>
</xsl:stylesheet>
