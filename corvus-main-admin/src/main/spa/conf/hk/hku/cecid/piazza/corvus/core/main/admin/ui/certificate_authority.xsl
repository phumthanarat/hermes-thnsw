<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:output method="html"/>
<xsl:template match="/ca">

<div class="stat-grid">
  <div class="stat-card">
    <h3>Internal Certificate Authority</h3>
    <div class="stat-row"><span class="label">Subject</span><span class="value" style="font-size:12px;"><xsl:value-of select="ca_info/subject" /></span></div>
    <div class="stat-row"><span class="label">Expires</span><span class="value"><xsl:value-of select="ca_info/not_after" /></span></div>
    <div class="stat-row"><span class="label">CRL Endpoint</span><span class="value" style="font-size:12px;"><xsl:value-of select="ca_info/crl_url" /></span></div>
  </div>
</div>

<br/>

<xsl:if test="issued_cert">
<div style="background:var(--bg); border:1px solid var(--border); border-radius:var(--radius); padding:16px 18px; margin-bottom:12px;">
  <b>Issued certificate, serial <xsl:value-of select="issued_cert/serial_number" /></b><br/>
  <span style="font-size:12px; color:var(--text-soft);"><xsl:value-of select="issued_cert/subject" /></span>
  <br/><br/>
  <textarea readonly="readonly" rows="14" style="width:100%; font-family:monospace; font-size:12px;" onclick="this.select();">
    <xsl:value-of select="issued_cert/pem" />
  </textarea>
  <xsl:if test="issued_cert/private_key_pem">
    <br/>
    <b style="color:var(--danger);">Private key -- copy this now, it is shown only this once and is not stored:</b><br/>
    <textarea readonly="readonly" rows="14" style="width:100%; font-family:monospace; font-size:12px;" onclick="this.select();">
      <xsl:value-of select="issued_cert/private_key_pem" />
    </textarea>
  </xsl:if>
</div>
</xsl:if>

<form name="issueCertForm" method="post" action="ca">
<input type="hidden" name="action" value="issue_cert" />
<table border="0" cellpadding="2" cellspacing="2" width="100%">
  <tr>
    <th colspan="2" align="left">Issue Certificate From CSR</th>
  </tr>
  <tr>
    <td width="40%">CSR (PEM)<br/><small><font color="gray">Paste a PKCS#10 "CERTIFICATE REQUEST" block, e.g. from the CSR generator on the Certificates tab</font></small></td>
    <td width="60%"><textarea name="csr_pem" rows="10" style="width:100%; font-family:monospace; font-size:12px;"></textarea></td>
  </tr>
  <tr>
    <td width="40%">Subject Alternative Names<br/><small><font color="gray">Comma-separated, e.g. DNS:partner.example.com, IP:203.0.113.10 -- leave blank for none</font></small></td>
    <td width="60%"><input type="text" name="cert_san" size="60" /></td>
  </tr>
  <tr>
    <td width="40%">Validity (days)</td>
    <td width="60%"><input type="text" name="validity_days" size="10" value="730" /></td>
  </tr>
  <tr>
    <td width="40%"></td>
    <td width="60%"><br/><input type="Submit" value="Issue Certificate" /><br/></td>
  </tr>
</table>
</form>

<br/>

<form name="quickIssueForm" method="post" action="ca">
<input type="hidden" name="action" value="quick_issue" />
<table border="0" cellpadding="2" cellspacing="2" width="100%">
  <tr>
    <th colspan="2" align="left">Quick Issue (generate a new key pair and certificate together)</th>
  </tr>
  <tr>
    <td width="40%">Subject DN<br/><small><font color="gray">e.g. CN=partner.example.com,O=Partner Co,C=TH</font></small></td>
    <td width="60%"><input type="text" name="quick_subject_dn" size="60" /></td>
  </tr>
  <tr>
    <td width="40%">Subject Alternative Names<br/><small><font color="gray">Comma-separated, e.g. DNS:partner.example.com, IP:203.0.113.10 -- leave blank for none</font></small></td>
    <td width="60%"><input type="text" name="quick_san" size="60" /></td>
  </tr>
  <tr>
    <td width="40%">Validity (days)</td>
    <td width="60%"><input type="text" name="quick_validity_days" size="10" value="730" /></td>
  </tr>
  <tr>
    <td width="40%"></td>
    <td width="60%"><br/><input type="Submit" value="Generate Key + Issue Certificate" /><br/></td>
  </tr>
</table>
</form>

<br/>

<table border="0" cellpadding="2" cellspacing="2" width="100%">
  <tr>
    <th>Serial</th>
    <th>Subject</th>
    <th>Issued</th>
    <th>Status</th>
    <th></th>
  </tr>
  <xsl:for-each select="issued_certs/cert">
  <tr>
    <td><code><xsl:value-of select="./serial_number" /></code></td>
    <td style="font-size:12px;"><xsl:value-of select="./subject_dn" /></td>
    <td><xsl:value-of select="./issued_timestamp" /></td>
    <td>
      <xsl:choose>
        <xsl:when test="./revoked = 'true'">
          <span class="badge badge-danger">Revoked</span>
        </xsl:when>
        <xsl:otherwise>
          <span class="badge badge-success">Active</span>
        </xsl:otherwise>
      </xsl:choose>
    </td>
    <td>
      <xsl:if test="./revoked != 'true'">
        <form method="post" action="ca" style="display:inline;" onSubmit="return confirm('Revoke this certificate? This cannot be undone.');">
          <input type="hidden" name="action" value="revoke_cert" />
          <input type="hidden" name="serial_number"><xsl:attribute name="value"><xsl:value-of select="./serial_number" /></xsl:attribute></input>
          <input type="submit" value="Revoke" />
        </form>
      </xsl:if>
    </td>
  </tr>
  </xsl:for-each>
</table>

<br/>

<div class="stat-grid">
  <div class="stat-card">
    <h3>Trusted External CAs</h3>
    <div class="stat-row"><span class="label">Total Trusted CAs</span><span class="value big"><xsl:value-of select="count(trusted_cas/ca)" /></span></div>
  </div>
</div>

<span style="font-size:13px; color:var(--text-soft);">
  Importing an external CA's root certificate here lets revocation checking validate partner
  certificates that CA issued (not just this gateway's own certs and its internal CA's above)
  -- it does not affect signature verification or encryption, only OCSP/CRL revocation status.
</span>

<xsl:if test="count(trusted_cas/ca) &gt; 0">
<table border="0" cellpadding="2" cellspacing="2" width="100%">
  <tr>
    <th>Subject</th>
    <th>Expires</th>
    <th>Fingerprint (SHA-1)</th>
    <th></th>
  </tr>
  <xsl:for-each select="trusted_cas/ca">
  <tr>
    <td style="font-size:12px;"><xsl:value-of select="./subject" /></td>
    <td><xsl:value-of select="./not_after" /></td>
    <td style="font-size:11px; font-family:monospace;"><xsl:value-of select="./fingerprint" /></td>
    <td>
      <form method="post" action="ca" style="display:inline;" onSubmit="return confirm('Remove this trusted CA?');">
        <input type="hidden" name="action" value="delete_trusted_ca" />
        <input type="hidden" name="fingerprint"><xsl:attribute name="value"><xsl:value-of select="./fingerprint" /></xsl:attribute></input>
        <input type="submit" value="Remove" />
      </form>
    </td>
  </tr>
  </xsl:for-each>
</table>
<br/>
</xsl:if>

<form name="importTrustedCaForm" method="post" action="ca">
<input type="hidden" name="action" value="import_trusted_ca" />
<table border="0" cellpadding="2" cellspacing="2" width="100%">
  <tr>
    <th colspan="2" align="left">Import External CA Root Certificate</th>
  </tr>
  <tr>
    <td width="40%">Certificate (PEM)<br/><small><font color="gray">Paste a "CERTIFICATE" PEM block for the CA's root cert</font></small></td>
    <td width="60%"><textarea name="trusted_ca_pem" rows="10" style="width:100%; font-family:monospace; font-size:12px;"></textarea></td>
  </tr>
  <tr>
    <td width="40%"></td>
    <td width="60%"><br/><input type="Submit" value="Import CA Certificate" /><br/></td>
  </tr>
</table>
</form>

</xsl:template>
</xsl:stylesheet>
