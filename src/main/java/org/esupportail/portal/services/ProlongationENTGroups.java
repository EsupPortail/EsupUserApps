package org.esupportail.portal.services;

import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import javax.naming.CommunicationException;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import org.esupportail.portal.services.prolongationENT.ACLs;
import org.esupportail.portal.services.prolongationENT.Groups;
import org.esupportail.portal.services.prolongationENT.Ldap;
import org.esupportail.portal.services.prolongationENT.Utils;

class ProlongationENTGroups {

    String current_idpAuthnRequest_url;
    List<String> minimal_attrs;
    Groups groups;
    Map<String, ProlongationENTApp> APPS;
    Map<String, List<String>> LAYOUT;
    Ldap ldap;
    Log log = LogFactory.getLog(ProlongationENTGroups.class);
    
    public ProlongationENTGroups(JsonObject conf, JsonObject apps_conf, JsonObject auth_conf) {
    	Gson gson = new Gson();
        current_idpAuthnRequest_url = conf.get("current_idpAuthnRequest_url").getAsString();
    	minimal_attrs = gson.fromJson(conf.get("wanted_user_attributes"), 
    			new TypeToken< List<String> >() {}.getType());
        ldap = new Ldap(gson.fromJson(auth_conf.get("ldap"), Ldap.LdapConf.class));
        groups = new Groups(gson.<Map<String, Map<String, Object>>>fromJson(apps_conf.get("GROUPS"), 
											   new TypeToken< Map<String, Map<String, Object>> >() {}.getType()));
		LAYOUT = gson.fromJson(apps_conf.get("LAYOUT"), 
				new TypeToken< Map<String, List<String>> >() {}.getType());
		APPS = gson.fromJson(apps_conf.get("APPS"), 
				new TypeToken< Map<String, ProlongationENTApp> >() {}.getType());

		Map<String, ProlongationENTApp> APPS_ATTRS = gson.fromJson(apps_conf.get("APPS_ATTRS"), 
				new TypeToken< Map<String, ProlongationENTApp> >() {}.getType());
                for (ProlongationENTApp app : APPS.values()) {
                    if (app.inherit != null) {
                        app.merge(APPS_ATTRS.get(app.inherit));
                    }
                }

                compute_default_cookies_path_and_serviceRegex();
    }

    public Map<String, String> getApp(String appId) {
    	return APPS.get(appId).export();
    }

    public Set<String> computeValidApps(String uid, boolean wantImpersonate) {
        return computeValidAppsRaw(getLdapPeopleInfo(uid), wantImpersonate);
    }

    public Map<String, List<String>> getLdapPeopleInfo(String uid) {
	return ldap.getLdapPeopleInfo(uid, compute_wanted_attributes());
    }
    
    public Set<String> computeValidAppsRaw(Map<String, List<String>> person, boolean wantImpersonate) {
        String user = person.get("uid").get(0);
        Map<String, Boolean> groupsCache = new HashMap<>();

        Set<String> r = new HashSet<>();
        for (String appId : APPS.keySet()) {
            ProlongationENTApp app_ = APPS.get(appId);
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
        for (ProlongationENTApp app : APPS.values()) {
            if (app.url != null && app.admins != null) {
                compute_default_cookies_path_and_serviceRegex(app);
            }
        }
    }
    
    private void compute_default_cookies_path_and_serviceRegex(ProlongationENTApp app) {
        URL url = Utils.toURL(app.url);

        // default path is:
        // /     for http://foo/bar
        // /bar/ for http://foo/bar/ or http://foo/bar/boo/xxx
        String path = url.getPath().replaceFirst("/[^/]*$", "").replaceFirst("^(/[^/]*)/.*", "$1") + "/";
        
        if (app.cookies.path == null) app.cookies.path = path;
        if (app.serviceRegex == null) app.serviceRegex = Pattern.quote(url.getProtocol() + "://" + url.getHost() + path) + ".*";
    }
    
}
