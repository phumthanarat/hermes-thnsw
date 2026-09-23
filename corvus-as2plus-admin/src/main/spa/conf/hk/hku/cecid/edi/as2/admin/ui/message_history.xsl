<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:output method="html"/>
<xsl:template match="/message_history">
    

<!-- Style Sheet for text area -->
<style type="text/css">
textarea.special {
font-size:12;
background-color:#C0C0C0;
}
</style>

<!-- Java Script for download the message -->
<script>
function viewMessage(mid, mbox, receipt) {
    document.viewMessageForm.message_id.value=mid;
    document.viewMessageForm.message_box.value=mbox;
    document.viewMessageForm.is_download_receipt.value=receipt;
    document.viewMessageForm.mode.value='';
    document.viewMessageForm.target='';
    document.viewMessageForm.submit();
}

function viewMessageInline(mid, mbox, receipt) {
    document.viewMessageForm.message_id.value=mid;
    document.viewMessageForm.message_box.value=mbox;
    document.viewMessageForm.is_download_receipt.value=receipt;
    document.viewMessageForm.mode.value='view';
    document.viewMessageForm.target='_blank';
    document.viewMessageForm.submit();
}

function showMDN(omid, ombox) {
    document.showMDNForm.original_message_id.value=omid;
    document.showMDNForm.original_message_box.value=ombox;
    document.showMDNForm.submit();
}

function showMessage(mid, mbox) {
	document.showMessageForm.message_id.value=mid;
	document.showMessageForm.message_box.value=mbox;
	document.showMessageForm.submit();
}

function showResendAsNewMessage(pmid) {
	document.showResendMessageForm.primal_message_id.value=pmid;
	document.showResendMessageForm.submit();
}
</script>

<br/>

<!-- search form - start -->
<table border="0" cellpadding="2" cellspacing="2" width="100%">
    <tr><td align="center" bgcolor="#6699ff"><font color="white"><b>Search Message(s)</b></font></td></tr>
    <tr><td bgcolor="#6699ff"/></tr>
</table>

<form name="messageSearchForm" method="post" action="message_history">
    <table border="0" cellpadding="2" cellspacing="2" width="100%">  
      <tr>
        <td width="20%">Message ID</td>
        <td width="30%">
            <input type="text">
                <xsl:attribute name="name">message_id</xsl:attribute>
                <xsl:attribute name="value"><xsl:value-of select="/message_history/search_criteria/message_id" /></xsl:attribute>
            </input>
        </td>
        <td width="20%">Message Box</td>
        <td width="30%">
            Inbox
            <input type="radio">
                <xsl:attribute name="name">message_box</xsl:attribute>
                <xsl:attribute name="value">IN</xsl:attribute>
                <xsl:if test="'IN'=/message_history/search_criteria/message_box">
                    <xsl:attribute name="CHECKED"></xsl:attribute>
                </xsl:if>                
            </input>
            Outbox
            <input type="radio">
                <xsl:attribute name="name">message_box</xsl:attribute>
                <xsl:attribute name="value">OUT</xsl:attribute>
                <xsl:if test="'OUT'=/message_history/search_criteria/message_box">
                    <xsl:attribute name="CHECKED"></xsl:attribute>
                </xsl:if>  
            </input>
            All
            <input type="radio">
                <xsl:attribute name="name">message_box</xsl:attribute>
                <xsl:attribute name="value"></xsl:attribute>
                <xsl:if test="''=/message_history/search_criteria/message_box">
                    <xsl:attribute name="CHECKED"></xsl:attribute>
                </xsl:if>
                <xsl:if test="''=/message_history/search_criteria/message_box">
                    <xsl:attribute name="CHECKED"></xsl:attribute>
                </xsl:if>  
            </input>
        </td>
      </tr>
            
      <tr>
        <td width="20%">AS2 From</td>
        <td width="30%">
            <input type="text">
                <xsl:attribute name="name">as2_from</xsl:attribute>
                <xsl:attribute name="value"><xsl:value-of select="/message_history/search_criteria/as2_from" /></xsl:attribute>
            </input>
        </td>
        <td width="20%">AS2 To</td>
        <td width="30%">
            <input type="text">
                <xsl:attribute name="name">as2_to</xsl:attribute>
                <xsl:attribute name="value"><xsl:value-of select="/message_history/search_criteria/as2_to" /></xsl:attribute>
            </input>
        </td>
      </tr>
          
      <tr>
        <td width="20%">Status</td>
        <td width="30%">
            <select name="status">
                <xsl:element name="option">
                    <xsl:attribute name="value">RC</xsl:attribute>
                    <xsl:if test="'RC'=/message_history/search_criteria/status">
                        <xsl:attribute name="SELECTED"></xsl:attribute>
                    </xsl:if>Received
                </xsl:element>
                <xsl:element name="option">
                    <xsl:attribute name="value">PD</xsl:attribute>
                    <xsl:if test="'PD'=/message_history/search_criteria/status">
                        <xsl:attribute name="SELECTED"></xsl:attribute>
                    </xsl:if>Pending
                </xsl:element>
                <xsl:element name="option">
                    <xsl:attribute name="value">PR</xsl:attribute>
                    <xsl:if test="'PR'=/message_history/search_criteria/status">
                        <xsl:attribute name="SELECTED"></xsl:attribute>
                    </xsl:if>Processing   
                </xsl:element>
                <xsl:element name="option">
                    <xsl:attribute name="value">DL</xsl:attribute>
                    <xsl:if test="'DL'=/message_history/search_criteria/status">
                        <xsl:attribute name="SELECTED"></xsl:attribute>
                    </xsl:if>Delivered    
                </xsl:element>
                <xsl:element name="option">
                    <xsl:attribute name="value">DF</xsl:attribute>
                    <xsl:if test="'DF'=/message_history/search_criteria/status">
                        <xsl:attribute name="SELECTED"></xsl:attribute>
                    </xsl:if>Delivery Failure    
                </xsl:element>
                <xsl:element name="option">
                    <xsl:attribute name="value">PS</xsl:attribute>
                    <xsl:if test="'PS'=/message_history/search_criteria/status">
                        <xsl:attribute name="SELECTED"></xsl:attribute>
                    </xsl:if>Processed
                </xsl:element>
                <xsl:element name="option">
                    <xsl:attribute name="value">PE</xsl:attribute>
                    <xsl:if test="'PE'=/message_history/search_criteria/status">
                        <xsl:attribute name="SELECTED"></xsl:attribute>
                    </xsl:if>Processed Error
                </xsl:element>
                <xsl:element name="option">
                    <xsl:attribute name="value"></xsl:attribute>
                    <xsl:if test="''=/message_history/search_criteria/status">
                        <xsl:attribute name="SELECTED"></xsl:attribute>
                    </xsl:if>
                    <xsl:if test="''=/message_history/search_criteria/status">
                        <xsl:attribute name="SELECTED"></xsl:attribute>
                    </xsl:if>All    
                </xsl:element>
            </select>
        </td>        
        <td width="20%">Show Detail?</td>
        <td width="30%">
            Yes
            <input type="radio">
                <xsl:attribute name="name">is_detail</xsl:attribute>
                <xsl:attribute name="value">true</xsl:attribute>     
                <xsl:if test="'true'=/message_history/search_criteria/is_detail">
                    <xsl:attribute name="CHECKED"></xsl:attribute>
                </xsl:if>       
            </input>
            No
            <input type="radio">
                <xsl:attribute name="name">is_detail</xsl:attribute>
                <xsl:attribute name="value">false</xsl:attribute>
                <xsl:if test="'false'=/message_history/search_criteria/is_detail">
                    <xsl:attribute name="CHECKED"></xsl:attribute>
                </xsl:if>
                <xsl:if test="''=/message_history/search_criteria/is_detail">
                    <xsl:attribute name="CHECKED"></xsl:attribute>
                </xsl:if>
            </input>
        </td>
      </tr>
      
      <tr>
        <td width="20%">Messages for the Last</td>
        <td width="30%">
        	<select name="message_time">
        		<xsl:element name="option">
        			<xsl:attribute name="value"></xsl:attribute>
        			<xsl:if test="'all'=/message_history/search_criteria/message_time">
        				<xsl:attribute name="SELECTED"></xsl:attribute>
        			</xsl:if>
        			<xsl:if test="''=/message_history/search_criteria/message_time">
        				<xsl:attribute name="SELECTED"></xsl:attribute>
        			</xsl:if>All
        		</xsl:element>
        		<xsl:element name="option">
        			<xsl:attribute name="value">1</xsl:attribute>
        			<xsl:if test="'1'=/message_history/search_criteria/message_time">
        				<xsl:attribute name="SELECTED"></xsl:attribute>
        			</xsl:if>One Month
        		</xsl:element>
        		<xsl:element name="option">
        			<xsl:attribute name="value">3</xsl:attribute>
        			<xsl:if test="'3'=/message_history/search_criteria/message_time">
        				<xsl:attribute name="SELECTED"></xsl:attribute>
        			</xsl:if>Three Months
        		</xsl:element>
        		<xsl:element name="option">
        			<xsl:attribute name="value">6</xsl:attribute>
        			<xsl:if test="'6'=/message_history/search_criteria/message_time">
        				<xsl:attribute name="SELECTED"></xsl:attribute>
        			</xsl:if>Six Months
        		</xsl:element>
        	</select>			
        </td>
        <td width="20%">Number of Messages</td>
        <td width="30%">
            <select name="num_of_messages">
                <xsl:element name="option">
                    <xsl:if test="'20'=/message_history/search_criteria/num_of_messages">
                        <xsl:attribute name="SELECTED"></xsl:attribute>
                    </xsl:if>
                    <xsl:if test="''=/message_history/search_criteria/num_of_messages">
                        <xsl:attribute name="SELECTED"></xsl:attribute>
                    </xsl:if>20    
                </xsl:element>
                <xsl:element name="option">
                    <xsl:if test="'50'=/message_history/search_criteria/num_of_messages">
                        <xsl:attribute name="SELECTED"></xsl:attribute>
                    </xsl:if>50    
                </xsl:element>
                <xsl:element name="option">
                    <xsl:if test="'100'=/message_history/search_criteria/num_of_messages">
                        <xsl:attribute name="SELECTED"></xsl:attribute>
                    </xsl:if>100    
                </xsl:element>
                <xsl:element name="option">
                    <xsl:if test="'500'=/message_history/search_criteria/num_of_messages">
                        <xsl:attribute name="SELECTED"></xsl:attribute>
                    </xsl:if>500    
                </xsl:element>
            </select>
        </td>        
      </tr>  
      
      <tr>
        <td width="20%"></td>
        <td width="30%"></td>
        <td width="20%"></td>
        <td width="30%"><input type="Submit" name="action" value="search"/></td>
      </tr> 
      
    </table>
</form>
<!-- search form - end --> 

<br/>

<div class="stat-grid">
  <div class="stat-card">
    <h3>Search Result</h3>
    <div class="stat-row"><span class="label">Total Number of Messages</span><span class="value big"><xsl:value-of select="/message_history/total_no_of_messages" /></span></div>
    <div class="stat-row"><span class="label">Number of Messages Returned</span><span class="value"><xsl:value-of select="count(message)" /></span></div>
  </div>
</div>

<br/>

<div class="page-links">
    <span>
        <xsl:if test="number(/message_history/search_criteria/offset) - number(/message_history/search_criteria/num_of_messages) >= 0">
            <a>
              <xsl:attribute name="href">message_history?offset=<xsl:value-of select="number(/message_history/search_criteria/offset) - number(/message_history/search_criteria/num_of_messages)" />&amp;message_id=<xsl:value-of select="/message_history/search_criteria/message_id" />&amp;message_box=<xsl:value-of select="/message_history/search_criteria/message_box" />&amp;as2_from=<xsl:value-of select="/message_history/search_criteria/as2_from" />&amp;as2_to=<xsl:value-of select="/message_history/search_criteria/as2_to" />&amp;status=<xsl:value-of select="/message_history/search_criteria/status" />&amp;num_of_messages=<xsl:value-of select="/message_history/search_criteria/num_of_messages" />&amp;is_detail=<xsl:value-of select="/message_history/search_criteria/is_detail" />&amp;primal_message_id=<xsl:value-of select="/message_history/search_criteria/primal_message_id"/></xsl:attribute>
              &#8249; Previous Page
            </a>
        </xsl:if>
    </span>
    <span>
        <xsl:if test="(number(/message_history/search_criteria/offset) + number(/message_history/search_criteria/num_of_messages)) != number(/message_history/total_no_of_messages)">
            <xsl:if test="count(message) = number(/message_history/search_criteria/num_of_messages)">
                <a>
                  <xsl:attribute name="href">message_history?offset=<xsl:value-of select="number(/message_history/search_criteria/offset) + count(message)" />&amp;message_id=<xsl:value-of select="/message_history/search_criteria/message_id" />&amp;message_box=<xsl:value-of select="/message_history/search_criteria/message_box" />&amp;as2_from=<xsl:value-of select="/message_history/search_criteria/as2_from" />&amp;as2_to=<xsl:value-of select="/message_history/search_criteria/as2_to" />&amp;status=<xsl:value-of select="/message_history/search_criteria/status" />&amp;num_of_messages=<xsl:value-of select="/message_history/search_criteria/num_of_messages" />&amp;is_detail=<xsl:value-of select="/message_history/search_criteria/is_detail" />&amp;primal_message_id=<xsl:value-of select="/message_history/search_criteria/primal_message_id"/></xsl:attribute>
                  Next Page &#8250;
                </a>
            </xsl:if>
        </xsl:if>
    </span>
</div>

<form name="viewMessageForm" method="post" action="./repository">
    <input type="hidden" name="message_id" value="" />
    <input type="hidden" name="message_box" value="" />
    <input type="hidden" name="is_download_receipt" value="" />
    <input type="hidden" name="mode" value="" />
</form>

<form name="showMDNForm" method="post" action="./message_history">
    <input type="hidden" name="original_message_id" value="" />
    <input type="hidden" name="original_message_box" value="" />
</form>

<form name="showMessageForm" method="post" action="./message_history">
	<input type="hidden" name="message_id" value=""/>
	<input type="hidden" name="message_box" value=""/>
</form>

<form name="showResendMessageForm" method="post" action="./message_history">
	<input type="hidden" name="primal_message_id" value=""/>
</form>

<!-- Loop the message history - start-->
<table border="0" cellpadding="2" cellspacing="2" width="100%">          
  <xsl:for-each select="message">
  <tr>
    <th colspan="2"/>
  </tr>
  <tr>
    <td width="40%"><xsl:value-of select="number(/message_history/search_criteria/offset) + position()" /></td>
    <td width="60%" align="right"><a href="#">Top</a></td>
  </tr>
  <tr>
    <td width="40%"><b>Message ID</b></td>
    <td width="60%"><b>
        <xsl:value-of select="./message_id" />
        <a href="#1" title="Click here to view the raw message content in a new tab" style="margin-left:8px;">
            <xsl:attribute name="onclick">viewMessageInline('<xsl:value-of select="./message_id" />','<xsl:value-of select="./message_box" />','false')</xsl:attribute>
            View
        </a>
        <a href="#1" title="Click here to download the message" style="margin-left:8px;">
            <xsl:attribute name="onclick">viewMessage('<xsl:value-of select="./message_id" />','<xsl:value-of select="./message_box" />','false')</xsl:attribute>
            &#8659; Download
        </a>
    </b></td>
  </tr>
  <tr>
    <td width="40%"><b>Message Box</b></td>
    <td width="60%"><b><xsl:value-of select="./message_box" /></b></td>
  </tr>
  <tr>
    <td width="40%">AS2 From</td>
    <td width="60%"><xsl:value-of select="./as2_from" /></td>
  </tr>
  <tr>
    <td width="40%">AS2 To</td>
    <td width="60%"><xsl:value-of select="./as2_to" /></td>
  </tr>
  <xsl:if test="./is_receipt != ''">
      <tr>
        <td width="40%">Is Receipt</td>
        <td width="60%"><xsl:value-of select="./is_receipt" /></td>
      </tr>
  </xsl:if>
  <xsl:if test="./is_receipt = 'false'">
      <xsl:if test="./is_receipt_requested != ''">
          <tr>
            <td width="40%">Is Receipt Requested</td>
            <td width="60%"><xsl:value-of select="./is_receipt_requested" /></td>
          </tr>
      </xsl:if>
      <xsl:if test="./is_receipt_requested = 'true'">
          <xsl:if test="./is_acknowledged != ''">
              <tr>
                <td width="40%">Is Acknowledged</td>
                <td width="60%">                        
                    <xsl:if test="./is_acknowledged = 'false'">
                        <xsl:value-of select="./is_acknowledged" />
                    </xsl:if>
                    
                    <xsl:if test="./is_acknowledged = 'true'">
                        <a href="#1" title="Click here to show the receipt(s)">
                            <xsl:attribute name="onclick">showMDN('<xsl:value-of select="./message_id" />','<xsl:value-of select="./message_box" />')</xsl:attribute>
                            <xsl:if test="./status = 'PE'">
                                <font color="red">Negative Acknowledgement</font>
                            </xsl:if>
                            <xsl:if test="./status != 'PE'">
                                <font color="blue">Positive Acknowledgement</font>
                            </xsl:if>
                        </a>
                    </xsl:if>
                </td>
              </tr>
          </xsl:if>
      </xsl:if>
  </xsl:if>  
  <xsl:if test="./receipt_url != ''">
      <tr>
        <td width="40%">Receipt URL</td>
        <td width="60%"><xsl:value-of select="./receipt_url" /></td>
      </tr>
  </xsl:if>
  <xsl:if test="./is_receipt = 'true'">
      <xsl:if test="./mic_value != ''">
          <tr>
            <td width="40%">MIC Value</td>
            <td width="60%"><xsl:value-of select="./mic_value" /></td>
          </tr>
      </xsl:if>
      <xsl:if test="./original_message_id != ''">
          <tr>
            <td width="40%">Original Message ID</td>
            <td width="60%"><xsl:value-of select="./original_message_id" /></td>
          </tr>
      </xsl:if>
  </xsl:if>
  <xsl:if test="./created_via = 'webservice_api'">
  <tr>
    <td width="40%">Source</td>
    <td width="60%"><span class="badge badge-info">Web Service API</span></td>
  </tr>
  </xsl:if>
  <tr>
    <td width="40%">Timestamp</td>
    <td width="60%"><xsl:value-of select="./time_stamp" /></td>
  </tr>
  <xsl:if test="./primal_message_id != ''">
  <tr>
    <td width="40%">Primal Message ID<br /> <small><font color="gray">Message that triggers [Resend as New]</font></small></td>
    <td width="60%">                        
      <a href="#1" title="Click here to show the primal message">
        <xsl:attribute name="onclick">showMessage('<xsl:value-of select="./primal_message_id" />','OUT')</xsl:attribute>
        <xsl:value-of select="./primal_message_id" />
	  </a>
	</td>
  </tr>
  </xsl:if>
  <xsl:if test="./has_resend_as_new = 'true'">
  <tr>
    <td width="40%">Has Resend As New</td>
    <td width="60%">                        
      <a href="#1" title="Click here to show the &quot;Resent As New&quot; messages">
        <xsl:attribute name="onclick">showResendAsNewMessage('<xsl:value-of select="./message_id" />')</xsl:attribute>
        Click here to show the "Resend As New" messages
	  </a>
	</td>
  </tr>
  </xsl:if>
  <tr>
    <td width="40%">Status</td>
    <td width="60%">
        <xsl:if test="./status = 'RC'">
            <span class="badge badge-info">Received</span>
        </xsl:if>
        <xsl:if test="./status = 'PD'">
            <span class="badge badge-neutral">Pending</span>
        </xsl:if>
        <xsl:if test="./status = 'PR'">
            <span class="badge badge-info">Processing</span>
        </xsl:if>
        <xsl:if test="./status = 'PS'">
            <span class="badge badge-success">Processed</span>
			<xsl:if test="(./message_box = 'OUT') and (./is_receipt = 'false')">
            	<form style="display: inline" name="resendAsNewForm" method="post" action="./resend_as_new">
	                <input type="hidden" name="primal_message_id">
	                    <xsl:attribute name="value">
	                        <xsl:value-of select="./message_id" />
	                    </xsl:attribute>
	                 </input>
	                <input type="submit" name="submit" value="Resend as new" />
	            </form>
            </xsl:if>
        </xsl:if>
        <xsl:if test="./status = 'PE'">
            <span class="badge badge-danger">Processed Error</span>
			<xsl:if test="(./message_box = 'OUT') and (./is_receipt = 'false')">
            	<form style="display: inline" name="resendAsNewForm" method="post" action="./resend_as_new">
	                <input type="hidden" name="primal_message_id">
	                    <xsl:attribute name="value">
	                        <xsl:value-of select="./message_id" />
	                    </xsl:attribute>
	                 </input>
	                <input type="submit" name="submit" value="Resend as new" />
	            </form>
            </xsl:if>
        </xsl:if>
        <xsl:if test="./status = 'DL'">
            <span class="badge badge-success">Delivered</span>
            <xsl:if test="(./message_box = 'OUT') and (./is_receipt = 'false')">
            	<form style="display: inline" name="resendAsNewForm" method="post" action="./resend_as_new">
	                <input type="hidden" name="primal_message_id">
	                    <xsl:attribute name="value">
	                        <xsl:value-of select="./message_id" />
	                    </xsl:attribute>
	                 </input>
	                <input type="submit" name="submit" value="Resend as new" />
	            </form>
            </xsl:if>
        </xsl:if>
        <xsl:if test="./status = 'DF'">
        	<span class="badge badge-danger">Delivery Failure</span>
        	<xsl:if test="(./message_box = 'OUT') and (./is_receipt = 'false')">
            	<form style="display: inline" name="changeMessageStatusForm" method="post" action="./change_message_status" onSubmit="return confirm('Are you sure to retry this message?\r\nTo know more, please refer to\r\nAdministration Tool User Guide [Section 5.5.1]');">
                	<input type="hidden" name="message_id">
                    	<xsl:attribute name="value">
                        	<xsl:value-of select="./message_id" />
                    	</xsl:attribute>
                 	</input>
                	<input type="submit" name="submit" value="Retry" />
            	</form>
				<form style="display: inline" name="resendAsNewForm" method="post" action="./resend_as_new">
	                <input type="hidden" name="primal_message_id">
	                    <xsl:attribute name="value">
	                        <xsl:value-of select="./message_id" />
	                    </xsl:attribute>
	                 </input>
	                <input type="submit" name="submit" value="Resend as new" />
	            </form>
            </xsl:if>
        </xsl:if>
    </td>
  </tr>
  <xsl:if test="./status_description != ''">
      <tr>
        <td width="40%">Status Description</td>
        <td width="60%"><textarea class="special" rows="5" cols="55" wrap="HARD"><xsl:attribute name="readonly"></xsl:attribute><xsl:value-of select="./status_description" /></textarea></td>
      </tr>
  </xsl:if>

</xsl:for-each>
</table>
  
  <!-- Loop the message history - end-->
<br/>

<div class="page-links">
    <span>
        <xsl:if test="number(/message_history/search_criteria/offset) - number(/message_history/search_criteria/num_of_messages) >= 0">
            <a>
              <xsl:attribute name="href">message_history?offset=<xsl:value-of select="number(/message_history/search_criteria/offset) - number(/message_history/search_criteria/num_of_messages)" />&amp;message_id=<xsl:value-of select="/message_history/search_criteria/message_id" />&amp;message_box=<xsl:value-of select="/message_history/search_criteria/message_box" />&amp;as2_from=<xsl:value-of select="/message_history/search_criteria/as2_from" />&amp;as2_to=<xsl:value-of select="/message_history/search_criteria/as2_to" />&amp;status=<xsl:value-of select="/message_history/search_criteria/status" />&amp;num_of_messages=<xsl:value-of select="/message_history/search_criteria/num_of_messages" />&amp;is_detail=<xsl:value-of select="/message_history/search_criteria/is_detail" />&amp;primal_message_id=<xsl:value-of select="/message_history/search_criteria/primal_message_id"/></xsl:attribute>
              &#8249; Previous Page
            </a>
        </xsl:if>
    </span>
    <span>
        <xsl:if test="(number(/message_history/search_criteria/offset) + number(/message_history/search_criteria/num_of_messages)) != number(/message_history/total_no_of_messages)">
            <xsl:if test="count(message) = number(/message_history/search_criteria/num_of_messages)">
                <a>
                  <xsl:attribute name="href">message_history?offset=<xsl:value-of select="number(/message_history/search_criteria/offset) + count(message)" />&amp;message_id=<xsl:value-of select="/message_history/search_criteria/message_id" />&amp;message_box=<xsl:value-of select="/message_history/search_criteria/message_box" />&amp;as2_from=<xsl:value-of select="/message_history/search_criteria/as2_from" />&amp;as2_to=<xsl:value-of select="/message_history/search_criteria/as2_to" />&amp;status=<xsl:value-of select="/message_history/search_criteria/status" />&amp;num_of_messages=<xsl:value-of select="/message_history/search_criteria/num_of_messages" />&amp;is_detail=<xsl:value-of select="/message_history/search_criteria/is_detail" />&amp;primal_message_id=<xsl:value-of select="/message_history/search_criteria/primal_message_id"/></xsl:attribute>
                  Next Page &#8250;
                </a>
            </xsl:if>
        </xsl:if>
    </span>
</div>

</xsl:template>

<xsl:template match="/error">

<b>Error occurs</b>
<p>Operation : <xsl:value-of select="./operation" /></p>

<p>Exception :</p>
<pre><xsl:value-of select="./exception_message"/></pre>
</xsl:template>
</xsl:stylesheet>