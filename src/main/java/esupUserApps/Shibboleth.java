package esupUserApps;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static esupUserApps.Utils.*;

class Shibboleth {
    static class Conf {
        Set<String> bearerTokens;
        Map<String,String> header_map;
    }
    Conf conf;
    static Date lastUpdate;
    
    Logger log = LoggerFactory.getLogger(Shibboleth.class);
  
    Shibboleth(Conf conf, ServletContext sc) {
        this.conf = conf;
    }

    Ldap.Attrs getUserInfo(HttpServletRequest request, Set<String> wanted_user_attributes) {
        Set<String> wanted = new HashSet<>(wanted_user_attributes);
        wanted.addAll(conf.header_map.keySet());

        /*for (java.util.Enumeration<String> e = request.getHeaderNames(); e.hasMoreElements();) {
            String s = e.nextElement();
            log.warn("Headers in: " + s + " => " + request.getHeader(s));
         }*/
        
        Ldap.Attrs attrs = new Ldap.Attrs();
        for (String attr : wanted) {
            String shib_attr = firstNonNull(conf.header_map.get(attr), attr);
            String[] values = getShibHeader(request, shib_attr);
            if (values != null) {
                attrs.put(attr, Arrays.asList(values));
            }
        }
        
        return attrs;
    }

    static String[] getShibHeader(HttpServletRequest request, String name) {
        String value = request.getHeader(name);

        // handle apps with attributePrefix="AJP_" in shibboleth2.xml, cf https://wiki.shibboleth.net/confluence/display/SHIB2/NativeSPJavaInstall
        if (value == null) value = request.getHeader("AJP_" + name);
        
        if (isEmpty(value)) return null;
        
        // https://wiki.shibboleth.net/confluence/display/SHIB2/NativeSPAttributeAccess :
        // > multiple attribute values are separated by a semicolon, and semicolons in values are escaped with a backslash.
        // > The data should be interpreted as UTF-8, which is a superset of ASCII.

        // tomcat interprets headers as ISO-8859-1, so re-interpret as UTF-8:
        try {
            value = new String(value.getBytes("ISO-8859-1"), "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
        }

        return value.split(";");
    }
}
