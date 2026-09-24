<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:output method="html"/>

<xsl:template match="/partnerships">
	<br/>
	<xsl:if test="count(partnership)>0">

	    <div class="stat-grid">
	      <div class="stat-card">
	        <h3>Registered Partnerships</h3>
	        <div class="stat-row"><span class="label">Total Registered</span><span class="value big"><xsl:value-of select="count(partnership)" /></span></div>
	      </div>
	    </div>

	    <form enctype='multipart/form-data' name="partnershipForm" method="post" action="partnership">
	    <input type="hidden" name="request_action" id="pshipRequestAction" value="change" />
	    <input type="hidden" name="selected_partnership_id" id="selectedPartnershipId" value="" />
	    </form>

	    <input type="text" id="pshipSearch" placeholder="Search by CPA ID, service or action..." onkeyup="filterPships()"
	           style="width:100%; max-width: 100%; margin-bottom: 14px;" />

	    <table border="0" cellpadding="2" cellspacing="2" width="100%" id="pshipTable">
	      <tr>
	        <th>CPA ID</th>
	        <th>Service</th>
	        <th>Action</th>
	        <th>Partnership ID</th>
	        <th></th>
	      </tr>
	      <xsl:for-each select="partnership">
	        <tr>
	          <xsl:attribute name="data-s"><xsl:value-of select="translate(concat(./cpa_id,' ',./service,' ',./action_id,' ',./partnership_id),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz')" /></xsl:attribute>
	          <xsl:if test="./partnership_id=/partnerships/selected_partnership/partnership_id">
	            <xsl:attribute name="style">background: var(--accent-bg);</xsl:attribute>
	          </xsl:if>
	          <td><xsl:value-of select="./cpa_id" /></td>
	          <td><xsl:value-of select="./service" /></td>
	          <td><code><xsl:value-of select="./action_id" /></code></td>
	          <td style="font-size:12px; color:var(--text-soft); max-width:220px; overflow:hidden; text-overflow:ellipsis; white-space:nowrap;">
	            <xsl:attribute name="title"><xsl:value-of select="./partnership_id" /></xsl:attribute>
	            <xsl:value-of select="./partnership_id" />
	          </td>
	          <td style="white-space:nowrap;">
	            <a href="javascript:void(0)" onclick="selectPship(this.getAttribute('data-pid'))">
	              <xsl:attribute name="data-pid"><xsl:value-of select="./partnership_id" /></xsl:attribute>
	              Select
	            </a>
	            <xsl:if test="./service='urn:oasis:names:tc:ebxml-msg:service' and ./action_id='Ping'">
	              <a href="javascript:void(0)" style="margin-left:8px;" title="Send an ebMS Ping through this partnership; the partner answers with a Pong"
	                 onclick="sendPing(this.getAttribute('data-pid'))">
	                <xsl:attribute name="data-pid"><xsl:value-of select="./partnership_id" /></xsl:attribute>
	                Send Ping
	              </a>
	            </xsl:if>
	          </td>
	        </tr>
	      </xsl:for-each>
	    </table>

	    <br/>

	</xsl:if>
	
	<!-- The selected partnership -->
	<xsl:if test="count(/partnerships/selected_partnership) > 0">
	    <table border="0" cellpadding="2" cellspacing="2" width="100%">
	        <tr><td align="center" bgcolor="#6699ff"><font color="white"><b>Selected Partnership - <xsl:value-of select="/partnerships/selected_partnership/partnership_id" /></b></font></td></tr>
	        <tr><td bgcolor="#6699ff"/></tr>
	    </table>
		<xsl:for-each select="/partnerships/selected_partnership">
			<xsl:call-template name="print-form">
				<xsl:with-param name="form-name">partnershipUpdateForm</xsl:with-param>
				<xsl:with-param name="is-new">false</xsl:with-param>
			</xsl:call-template>
		</xsl:for-each>
	</xsl:if>
	  
	<!-- Add new partnership -->
	<table border="0" cellpadding="2" cellspacing="2" width="100%">
	    <tr><td align="center" bgcolor="#6699ff"><font color="white"><b><a href="javascript: showAddForm()">Add New Partnership</a></b></font></td></tr>
	    <tr><td bgcolor="#6699ff"/></tr>
	</table>
	<span id="addFormArea">
		<xsl:for-each select="/partnerships/add_partnership">
			<xsl:call-template name="print-form">
					<xsl:with-param name="form-name">partnershipAddForm</xsl:with-param>
					<xsl:with-param name="is-new">true</xsl:with-param>
			</xsl:call-template>
		</xsl:for-each>
	</span>

	<script>
	function selectPship(id) {
		document.getElementById('pshipRequestAction').value = 'change';
		document.getElementById('selectedPartnershipId').value = id;
		document.partnershipForm.submit();
	}
	function sendPing(id) {
		if (!confirm('Send an ebMS Ping through partnership ' + id + '?')) { return; }
		document.getElementById('pshipRequestAction').value = 'send_ping';
		document.getElementById('selectedPartnershipId').value = id;
		document.partnershipForm.submit();
	}
	function filterPships() {
		var q = document.getElementById('pshipSearch').value.toLowerCase();
		var rows = document.getElementById('pshipTable').getElementsByTagName('tr');
		for (var i = 1; i &lt; rows.length; i++) {
			var s = rows[i].getAttribute('data-s');
			if (s === null) { continue; }
			rows[i].style.display = (s.indexOf(q) !== -1) ? '' : 'none';
		}
	}

	var hiddenText = '';
	
	function showAddForm() {
		if (navigator.appName == 'Microsoft Internet Explorer') {
			swapText(document.getElementById('addFormArea'));
		}
	}
	
	function swapText(elem) {
			var tmpText = elem.innerHTML; 
			elem.innerHTML = hiddenText;
			hiddenText=tmpText;
	}
	
	<xsl:if test="count(/partnerships/selected_partnership) > 0">
		showAddForm();
	</xsl:if>
	
	</script>
</xsl:template>

<xsl:template name="print-cert">
	<xsl:param name="remove-btn"/>
	<hr noshade="" color="#000000" size="1"/>
	<u>Issuer DN</u><br/>
	<xsl:value-of select="./issuer" /><br/>
	<u>Subject DN</u><br/>
	<xsl:value-of select="./subject" /><br/>
	<u>Thumbprint</u><br/>
	<xsl:value-of select="./thumbprint" /><br/>
	<u>Validity</u><br/>
	From <xsl:value-of select="./valid-from" /> to <xsl:value-of select="./valid-to" /><br/>
	<hr noshade="" color="#000000" size="1"/>
	<input type="checkbox">
    	<xsl:attribute name="name"><xsl:value-of select='$remove-btn'/></xsl:attribute>
	</input>
	Remove the uploaded cert
</xsl:template>

<xsl:template name="print-yesno">
	<xsl:param name="element-name"/>
	<xsl:param name="selected-element"/>
	<xsl:param name="inverse"/>
	<xsl:element name="select">
    	<xsl:attribute name="name"><xsl:value-of select="$element-name"/></xsl:attribute>
		<xsl:call-template name="print-option">
			<xsl:with-param name="option-name">No</xsl:with-param>
			<xsl:with-param name="option-value">
			   <xsl:choose>
			      <xsl:when test="$inverse='true'">true</xsl:when>
			      <xsl:otherwise>false</xsl:otherwise>
			   </xsl:choose>
			</xsl:with-param>
			<xsl:with-param name="selected-value"><xsl:value-of select="$selected-element"/></xsl:with-param>
		</xsl:call-template>
		<xsl:call-template name="print-option">
			<xsl:with-param name="option-name">Yes</xsl:with-param>
			<xsl:with-param name="option-value">
			   <xsl:choose>
			      <xsl:when test="$inverse='true'">false</xsl:when>
			      <xsl:otherwise>true</xsl:otherwise>
			   </xsl:choose>
			</xsl:with-param>
			<xsl:with-param name="selected-value"><xsl:value-of select="$selected-element"/></xsl:with-param>
		</xsl:call-template>
    </xsl:element>
</xsl:template>

<xsl:template name="print-option">
	<xsl:param name="option-name"/>
	<xsl:param name="option-value"/>
	<xsl:param name="selected-value"/>
	<xsl:element name="option">
    	<xsl:attribute name="value"><xsl:value-of select="$option-value"/></xsl:attribute>
        <xsl:if test="$option-value=$selected-value">
        	<xsl:attribute name="SELECTED" />
	    </xsl:if>
        <xsl:value-of select="$option-name"/>
	</xsl:element>
</xsl:template>

<xsl:template name="print-form">
	<xsl:param name="form-name"/>
	<xsl:param name="is-new"/>
	
    <form enctype='multipart/form-data' method="post" action="partnership">  
    <xsl:attribute name="name"><xsl:value-of select="$form-name"/></xsl:attribute>
    
    <table border="0" cellpadding="2" cellspacing="2" width="100%">  
      <tr><td/><td/></tr>
      <tr>
        <th colspan="2" align="left">General</th>
      </tr>
      <tr>
        <td width="40%"><b>Partnership ID</b></td>
        <td width="60%">
    		<xsl:choose>
		    	<xsl:when test="$is-new='true'">
			        <input type="text" name="partnership_id">
			            <xsl:attribute name="value"><xsl:value-of select="./partnership_id" /></xsl:attribute>
			        </input>
				</xsl:when>
		    	<xsl:otherwise>
		        	<b><xsl:value-of select="./partnership_id" /></b>
				    <input type="hidden" name="partnership_id">
				        <xsl:attribute name="value"><xsl:value-of select="./partnership_id" /></xsl:attribute>
				    </input>        	
		    	</xsl:otherwise>
			</xsl:choose>
        </td>
      </tr>
      <tr>
        <td width="40%"><b>CPA ID</b></td>
        <td width="60%">
	        <input type="text" name="cpa_id">
	            <xsl:attribute name="value"><xsl:value-of select="./cpa_id" /></xsl:attribute>
	        </input>
        </td>
      </tr>
      <tr>
        <td width="40%"><b>Service</b></td>
        <td width="60%">
	        <input type="text" name="service">
	            <xsl:attribute name="value"><xsl:value-of select="./service" /></xsl:attribute>
	        </input>
        </td>
      </tr>
      <tr>
        <td width="40%"><b>Action</b></td>
        <td width="60%">
	        <input type="text" name="action_id">
	            <xsl:attribute name="value"><xsl:value-of select="./action_id" /></xsl:attribute>
	        </input>
        </td>
      </tr>
      <tr>
        <td width="40%">Disabled</td>
        <td width="60%">
        	<xsl:call-template name="print-yesno">
        		<xsl:with-param name="element-name">disabled</xsl:with-param>
        		<xsl:with-param name="selected-element"><xsl:value-of select="./disabled"/></xsl:with-param>
        	</xsl:call-template>
        </td>
      </tr>
      <tr>
        <td width="40%">Transport Endpoint</td>
        <td width="60%">
            <input type="text" size="50">
                <xsl:attribute name="name">transport_endpoint</xsl:attribute>
                <xsl:attribute name="value"><xsl:value-of select="./transport_endpoint" /></xsl:attribute>
            </input>
        </td>
      </tr>
      <tr>
        <td width="40%">Hostname Verified in SSL?</td>
        <td width="60%">
            <xsl:call-template name="print-yesno">
        		<xsl:with-param name="element-name">is_hostname_verified</xsl:with-param>
        		<xsl:with-param name="selected-element"><xsl:value-of select="./is_hostname_verified"/></xsl:with-param>
        	</xsl:call-template>
        </td>
      </tr>
        <tr>
            <td width="40%">Sync Reply Mode</td>
            <td width="60%">
                <select name="sync_reply_mode">
                    <xsl:element name="option"> <xsl:if 
                        test="'none'=./sync_reply_mode"> <xsl:attribute 
                        name="SELECTED"></xsl:attribute> </xsl:if>none 
                        </xsl:element>
                    <xsl:element name="option"> <xsl:if 
                        test="'mshSignalsOnly'=./sync_reply_mode"> 
                        <xsl:attribute name="SELECTED"></xsl:attribute> 
                        </xsl:if>mshSignalsOnly </xsl:element>                    
                </select>
            </td>
        </tr>
        <tr>
            <td width="40%">Acknowledgement Requested</td>
            <td width="60%">
                <select name="ack_requested">
                    <xsl:element name="option"> <xsl:if 
                        test="'never'=./ack_requested"> <xsl:attribute 
                        name="SELECTED"></xsl:attribute> </xsl:if>never 
                        </xsl:element>
                    <xsl:element name="option"> <xsl:if 
                        test="'always'=./ack_requested"> <xsl:attribute 
                        name="SELECTED"></xsl:attribute> </xsl:if>always 
                        </xsl:element>
                </select>
            </td>
        </tr>
        <tr>
            <td width="40%">Acknowledgement Signed Requested</td>
            <td width="60%">
                <select name="ack_sign_requested">
                    <xsl:element name="option"> <xsl:if 
                        test="'never'=./ack_sign_requested"> <xsl:attribute 
                        name="SELECTED"></xsl:attribute> </xsl:if>never 
                        </xsl:element>
                    <xsl:element name="option"> <xsl:if 
                        test="'always'=./ack_sign_requested"> <xsl:attribute 
                        name="SELECTED"></xsl:attribute> </xsl:if>always 
                        </xsl:element>
                </select>
            </td>
        </tr>
        <tr>
            <td width="40%">Duplicate Elimination</td>
            <td width="60%">
                <select name="dup_elimination">
                    <xsl:element name="option"> <xsl:if 
                        test="'never'=./dup_elimination"> <xsl:attribute 
                        name="SELECTED"></xsl:attribute> </xsl:if>never 
                        </xsl:element>
                    <xsl:element name="option"> <xsl:if 
                        test="'always'=./dup_elimination"> <xsl:attribute 
                        name="SELECTED"></xsl:attribute> </xsl:if>always 
                        </xsl:element>
                </select>
            </td>
        </tr>
        <tr>
            <td width="40%">Message Order</td>
            <td width="60%">
                <select name="message_order">
                    <xsl:element name="option"> <xsl:if 
                        test="'NotGuaranteed'=./message_order"> <xsl:attribute 
                        name="SELECTED"></xsl:attribute> </xsl:if>NotGuaranteed 
                        </xsl:element>
                    <xsl:element name="option"> <xsl:if 
                        test="'Guaranteed'=./message_order"> <xsl:attribute 
                        name="SELECTED"></xsl:attribute> </xsl:if>Guaranteed 
                        </xsl:element>
                </select>
            </td>
        </tr>
      <tr>
        <td width="40%">Signing Required?</td>
        <td width="60%">
            <xsl:call-template name="print-yesno">
        		<xsl:with-param name="element-name">sign_requested</xsl:with-param>
        		<xsl:with-param name="selected-element"><xsl:value-of select="./sign_requested"/></xsl:with-param>
        	</xsl:call-template>
        </td>
      </tr>
      <tr>
        <td width="40%">Encryption Required? (Mail Only)</td>
        <td width="60%">
            <xsl:call-template name="print-yesno">
        		<xsl:with-param name="element-name">encrypt_requested</xsl:with-param>
        		<xsl:with-param name="selected-element"><xsl:value-of select="./encrypt_requested"/></xsl:with-param>
        	</xsl:call-template>
        </td>
      </tr>
      <tr>
        <td width="40%">Certificate For Encryption</td>
        <td width="60%">
	        <xsl:choose>
	    	<xsl:when test="count(./encrypt_cert/*)>0">
            	<xsl:for-each select="./encrypt_cert">
            		<xsl:call-template name="print-cert">
            			<xsl:with-param name="remove-btn">encrypt_cert_remove</xsl:with-param>
            		</xsl:call-template>
            	</xsl:for-each>
			</xsl:when>
	    	<xsl:otherwise>
                <input type="file">
                    <xsl:attribute name="name">encrypt_cert</xsl:attribute>
                    <xsl:attribute name="value"></xsl:attribute>
                </input>
	    	</xsl:otherwise>
	        </xsl:choose>
        </td>
      </tr>
      <tr>
        <td width="40%">Maximum Retries</td>
        <td width="60%">
            <input type="text">
                <xsl:attribute name="name">retries</xsl:attribute>
                <xsl:attribute name="value"><xsl:value-of select="./retries" /></xsl:attribute>
                <xsl:if test="$is-new='true'">
					<xsl:attribute name="value">3</xsl:attribute>
				</xsl:if> 	
            </input>
        </td>
      </tr>
      <tr>
        <td width="40%">Retry Interval (ms)</td>
        <td width="60%">
            <input type="text">
                <xsl:attribute name="name">retry_interval</xsl:attribute>
                <xsl:attribute name="value"><xsl:value-of select="./retry_interval" /></xsl:attribute>
                <xsl:if test="$is-new='true'">
					<xsl:attribute name="value">60000</xsl:attribute>
				</xsl:if> 	
            </input>
        </td>
      </tr>
      <tr>
        <td width="40%">Certificate For Verification</td>
        <td width="60%">
	        <xsl:choose>
	    	<xsl:when test="count(./verify_cert/*)>0">
                <xsl:for-each select="./verify_cert">
            		<xsl:call-template name="print-cert">
            			<xsl:with-param name="remove-btn">verify_cert_remove</xsl:with-param>
            		</xsl:call-template>
            	</xsl:for-each>
			</xsl:when>
	    	<xsl:otherwise>
                <input type="file">
                    <xsl:attribute name="name">verify_cert</xsl:attribute>
                    <xsl:attribute name="value"></xsl:attribute>
                </input>
	    	</xsl:otherwise>
	        </xsl:choose>
        </td>
      </tr>     
      <tr>
        <td width="40%"></td>
        <td width="60%">
        	<br/>
    		<xsl:choose>
		    	<xsl:when test="$is-new='true'">
		        	<input type="Submit" name="request_action" value="add"  onClick="return confirm('Are you sure to add the partnership?');"/> 
				</xsl:when>
		    	<xsl:otherwise>
		        	<input type="Submit" name="request_action" value="update" onClick="return confirm('Are you sure to update the partnership?');"/> 
		        	<input type="Submit" name="request_action" value="delete" onClick="return confirm('Are you sure to delete the partnership?');"/>
		    	</xsl:otherwise>
			</xsl:choose>
        	<br/>
        </td>
      </tr>  
    </table>
    </form>
    <br/>
</xsl:template>

</xsl:stylesheet>