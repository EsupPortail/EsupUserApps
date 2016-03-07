package org.esupportail.portal.services.prolongationENT;

import java.net.URL;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

class ComputeLayout {

    String current_idpAuthnRequest_url;
    Set<String> minimal_attrs;
    Groups groups;
    Map<String, App> APPS;
    Map<String, List<String>> LAYOUT;
    Ldap ldap;
    Log log = LogFactory.getLog(ComputeLayout.class);
    
    ComputeLayout(MainConf conf, JsonObject apps_conf, JsonObject auth_conf) {
    	Gson gson = new Gson();
        current_idpAuthnRequest_url = conf.current_idpAuthnRequest_url;
    	minimal_attrs = conf.wanted_user_attributes;
        ldap = new Ldap(gson.fromJson(auth_conf.get("ldap"), Ldap.LdapConf.class));
        groups = new Groups(gson.<Map<String, Map<String, Object>>>fromJson(apps_conf.get("GROUPS"), 
											   new TypeToken< Map<String, Map<String, Object>> >() {}.getType()));
		LAYOUT = gson.fromJson(apps_conf.get("LAYOUT"), 
				new TypeToken< Map<String, List<String>> >() {}.getType());
		APPS = gson.fromJson(apps_conf.get("APPS"), 
				new TypeToken< Map<String, App> >() {}.getType());

		Map<String, App> APPS_ATTRS = gson.fromJson(apps_conf.get("APPS_ATTRS"), 
				new TypeToken< Map<String, App> >() {}.getType());
                for (App app : APPS.values()) {
                    if (app.inherit != null) {
                        app.merge(APPS_ATTRS.get(app.inherit));
                    }
                }

                compute_default_cookies_path_and_serviceRegex();
    }

    Map<String, String> getApp(String appId) {
    	return APPS.get(appId).export();
    }

    Set<String> computeValidApps(String uid, boolean wantImpersonate) {
        return computeValidAppsRaw(getLdapPeopleInfo(uid), wantImpersonate);
    }

    Ldap.Attrs getLdapPeopleInfo(String uid) {
	return ldap.getLdapPeopleInfo(uid, compute_wanted_attributes());
    }
    
    Set<String> computeValidAppsRaw(Ldap.Attrs person, boolean wantImpersonate) {
        String user = person.get("uid").get(0);
        Map<String, Boolean> groupsCache = new HashMap<>();

        Set<String> r = new HashSet<>();
        for (String appId : APPS.keySet()) {
            App app_ = APPS.get(appId);
            ACLs app = wantImpersonate ? app_.admins : app_;
            if (app == null) continue;
            Boolean found = false;

            if (app.users != null) {
                if (app.users.contains(user))
                    found = true;
            }

            if (!found && app.groups != null) {
                for (String group : app.groups) {
                    if (groups.hasGroup(person, group, groupsCache))
                        found = true;
                }
            }
            if (found) r.add(appId);
        }
        return r;
    }
  
    private Set<String> compute_wanted_attributes() {
        Set<String> r = groups.needed_ldap_attributes();
        r.add("memberOf"); // hard code memberOf
	r.addAll(minimal_attrs);
        return r;
  }

    private void compute_default_cookies_path_and_serviceRegex() {
        for (App app : APPS.values()) {
            if (app.url != null && app.admins != null) {
                compute_default_cookies_path_and_serviceRegex(app);
            }
        }
    }
    
    private void compute_default_cookies_path_and_serviceRegex(App app) {
        URL url = Utils.toURL(app.url);

        // default path is:
        // /     for http://foo/bar
        // /bar/ for http://foo/bar/ or http://foo/bar/boo/xxx
        String path = url.getPath().replaceFirst("/[^/]*$", "").replaceFirst("^(/[^/]*)/.*", "$1") + "/";
        
        if (app.cookies.path == null) app.cookies.path = path;
        if (app.serviceRegex == null) app.serviceRegex = Pattern.quote(url.getProtocol() + "://" + url.getHost() + path) + ".*";
    }
    
}
