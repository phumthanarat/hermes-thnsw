package hk.hku.cecid.hermes.api.admin.listener;

import hk.hku.cecid.piazza.commons.dao.DAOException;
import hk.hku.cecid.piazza.commons.servlet.http.HttpDispatcherContext;
import hk.hku.cecid.piazza.commons.util.PropertyTree;
import hk.hku.cecid.piazza.corvus.admin.handler.AdminPluginHandler;
import hk.hku.cecid.piazza.corvus.admin.listener.AdminPageletAdaptor;
import hk.hku.cecid.hermes.api.Constants;
import hk.hku.cecid.hermes.api.dao.ApiKeyDAO;
import hk.hku.cecid.hermes.api.dao.ApiKeyDVO;
import hk.hku.cecid.hermes.api.spa.ApiPlugin;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.xml.transform.Source;

/**
 * ApiOverviewPageletAdaptor is an admin pagelet adaptor which lists the REST
 * web service endpoints currently registered under the API dispatcher
 * context, sourced live from the dispatcher registry (not a hard-coded
 * list), so it always reflects what is actually active.
 *
 * @author Hermes2+ (piazza-commons/AdminPageletAdaptor pattern)
 */
public class ApiOverviewPageletAdaptor extends AdminPageletAdaptor {

    protected Source getCenterSource(HttpServletRequest request) {

        PropertyTree dom = new PropertyTree();
        dom.setProperty("/webservice", "");

        dom.setProperty("plugin/name", "Piazza Corvus API Plugin");
        dom.setProperty("plugin/status", ApiPlugin.core == null ? "Not activated" : "Activated");
        dom.setProperty("auth/method", "API Key");
        dom.setProperty("auth/header", Constants.API_KEY_HEADER);

        int totalKeys = 0;
        int enabledKeys = 0;
        try {
            ApiKeyDAO keyDAO = (ApiKeyDAO) ApiPlugin.core.dao.createDAO(ApiKeyDAO.class);
            List keys = keyDAO.findAllKeys();
            totalKeys = keys.size();
            for (Iterator i = keys.iterator(); i.hasNext(); ) {
                if (((ApiKeyDVO) i.next()).isEnabled()) {
                    enabledKeys++;
                }
            }
        } catch (DAOException e) {
            ApiPlugin.core.log.error("Unable to load API key counts for overview page", e);
        }
        dom.setProperty("auth/total_keys", String.valueOf(totalKeys));
        dom.setProperty("auth/enabled_keys", String.valueOf(enabledKeys));

        HttpDispatcherContext apiContext = HttpDispatcherContext.getContext(AdminPluginHandler.API_CONTEXT_ID);
        Properties info = apiContext.getRegisteredListenersInfo();

        Enumeration paths = info.keys();
        for (int i = 1; paths.hasMoreElements(); i++) {
            String path = paths.nextElement().toString();
            String listener = info.getProperty(path);

            dom.setProperty("endpoints/endpoint[" + i + "]/path", path);
            dom.setProperty("endpoints/endpoint[" + i + "]/url", request.getContextPath() + "/api" + path);
            dom.setProperty("endpoints/endpoint[" + i + "]/listener", listener);
        }

        return dom.getSource();
    }
}
