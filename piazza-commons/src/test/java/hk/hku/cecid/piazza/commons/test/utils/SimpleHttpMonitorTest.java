package hk.hku.cecid.piazza.commons.test.utils;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import junit.framework.TestCase;

public class SimpleHttpMonitorTest extends TestCase {

    private SimpleHttpMonitor monitor;
    private int port;

    public void setUp() throws Exception {
        ServerSocket probe = new ServerSocket(0);
        port = probe.getLocalPort();
        probe.close();
        monitor = new SimpleHttpMonitor(port);
        monitor.start();
        waitUntilListening();
    }

    public void tearDown() throws Exception {
        monitor.stop();
    }

    /**
     * A header line over 256 chars used to overrun the line buffer and kill
     * the monitor thread, losing the headers after it (the intermittent
     * HttpSenderUnitTest "Missing the Basic Authorization" failure).
     */
    public void testHeaderLineLongerThanBufferIsParsed() throws Exception {
        StringBuffer longValue = new StringBuffer();
        for (int i = 0; i < 1000; i++) {
            longValue.append((char) ('a' + i % 26));
        }
        send("POST / HTTP/1.1\r\n"
                + "Host: localhost\r\n"
                + "X-Long: " + longValue + "\r\n"
                + "Authorization: Basic dGVzdDp0ZXN0\r\n"
                + "Content-Length: 5\r\n"
                + "\r\n"
                + "hello");

        assertEquals(longValue.toString(), monitor.getHeaders().get("X-Long"));
        assertEquals("Basic dGVzdDp0ZXN0", monitor.getHeaders().get("Authorization"));
    }

    /**
     * A client that disconnects mid-header must not hang the monitor: end of
     * stream used to be read as char 0xFFFF and never ended the loop.
     */
    public void testClientDisconnectMidHeaderDoesNotHangMonitor() throws Exception {
        Socket partial = new Socket("localhost", port);
        partial.getOutputStream().write("POST / HTTP/1.1\r\nHost: loc".getBytes());
        partial.getOutputStream().flush();
        partial.close();

        send("POST / HTTP/1.1\r\n"
                + "X-Next: served\r\n"
                + "Content-Length: 0\r\n"
                + "\r\n");
        assertEquals("served", monitor.getHeaders().get("X-Next"));
    }

    /** Sends a raw request and waits for the monitor's response. */
    private void send(String request) throws Exception {
        Socket socket = new Socket("localhost", port);
        socket.setSoTimeout(5000);
        try {
            OutputStream out = socket.getOutputStream();
            out.write(request.getBytes("ISO-8859-1"));
            out.flush();
            InputStream in = socket.getInputStream();
            byte[] buffer = new byte[1024];
            // the monitor answers only after parsing the whole request
            assertTrue("no response from the monitor", in.read(buffer) > 0);
        } finally {
            socket.close();
        }
    }

    private void waitUntilListening() throws Exception {
        for (int i = 0; i < 50; i++) {
            try {
                new Socket("localhost", port).close();
                // that probe connection is served as an empty request
                Thread.sleep(100);
                return;
            } catch (java.net.ConnectException notYet) {
                Thread.sleep(100);
            }
        }
        fail("monitor did not start listening on port " + port);
    }
}
