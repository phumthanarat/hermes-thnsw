<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:output method="html"/>

<xsl:template match="/account">
	<br/>
	<div class="stat-grid">
	  <div class="stat-card">
	    <h3>My Account</h3>
	    <div class="stat-row"><span class="label">User</span><span class="value"><xsl:value-of select="username" /></span></div>
	    <div class="stat-row"><span class="label">Access level</span><span class="value"><xsl:value-of select="level" /></span></div>
	  </div>
	</div>

	<xsl:if test="must_change='true'">
	  <p><span class="badge badge-warning">Action needed</span>
	    Set a new password before using the admin console.</p>
	</xsl:if>

	<xsl:if test="changed!='true'">
	<form method="post" action="account">
	<table border="0" cellpadding="2" cellspacing="2" width="100%">
	  <tr><th colspan="2" align="left">Change password</th></tr>
	  <tr><td width="30%">Current password</td><td><input type="password" name="current_password" size="24" required="required" autocomplete="current-password" /></td></tr>
	  <tr><td>New password</td><td>
	    <input type="password" name="new_password" size="24" required="required" autocomplete="new-password">
	      <xsl:attribute name="minlength"><xsl:value-of select="min_password_length" /></xsl:attribute>
	    </input>
	    <span style="color:var(--text-soft); font-size:12px;"> at least <xsl:value-of select="min_password_length" /> characters</span>
	  </td></tr>
	  <tr><td>Confirm new password</td><td><input type="password" name="confirm_password" size="24" required="required" autocomplete="new-password" /></td></tr>
	  <tr><td></td><td><input type="submit" value="Change password" /></td></tr>
	</table>
	</form>
	</xsl:if>
	<xsl:if test="changed='true'">
	  <p><a href="home">Continue to the admin console</a></p>
	</xsl:if>
</xsl:template>
</xsl:stylesheet>
