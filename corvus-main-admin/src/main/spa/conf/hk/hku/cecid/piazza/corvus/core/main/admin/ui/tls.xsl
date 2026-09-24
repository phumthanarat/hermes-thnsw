<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:output method="html"/>

<xsl:template match="/tls">
	<br/>
	<xsl:choose>
	  <xsl:when test="unavailable">
	    <p><span class="badge badge-warning">Not managed here</span> <xsl:value-of select="unavailable" /></p>
	  </xsl:when>
	  <xsl:otherwise>
	    <div class="stat-grid">
	      <div class="stat-card">
	        <h3>Current HTTPS certificate</h3>
	        <div class="stat-row"><span class="label">Subject</span><span class="value"><xsl:value-of select="cert/subject" /></span></div>
	        <div class="stat-row"><span class="label">Issuer</span><span class="value">
	          <xsl:value-of select="cert/issuer" />
	          <xsl:if test="cert/self_signed='true'"> <span class="badge badge-warning">Self-signed</span></xsl:if>
	        </span></div>
	        <div class="stat-row"><span class="label">Names</span><span class="value"><xsl:value-of select="cert/names" /></span></div>
	        <div class="stat-row"><span class="label">Valid</span><span class="value">
	          <xsl:value-of select="cert/not_before" /> to <xsl:value-of select="cert/not_after" />
	          <xsl:choose>
	            <xsl:when test="number(cert/days_left) &lt; 0"> <span class="badge badge-danger">Expired</span></xsl:when>
	            <xsl:when test="number(cert/days_left) &lt; 30"> <span class="badge badge-warning"><xsl:value-of select="cert/days_left" /> days left</span></xsl:when>
	            <xsl:otherwise> <span class="badge badge-success"><xsl:value-of select="cert/days_left" /> days left</span></xsl:otherwise>
	          </xsl:choose>
	        </span></div>
	        <div class="stat-row"><span class="label">Key</span><span class="value"><xsl:value-of select="cert/key_type" /></span></div>
	        <div class="stat-row"><span class="label">SHA-256</span><span class="value" style="font-size:11px; overflow-wrap:anywhere;"><xsl:value-of select="cert/fingerprint" /></span></div>
	      </div>
	    </div>

	    <p style="color:var(--text-soft);">
	      A new certificate is checked (its key must match, it must be valid now), takes effect
	      straight away for new connections, and the previous one is kept as server.p12.previous.
	    </p>

	    <form method="post" action="tls" onsubmit="return confirm('Replace the HTTPS certificate?')">
	    <input type="hidden" name="request_action" value="pem" />
	    <table border="0" cellpadding="2" cellspacing="2" width="100%">
	      <tr><th colspan="2" align="left">Install from PEM (e.g. from your certificate authority)</th></tr>
	      <tr><td width="30%">Certificate, then any intermediate certificates</td>
	          <td><textarea name="certificate" rows="6" cols="70" required="required" placeholder="-----BEGIN CERTIFICATE-----"></textarea></td></tr>
	      <tr><td>Private key</td>
	          <td><textarea name="private_key" rows="6" cols="70" required="required" placeholder="-----BEGIN PRIVATE KEY-----"></textarea></td></tr>
	      <tr><td>Key password (if encrypted)</td><td><input type="password" name="key_password" size="24" autocomplete="off" /></td></tr>
	      <tr><td></td><td><input type="submit" value="Install" /></td></tr>
	    </table>
	    </form>

	    <br/>
	    <form method="post" action="tls" onsubmit="return confirm('Replace the HTTPS certificate?')">
	    <input type="hidden" name="request_action" value="pkcs12" />
	    <input type="hidden" name="pkcs12_base64" id="pkcs12Data" value="" />
	    <table border="0" cellpadding="2" cellspacing="2" width="100%">
	      <tr><th colspan="2" align="left">Install from a PKCS#12 / PFX file</th></tr>
	      <tr><td width="30%">File (.p12 / .pfx)</td><td><input type="file" accept=".p12,.pfx" onchange="readPkcs12(this)" required="required" /></td></tr>
	      <tr><td>File password</td><td><input type="password" name="pkcs12_password" size="24" autocomplete="off" /></td></tr>
	      <tr><td></td><td><input type="submit" value="Install" /></td></tr>
	    </table>
	    </form>

	    <br/>
	    <form method="post" action="tls" onsubmit="return confirm('Replace the HTTPS certificate with a new self-signed one? Browsers will warn about it.')">
	    <input type="hidden" name="request_action" value="self_signed" />
	    <table border="0" cellpadding="2" cellspacing="2" width="100%">
	      <tr><th colspan="2" align="left">Generate a new self-signed certificate</th></tr>
	      <tr><td width="30%">Host name</td><td>
	        <input type="text" name="hostname" size="30" required="required"><xsl:attribute name="value"><xsl:value-of select="hostname" /></xsl:attribute></input>
	        <input type="submit" value="Generate" style="margin-left:8px;" />
	      </td></tr>
	    </table>
	    </form>

	    <script>
	    function readPkcs12(input) {
	      var reader = new FileReader();
	      reader.onload = function () { document.getElementById('pkcs12Data').value = reader.result; };
	      if (input.files.length) { reader.readAsDataURL(input.files[0]); }
	    }
	    </script>
	  </xsl:otherwise>
	</xsl:choose>
</xsl:template>
</xsl:stylesheet>
