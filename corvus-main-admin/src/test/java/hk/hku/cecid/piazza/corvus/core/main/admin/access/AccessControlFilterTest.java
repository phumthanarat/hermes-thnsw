package hk.hku.cecid.piazza.corvus.core.main.admin.access;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import junit.framework.TestCase;

public class AccessControlFilterTest extends TestCase {

    private static HttpServletRequest request(final boolean secure, String... headers) {
        final Map<String, String> map = new HashMap<String, String>();
        for (int i = 0; i < headers.length; i += 2) {
            map.put(headers[i], headers[i + 1]);
        }
        return (HttpServletRequest) Proxy.newProxyInstance(AccessControlFilterTest.class.getClassLoader(),
                new Class[] { HttpServletRequest.class }, (proxy, m, args) -> {
                    if (m.getName().equals("getHeader")) {
                        return map.get(args[0]);
                    }
                    if (m.getName().equals("isSecure")) {
                        return secure;
                    }
                    throw new UnsupportedOperationException(m.getName());
                });
    }

    public void testSameSitePostsAreAccepted() {
        assertFalse(AccessControlFilter.isCrossSite(request(true,
                "Host", "localhost:18443", "Origin", "https://localhost:18443")));
        assertFalse(AccessControlFilter.isCrossSite(request(true,
                "Host", "gw.example.com", "Origin", "https://gw.example.com")));
        assertFalse(AccessControlFilter.isCrossSite(request(true,
                "Host", "gw.example.com:443", "Referer", "https://gw.example.com/corvus/admin/ebms/ping")));
        // tools that send neither header
        assertFalse(AccessControlFilter.isCrossSite(request(false, "Host", "localhost:18080")));
    }

    public void testCrossSitePostsAreRejected() {
        assertTrue(AccessControlFilter.isCrossSite(request(true,
                "Host", "localhost:18443", "Origin", "https://evil.example")));
        assertTrue(AccessControlFilter.isCrossSite(request(true,
                "Host", "localhost:18443", "Origin", "https://localhost:28443")));
        assertTrue(AccessControlFilter.isCrossSite(request(true,
                "Host", "localhost:18443", "Referer", "https://evil.example/page")));
        assertTrue(AccessControlFilter.isCrossSite(request(true,
                "Host", "localhost:18443", "Origin", "not a url")));
    }
}
