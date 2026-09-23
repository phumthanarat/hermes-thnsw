<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:output method="html"/>
<xsl:template match="/documents">

<div class="stat-grid">
  <div class="stat-card">
    <h3>Document Catalog</h3>
    <div class="stat-row"><span class="label">Trading Partners</span><span class="value big"><xsl:value-of select="./summary/cpa_count" /></span></div>
    <div class="stat-row"><span class="label">Document Channels</span><span class="value big"><xsl:value-of select="./summary/partnership_count" /></span></div>
  </div>
</div>

<input type="text" id="docSearch" placeholder="Search by partner, service or document code..." onkeyup="filterDocs()"
       style="width:100%; max-width: 100%; margin-bottom: 14px;" />

<table border="0" cellpadding="2" cellspacing="2" width="100%" id="docTable">
  <tr>
    <th>Partner (CPA)</th>
    <th>Service</th>
    <th>Document / Action</th>
    <th>Endpoint</th>
    <th>Status</th>
  </tr>
  <xsl:for-each select="cpa">
    <xsl:variable name="cpaId" select="./id" />
    <xsl:for-each select="service">
      <xsl:variable name="serviceName" select="./name" />
      <xsl:for-each select="action">
        <tr>
          <xsl:attribute name="data-s"><xsl:value-of select="translate(concat($cpaId,' ',$serviceName,' ',./name),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz')" /></xsl:attribute>
          <td><xsl:value-of select="$cpaId" /></td>
          <td><xsl:value-of select="$serviceName" /></td>
          <td><code><xsl:value-of select="./name" /></code></td>
          <td style="font-size:12px; color:var(--text-soft);"><xsl:value-of select="./endpoint" /></td>
          <td>
            <xsl:choose>
              <xsl:when test="./disabled='true'">Disabled</xsl:when>
              <xsl:otherwise>Active</xsl:otherwise>
            </xsl:choose>
          </td>
        </tr>
      </xsl:for-each>
    </xsl:for-each>
  </xsl:for-each>
</table>

<script>
function filterDocs() {
  var q = document.getElementById('docSearch').value.toLowerCase();
  var rows = document.getElementById('docTable').getElementsByTagName('tr');
  for (var i = 1; i &lt; rows.length; i++) {
    var s = rows[i].getAttribute('data-s');
    if (s === null) { continue; }
    rows[i].style.display = (s.indexOf(q) !== -1) ? '' : 'none';
  }
}
</script>

</xsl:template>
</xsl:stylesheet>
