package org.ovirt.engine.core.aaa.filters;

import java.util.Collections;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.servlet.http.HttpServletRequest;

import org.ovirt.engine.core.common.constants.SessionConstants;
import org.ovirt.engine.core.common.interfaces.BackendLocal;

public class FiltersHelper {

    public static class Constants {
        public final static String REQUEST_AUTH_RECORD_KEY = "ovirt_aaa_auth_record";
        public final static String REQUEST_SCHEMES_KEY = "ovirt_aaa_schemes";
        public final static String REQUEST_PROFILE_KEY = "ovirt_aaa_profile";
        public final static String REQUEST_AUTH_TYPE_KEY = "ovirt_aaa_auth_type";
        public final static String HEADER_AUTHORIZATION = "Authorization";
        public static final String HEADER_WWW_AUTHENTICATE = "WWW-Authenticate";
        public static final String HEADER_PREFER = "Prefer";
        public static final String HEADER_JSESSIONID_COOKIE = "JSESSIONID";
    }

    public static BackendLocal getBackend(Context context) {

        try {
            return (BackendLocal) context.lookup("java:global/engine/bll/Backend!org.ovirt.engine.core.common.interfaces.BackendLocal");
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean isAuthenticated(HttpServletRequest request) {
        return (request.getSession(false) != null && request.getSession(false)
                .getAttribute(SessionConstants.HTTP_SESSION_ENGINE_SESSION_ID_KEY) != null)
                || request.getAttribute(SessionConstants.HTTP_SESSION_ENGINE_SESSION_ID_KEY) != null;
    }

    public static boolean isPersistentAuth(HttpServletRequest req) {
        return Collections.list(req.getHeaders(FiltersHelper.Constants.HEADER_PREFER)).contains("persistent-auth");
    }

}
