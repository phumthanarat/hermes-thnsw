package hk.hku.cecid.piazza.corvus.core.main.admin.listener;

import hk.hku.cecid.piazza.commons.servlet.http.HttpDispatcherContext;
import hk.hku.cecid.piazza.commons.util.PropertyTree;
import hk.hku.cecid.piazza.corvus.admin.listener.AdminPageletAdaptor;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.xml.transform.Source;

/**
 * HttpdPageletAdaptor is an admin pagelet adaptor which provides an admin 
 * function of the default HTTP dispatcher.
 * 
 * @author Hugo Y. K. Lam
 *
 */
public class HttpdPageletAdaptor extends AdminPageletAdaptor {

    // Tomcat has no "disabled" attribute for a Connector -- the only way to turn
    // one off is to remove it from server.xml. These sentinel comments let the
    // Connector element stay in place (so its configured attributes aren't lost)
    // while making it invisible to Tomcat's XML parser.
    private static final String HTTP_DISABLED_START = "<!--HERMES-PORT:HTTP:DISABLED-START-->";
    private static final String HTTP_DISABLED_END = "<!--HERMES-PORT:HTTP:DISABLED-END-->";
    private static final String HTTPS_DISABLED_START = "<!--HERMES-PORT:HTTPS:DISABLED-START-->";
    private static final String HTTPS_DISABLED_END = "<!--HERMES-PORT:HTTPS:DISABLED-END-->";

    // Matches only the live Connector element (port= immediately follows
    // "Connector"), never the pre-existing commented-out "shared executor"
    // example a few lines below it, which starts with executor= instead.
    private static final Pattern HTTP_CONNECTOR = Pattern.compile(
            "<Connector\\s+port=\"(\\d+)\"\\s+protocol=\"HTTP/1\\.1\".*?/>", Pattern.DOTALL);
    private static final Pattern HTTPS_CONNECTOR = Pattern.compile(
            "<Connector\\s+port=\"(\\d+)\"\\s+protocol=\"org\\.apache\\.coyote\\.http11\\.Http11NioProtocol\".*?/>",
            Pattern.DOTALL);

    /**
     * Generates the transformation source of the default HTTP dispatcher.
     *
     * @see hk.hku.cecid.piazza.corvus.admin.listener.AdminPageletAdaptor#getCenterSource(javax.servlet.http.HttpServletRequest)
     */
    protected Source getCenterSource(HttpServletRequest request) {

        PropertyTree dom = new PropertyTree();
        dom.setProperty("/httpd", "");

        String STATUS_HALTED = "Halted";
        String STATUS_RUNNING = "Running";

        String chstatus = request.getParameter(REQ_PARAM_ACTION);
        if ("halt".equals(chstatus)) {
            HttpDispatcherContext.getDefaultContext().halt();
        }
        else if ("resume".equals(chstatus)) {
            HttpDispatcherContext.getDefaultContext().resume();
        }
        else if ("toggle_connector".equals(chstatus)) {
            toggleConnector(request.getParameter("connector"));
        }
        else if ("update_connector_port".equals(chstatus)) {
            updateConnectorPort(request.getParameter("connector"), request.getParameter("port"));
        }
        else if ("ping_test".equals(chstatus)) {
            runPingTest(request, dom);
        }

        HttpDispatcherContext dispatcherContext = HttpDispatcherContext.getDefaultContext();
        String status = dispatcherContext.isHalted()? (dispatcherContext.isHalting()? "Being ":"")+STATUS_HALTED:STATUS_RUNNING;
        
        String action = "";
        if (status.equals(STATUS_HALTED)) {
            action = "resume";
        }
        else if (status.equals(STATUS_RUNNING)) {
            action = "halt";
        }
        
        dom.setProperty("status/state", status);
        dom.setProperty("status/action", action);
        dom.setProperty("status/threads", String.valueOf(dispatcherContext.getCurrentThreadCount()));
        
        Iterator contextListeners = dispatcherContext.getContextListeners().iterator();
        for (int i=1; contextListeners.hasNext(); i++) {
            String listener = contextListeners.next().getClass().getName();
            dom.setProperty("context-listeners/listener["+i+"]", listener);
        }
        
        Iterator filters = dispatcherContext.getRequestFilters().iterator();
        for (int i=1; filters.hasNext(); i++) {
            String filter = filters.next().getClass().getName();
            dom.setProperty("request-filters/filter["+i+"]", filter);
        }
        
        Properties info = dispatcherContext.getRegisteredListenersInfo();
        Enumeration pathInfos = info.keys();
        for (int i=1; pathInfos.hasMoreElements(); i++) {
            String pathInfo = pathInfos.nextElement().toString();
            String listener = info.getProperty(pathInfo);
            dom.setProperty("request-listeners/listener["+i+"]/context", pathInfo);
            dom.setProperty("request-listeners/listener["+i+"]/listener", listener);
        }

        appendConnectorInfo(dom);

        // request.getServerName()/getServerPort() reflect whatever host:port
        // the browser used to reach this page -- behind Docker's port
        // mapping (e.g. host 18443 -> container 8443), that's not reachable
        // from inside the container the ping actually runs from. Use the
        // real internal port (the same system property server.xml's
        // Connector itself resolves, see deploy/app_server/setenv.sh) and
        // localhost, since a self-ping never needs to leave the container.
        String internalPort = System.getProperty("catalina.https.port", "8443");
        dom.setProperty("ping_test/default_target", "https://localhost:" + internalPort + "/corvus/httpd/wsping");

        return dom.getSource();
    }

    /**
     * Ping-pongs {@code target_url} (defaults to this same gateway's own
     * WSPingService, but can point at another Hermes instance to check
     * server-to-server reachability) by POSTing a SOAP ping request and
     * checking for the "pong" reply the service always sends back --
     * https://.../corvus/httpd/wsping, see corvus-main's WSPingService.
     */
    private void runPingTest(HttpServletRequest request, PropertyTree dom) {
        String targetUrl = request.getParameter("target_url");
        if (targetUrl == null || targetUrl.trim().length() == 0) {
            dom.setProperty("ping_test/result", "error");
            dom.setProperty("ping_test/message", "Target URL cannot be empty");
            return;
        }
        targetUrl = targetUrl.trim();
        dom.setProperty("ping_test/target", targetUrl);

        String soapRequest = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">"
                + "<soapenv:Body><action xmlns=\"http://service.main.core.corvus.piazza.cecid.hku.hk/\">ping</action>"
                + "</soapenv:Body></soapenv:Envelope>";

        long start = System.currentTimeMillis();
        try {
            URL url = new URL(targetUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("Content-Type", "text/xml; charset=UTF-8");
            conn.setRequestProperty("SOAPAction", "\"\"");

            OutputStream out = conn.getOutputStream();
            out.write(soapRequest.getBytes("UTF-8"));
            out.close();

            int responseCode = conn.getResponseCode();
            InputStream in = responseCode < 400 ? conn.getInputStream() : conn.getErrorStream();
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            byte[] chunk = new byte[4096];
            int read;
            while (in != null && (read = in.read(chunk)) != -1) {
                buf.write(chunk, 0, read);
            }
            String responseBody = buf.toString("UTF-8");
            long elapsed = System.currentTimeMillis() - start;

            dom.setProperty("ping_test/latency_ms", String.valueOf(elapsed));
            dom.setProperty("ping_test/http_status", String.valueOf(responseCode));

            if (responseCode == 200 && responseBody.indexOf("pong") >= 0) {
                dom.setProperty("ping_test/result", "success");
                dom.setProperty("ping_test/message", "pong received in " + elapsed + " ms");
            } else {
                dom.setProperty("ping_test/result", "error");
                dom.setProperty("ping_test/message", "Unexpected response (HTTP " + responseCode + ")");
            }
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            dom.setProperty("ping_test/result", "error");
            dom.setProperty("ping_test/latency_ms", String.valueOf(elapsed));
            dom.setProperty("ping_test/message", "Unable to reach target: " + e.getMessage());
        }
    }

    /**
     * Reads the live Tomcat server.xml and reports the current port number and
     * enabled/disabled state of the HTTP and HTTPS connectors, so the admin can
     * see and change which ports the console itself listens on (closed-by-default
     * once disabled, matching the same "must be explicitly opened" posture as
     * every other port type in the system).
     */
    private void appendConnectorInfo(PropertyTree dom) {
        String content = readServerXml();
        if (content == null) {
            return;
        }

        appendOneConnector(dom, 1, "http", "HTTP", content, HTTP_CONNECTOR,
                HTTP_DISABLED_START, HTTP_DISABLED_END);
        appendOneConnector(dom, 2, "https", "HTTPS", content, HTTPS_CONNECTOR,
                HTTPS_DISABLED_START, HTTPS_DISABLED_END);
        dom.setProperty("restart-required", isRestartRequired() ? "true" : "false");
    }

    private void appendOneConnector(PropertyTree dom, int index, String id, String name, String content,
            Pattern connectorPattern, String disabledStart, String disabledEnd) {
        Matcher m = connectorPattern.matcher(content);
        if (!m.find()) {
            return;
        }
        String port = m.group(1);
        boolean enabled = !content.contains(disabledStart);

        dom.setProperty("connectors/connector[" + index + "]/id", id);
        dom.setProperty("connectors/connector[" + index + "]/name", name);
        dom.setProperty("connectors/connector[" + index + "]/port", port);
        dom.setProperty("connectors/connector[" + index + "]/enabled", enabled ? "true" : "false");
    }

    private void toggleConnector(String connectorId) {
        String content = readServerXml();
        if (content == null || connectorId == null) {
            return;
        }

        String disabledStart, disabledEnd;
        Pattern connectorPattern;
        if ("http".equals(connectorId)) {
            disabledStart = HTTP_DISABLED_START;
            disabledEnd = HTTP_DISABLED_END;
            connectorPattern = HTTP_CONNECTOR;
        } else if ("https".equals(connectorId)) {
            disabledStart = HTTPS_DISABLED_START;
            disabledEnd = HTTPS_DISABLED_END;
            connectorPattern = HTTPS_CONNECTOR;
        } else {
            return;
        }

        boolean currentlyEnabled = !content.contains(disabledStart);
        if (currentlyEnabled) {
            // Refuse to disable the last remaining open port -- that would lock
            // the console out entirely on next restart with no way back in
            // short of editing server.xml by hand inside the container.
            boolean otherEnabled = "http".equals(connectorId)
                    ? !content.contains(HTTPS_DISABLED_START)
                    : !content.contains(HTTP_DISABLED_START);
            if (!otherEnabled) {
                return;
            }
            Matcher m = connectorPattern.matcher(content);
            if (!m.find()) {
                return;
            }
            String tag = m.group();
            String replacement = disabledStart + "\n    " + tag + "\n" + disabledEnd;
            content = content.substring(0, m.start()) + replacement + content.substring(m.end());
        } else {
            int start = content.indexOf(disabledStart);
            int end = content.indexOf(disabledEnd);
            if (start < 0 || end < 0) {
                return;
            }
            String inner = content.substring(start + disabledStart.length(), end).trim();
            content = content.substring(0, start) + inner + content.substring(end + disabledEnd.length());
        }

        writeServerXml(content);
    }

    private void updateConnectorPort(String connectorId, String newPort) {
        if (newPort == null || !newPort.matches("[0-9]{1,5}")) {
            return;
        }
        String content = readServerXml();
        if (content == null || connectorId == null) {
            return;
        }

        Pattern connectorPattern = "http".equals(connectorId) ? HTTP_CONNECTOR
                : "https".equals(connectorId) ? HTTPS_CONNECTOR : null;
        if (connectorPattern == null) {
            return;
        }

        Matcher m = connectorPattern.matcher(content);
        if (!m.find()) {
            return;
        }
        String tag = m.group();
        String updatedTag = tag.replaceFirst("port=\"\\d+\"", "port=\"" + newPort + "\"");
        content = content.substring(0, m.start()) + updatedTag + content.substring(m.end());

        writeServerXml(content);
    }

    private File getServerXmlFile() {
        String catalinaBase = System.getProperty("catalina.base");
        if (catalinaBase == null) {
            return null;
        }
        return new File(catalinaBase, "conf/server.xml");
    }

    private String readServerXml() {
        File file = getServerXmlFile();
        if (file == null || !file.isFile()) {
            return null;
        }
        try {
            return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }

    private void writeServerXml(String content) {
        File file = getServerXmlFile();
        if (file == null) {
            return;
        }
        try {
            Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            // Best-effort: if the file can't be written (e.g. read-only mount),
            // the admin page simply keeps showing the previous state.
        }
    }

    /**
     * Tomcat only re-reads server.xml on startup, so a port/enabled change made
     * through this page never matches the connector the JVM is actually running
     * with until the container is restarted; this flags that gap for the UI.
     */
    private boolean isRestartRequired() {
        String content = readServerXml();
        if (content == null) {
            return false;
        }
        String httpPort = firstGroup(HTTP_CONNECTOR, content);
        String httpsPort = firstGroup(HTTPS_CONNECTOR, content);
        boolean httpEnabled = !content.contains(HTTP_DISABLED_START);
        boolean httpsEnabled = !content.contains(HTTPS_DISABLED_START);

        return !"8080".equals(httpPort) || !"8443".equals(httpsPort) || !httpEnabled || !httpsEnabled;
    }

    private String firstGroup(Pattern pattern, String content) {
        Matcher m = pattern.matcher(content);
        return m.find() ? m.group(1) : null;
    }
}
