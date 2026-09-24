<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:output method="html"/>

<xsl:template match="/cpa_ping">
	<br/>
	<div class="stat-grid">
	  <div class="stat-card">
	    <h3>CPA Ping</h3>
	    <div class="stat-row"><span class="label">CPAs</span><span class="value big"><xsl:value-of select="count(cpa)" /></span></div>
	    <div class="stat-row"><span class="label">Pong received</span><span class="value"><xsl:value-of select="count(cpa[result='ok'])" /></span></div>
	    <div class="stat-row"><span class="label">Error / failed</span><span class="value"><xsl:value-of select="count(cpa[result='error' or result='failed'])" /></span></div>
	    <div class="stat-row"><span class="label">Waiting for Pong</span><span class="value"><xsl:value-of select="count(cpa[result='waiting'])" /></span></div>
	  </div>
	</div>

	<p style="color:var(--text-soft);">
	  Sends an ebMS Ping to each CPA's partner MSH. A CPA without its own Ping partnership
	  (Service <code>urn:oasis:names:tc:ebxml-msg:service</code>, Action <code>Ping</code>) uses the
	  endpoint and security settings of the partnership shown under "Route via".
	  Party IDs are suggested from the latest message of that CPA; a partner may reject a Ping
	  whose party IDs don't match its CPA.
	</p>

	<xsl:if test="count(cpa)=0">
	  <p>No enabled partnership found.</p>
	</xsl:if>

	<xsl:if test="count(cpa)>0">
	<form name="cpaPingForm" method="post" action="ping" onsubmit="return confirmPing(this)">
	  <input type="text" id="cpaSearch" placeholder="Search by CPA ID or endpoint..." onkeyup="filterCpas()"
	         style="width:100%; max-width:100%; margin-bottom:14px;" />
	  <div style="margin-bottom:10px;">
	    <input type="submit" name="ping_selected" value="Ping selected" />
	    <a href="ping" style="margin-left:12px;">Refresh</a>
	  </div>
	  <table border="0" cellpadding="2" cellspacing="2" width="100%" id="cpaTable">
	    <tr>
	      <th><input type="checkbox" id="selectAll" onclick="selectAllCpas(this.checked)" title="Select all shown" /></th>
	      <th>CPA ID</th>
	      <th>Route via</th>
	      <th>From Party ID</th>
	      <th>To Party ID</th>
	      <th>Last Ping</th>
	      <th></th>
	    </tr>
	    <xsl:for-each select="cpa">
	      <tr>
	        <xsl:attribute name="data-s"><xsl:value-of select="translate(concat(./cpa_id,' ',./endpoint),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz')" /></xsl:attribute>
	        <td>
	          <input type="checkbox" name="selected" class="cpa-select">
	            <xsl:attribute name="value"><xsl:value-of select="./row" /></xsl:attribute>
	          </input>
	          <input type="hidden">
	            <xsl:attribute name="name">cpa_<xsl:value-of select="./row" /></xsl:attribute>
	            <xsl:attribute name="value"><xsl:value-of select="./cpa_id" /></xsl:attribute>
	          </input>
	        </td>
	        <td>
	          <b><xsl:value-of select="./cpa_id" /></b>
	          <div style="font-size:12px; color:var(--text-soft);"><xsl:value-of select="./partnership_count" /> partnership(s)</div>
	        </td>
	        <td style="font-size:12px; max-width:260px; overflow:hidden; text-overflow:ellipsis; white-space:nowrap;">
	          <xsl:attribute name="title"><xsl:value-of select="./route_partnership_id" /></xsl:attribute>
	          <xsl:choose>
	            <xsl:when test="./has_ping_partnership='true'"><span class="badge badge-info">Ping partnership</span></xsl:when>
	            <xsl:otherwise><span class="badge badge-neutral">Borrowed</span></xsl:otherwise>
	          </xsl:choose>
	          <div style="color:var(--text-soft);"><xsl:value-of select="./endpoint" /></div>
	        </td>
	        <td>
	          <input type="text" size="14" placeholder="hermes-admin">
	            <xsl:attribute name="name">from_<xsl:value-of select="./row" /></xsl:attribute>
	            <xsl:attribute name="value"><xsl:value-of select="./from_party_id" /></xsl:attribute>
	          </input>
	        </td>
	        <td>
	          <input type="text" size="14">
	            <xsl:attribute name="name">to_<xsl:value-of select="./row" /></xsl:attribute>
	            <xsl:attribute name="value"><xsl:value-of select="./to_party_id" /></xsl:attribute>
	            <xsl:attribute name="placeholder"><xsl:value-of select="./cpa_id" /></xsl:attribute>
	          </input>
	        </td>
	        <td style="font-size:12px;">
	          <xsl:choose>
	            <xsl:when test="./result='ok'"><span class="badge badge-success">Pong</span></xsl:when>
	            <xsl:when test="./result='error'"><span class="badge badge-danger">Error</span></xsl:when>
	            <xsl:when test="./result='failed'"><span class="badge badge-danger">Delivery failed</span></xsl:when>
	            <xsl:when test="./result='waiting'"><span class="badge badge-warning">Waiting</span></xsl:when>
	            <xsl:otherwise><span class="badge badge-neutral">Never pinged</span></xsl:otherwise>
	          </xsl:choose>
	          <xsl:if test="./ping_time">
	            <div style="color:var(--text-soft);">Ping <xsl:value-of select="./ping_time" /></div>
	          </xsl:if>
	          <xsl:if test="./reply_time">
	            <div style="color:var(--text-soft);">Reply <xsl:value-of select="./reply_time" /></div>
	          </xsl:if>
	          <xsl:if test="./detail!=''">
	            <div style="color:var(--danger);"><xsl:value-of select="./detail" /></div>
	          </xsl:if>
	        </td>
	        <td style="white-space:nowrap;">
	          <button type="submit" name="ping_one">
	            <xsl:attribute name="value"><xsl:value-of select="./row" /></xsl:attribute>
	            Ping
	          </button>
	          <xsl:if test="./ping_message_id">
	            <a style="margin-left:8px;" title="Show this Ping in Message History">
	              <xsl:attribute name="href">message_history?message_id=<xsl:value-of select="./ping_message_id" />&amp;message_box=outbox</xsl:attribute>
	              History
	            </a>
	          </xsl:if>
	        </td>
	      </tr>
	    </xsl:for-each>
	  </table>
	</form>
	</xsl:if>

	<script>
	var pingButton = null;
	document.addEventListener('click', function (e) {
		if (e.target &amp;&amp; e.target.type === 'submit') { pingButton = e.target; }
	});
	function confirmPing(form) {
		var count = 1;
		if (pingButton &amp;&amp; pingButton.name === 'ping_selected') {
			count = 0;
			var boxes = form.querySelectorAll('.cpa-select');
			for (var i = 0; i &lt; boxes.length; i++) { if (boxes[i].checked) { count++; } }
			if (count === 0) { alert('Select at least one CPA'); return false; }
		}
		return confirm('Send an ebMS Ping to ' + count + ' CPA(s)?');
	}
	function selectAllCpas(checked) {
		var rows = document.getElementById('cpaTable').rows;
		for (var i = 1; i &lt; rows.length; i++) {
			if (rows[i].style.display === 'none') { continue; }
			var box = rows[i].querySelector('.cpa-select');
			if (box) { box.checked = checked; }
		}
	}
	function filterCpas() {
		var q = document.getElementById('cpaSearch').value.toLowerCase();
		var rows = document.getElementById('cpaTable').rows;
		for (var i = 1; i &lt; rows.length; i++) {
			var s = rows[i].getAttribute('data-s') || '';
			rows[i].style.display = s.indexOf(q) &gt;= 0 ? '' : 'none';
		}
	}
	</script>
</xsl:template>
</xsl:stylesheet>
