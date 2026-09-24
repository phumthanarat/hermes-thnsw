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

<form name="viewReferenceForm" method="post" action="./document_reference">
    <input type="hidden" name="reference_id" value="" />
    <input type="hidden" name="mode" value="" />
</form>

<form name="exportForm" method="post" action="./document_export">
    <input type="hidden" name="partnership_id" value="" />
    <input type="hidden" name="format" value="" />
</form>

<form name="deleteReferenceForm" method="post" action="./documents">
    <input type="hidden" name="action" value="delete_reference" />
    <input type="hidden" name="reference_id" value="" />
</form>

<form name="toggleReferenceForm" method="post" action="./documents">
    <input type="hidden" name="action" value="toggle_reference" />
    <input type="hidden" name="reference_id" value="" />
</form>

<table border="0" cellpadding="2" cellspacing="2" width="100%" id="docTable">
  <tr>
    <th>Partner (CPA)</th>
    <th>Service</th>
    <th>Document / Action</th>
    <th>Endpoint</th>
    <th>Status</th>
    <th>Reference Files</th>
    <th title="Integration files for this document type">Export</th>
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
          <td style="font-size:12px;">
            <xsl:for-each select="reference">
              <div style="white-space:nowrap;">
                <span class="badge badge-info"><xsl:value-of select="./file_type" /></span>
                <xsl:choose>
                  <xsl:when test="./disabled = 'true'">
                    <span class="badge badge-neutral">Disabled</span>
                  </xsl:when>
                  <xsl:otherwise>
                    <span class="badge badge-success">Active</span>
                  </xsl:otherwise>
                </xsl:choose>
                <a href="#1" style="margin-left:4px;">
                  <xsl:attribute name="onclick">
                    document.viewReferenceForm.reference_id.value='<xsl:value-of select="./id" />';
                    document.viewReferenceForm.mode.value='view';
                    document.viewReferenceForm.target='_blank';
                    document.viewReferenceForm.submit();
                  </xsl:attribute>
                  <xsl:value-of select="./filename" />
                </a>
                <a href="#1" title="Download" style="margin-left:4px;">
                  <xsl:attribute name="onclick">
                    document.viewReferenceForm.reference_id.value='<xsl:value-of select="./id" />';
                    document.viewReferenceForm.mode.value='';
                    document.viewReferenceForm.target='';
                    document.viewReferenceForm.submit();
                  </xsl:attribute>
                  &#8659;
                </a>
                <a href="#1" style="margin-left:4px;">
                  <xsl:attribute name="title">
                    <xsl:choose>
                      <xsl:when test="./disabled = 'true'">Enable</xsl:when>
                      <xsl:otherwise>Disable</xsl:otherwise>
                    </xsl:choose>
                  </xsl:attribute>
                  <xsl:attribute name="onclick">
                    document.toggleReferenceForm.reference_id.value='<xsl:value-of select="./id" />';
                    document.toggleReferenceForm.submit();
                  </xsl:attribute>
                  <xsl:choose>
                    <xsl:when test="./disabled = 'true'">&#9679;</xsl:when>
                    <xsl:otherwise>&#9711;</xsl:otherwise>
                  </xsl:choose>
                </a>
                <a href="#1" title="Remove" style="margin-left:4px; color:var(--danger);">
                  <xsl:attribute name="onclick">
                    if (confirm('Remove this reference file?')) {
                      document.deleteReferenceForm.reference_id.value='<xsl:value-of select="./id" />';
                      document.deleteReferenceForm.submit();
                    }
                  </xsl:attribute>
                  &#10005;
                </a>
              </div>
            </xsl:for-each>
          </td>
          <td style="font-size:12px; white-space:nowrap;">
            <a href="#1" onclick="exportDoc(this.getAttribute('data-pid'), 'wsdl')" title="WSDL of the Hermes sender web service for this document"><xsl:attribute name="data-pid"><xsl:value-of select="./partnership_id" /></xsl:attribute>WSDL</a> · <a href="#1" onclick="exportDoc(this.getAttribute('data-pid'), 'xsd')" title="Schema of the sender request for this document"><xsl:attribute name="data-pid"><xsl:value-of select="./partnership_id" /></xsl:attribute>XSD</a> · <a href="#1" onclick="exportDoc(this.getAttribute('data-pid'), 'soap')" title="Sample SOAP request to the Hermes sender web service"><xsl:attribute name="data-pid"><xsl:value-of select="./partnership_id" /></xsl:attribute>SOAP</a> · <a href="#1" onclick="exportDoc(this.getAttribute('data-pid'), 'xml')" title="Sample ebXML message Hermes sends to the partner"><xsl:attribute name="data-pid"><xsl:value-of select="./partnership_id" /></xsl:attribute>XML</a> · <a href="#1" onclick="exportDoc(this.getAttribute('data-pid'), 'zip')" title="All of these plus the attached reference files"><xsl:attribute name="data-pid"><xsl:value-of select="./partnership_id" /></xsl:attribute>ZIP</a>
          </td>
        </tr>
      </xsl:for-each>
    </xsl:for-each>
  </xsl:for-each>
</table>

<br/>

<form name="uploadReferenceForm" method="post" action="./documents" enctype="multipart/form-data">
<table border="0" cellpadding="2" cellspacing="2" width="100%">
  <tr>
    <th colspan="2" align="left">Attach a Reference File (.xsd / .wsdl / .xml)</th>
  </tr>
  <tr>
    <td width="40%">CPA ID<br/><small><font color="gray">Copy the exact value from the "Partner (CPA)" column above</font></small></td>
    <td width="60%"><input type="text" name="cpa_id" size="40" /></td>
  </tr>
  <tr>
    <td width="40%">Service<br/><small><font color="gray">Copy the exact value from the "Service" column above</font></small></td>
    <td width="60%"><input type="text" name="service" size="40" /></td>
  </tr>
  <tr>
    <td width="40%">Action<br/><small><font color="gray">Copy the exact value from the "Document / Action" column above</font></small></td>
    <td width="60%"><input type="text" name="doc_action" size="40" /></td>
  </tr>
  <tr>
    <td width="40%">File</td>
    <td width="60%"><input type="file" name="reference_file" accept=".xsd,.wsdl,.xml" /></td>
  </tr>
  <tr>
    <td width="40%">Description</td>
    <td width="60%"><input type="text" name="description" size="60" /></td>
  </tr>
  <tr>
    <td width="40%"></td>
    <td width="60%"><br/><input type="Submit" value="Attach File" /><br/></td>
  </tr>
</table>
</form>

<script>
function exportDoc(pid, format) {
  document.exportForm.partnership_id.value = pid;
  document.exportForm.format.value = format;
  document.exportForm.submit();
}
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
