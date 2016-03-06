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
import org.esupportail.portal.services.prolongationENT.Utils;

class ProlongationENTGroups {

    String current_idpAuthnRequest_url;
    List<String> minimal_attrs;
    Map<String, Map<String, List<Pattern>>> GROUPS;
    Map<String, ProlongationENTApp> APPS;
    Map<String, List<String>> LAYOUT;

    class LdapConf {
        String url, bindDN, bindPasswd, peopleDN;
    }
    LdapConf ldapConf;

    DirContext dirContext;
    Log log = LogFactory.getLog(ProlongationENTGroups.class);
    
    public ProlongationENTGroups(JsonObject conf, JsonObject apps_conf, JsonObject auth_conf) {
    	Gson gson = new Gson();
        current_idpAuthnRequest_url = conf.get("current_idpAuthnRequest_url").getAsString();
    	minimal_attrs = gson.fromJson(conf.get("wanted_user_attributes"), 
    			new TypeToken< List<String> >() {}.getType());
        ldapConf = gson.fromJson(auth_conf.get("ldap"), LdapConf.class);
        GROUPS = prepareRegexes(gson.<Map<String, Map<String, Object>>>fromJson(apps_conf.get("GROUPS"), 
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
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
	private Map<String, Map<String, List<Pattern>>> prepareRegexes(Map<String, Map<String, Object>> m) {
        for (Map<String,Object> attr2regexes : m.values()) {
            for (String attr : attr2regexes.keySet()) {
                Object regexes = attr2regexes.get(attr);
                List<Pattern> p = new LinkedList<>();
                if (regexes instanceof String) {
                    regexes = Collections.singletonList(regexes);
                }
                for (String regex : (List<String>) regexes) {
                    p.add(Pattern.compile(regex));
                }
                attr2regexes.put(attr, p);
            }
        }
        // down casting to precise type since we've done the job now
        return (Map) m;
    }
    
    synchronized DirContext getDirContext() {
        if (dirContext == null) {
            dirContext = ldap_connect();
        }
        return dirContext;
    }

    @SuppressWarnings("unchecked")
	Map<String, List<String>> getLdapInfo(String dn, Set<String> wanted_attributes) {
    	try {
	        Attributes attrs = getAttributes(dn, wanted_attributes.toArray(new String[0]));
	        Map<String, List<String>> r = new HashMap<>();
	        for (String attr : wanted_attributes) {
                    Attribute vals = attrs.get(attr.toLowerCase());
					if (vals != null)
                        r.put(attr, Collections.list((NamingEnumeration<String>) vals.getAll()));
	        }
	        return r;
    	} catch (NamingException e) {
    		return null;
    	}
    }

    private Attributes getAttributes(String dn, String[] wanted_attributes) throws NamingException {
        try {
            return getDirContext().getAttributes(dn, wanted_attributes);
        } catch (CommunicationException e) {
            // retry, maybe a new LDAP connection will work
            dirContext = null;
            return getDirContext().getAttributes(dn, wanted_attributes);
        }
    }
    
    public Map<String, List<String>> getLdapPeopleInfo(String uid) {
        return getLdapInfo("uid=" + uid + "," + ldapConf.peopleDN, compute_wanted_attributes());
    }

    public Map<String, String> getApp(String appId) {
    	return APPS.get(appId).export();
    }

    public boolean hasGroup(Map<String, List<String>> person, String name) {
        if (!GROUPS.containsKey(name)) {
            return hasPlainLdapGroup(person, name);
        }
            
        for (Entry<String, List<Pattern>> attr_regexes : GROUPS.get(name).entrySet()) {
            String attr = attr_regexes.getKey();
                
            List<String> attrValues = person.get(attr);
            if (attrValues == null) {
                log.warn("missing attribute " + attr);
                return false;
            }
            for (Pattern regex : attr_regexes.getValue()) {
                boolean okOne = false;
                for (String v : attrValues) {
                    if (regex.matcher(v).matches()) {
                        okOne = true;
                        break;
                    }
                }
                if (!okOne) return false;
            }
        }
        return true;
    }

    public boolean hasPlainLdapGroup(Map<String, List<String>> person, String name) {
        // check memberOf
        List<String> vals = person.get("memberOf");
        if (vals != null) {
            for (String val : vals)
                if (val.startsWith("cn=" + name + ",")) return true;
        }
        return false;
    }

    public boolean hasGroup(Map<String, List<String>> person, String name, Map<String, Boolean> cache) {
        Boolean r = cache.get(name);
        if (r == null) {
            r = hasGroup(person, name);
            cache.put(name, r);
        }
        return r;
    }

    public Set<String> computeValidApps(String uid, boolean wantImpersonate) {
        return computeValidAppsRaw(getLdapPeopleInfo(uid), wantImpersonate);
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
                    if (hasGroup(person, group, groupsCache))
                        found = true;
                }
            }
            if (found) r.add(appId);
        }
        return r;
    }
  
    private Set<String> compute_wanted_attributes() {
        Set<String> r = new HashSet<>(minimal_attrs);
        r.add("memberOf"); // hard code memberOf

        for (Map<String, List<Pattern>> attr2regexes : GROUPS.values()) {
            r.addAll(attr2regexes.keySet());
        }
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

    private DirContext ldap_connect() {
	Map<String,String> env =
	    Utils.asMap(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory")
	           .add(Context.PROVIDER_URL, ldapConf.url)
	           .add(Context.SECURITY_AUTHENTICATION, "simple")
	           .add(Context.SECURITY_PRINCIPAL, ldapConf.bindDN)
	           .add(Context.SECURITY_CREDENTIALS, ldapConf.bindPasswd);

	try {
	    return new InitialDirContext(new Hashtable<>(env));
	} catch (NamingException e) {
	    log.error("error connecting to ldap server", e);
            throw new RuntimeException("error connecting to ldap server");
        }
    }
    
}
