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
	    <div class="stat-row"><span class="label">Password changed</span><span class="value">
	      <xsl:choose><xsl:when test="password_changed!=''"><xsl:value-of select="password_changed" /></xsl:when><xsl:otherwise>-</xsl:otherwise></xsl:choose>
	    </span></div>
	    <div class="stat-row"><span class="label">Two-factor sign-in</span><span class="value">
	      <xsl:choose>
	        <xsl:when test="two_factor='true'"><span class="badge badge-success">On</span></xsl:when>
	        <xsl:otherwise><span class="badge badge-neutral">Off</span></xsl:otherwise>
	      </xsl:choose>
	    </span></div>
	  </div>
	</div>

	<xsl:if test="must_change='true'">
	  <p><span class="badge badge-warning">Action needed</span> Set a new password before using the admin console.</p>
	</xsl:if>
	<xsl:if test="must_change!='true' and expired='true'">
	  <p><span class="badge badge-warning">Action needed</span> Your password has expired: set a new one to continue.</p>
	</xsl:if>
	<xsl:if test="must_change!='true' and expired!='true' and two_factor!='true' and two_factor_required='true'">
	  <p><span class="badge badge-warning">Action needed</span> Administrators must use two-factor sign-in: set it up below to continue.</p>
	</xsl:if>

	<form method="post" action="account">
	<input type="hidden" name="request_action" value="change_password" />
	<table border="0" cellpadding="2" cellspacing="2" width="100%">
	  <tr><th colspan="2" align="left">Change password</th></tr>
	  <tr><td width="30%">Current password</td><td><input type="password" name="current_password" size="24" required="required" autocomplete="current-password" /></td></tr>
	  <tr><td>New password</td><td>
	    <input type="password" name="new_password" size="24" required="required" autocomplete="new-password" />
	    <div style="color:var(--text-soft); font-size:12px;"><xsl:value-of select="policy" /></div>
	  </td></tr>
	  <tr><td>Confirm new password</td><td><input type="password" name="confirm_password" size="24" required="required" autocomplete="new-password" /></td></tr>
	  <tr><td></td><td><input type="submit" value="Change password" /></td></tr>
	</table>
	</form>

	<br/>
	<table border="0" cellpadding="2" cellspacing="2" width="100%">
	  <tr><th colspan="2" align="left">Two-factor sign-in</th></tr>
	  <xsl:choose>
	    <xsl:when test="two_factor='true'">
	      <tr><td colspan="2">On: each sign-in asks for a code from your authenticator app.</td></tr>
	      <xsl:if test="two_factor_required!='true'">
	        <tr><td width="30%">Turn off</td><td>
	          <form method="post" action="account" style="display:inline">
	            <input type="hidden" name="request_action" value="2fa_disable" />
	            <input type="text" name="code" size="8" inputmode="numeric" placeholder="Code" required="required" autocomplete="one-time-code" />
	            <input type="submit" value="Turn off" />
	          </form>
	        </td></tr>
	      </xsl:if>
	    </xsl:when>
	    <xsl:when test="enrol">
	      <tr><td width="30%">1. Scan with your authenticator app</td><td>
	        <img alt="QR code" width="200" height="200" style="background:#fff; padding:4px; border-radius:6px;">
	          <xsl:attribute name="src"><xsl:value-of select="enrol/qr" /></xsl:attribute>
	        </img>
	        <div style="font-size:12px; color:var(--text-soft);">Or enter this key: <code><xsl:value-of select="enrol/secret" /></code></div>
	      </td></tr>
	      <tr><td>2. Enter the code it shows</td><td>
	        <form method="post" action="account" style="display:inline">
	          <input type="hidden" name="request_action" value="2fa_confirm" />
	          <input type="text" name="code" size="8" inputmode="numeric" placeholder="123456" required="required" autocomplete="one-time-code" autofocus="autofocus" />
	          <input type="submit" value="Turn on" />
	        </form>
	      </td></tr>
	    </xsl:when>
	    <xsl:otherwise>
	      <tr><td colspan="2">
	        Adds a code from an authenticator app (Google or Microsoft Authenticator, ...) to each sign-in.
	        <form method="post" action="account" style="display:inline; margin-left:8px;">
	          <input type="hidden" name="request_action" value="2fa_start" />
	          <input type="submit" value="Set up" />
	        </form>
	      </td></tr>
	    </xsl:otherwise>
	  </xsl:choose>
	</table>
</xsl:template>
</xsl:stylesheet>
