package esupUserApps;

import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static esupUserApps.Utils.asMap;
    
class Ldap {
    @SuppressWarnings("serial")
    static class Attrs extends HashMap<String, List<String>> {}

    static class LdapConf {
        String url, bindDN, bindPasswd, peopleDN;
        Integer connectTimeout_ms = 5 * 1000;
        Integer readTimeout_ms = 30 * 1000;
    }
    LdapConf ldapConf;
    DirContext dirContext;
    Logger log = LoggerFactory.getLogger(Ldap.class);

    Ldap(LdapConf ldapConf) {
        this.ldapConf = ldapConf;
    }
    
    private DirContext ldap_connect() {
        Map<String,String> env =
            asMap(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory")
             .add(Context.PROVIDER_URL, ldapConf.url)
             .add(Context.SECURITY_AUTHENTICATION, "simple")
             .add(Context.SECURITY_PRINCIPAL, ldapConf.bindDN)
             .add(Context.SECURITY_CREDENTIALS, ldapConf.bindPasswd)
             .add("com.sun.jndi.ldap.connect.timeout", "" + ldapConf.connectTimeout_ms)
             .add("com.sun.jndi.ldap.read.timeout", "" + ldapConf.readTimeout_ms);

        try {
            return new InitialDirContext(new Hashtable<>(env));
        } catch (NamingException e) {
            log.error("error connecting to ldap server", e);
            throw new RuntimeException("error connecting to ldap server");
        }
    }

    synchronized DirContext getDirContext() {
        if (dirContext == null) {
            dirContext = ldap_connect();
        }
        return dirContext;
    }

    @SuppressWarnings("unchecked")
    Attrs searchOne(String dn, String filter, Collection<String> wanted_attributes) {
        try {
            Attributes attrs = searchOne(dn, filter, wanted_attributes.toArray(new String[0]));
            Attrs r = new Attrs();
            if (attrs == null) return r;
            for (String attr : wanted_attributes) {
                Attribute vals = attrs.get(attr.toLowerCase());
                if (vals != null)
                    r.put(attr, Collections.list((NamingEnumeration<String>) vals.getAll()));
            }
            return r;
        } catch (javax.naming.NameNotFoundException e) {
            throw new RuntimeException("invalid dn " + dn);
        } catch (NamingException e) {
            log.error("", e);
            return null;
        }
    }

    private Attributes searchOneRaw(String dn, String filter, String[] wanted_attributes) throws NamingException {
        SearchControls ctrls = new SearchControls();
        ctrls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        ctrls.setReturningAttributes(wanted_attributes);
        ctrls.setCountLimit(2);
        Enumeration<SearchResult> l = getDirContext().search(dn, filter, ctrls);
        if (!l.hasMoreElements()) return null;
        SearchResult r = l.nextElement();
        return r != null && !l.hasMoreElements() ? r.getAttributes() : null;
    }

    private Attributes searchOne(String dn, String filter, String[] wanted_attributes) throws NamingException {
        try {
            return searchOneRaw(dn, filter, wanted_attributes);
        } catch (NamingException e) {
            log.info("LDAP error. Will retry once with a new LDAP connection", e.toString());
            dirContext = null; // NB: no need to explictly close previous dirContext, it will closed on garbage collection
            return searchOneRaw(dn, filter, wanted_attributes);
        }
    }
    
    Attrs getLdapPeopleInfo(String attr, String val, Collection<String> wanted_attributes) {
        return searchOne(ldapConf.peopleDN, "(" + attr + "=" + val + ")", wanted_attributes);
    }

    void mergeAttrs(Attrs attrs, Attrs more) {
        if (more == null) return;
        for (String name: more.keySet())
            if (!attrs.containsKey(name))
                attrs.put(name, more.get(name));
    }
    
    static String getFirst(Attrs attrs, String name) {
        List<String> val = attrs.get(name);
        return val != null ? val.get(0) : null;
    }

}
