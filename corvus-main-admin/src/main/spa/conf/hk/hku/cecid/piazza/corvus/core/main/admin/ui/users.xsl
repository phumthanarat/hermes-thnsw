<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:output method="html"/>

<xsl:template match="/users">
	<br/>
	<p style="color:var(--text-soft);">
	  <b>Administrator</b>: everything, including settings and users.
	  <b>Operator</b>: views everything and runs operations (Ping, delete/resend messages) but changes no settings.
	  <b>Viewer</b>: views and searches only.
	  Other users (e.g. the REST API user) are listed but not managed here.
	</p>

	<table border="0" cellpadding="2" cellspacing="2" width="100%">
	  <tr><th>User</th><th>Access level</th><th>Status</th><th>Reset password</th><th></th></tr>
	  <xsl:for-each select="user">
	    <tr>
	      <td>
	        <b><xsl:value-of select="username" /></b>
	        <xsl:if test="self='true'"> <span class="badge badge-info">you</span></xsl:if>
	      </td>
	      <xsl:choose>
	        <xsl:when test="console='true'">
	          <td>
	            <form method="post" action="users" style="display:inline">
	              <input type="hidden" name="request_action" value="set_level" />
	              <input type="hidden" name="username"><xsl:attribute name="value"><xsl:value-of select="username" /></xsl:attribute></input>
	              <select name="level" onchange="this.form.submit()">
	                <xsl:variable name="current" select="level" />
	                <xsl:for-each select="/users/level">
	                  <option>
	                    <xsl:attribute name="value"><xsl:value-of select="role" /></xsl:attribute>
	                    <xsl:if test="role=$current"><xsl:attribute name="selected">selected</xsl:attribute></xsl:if>
	                    <xsl:value-of select="label" />
	                  </option>
	                </xsl:for-each>
	              </select>
	            </form>
	          </td>
	          <td>
	            <xsl:choose>
	              <xsl:when test="disabled='true'"><span class="badge badge-neutral">Disabled</span></xsl:when>
	              <xsl:otherwise><span class="badge badge-success">Active</span></xsl:otherwise>
	            </xsl:choose>
	            <xsl:if test="must_change='true'"> <span class="badge badge-warning">Must change password</span></xsl:if>
	          </td>
	          <td>
	            <form method="post" action="users" style="display:inline; white-space:nowrap;">
	              <input type="hidden" name="request_action" value="reset_password" />
	              <input type="hidden" name="username"><xsl:attribute name="value"><xsl:value-of select="username" /></xsl:attribute></input>
	              <input type="password" name="password" size="12" placeholder="New password" autocomplete="new-password" required="required" />
	              <label style="font-size:12px;"><input type="checkbox" name="must_change" value="true" checked="checked" /> must change</label>
	              <input type="submit" value="Reset" />
	            </form>
	          </td>
	          <td style="white-space:nowrap;">
	            <xsl:if test="self!='true'">
	              <form method="post" action="users" style="display:inline">
	                <input type="hidden" name="request_action" value="toggle_disabled" />
	                <input type="hidden" name="username"><xsl:attribute name="value"><xsl:value-of select="username" /></xsl:attribute></input>
	                <input type="submit">
	                  <xsl:attribute name="value"><xsl:choose><xsl:when test="disabled='true'">Enable</xsl:when><xsl:otherwise>Disable</xsl:otherwise></xsl:choose></xsl:attribute>
	                </input>
	              </form>
	              <form method="post" action="users" style="display:inline; margin-left:6px;"
	                    onsubmit="return confirm('Delete this user?')">
	                <input type="hidden" name="request_action" value="delete" />
	                <input type="hidden" name="username"><xsl:attribute name="value"><xsl:value-of select="username" /></xsl:attribute></input>
	                <input type="submit" value="Delete" />
	              </form>
	            </xsl:if>
	          </td>
	        </xsl:when>
	        <xsl:otherwise>
	          <td colspan="4" style="color:var(--text-soft);">Not a console user (roles: <xsl:value-of select="level_label" />)</td>
	        </xsl:otherwise>
	      </xsl:choose>
	    </tr>
	  </xsl:for-each>
	</table>

	<br/>
	<form method="post" action="users">
	<input type="hidden" name="request_action" value="add" />
	<table border="0" cellpadding="2" cellspacing="2" width="100%">
	  <tr><th colspan="2" align="left">Add a user</th></tr>
	  <tr><td width="30%">Username</td><td><input type="text" name="username" size="24" required="required" pattern="[A-Za-z0-9._@-]{{1,64}}" /></td></tr>
	  <tr><td>Password</td><td>
	    <input type="password" name="password" size="24" required="required" autocomplete="new-password">
	      <xsl:attribute name="minlength"><xsl:value-of select="min_password_length" /></xsl:attribute>
	    </input>
	    <span style="color:var(--text-soft); font-size:12px;"> at least <xsl:value-of select="min_password_length" /> characters</span>
	  </td></tr>
	  <tr><td>Access level</td><td>
	    <select name="level">
	      <xsl:for-each select="level">
	        <option>
	          <xsl:attribute name="value"><xsl:value-of select="role" /></xsl:attribute>
	          <xsl:if test="role='viewer'"><xsl:attribute name="selected">selected</xsl:attribute></xsl:if>
	          <xsl:value-of select="label" />
	        </option>
	      </xsl:for-each>
	    </select>
	  </td></tr>
	  <tr><td></td><td><label><input type="checkbox" name="must_change" value="true" checked="checked" /> Must change the password at first sign-in</label></td></tr>
	  <tr><td></td><td><input type="submit" value="Add user" /></td></tr>
	</table>
	</form>
</xsl:template>
</xsl:stylesheet>
