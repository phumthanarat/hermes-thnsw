<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:output method="html"/>
<xsl:template match="/certificates">

<div class="stat-grid">
  <div class="stat-card">
    <h3>Certificate Health</h3>
    <div class="stat-row"><span class="label">Total Certificates</span><span class="value big"><xsl:value-of select="summary/total_count" /></span></div>
    <div class="stat-row"><span class="label">Valid</span><span class="value"><xsl:value-of select="summary/valid_count" /></span></div>
    <div class="stat-row"><span class="label">Expiring Soon (&lt;=30d)</span><span class="value"><xsl:value-of select="summary/expiring_soon_count" /></span></div>
    <div class="stat-row"><span class="label">Expired / Error</span><span class="value"><xsl:value-of select="summary/expired_count" /></span></div>
  </div>
</div>

<br/>

<table border="0" cellpadding="2" cellspacing="2" width="100%">
  <tr>
    <th>Category</th>
    <th>Owner</th>
    <th>Role</th>
    <th>Subject</th>
    <th>Expires</th>
    <th>Status</th>
  </tr>
  <xsl:for-each select="cert">
  <tr>
    <td><xsl:value-of select="./category" /></td>
    <td style="font-size:12px;"><xsl:value-of select="./owner" /></td>
    <td><xsl:value-of select="./role" /></td>
    <td style="font-size:11px; color:var(--text-soft);">
      <xsl:choose>
        <xsl:when test="./status = 'error'">
          <span style="color:var(--danger);">Unable to read certificate: <xsl:value-of select="./error" /></span>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="./subject" />
        </xsl:otherwise>
      </xsl:choose>
    </td>
    <td>
      <xsl:choose>
        <xsl:when test="./status != 'error'">
          <xsl:value-of select="./not_after" /> (<xsl:value-of select="./days_remaining" /> days)
        </xsl:when>
      </xsl:choose>
    </td>
    <td>
      <xsl:choose>
        <xsl:when test="./status = 'valid'">
          <span class="badge badge-success">Valid</span>
        </xsl:when>
        <xsl:when test="./status = 'expiring_soon'">
          <span class="badge badge-warning">Expiring Soon</span>
        </xsl:when>
        <xsl:when test="./status = 'expired'">
          <span class="badge badge-danger">Expired</span>
        </xsl:when>
        <xsl:otherwise>
          <span class="badge badge-danger">Error</span>
        </xsl:otherwise>
      </xsl:choose>
    </td>
  </tr>
  </xsl:for-each>
</table>

<br/>

<xsl:if test="generated_csr">
<div style="background:var(--bg); border:1px solid var(--border); border-radius:var(--radius); padding:16px 18px; margin-bottom:12px;">
  <b>Generated CSR for <xsl:value-of select="generated_csr/key_id" /></b><br/>
  <span style="font-size:12px; color:var(--text-soft);"><xsl:value-of select="generated_csr/subject" /></span>
  <br/><br/>
  <textarea readonly="readonly" rows="14" style="width:100%; font-family:monospace; font-size:12px;" onclick="this.select();">
    <xsl:value-of select="generated_csr/pem" />
  </textarea>
  <xsl:if test="generated_csr/private_key_pem">
    <br/>
    <b style="color:var(--danger);">Private key -- copy this now, it is shown only this once and is not stored:</b><br/>
    <textarea readonly="readonly" rows="14" style="width:100%; font-family:monospace; font-size:12px;" onclick="this.select();">
      <xsl:value-of select="generated_csr/private_key_pem" />
    </textarea>
  </xsl:if>
</div>
</xsl:if>

<form name="generateCsrForm" method="post" action="certificates">
<input type="hidden" name="action" value="generate_csr" />
<table border="0" cellpadding="2" cellspacing="2" width="100%">
  <tr>
    <th colspan="2" align="left">Generate Certificate Signing Request (CSR)</th>
  </tr>
  <tr>
    <td width="40%">Key<br/><small><font color="gray">Which of the gateway's own keys this CSR is for -- ignored if "Generate a new key pair" below is checked</font></small></td>
    <td width="60%">
      <select name="key_id">
        <option value="as2plus">AS2plus</option>
        <option value="ebms-signature">ebMS (Signature)</option>
        <option value="ebms-decryption">ebMS (Decryption)</option>
        <option value="sfrm">SFRM</option>
      </select>
    </td>
  </tr>
  <tr>
    <td width="40%">Generate a new key pair<br/><small><font color="gray">Instead of reusing the key above -- the new private key is shown once below and not stored anywhere</font></small></td>
    <td width="60%"><input type="checkbox" name="generate_new_key" /></td>
  </tr>
  <tr>
    <td width="40%">Common Name (CN)</td>
    <td width="60%"><input type="text" name="csr_cn" size="40" /></td>
  </tr>
  <tr>
    <td width="40%">Subject Alternative Names<br/><small><font color="gray">Comma-separated, e.g. DNS:partner.example.com, IP:203.0.113.10 -- leave blank for none</font></small></td>
    <td width="60%"><input type="text" name="csr_san" size="60" /></td>
  </tr>
  <tr>
    <td width="40%">Organization (O)</td>
    <td width="60%"><input type="text" name="csr_o" size="40" /></td>
  </tr>
  <tr>
    <td width="40%">Organizational Unit (OU)</td>
    <td width="60%"><input type="text" name="csr_ou" size="40" /></td>
  </tr>
  <tr>
    <td width="40%">Locality (L)</td>
    <td width="60%"><input type="text" name="csr_l" size="40" /></td>
  </tr>
  <tr>
    <td width="40%">State (ST)</td>
    <td width="60%"><input type="text" name="csr_st" size="40" /></td>
  </tr>
  <tr>
    <td width="40%">Country (C)<br/><small><font color="gray">2-letter code, e.g. TH</font></small></td>
    <td width="60%"><input type="text" name="csr_c" size="5" maxlength="2" /></td>
  </tr>
  <tr>
    <td width="40%"></td>
    <td width="60%"><br/><input type="Submit" value="Generate CSR" /><br/></td>
  </tr>
</table>
</form>

</xsl:template>
</xsl:stylesheet>
