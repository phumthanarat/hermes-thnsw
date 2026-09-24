<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:output method="html"/>

<xsl:template match="/housekeeping">
	<br/>
	<div class="stat-grid">
	  <div class="stat-card">
	    <h3>Housekeeping</h3>
	    <div class="stat-row"><span class="label">Schedule</span><span class="value">
	      <xsl:choose>
	        <xsl:when test="enabled='true'"><span class="badge badge-success">On</span></xsl:when>
	        <xsl:otherwise><span class="badge badge-neutral">Off</span></xsl:otherwise>
	      </xsl:choose>
	    </span></div>
	    <div class="stat-row"><span class="label">Next run</span><span class="value">
	      <xsl:choose>
	        <xsl:when test="next_run!=''"><xsl:value-of select="next_run" /></xsl:when>
	        <xsl:otherwise>-</xsl:otherwise>
	      </xsl:choose>
	    </span></div>
	    <div class="stat-row"><span class="label">Last run</span><span class="value">
	      <xsl:choose>
	        <xsl:when test="last_run!=''"><xsl:value-of select="last_run" /></xsl:when>
	        <xsl:otherwise>Never</xsl:otherwise>
	      </xsl:choose>
	    </span></div>
	    <xsl:if test="last_result!=''">
	      <div class="stat-row"><span class="label">Last result</span><span class="value" style="font-size:12px;"><xsl:value-of select="last_result" /></span></div>
	    </xsl:if>
	  </div>
	</div>

	<p style="color:var(--text-soft);">
	  Permanently deletes old messages (with their content) once a day. Only messages in a final
	  status are ever deleted: never one still pending or being processed, nor an inbound message
	  the back-end application has not collected yet. Use Preview to see how many messages the
	  settings select before saving or running them.
	</p>

	<form name="housekeepingForm" method="post" action="housekeeping" onsubmit="return confirmHousekeeping()">
	<input type="hidden" name="request_action" id="hkAction" value="save" />
	<table border="0" cellpadding="2" cellspacing="2" width="100%">
	  <tr>
	    <td width="30%">Run every day</td>
	    <td>
	      <label>
	        <input type="checkbox" name="enabled" value="true">
	          <xsl:if test="enabled='true'"><xsl:attribute name="checked">checked</xsl:attribute></xsl:if>
	        </input>
	        Enabled, at
	      </label>
	      <input type="time" name="run_time" required="required">
	        <xsl:attribute name="value"><xsl:value-of select="run_time" /></xsl:attribute>
	      </input>
	      <span style="color:var(--text-soft); font-size:12px;"> (server time)</span>
	    </td>
	  </tr>
	  <tr>
	    <td>Keep messages for</td>
	    <td>
	      <input type="number" name="retention_days" min="1" size="6" required="required">
	        <xsl:attribute name="value"><xsl:value-of select="retention_days" /></xsl:attribute>
	      </input>
	      days
	      <span style="color:var(--text-soft); font-size:12px;"> (a run now deletes messages older than <xsl:value-of select="cut_off" />)</span>
	    </td>
	  </tr>
	  <tr>
	    <td>Message box</td>
	    <td>
	      <select name="message_box">
	        <option value="all"><xsl:if test="message_box='all'"><xsl:attribute name="selected">selected</xsl:attribute></xsl:if>Inbox and outbox</option>
	        <option value="inbox"><xsl:if test="message_box='inbox'"><xsl:attribute name="selected">selected</xsl:attribute></xsl:if>Inbox only</option>
	        <option value="outbox"><xsl:if test="message_box='outbox'"><xsl:attribute name="selected">selected</xsl:attribute></xsl:if>Outbox only</option>
	      </select>
	    </td>
	  </tr>
	  <tr>
	    <td>Message types</td>
	    <td>
	      <label style="margin-right:12px;">
	        <input type="checkbox" name="all_types" value="all" id="hkAllTypes" onclick="toggleTypes()">
	          <xsl:if test="all_types='true'"><xsl:attribute name="checked">checked</xsl:attribute></xsl:if>
	        </input>
	        All
	      </label>
	      <xsl:for-each select="type">
	        <label style="margin-right:12px; white-space:nowrap;">
	          <input type="checkbox" name="message_type" class="hk-type">
	            <xsl:attribute name="value"><xsl:value-of select="name" /></xsl:attribute>
	            <xsl:if test="checked='true'"><xsl:attribute name="checked">checked</xsl:attribute></xsl:if>
	          </input>
	          <xsl:value-of select="name" />
	        </label>
	      </xsl:for-each>
	    </td>
	  </tr>
	  <tr>
	    <td>Statuses</td>
	    <td>
	      <xsl:for-each select="status">
	        <label style="margin-right:12px; white-space:nowrap;">
	          <input type="checkbox" name="status">
	            <xsl:attribute name="value"><xsl:value-of select="code" /></xsl:attribute>
	            <xsl:if test="checked='true'"><xsl:attribute name="checked">checked</xsl:attribute></xsl:if>
	          </input>
	          <xsl:value-of select="label" />
	        </label>
	      </xsl:for-each>
	    </td>
	  </tr>
	  <tr>
	    <td></td>
	    <td>
	      <br/>
	      <input type="submit" value="Save" onclick="setHkAction('save')" />
	      <input type="submit" value="Preview" style="margin-left:8px;" onclick="setHkAction('preview')" />
	      <input type="submit" value="Save and run now" style="margin-left:8px;" onclick="setHkAction('run')" />
	    </td>
	  </tr>
	</table>
	</form>

	<script>
	function setHkAction(action) { document.getElementById('hkAction').value = action; }
	function confirmHousekeeping() {
		if (document.getElementById('hkAction').value !== 'run') { return true; }
		return confirm('Permanently delete the selected old messages now?\nThis cannot be undone.');
	}
	function toggleTypes() {
		var all = document.getElementById('hkAllTypes').checked;
		var boxes = document.querySelectorAll('.hk-type');
		for (var i = 0; i &lt; boxes.length; i++) {
			if (all) { boxes[i].checked = true; }
			boxes[i].disabled = all;
		}
	}
	toggleTypes();
	</script>
</xsl:template>
</xsl:stylesheet>
