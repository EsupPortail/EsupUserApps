package prolongationENT;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.LogFactory;

import static prolongationENT.Utils.*;
import static prolongationENT.Ldap.getFirst;

public class ComputeBandeau {           
    Conf.Main conf = null;
    ComputeApps computeApps;
    Stats stats;
    TopAppsAgimus topApps, topApps_latest;

    static String prev_host_attr = "prev_host";
    static String prev_time_attr = "prev_time";    

    org.apache.commons.logging.Log log = LogFactory.getLog(ComputeBandeau.class);

    public ComputeBandeau(Conf.Main conf) {
        this.conf = conf;
        computeApps = new ComputeApps(conf);
        stats = new Stats(conf);      
        if (conf.topApps.stable != null) {
            topApps        = new TopAppsAgimus(conf.topApps.stable);
            topApps_latest = new TopAppsAgimus(conf.topApps.latest);
        }
    }
    
    void layout(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (hasValidProxyKey(request)) {
            proxied_layout(request, response);
        }  else if (request.getParameter("noCache") != null) {
            needAuthentication(request, response);
        } else {
            String userId = get_CAS_userId(request);
            if (userId == null) {
                needAuthentication(request, response);
            } else {
                layout(request, response, userId);
            }
        }
    }

    void proxied_layout(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Ldap.Attrs attrs = computeApps.getShibbolethUserInfo(request);
        String userId = firstNonNull(getFirst(attrs, "uid"), getFirst(attrs, "id"));
        if (userId != null) {
            layout(request, response, userId, userId, attrs);
        } else {
            respond_json_or_jsonp(request, response, asMap("error", "Unauthorized"));
        }
    }

    void layout(HttpServletRequest request, HttpServletResponse response, String userId) throws IOException {
        String forcedId = request.getParameter("uid");  
        if (forcedId != null) {
            List<String> memberOf = computeApps.getLdapPeopleInfo(userId).get("memberOf");
            if (conf.admins.contains(userId) ||
                memberOf != null && firstCommonElt(memberOf, conf.admins) != null) {
                // ok
            } else {
                forcedId = null;
            }
        }
        if (forcedId == null) forcedId = userId;
        layout(request, response, forcedId, userId, computeApps.getLdapPeopleInfo(forcedId));
    }
                
    void layout(HttpServletRequest request, HttpServletResponse response, String userId, String realUserId, Ldap.Attrs attrs) throws IOException {
        Map<String,Export.App> userChannels = userChannels(attrs);
        List<Export.Layout> userLayout = userLayout(conf.LAYOUT, userChannels);

        stats.log(request, realUserId, userChannels.keySet());
        
        boolean is_old =
            !conf.isCasSingleSignOutWorking &&
            is_old(request) && request.getParameter("auth_checked") == null; // checking auth_checked should not be necessary since having "auth_checked" implies having gone through cleanupSession & CAS and so prev_time should not be set. But it seems firefox can bypass the initial redirect and go straight to CAS without us having cleaned the session... and then a dead-loop always asking for not-old version

        Map<String, Object> js_data =
            asMapO("user", userId)
             .add("userAttrs", exportAttrs(userId, attrs))
             .add("layout", asMap("folders", userLayout));
        if (!realUserId.equals(userId)) js_data.put("realUserId", realUserId);
        if (getCookieCasImpersonate(request) != null) {
            js_data.put("canImpersonate", computeApps.computeValidApps(realUserId, true));
        }
        if (topApps != null) {
            js_data.put("topApps", hasParameter(request, "latestTopApps") ? topApps_latest.get(attrs) : topApps.get(attrs));
        }

        String callback = request.getParameter("callback");
        if (callback == null) {
            // mostly for debugging purpose
            respond_json(response, js_data);
            return;
        }

        String js_data_ = json_encode(js_data);
        String hash = computeMD5(js_data_);
        Map<String, Object> js_params =
            asMapO("is_old", is_old)
              .add("hash", hash);
        
        respond_js(response,
                   hash.equals(request.getParameter("if_none_match")) ?
                     "// not update needed" :
                     callback + "(\n\n" + js_data_ + ",\n\n" + json_encode(js_params) + "\n\n)");
    }

    void canImpersonate(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String uid = request.getParameter("uid");
        String service = request.getParameter("service");
        Collection<String> appIds = computeApps.canImpersonate(uid, service);
        if (appIds.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        } else {
            respond_json(response, appIds);
        }
    }
    
    void redirect(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        String appId = request.getParameter("id");
        if (appId == null) { bad_request(response, "missing 'id=xxx' parameter"); return; }

        App app = conf.APPS.get(appId);
        if (app == null) { bad_request(response, "invalid appId " + appId); return; }
        
        String location = get_url(app, appId, null, conf.current_idpAuthnRequest_url);

        // Below rely on /ProlongationENT/redirect proxied in applications.
        // Example for Apache:
        //   ProxyPass /ProlongationENT https://ent.univ.fr/ProlongationENT
        if (hasParameter(request, "relog")) {
            removeCookies(request, response, app.cookies);
        }
        if (hasParameter(request, "impersonate")) {
            String wantedUid = getCookieCasImpersonate(request);
            response.addCookie(newCookie("CAS_IMPERSONATED", wantedUid, app.cookies.path));
        }
            
        response.sendRedirect(location);
    }

    void needAuthentication(HttpServletRequest request, HttpServletResponse response) throws IOException {
            if (request.getParameter("auth_checked") == null) {
                cleanupSession(request);
                String final_url = conf.prolongationENT_url + "/layout?auth_checked"
                    + (request.getQueryString() != null ? "&" + request.getQueryString() : "");
                response.sendRedirect(via_CAS(conf.cas_login_url, final_url) + "&gateway=true");
            } else {
                // user is not authenticated.
                respond_json_or_jsonp(request, response, asMap("error", "Unauthorized"));
            }
    }
    
    void removeCookies(HttpServletRequest request, HttpServletResponse response, App.Cookies toRemove) {
        for (String prefix : toRemove.name_prefixes()) {
            for(Cookie c : getCookies(request)) { 
                if (!c.getName().startsWith(prefix)) continue;
                response.addCookie(newCookie(c.getName(), null, toRemove.path()));
            }
        }
        for (String name : toRemove.names()) {
            response.addCookie(newCookie(name, null, toRemove.path()));
        }
    }
        
    static long time_before_forcing_CAS_authentication_again(boolean different_referrer) {
        return different_referrer ? 10 : 120; // seconds
    }
   
    /* ******************************************************************************** */
    /* heuristics to detect if user may have changed (unneeded if CAS single logout?) */
    /* ******************************************************************************** */   
    boolean referer_hostname_changed(HttpServletRequest request, HttpSession session) {
        String referer = request.getHeader("Referer");
        if (referer == null) return false;

        String current_host = url2host(referer);
        debug_msg("current_host " + current_host);
        boolean changed = false;
        String prev_host = (String) session.getAttribute(prev_host_attr);
        if (prev_host != null) {
            debug_msg("prev_host " + prev_host);
            changed = !prev_host.equals(current_host);
            if (changed) debug_msg("referer_hostname_changed: previous=" + session.getAttribute(prev_host_attr) + " current=" + current_host);
        }
        session.setAttribute(prev_host_attr, current_host);
        return changed;
    }

    boolean is_old(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        long max_age = time_before_forcing_CAS_authentication_again(referer_hostname_changed(request, session));
        long now = now();
        boolean is_old = false;
        if (session.getAttribute(prev_time_attr) != null) {
            long age = now - (Long) session.getAttribute(prev_time_attr);
            is_old = age > max_age;
            if (is_old) debug_msg("response is potentially old: age is " + age + " (more than " + max_age + ")");
        } else {
            session.setAttribute(prev_time_attr, now);
        }
        return is_old;
    }
    
    /* ******************************************************************************** */
    /* compute user's layout & channels */
    /* ******************************************************************************** */   
    Ldap.Attrs exportAttrs(String userId, Ldap.Attrs attrs) {
        Ldap.Attrs user = new Ldap.Attrs();
        for (String attr: conf.wanted_user_attributes) {
            List<String> val = attrs.get(attr);
            if (val != null)
                user.put(attr, val);
        }
        user.put("id", java.util.Collections.singletonList(userId));
        return user;
    }
    
    List<Export.Layout> userLayout(Map<String, List<String>> layout, Map<String, Export.App> userChannels) {
        List<Export.Layout> rslt = new ArrayList<>();
        for (Map.Entry<String, List<String>> e : layout.entrySet()) {
            List<Export.App> l = new ArrayList<>();
            for (String fname : e.getValue()) {
                Export.App app = userChannels.get(fname);
                if (app != null)
                    l.add(app);
            }
            if (!l.isEmpty())
                rslt.add(new Export.Layout(e.getKey(), l));
        }
        return rslt;  
    }
    
    Map<String, Export.App> userChannels(Ldap.Attrs person) {
        Map<String, Export.App> rslt = new HashMap<>();
        String idpId = getFirst(person, "Shib-Identity-Provider");
        String idpAuthnRequest_url = firstNonNull(getFirst(person, "SingleSignOnService-url"), conf.current_idpAuthnRequest_url);
        
        for (String fname : computeApps.computeValidApps(person, false)) {
            App app = conf.APPS.get(fname);
            rslt.put(fname, new Export.App(fname, app, get_url(app, fname, idpId, idpAuthnRequest_url)));
        }
        return rslt;
    }
    
    /* ******************************************************************************** */
    /* generate links */
    /* ******************************************************************************** */   
    // quick'n'dirty version: it expects a simple mapping from url to SP entityId and SP SAML v1 url
    static String via_idpAuthnRequest_url(String idpAuthnRequest_url, String url, String shibbolethSPPrefix) {
        String spId = url.replaceFirst("(://[^/]*)(.*)", "$1");
        String shire = spId + shibbolethSPPrefix + "Shibboleth.sso/SAML/POST";
        return String.format("%s?shire=%s&target=%s&providerId=%s", idpAuthnRequest_url, shire, urlencode(url), spId);
    }

    static String url_maybe_adapt_idp(String idpAuthnRequest_url, String url, String shibbolethSPPrefix) {
        if (idpAuthnRequest_url != null && shibbolethSPPrefix != null) {
            String realUrl = url;
            url = via_idpAuthnRequest_url(idpAuthnRequest_url, url, shibbolethSPPrefix);
            
            // HACK for test ProlongationENT: handle apps using production federation
            if (!realUrl.contains("test")) url = url.replace("idp-test", "idp");
            //debug_msg("personalized shib url is now " + url);
        }
        return url;
    }

    String get_url(App app, String appId, String idpId, String idpAuthnRequest_url) {
        String url = app.url;
        url = url.replace("{fname}", appId);
        url = url.replace("{idpId_ifShib}", firstNonNull(idpId, ""));
        url = url.replace("{idpId}", firstNonNull(idpId, conf.current_idpId));
        url = url_maybe_adapt_idp(idpAuthnRequest_url, url, app.shibbolethSPPrefix);
        return url;
    }
    
    void cleanupSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute(prev_time_attr);
            session.removeAttribute(prev_host_attr);
        }
    }
    
    String getCookieCasImpersonate(HttpServletRequest request) {
        return conf.cas_impersonate != null ? getCookie(request, conf.cas_impersonate.cookie_name) : null;
    }

    boolean hasValidProxyKey(HttpServletRequest request) {
        String key = request.getHeader("ProlongationENT-Proxy-Key");
        return key != null && conf.shibboleth != null &&
            conf.shibboleth.proxyKeys.contains(key);
    }
    
    /* ******************************************************************************** */
    /* simple helper functions */
    /* ******************************************************************************** */   
    void debug_msg(String msg) {
        //log.warn("DEBUG " + msg);
    }
        
    private static Utils.MapBuilder<Object> asMapO(String k, Object v) {
        return asMap(k, v);
    }
        
}

