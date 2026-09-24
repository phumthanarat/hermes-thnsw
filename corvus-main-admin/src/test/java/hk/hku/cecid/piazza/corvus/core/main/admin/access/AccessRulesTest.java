package hk.hku.cecid.piazza.corvus.core.main.admin.access;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import junit.framework.TestCase;

public class AccessRulesTest extends TestCase {

    private static HttpServletRequest request(final String method, String... params) {
        final Map<String, String> map = new HashMap<String, String>();
        for (int i = 0; i < params.length; i += 2) {
            map.put(params[i], params[i + 1]);
        }
        return (HttpServletRequest) Proxy.newProxyInstance(AccessRulesTest.class.getClassLoader(),
                new Class[] { HttpServletRequest.class }, (proxy, m, args) -> {
                    if (m.getName().equals("getMethod")) {
                        return method;
                    }
                    if (m.getName().equals("getParameter")) {
                        return map.get(args[0]);
                    }
                    throw new UnsupportedOperationException(m.getName());
                });
    }

    private static AccessLevel required(String method, String path, String... params) {
        return AccessRules.required(path, request(method, params));
    }

    public void testViewerSeesPagesAndSearches() {
        assertEquals(AccessLevel.VIEWER, required("GET", "/ebms/message_history"));
        assertEquals(AccessLevel.VIEWER, required("GET", "/ebms/partnership"));
        assertEquals(AccessLevel.VIEWER, required("POST", "/ebms/message_history", "message_type", "Ping"));
        assertEquals(AccessLevel.VIEWER, required("POST", "/as2/message_history"));
        assertEquals(AccessLevel.VIEWER, required("POST", "/ebms/repository"));
        assertEquals(AccessLevel.VIEWER, required("POST", "/ebms/document_export", "format", "zip"));
        assertEquals(AccessLevel.VIEWER, required("POST", "/access/account"));
    }

    public void testOperatorRunsOperations() {
        assertEquals(AccessLevel.OPERATOR, required("POST", "/ebms/ping", "ping_one", "1"));
        assertEquals(AccessLevel.OPERATOR, required("POST", "/ebms/message_history", "request_action", "delete"));
        assertEquals(AccessLevel.OPERATOR, required("POST", "/ebms/resend_as_new"));
        assertEquals(AccessLevel.OPERATOR, required("POST", "/as2/change_message_status"));
        assertEquals(AccessLevel.OPERATOR, required("GET", "/main/httpd", "action", "ping_test"));
    }

    public void testAdministratorForSettingsAndAnythingUnlisted() {
        assertEquals(AccessLevel.ADMINISTRATOR, required("POST", "/ebms/partnership"));
        assertEquals(AccessLevel.ADMINISTRATOR, required("POST", "/ebms/housekeeping", "request_action", "save"));
        assertEquals(AccessLevel.ADMINISTRATOR, required("POST", "/ebms/agreement_upload"));
        assertEquals(AccessLevel.ADMINISTRATOR, required("POST", "/ebms/ping", "save_parties", "1"));
        assertEquals(AccessLevel.ADMINISTRATOR, required("POST", "/main/listenerports"));
        assertEquals(AccessLevel.ADMINISTRATOR, required("POST", "/some/new_page"));
        // pages with secrets, and actions taken on GET
        assertEquals(AccessLevel.ADMINISTRATOR, required("GET", "/access/users"));
        assertEquals(AccessLevel.ADMINISTRATOR, required("GET", "/api/keys"));
        assertEquals(AccessLevel.ADMINISTRATOR, required("GET", "/main/httpd", "action", "halt"));
        assertEquals(AccessLevel.ADMINISTRATOR, required("GET", "/home", "action", "gc"));
    }

    public void testLevelsInclude() {
        assertTrue(AccessLevel.ADMINISTRATOR.includes(AccessLevel.OPERATOR));
        assertTrue(AccessLevel.OPERATOR.includes(AccessLevel.VIEWER));
        assertFalse(AccessLevel.VIEWER.includes(AccessLevel.OPERATOR));
        assertFalse(AccessLevel.OPERATOR.includes(AccessLevel.ADMINISTRATOR));
    }
}
