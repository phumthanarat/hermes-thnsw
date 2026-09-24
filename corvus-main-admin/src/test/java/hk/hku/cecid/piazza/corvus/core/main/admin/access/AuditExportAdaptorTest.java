package hk.hku.cecid.piazza.corvus.core.main.admin.access;

import junit.framework.TestCase;

public class AuditExportAdaptorTest extends TestCase {

    public void testQuotesAndNeutralisesFormulas() {
        assertEquals("\"plain\"", AuditExportAdaptor.csv("plain"));
        assertEquals("\"say \"\"hi\"\"\"", AuditExportAdaptor.csv("say \"hi\""));
        assertEquals("\"'=HYPERLINK(1)\"", AuditExportAdaptor.csv("=HYPERLINK(1)"));
        assertEquals("\"'+1\"", AuditExportAdaptor.csv("+1"));
        assertEquals("", AuditExportAdaptor.csv(null));
    }
}
