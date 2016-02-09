package org.esupportail.portal.services;

import java.util.Collections;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import java.io.PrintWriter;

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
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;


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
    
    public ProlongationENTGroups(String raw_json_conf, String raw_apps_conf, String raw_auth_conf) {
    	JsonParser parser = new JsonParser();
    	JsonObject conf = parser.parse(raw_json_conf).getAsJsonObject();
    	JsonObject apps_conf = parser.parse(raw_apps_conf).getAsJsonObject();
    	JsonObject auth_conf = parser.parse(raw_auth_conf).getAsJsonObject();
    	
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

                exportToUportal("/usr/local/esup/db-export");
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
	private Map<String, Map<String, List<Pattern>>> prepareRegexes(Map<String, Map<String, Object>> m) {
        for (Map<String,Object> attr2regexes : m.values()) {
            for (String attr : attr2regexes.keySet()) {
                Object regexes = attr2regexes.get(attr);
                List<Pattern> p = new LinkedList<Pattern>();
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
	        Map<String, List<String>> r = new HashMap<String, List<String>>();
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

    public Set<String> computeValidAppsRaw(Map<String, List<String>> person) {        
        String user = person.get("uid").get(0);
        Map<String, Boolean> groupsCache = new HashMap<String, Boolean>();

        Set<String> r = new HashSet<String>();
        for (String appId : APPS.keySet()) {
            ProlongationENTApp app = APPS.get(appId);
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
        Set<String> r = new HashSet<String>(minimal_attrs);
        r.add("memberOf"); // hard code memberOf

        for (Map<String, List<Pattern>> attr2regexes : GROUPS.values()) {
            r.addAll(attr2regexes.keySet());
        }
        return r;
  }
    

    private DirContext ldap_connect() {
	Hashtable<String,String> env = new Hashtable<String,String>();
	env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
	env.put(Context.PROVIDER_URL, ldapConf.url);
	env.put(Context.SECURITY_AUTHENTICATION, "simple");
	env.put(Context.SECURITY_PRINCIPAL, ldapConf.bindDN);
	env.put(Context.SECURITY_CREDENTIALS, ldapConf.bindPasswd);

	try {
	    return new InitialDirContext(env);
	} catch (NamingException e) {
	    log.error("error connecting to ldap server", e);
            throw new RuntimeException("error connecting to ldap server");
        }
    }


    private void exportToUportal(String destDir) {
        exportLayout(destDir + "/fragment-layout/all-lo.fragment-layout");
        for (String appId : APPS.keySet()) {
            exportAPP(destDir + "/channel/" + appId + ".channel", appId);
        }
    }

    private void exportLayout(String destFile) {
        try {
            PrintWriter writer = new PrintWriter(destFile, "UTF-8");
            writer.println("<layout xmlns:dlm='http://www.uportal.org/layout/dlm' script='classpath://org/jasig/portal/io/import-layout_v3-2.crn' username='all-lo'>");
            writer.println("  <folder ID='s1' hidden='false' immutable='true' name='Root folder' type='root' unremovable='true'>");    
            writer.println("    <folder ID='s2' hidden='false' immutable='true' name='Header folder' type='header' unremovable='true'>");
            writer.println("      <channel dlm:moveAllowed='false' dlm:deleteAllowed='false' fname='fragment-admin-exit' unremovable='true' hidden='false' immutable='true' ID='n3'/>");
            writer.println("    </folder>");
            writer.println("    <folder ID='s4' hidden='false' immutable='true' name='Footer folder' type='footer' unremovable='true'/>");
            writer.println("");

            int nb = 10;
            for (Map.Entry<String, List<String>> e : LAYOUT.entrySet()) {
                writer.println("    <folder ID='s" + nb++ + "' hidden='false' immutable='true' name='" + e.getKey() + "' type='regular' dlm:addChildAllowed='false' dlm:deleteAllowed='false' dlm:editAllowed='false' dlm:moveAllowed='false'  unremovable='true'>");
                writer.println("      <folder ID='s" + nb++ + "' hidden='false' immutable='true' name='Column' type='regular' dlm:addChildAllowed='false' dlm:deleteAllowed='false' dlm:editAllowed='false' dlm:moveAllowed='false'  unremovable='true'>");
                for (String fname : e.getValue())
                    writer.println("        <channel dlm:moveAllowed='false' dlm:deleteAllowed='false' fname='" + fname + "' unremovable='true' hidden='false' immutable='true' ID='n" + nb++ + "'/>");
                writer.println("      </folder>");
                writer.println("    </folder>");
            }
            writer.println("  </folder>");
            writer.println("</layout>");
            writer.close();
        } catch (Exception e) {
            log.warn("error creating " + destFile, e);
        }
    }

    private void exportAPP(String destFile, String appId) {
        ProlongationENTApp app = APPS.get(appId);
        if (app.url == null) return;

        String url = ProlongationENT.url_maybe_adapt_idp(current_idpAuthnRequest_url, app.url, app.shibbolethSPPrefix);
        try {
            PrintWriter writer = new PrintWriter(destFile, "UTF-8");

    writer.println("");
    writer.println("<channel-definition script=\"classpath://org/jasig/portal/io/import-channel_v3-2.crn\">");
    writer.println("  <title>" + app.title + "</title>");
    writer.println("  <name>" + app.getTexte() + "</name>");
    writer.println("  <fname>" + appId + "</fname>");
    writer.println("  <desc>" + app.getDescription() + "</desc>");
    writer.println("  <type>Inline Frame</type>");
    writer.println("  <class>org.jasig.portal.channels.portlet.CSpringPortletAdaptor</class>");
    writer.println("  <timeout>5000000</timeout>");
    writer.println("  <hasedit>N</hasedit>");
    writer.println("  <hashelp>" + (app.hashelp ? "Y" : "N") + "</hashelp>");
    writer.println("  <hasabout>N</hasabout>");
    writer.println("  <secure>N</secure>");
    writer.println("  <locale>en_US</locale>");
    writer.println("  <categories>");
    writer.println("  </categories>");
    writer.println("  <groups>");
    if (app.groups != null) {
        for (String group : new TreeSet<String>(app.groups)) {
            writer.println("    <group>" + group + "</group>");
        }
    }
    writer.println("  </groups>");
    writer.println("  <users>");
    if (app.users != null) {
        for (String user : new TreeSet<String>(app.users)) {
            writer.println("    <user>" + user + "</user>");
        }
    }
    writer.println("    <user>all-lo</user>");
    writer.println("  </users>");
    writer.println("  <parameters>");
    writer.println("    <parameter> ");
    writer.println("      <name>alternate</name>  ");
    writer.println("      <value>false</value>  ");
    writer.println("      <description></description>  ");
    writer.println("      <ovrd>N</ovrd> ");
    writer.println("    </parameter>");
    writer.println("    <parameter> ");
    writer.println("      <name>disableDynamicTitle</name>  ");
    writer.println("      <value>true</value>  ");
    writer.println("      <description></description>  ");
    writer.println("      <ovrd>N</ovrd> ");
    writer.println("    </parameter>");
    writer.println("    <parameter> ");
    writer.println("      <name>hideFromMobile</name>  ");
    writer.println("      <value>" + app.hideFromMobile + "</value>  ");
    writer.println("      <description></description>  ");
    writer.println("      <ovrd>N</ovrd> ");
    writer.println("    </parameter>");
    writer.println("    <parameter> ");
    writer.println("      <name>portletName</name>  ");
    writer.println("      <value>IFrame</value>  ");
    writer.println("      <description></description>  ");
    writer.println("      <ovrd>N</ovrd> ");
    writer.println("    </parameter>");
    writer.println("    <parameter> ");
    writer.println("      <name>showChrome</name>  ");
    writer.println("      <value>true</value>  ");
    writer.println("      <description></description>  ");
    writer.println("      <ovrd>N</ovrd> ");
    writer.println("    </parameter>");
    writer.println("    <parameter> ");
    writer.println("      <name>highlight</name>  ");
    writer.println("      <value>false</value>  ");
    writer.println("      <description></description>  ");
    writer.println("      <ovrd>N</ovrd> ");
    writer.println("    </parameter>");
    writer.println("    <parameter> ");
    writer.println("      <name>isFrameworkPortlet</name>  ");
    writer.println("      <value>true</value>  ");
    writer.println("      <description></description>  ");
    writer.println("      <ovrd>N</ovrd> ");
    writer.println("    </parameter>");
    writer.println("  </parameters>");
    writer.println("  <portletPreferences>");
    writer.println("    <portletPreference> ");
    writer.println("      <name>height</name>  ");
    writer.println("      <read-only>true</read-only>  ");
    writer.println("      <values>");
    writer.println("        <value>600</value>");
    writer.println("      </values> ");
    writer.println("    </portletPreference>");
    writer.println("    <portletPreference> ");
    writer.println("      <name>url</name>  ");
    writer.println("      <read-only>true</read-only>  ");
    writer.println("      <values>");
    writer.println("        <value>" + escapeXml(url) + "</value>");
    writer.println("      </values> ");
    writer.println("    </portletPreference>");
    writer.println("  </portletPreferences>");
    writer.print("</channel-definition>");
            writer.close();
        } catch (Exception e) {
            log.warn("error creating " + destFile, e);
        }
    }

    String escapeXml(String s) {
        return s.replace("&", "&amp;");
    }
    
}
