package hk.hku.cecid.edi.sfrm.task;

import java.io.ByteArrayOutputStream;
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

import hk.hku.cecid.edi.sfrm.dao.HttpChannelDVO;
import hk.hku.cecid.edi.sfrm.spa.SFRMProcessor;
import hk.hku.cecid.piazza.commons.security.KeyStoreManager;

/**
 * HttpChannelListenerManager keeps a set of standalone HTTP(S) listeners --
 * one per enabled {@code sfrm_http_channel} row -- in sync with the database.
 * Each listener binds its own configured address/port, separate from the
 * shared admin/web management port, and simply forwards whatever it
 * receives to the existing internal handler for the channel's
 * {@link HttpChannelDVO#getTargetService() target service} (the same
 * handler the shared port already dispatches to under
 * /corvus/httpd/&lt;service&gt;/inbound) -- so adding a port here does not
 * duplicate any protocol logic, it only gives that traffic its own socket.
 *
 * {@link #reconcile(List)} is idempotent and cheap to call every collector
 * cycle: a channel whose configuration has not changed since the last call
 * is left running untouched; a channel that disappeared (disabled/deleted)
 * has its listener stopped; a channel that is new or whose bind
 * address/port/TLS/target changed is (re)started.
 *
 * @author Hermes2+ (HTTP port type)
 */
public class HttpChannelListenerManager {

    // The Tomcat connector this webapp is actually deployed on -- see
    // deploy/app_server/server.xml. Proxied requests are always plain HTTP
    // over loopback regardless of the external listener's own TLS setting,
    // since the hop never leaves the container.
    private static final int INTERNAL_HTTP_PORT = 8080;
    private static final String INTERNAL_CONTEXT = "/corvus";

    private static final Map TARGET_SERVICE_PATHS = new HashMap();
    static {
        TARGET_SERVICE_PATHS.put("ebms", "/httpd/ebms/inbound");
        TARGET_SERVICE_PATHS.put("as2", "/httpd/as2/inbound");
        TARGET_SERVICE_PATHS.put("sfrm", "/httpd/sfrm/inbound");
    }

    private static final Map RUNNING = new HashMap();

    private HttpChannelListenerManager() {
    }

    /**
     * Starts, restarts or stops listeners so the running set matches
     * exactly the given enabled channels.
     */
    public static synchronized void reconcile(List enabledChannels) {
        Set stillEnabled = new HashSet();
        for (Iterator i = enabledChannels.iterator(); i.hasNext(); ) {
            HttpChannelDVO channel = (HttpChannelDVO) i.next();
            stillEnabled.add(channel.getChannelId());
            RunningListener existing = (RunningListener) RUNNING.get(channel.getChannelId());
            if (existing != null && existing.matches(channel)) {
                continue;
            }
            if (existing != null) {
                stop(channel.getChannelId());
            }
            start(channel);
        }

        // Stop anything running that is no longer in the enabled set.
        Set toStop = new HashSet(RUNNING.keySet());
        toStop.removeAll(stillEnabled);
        for (Iterator i = toStop.iterator(); i.hasNext(); ) {
            stop((String) i.next());
        }
    }

    private static void start(HttpChannelDVO channel) {
        try {
            InetSocketAddress addr = new InetSocketAddress(channel.getBindAddress(), channel.getPort());
            String targetPath = (String) TARGET_SERVICE_PATHS.get(channel.getTargetService());
            if (targetPath == null) {
                SFRMProcessor.getInstance().getLogger().error(
                        "HTTP channel '" + channel.getName() + "' has unknown target service '"
                                + channel.getTargetService() + "', not starting");
                return;
            }

            HttpServer server;
            if (channel.isUseTls()) {
                HttpsServer httpsServer = HttpsServer.create(addr, 0);
                httpsServer.setHttpsConfigurator(new HttpsConfigurator(buildSslContext()));
                server = httpsServer;
            } else {
                server = HttpServer.create(addr, 0);
            }

            server.createContext("/", new ProxyHandler(channel.getName(), targetPath));
            server.setExecutor(Executors.newCachedThreadPool());
            server.start();

            RUNNING.put(channel.getChannelId(), new RunningListener(channel, server));
            SFRMProcessor.getInstance().getLogger().info(
                    "HTTP channel '" + channel.getName() + "' listening on "
                            + (channel.isUseTls() ? "https://" : "http://")
                            + channel.getBindAddress() + ":" + channel.getPort()
                            + " -> " + channel.getTargetService());
        } catch (Exception e) {
            SFRMProcessor.getInstance().getLogger().error(
                    "HTTP channel '" + channel.getName() + "' failed to start on "
                            + channel.getBindAddress() + ":" + channel.getPort(), e);
        }
    }

    private static void stop(String channelId) {
        RunningListener listener = (RunningListener) RUNNING.remove(channelId);
        if (listener == null) {
            return;
        }
        try {
            listener.server.stop(0);
            SFRMProcessor.getInstance().getLogger().info(
                    "HTTP channel '" + listener.channel.getName() + "' stopped");
        } catch (Exception e) {
            SFRMProcessor.getInstance().getLogger().error(
                    "HTTP channel '" + listener.channel.getName() + "' failed to stop cleanly", e);
        }
    }

    /**
     * Stops every listener this manager started. Best-effort -- called when
     * the SFRM module shuts down.
     */
    public static synchronized void stopAll() {
        Set ids = new HashSet(RUNNING.keySet());
        for (Iterator i = ids.iterator(); i.hasNext(); ) {
            stop((String) i.next());
        }
    }

    /**
     * Builds an SSLContext from the SFRM plugin's already-configured shared
     * keystore (see the "keystore-manager" component in
     * sfrm.module.core.xml), without needing that keystore's own password:
     * the already-unlocked private key/cert chain are copied into a fresh,
     * in-memory keystore under a throwaway password known only here.
     */
    private static SSLContext buildSslContext() throws Exception {
        KeyStoreManager ksm = SFRMProcessor.getInstance().getKeyStoreManager();
        char[] throwawayPass = "http-channel".toCharArray();

        KeyStore inMemory = KeyStore.getInstance("JKS");
        inMemory.load(null, null);
        inMemory.setKeyEntry(ksm.getAlias(), ksm.getPrivateKey(), throwawayPass, ksm.getCertificateChain());

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(inMemory, throwawayPass);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), null, null);
        return sslContext;
    }

    private static class RunningListener {
        final HttpChannelDVO channel;
        final HttpServer server;

        RunningListener(HttpChannelDVO channel, HttpServer server) {
            this.channel = channel;
            this.server = server;
        }

        boolean matches(HttpChannelDVO other) {
            return equalsOrNull(channel.getBindAddress(), other.getBindAddress())
                    && channel.getPort() == other.getPort()
                    && channel.isUseTls() == other.isUseTls()
                    && equalsOrNull(channel.getTargetService(), other.getTargetService());
        }

        private static boolean equalsOrNull(String a, String b) {
            return a == null ? b == null : a.equals(b);
        }
    }

    /**
     * Forwards whatever is received verbatim (method, headers, body) to the
     * internal handler and relays its response back -- a plain reverse
     * proxy, no SFRM-specific parsing here.
     */
    private static class ProxyHandler implements HttpHandler {

        private final String channelName;
        private final String targetPath;

        ProxyHandler(String channelName, String targetPath) {
            this.channelName = channelName;
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
                SFRMProcessor.getInstance().getLogger().error(
                        "HTTP channel '" + channelName + "' failed to forward a request", e);
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
