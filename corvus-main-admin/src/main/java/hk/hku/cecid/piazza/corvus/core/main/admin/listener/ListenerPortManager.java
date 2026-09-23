package hk.hku.cecid.piazza.corvus.core.main.admin.listener;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.security.KeyStore;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;

import hk.hku.cecid.piazza.corvus.core.main.admin.AdminMainProcessor;
import hk.hku.cecid.piazza.corvus.core.main.admin.dao.ListenerPortDVO;

/**
 * ListenerPortManager keeps a set of standalone HTTP(S) listeners -- one per
 * enabled {@code listener_port} row -- in sync with the database. Each one
 * binds its own configured address/port (e.g. a dedicated
 * https://203.0.113.10:5443 handed to one particular partner, distinct from
 * every other partner's own dedicated port and from the shared admin/web
 * port) and simply forwards whatever it receives to the existing internal
 * handler for the row's {@link ListenerPortDVO#getTargetService() target
 * service} -- the same handler the shared admin/web port already dispatches
 * to under /corvus/httpd/&lt;service&gt;/inbound -- so this does not
 * duplicate any protocol logic, it only gives that traffic its own socket.
 *
 * This lives in corvus-main-admin (not any one protocol plugin) because it
 * is a core gateway capability: HTTP/HTTPS delivery is already built in for
 * every protocol module on the shared port, and a dedicated per-partner port
 * is just an alternate front door onto that same handling, not a new
 * protocol feature.
 *
 * {@link #reconcile(List)} is idempotent and cheap to call repeatedly: a
 * port whose configuration has not changed since the last call is left
 * running untouched; a port that disappeared (disabled/deleted) has its
 * listener stopped; a port that is new or whose bind address/port/TLS/target
 * changed is (re)started.
 */
public class ListenerPortManager {

    // The Tomcat connector this webapp is actually deployed on -- see
    // deploy/app_server/server.xml, which reads this same system property.
    // Proxied requests are always plain HTTP over loopback regardless of the
    // external listener's own TLS setting, since the hop never leaves the
    // container.
    private static final int INTERNAL_HTTP_PORT =
            Integer.parseInt(System.getProperty("catalina.http.port", "8080"));
    private static final String INTERNAL_CONTEXT = "/corvus";

    // The same self-signed keystore Tomcat's own HTTPS connector uses (see
    // deploy/app_server/Dockerfile) -- one cert for the whole gateway,
    // reused here rather than requiring a second one to be provisioned.
    private static final String SHARED_KEYSTORE_PATH = "/etc/tomcat8/ssl/keystore.jks";
    private static final String SHARED_KEYSTORE_PASS = "corvusssl";
    private static final String SHARED_KEYSTORE_ALIAS = "corvus";

    private static final Map TARGET_SERVICE_PATHS = new HashMap();
    static {
        TARGET_SERVICE_PATHS.put("ebms", "/httpd/ebms/inbound");
        TARGET_SERVICE_PATHS.put("as2", "/httpd/as2/inbound");
        TARGET_SERVICE_PATHS.put("sfrm", "/httpd/sfrm/inbound");
    }

    private static final Map RUNNING = new HashMap();

    private ListenerPortManager() {
    }

    /**
     * Starts, restarts or stops listeners so the running set matches
     * exactly the given enabled ports.
     */
    public static synchronized void reconcile(List enabledPorts) {
        Set stillEnabled = new HashSet();
        for (Iterator i = enabledPorts.iterator(); i.hasNext(); ) {
            ListenerPortDVO port = (ListenerPortDVO) i.next();
            stillEnabled.add(port.getPortId());
            RunningListener existing = (RunningListener) RUNNING.get(port.getPortId());
            if (existing != null && existing.matches(port)) {
                continue;
            }
            if (existing != null) {
                stop(port.getPortId());
            }
            start(port);
        }

        // Stop anything running that is no longer in the enabled set.
        Set toStop = new HashSet(RUNNING.keySet());
        toStop.removeAll(stillEnabled);
        for (Iterator i = toStop.iterator(); i.hasNext(); ) {
            stop((String) i.next());
        }
    }

    private static void start(ListenerPortDVO port) {
        try {
            InetSocketAddress addr = new InetSocketAddress(port.getBindAddress(), port.getPort());
            String targetPath = (String) TARGET_SERVICE_PATHS.get(port.getTargetService());
            if (targetPath == null) {
                AdminMainProcessor.core.log.error(
                        "Listener port '" + port.getName() + "' has unknown target service '"
                                + port.getTargetService() + "', not starting");
                return;
            }

            HttpServer server;
            if (port.isUseTls()) {
                HttpsServer httpsServer = HttpsServer.create(addr, 0);
                httpsServer.setHttpsConfigurator(new HttpsConfigurator(buildSslContext()));
                server = httpsServer;
            } else {
                server = HttpServer.create(addr, 0);
            }

            server.createContext("/", new ProxyHandler(port.getName(), targetPath));
            server.setExecutor(Executors.newCachedThreadPool());
            server.start();

            RUNNING.put(port.getPortId(), new RunningListener(port, server));
            AdminMainProcessor.core.log.info(
                    "Listener port '" + port.getName() + "' listening on "
                            + (port.isUseTls() ? "https://" : "http://")
                            + port.getBindAddress() + ":" + port.getPort()
                            + " -> " + port.getTargetService());
        } catch (Exception e) {
            AdminMainProcessor.core.log.error(
                    "Listener port '" + port.getName() + "' failed to start on "
                            + port.getBindAddress() + ":" + port.getPort(), e);
        }
    }

    private static void stop(String portId) {
        RunningListener listener = (RunningListener) RUNNING.remove(portId);
        if (listener == null) {
            return;
        }
        try {
            listener.server.stop(0);
            AdminMainProcessor.core.log.info(
                    "Listener port '" + listener.port.getName() + "' stopped");
        } catch (Exception e) {
            AdminMainProcessor.core.log.error(
                    "Listener port '" + listener.port.getName() + "' failed to stop cleanly", e);
        }
    }

    /**
     * Stops every listener this manager started. Best-effort -- called when
     * the admin module shuts down.
     */
    public static synchronized void stopAll() {
        Set ids = new HashSet(RUNNING.keySet());
        for (Iterator i = ids.iterator(); i.hasNext(); ) {
            stop((String) i.next());
        }
    }

    private static SSLContext buildSslContext() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("JKS");
        InputStream in = new FileInputStream(SHARED_KEYSTORE_PATH);
        try {
            keyStore.load(in, SHARED_KEYSTORE_PASS.toCharArray());
        } finally {
            in.close();
        }

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, SHARED_KEYSTORE_PASS.toCharArray());

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), null, null);
        return sslContext;
    }

    private static class RunningListener {
        final ListenerPortDVO port;
        final HttpServer server;

        RunningListener(ListenerPortDVO port, HttpServer server) {
            this.port = port;
            this.server = server;
        }

        boolean matches(ListenerPortDVO other) {
            return equalsOrNull(port.getBindAddress(), other.getBindAddress())
                    && port.getPort() == other.getPort()
                    && port.isUseTls() == other.isUseTls()
                    && equalsOrNull(port.getTargetService(), other.getTargetService());
        }

        private static boolean equalsOrNull(String a, String b) {
            return a == null ? b == null : a.equals(b);
        }
    }

    /**
     * Forwards whatever is received verbatim (method, headers, body) to the
     * internal handler and relays its response back -- a plain reverse
     * proxy, no protocol-specific parsing here.
     */
    private static class ProxyHandler implements HttpHandler {

        private final String portName;
        private final String targetPath;

        ProxyHandler(String portName, String targetPath) {
            this.portName = portName;
            this.targetPath = targetPath;
        }

        public void handle(HttpExchange exchange) throws IOException {
            try {
                URL target = new URL("http", "localhost", INTERNAL_HTTP_PORT, INTERNAL_CONTEXT + targetPath);
                HttpURLConnection conn = (HttpURLConnection) target.openConnection();
                conn.setRequestMethod(exchange.getRequestMethod());
                conn.setDoInput(true);
                conn.setInstanceFollowRedirects(false);

                copyRequestHeaders(exchange, conn);

                byte[] requestBody = readFully(exchange.getRequestBody());
                if (requestBody.length > 0 || "POST".equalsIgnoreCase(exchange.getRequestMethod())
                        || "PUT".equalsIgnoreCase(exchange.getRequestMethod())) {
                    conn.setDoOutput(true);
                    OutputStream out = conn.getOutputStream();
                    out.write(requestBody);
                    out.close();
                }

                int responseCode = conn.getResponseCode();
                InputStream responseStream = responseCode < 400 ? conn.getInputStream() : conn.getErrorStream();
                byte[] responseBody = responseStream == null ? new byte[0] : readFully(responseStream);

                copyResponseHeaders(conn, exchange);
                exchange.sendResponseHeaders(responseCode, responseBody.length);
                OutputStream os = exchange.getResponseBody();
                os.write(responseBody);
                os.close();
            } catch (Exception e) {
                AdminMainProcessor.core.log.error(
                        "Listener port '" + portName + "' failed to forward a request", e);
                exchange.sendResponseHeaders(502, -1);
            } finally {
                exchange.close();
            }
        }

        private void copyRequestHeaders(HttpExchange exchange, HttpURLConnection conn) {
            Map headers = exchange.getRequestHeaders();
            for (Iterator i = headers.keySet().iterator(); i.hasNext(); ) {
                String name = (String) i.next();
                if ("Host".equalsIgnoreCase(name) || "Content-Length".equalsIgnoreCase(name)
                        || "Transfer-Encoding".equalsIgnoreCase(name)) {
                    // Let HttpURLConnection manage these itself.
                    continue;
                }
                List values = (List) headers.get(name);
                for (Iterator v = values.iterator(); v.hasNext(); ) {
                    conn.addRequestProperty(name, (String) v.next());
                }
            }
        }

        private void copyResponseHeaders(HttpURLConnection conn, HttpExchange exchange) {
            Map headers = conn.getHeaderFields();
            for (Iterator i = headers.keySet().iterator(); i.hasNext(); ) {
                String name = (String) i.next();
                if (name == null || "Transfer-Encoding".equalsIgnoreCase(name)
                        || "Content-Length".equalsIgnoreCase(name)) {
                    continue;
                }
                List values = (List) headers.get(name);
                for (Iterator v = values.iterator(); v.hasNext(); ) {
                    exchange.getResponseHeaders().add(name, (String) v.next());
                }
            }
        }

        private byte[] readFully(InputStream in) throws IOException {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            int read;
            while ((read = in.read(chunk)) != -1) {
                buf.write(chunk, 0, read);
            }
            return buf.toByteArray();
        }
    }
}
