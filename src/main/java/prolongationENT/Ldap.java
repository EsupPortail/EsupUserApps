package prolongationENT;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
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

class Ldap {
    static class Attrs extends HashMap<String, List<String>> {}

    class LdapConf {
        String url, bindDN, bindPasswd, peopleDN;
    }
    LdapConf ldapConf;
    DirContext dirContext;
    Log log = LogFactory.getLog(Ldap.class);

    Ldap(LdapConf ldapConf) {
	this.ldapConf = ldapConf;
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

    synchronized DirContext getDirContext() {
        if (dirContext == null) {
            dirContext = ldap_connect();
        }
        return dirContext;
    }

    @SuppressWarnings("unchecked")
	Attrs getLdapInfo(String dn, Collection<String> wanted_attributes) {
    	try {
	        Attributes attrs = getAttributes(dn, wanted_attributes.toArray(new String[0]));
	        Attrs r = new Attrs();
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
    
    Attrs getLdapPeopleInfo(String uid, Collection<String> wanted_attributes) {
        return getLdapInfo("uid=" + uid + "," + ldapConf.peopleDN, wanted_attributes);
    }

}
