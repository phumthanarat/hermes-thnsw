package hk.hku.cecid.hermes.api.admin.listener;

import java.security.SecureRandom;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.xml.transform.Source;

import hk.hku.cecid.hermes.api.dao.ApiKeyDAO;
import hk.hku.cecid.hermes.api.dao.ApiKeyDVO;
import hk.hku.cecid.hermes.api.spa.ApiPlugin;
import hk.hku.cecid.piazza.commons.dao.DAOException;
import hk.hku.cecid.piazza.commons.util.PropertyTree;
import hk.hku.cecid.piazza.corvus.admin.listener.AdminPageletAdaptor;

/**
 * ApiKeyPageletAdaptor is an admin pagelet adaptor for issuing and revoking
 * REST API keys, stored in the api_key table so they can be managed without
 * a redeploy.
 */
public class ApiKeyPageletAdaptor extends AdminPageletAdaptor {

    protected Source getCenterSource(HttpServletRequest request) {

        String action = request.getParameter(REQ_PARAM_ACTION);

        if ("post".equalsIgnoreCase(request.getMethod())) {
            if ("add".equalsIgnoreCase(action)) {
                handleAdd(request);
            }
            else if ("revoke".equalsIgnoreCase(action)) {
                handleRevoke(request);
            }
            else if ("delete".equalsIgnoreCase(action)) {
                handleDelete(request);
            }
        }

        PropertyTree dom = new PropertyTree();
        dom.setProperty("/keys", "");

        try {
            ApiKeyDAO dao = (ApiKeyDAO) ApiPlugin.core.dao.createDAO(ApiKeyDAO.class);
            List allKeys = dao.findAllKeys();
            Iterator iter = allKeys.iterator();
            for (int i = 1; iter.hasNext(); i++) {
                ApiKeyDVO key = (ApiKeyDVO) iter.next();
                dom.setProperty("key[" + i + "]/api_key", key.getApiKey());
                dom.setProperty("key[" + i + "]/client_name", key.getClientName());
                dom.setProperty("key[" + i + "]/enabled", key.isEnabled() ? "true" : "false");
                dom.setProperty("key[" + i + "]/created", String.valueOf(key.getCreatedTimestamp()));
            }
        }
        catch (DAOException e) {
            request.setAttribute(ATTR_MESSAGE, "Unable to load API keys: " + e.getMessage());
        }

        return dom.getSource();
    }

    private void handleAdd(HttpServletRequest request) {
        String clientName = request.getParameter("client_name");
        if (clientName == null || clientName.trim().length() == 0) {
            request.setAttribute(ATTR_MESSAGE, "Client name is required to issue a key");
            return;
        }
        try {
            ApiKeyDAO dao = (ApiKeyDAO) ApiPlugin.core.dao.createDAO(ApiKeyDAO.class);
            ApiKeyDVO dvo = (ApiKeyDVO) dao.createDVO();
            dvo.setApiKey(generateKey());
            dvo.setClientName(clientName.trim());
            dvo.setEnabled(true);
            dvo.setCreatedTimestamp(new java.sql.Timestamp(System.currentTimeMillis()));
            dao.create(dvo);
            request.setAttribute(ATTR_MESSAGE, "API key issued for '" + clientName.trim() + "': " + dvo.getApiKey());
        }
        catch (DAOException e) {
            request.setAttribute(ATTR_MESSAGE, "Unable to issue API key: " + e.getMessage());
        }
    }

    private void handleRevoke(HttpServletRequest request) {
        String apiKey = request.getParameter("api_key");
        if (apiKey == null || apiKey.length() == 0) {
            return;
        }
        try {
            ApiKeyDAO dao = (ApiKeyDAO) ApiPlugin.core.dao.createDAO(ApiKeyDAO.class);
            ApiKeyDVO dvo = (ApiKeyDVO) dao.createDVO();
            dvo.setApiKey(apiKey);
            if (dao.retrieve(dvo)) {
                dvo.setEnabled(false);
                dao.persist(dvo);
                request.setAttribute(ATTR_MESSAGE, "API key revoked");
            }
        }
        catch (DAOException e) {
            request.setAttribute(ATTR_MESSAGE, "Unable to revoke API key: " + e.getMessage());
        }
    }

    private void handleDelete(HttpServletRequest request) {
        String apiKey = request.getParameter("api_key");
        if (apiKey == null || apiKey.length() == 0) {
            return;
        }
        try {
            ApiKeyDAO dao = (ApiKeyDAO) ApiPlugin.core.dao.createDAO(ApiKeyDAO.class);
            ApiKeyDVO dvo = (ApiKeyDVO) dao.createDVO();
            dvo.setApiKey(apiKey);
            if (dao.remove(dvo)) {
                request.setAttribute(ATTR_MESSAGE, "API key deleted");
            }
        }
        catch (DAOException e) {
            request.setAttribute(ATTR_MESSAGE, "Unable to delete API key: " + e.getMessage());
        }
    }

    private String generateKey() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(0xff & bytes[i]);
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }
}
