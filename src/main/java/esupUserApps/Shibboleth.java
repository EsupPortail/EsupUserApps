package esupUserApps;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import static esupUserApps.Utils.*;

class Shibboleth {
    static class Conf {
        String federation_metadata_url = "https://federation.renater.fr/renater/idps-renater-metadata.xml";
        String federation_metadata_cru_url = "https://federation.renater.fr/idp/profile/Metadata/SAML";
        Set<String> bearerTokens;
        Map<String,String> header_map;
    }
    Conf conf;
    File SSO_urls_backing_file;
    Map<String,String> SSO_urls;
    static Date lastUpdate;
    
    Logger log = LoggerFactory.getLogger(Shibboleth.class);
  
    Shibboleth(Conf conf, ServletContext sc) {
        this.conf = conf;

        SSO_urls_backing_file = new File((File) sc.getAttribute("javax.servlet.context.tempdir"),
                                         "SingleSignOnService_urls.json");
        try {
            SSO_urls = new Gson().fromJson(file_get_contents(SSO_urls_backing_file),
                                           new TypeToken<Map<String, String>>(){}.getType());
        } catch (Exception e) {
            retrieve_SSO_urls();
        }

        may_retrieve_SSO_urls_in_background();
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
        String SSO_url = get_SSO_url(request);
        if (SSO_url != null)
            attrs.put("SingleSignOnService-url", Collections.singletonList(SSO_url));
        
        return attrs;
    }

    String get_SSO_url(HttpServletRequest request) {
        String idpId = request.getHeader("Shib-Identity-Provider");
        return isEmpty(idpId) ? null : SSO_urls.get(idpId);        
    }
    
    void may_retrieve_SSO_urls_in_background() {
        long one_day = 24 * 60 * 60 * 1000;
        if (lastUpdate != null && delta_ms(lastUpdate, new Date()) < one_day) return;
        
        // set it fast, to ensure we do not retrieve_SSO_urls multiple times
        lastUpdate = new Date();

        // do it in background
        new Thread(new Runnable() {
                    public void run() {
                        retrieve_SSO_urls();
                    }
        }).start();
    }

    synchronized void retrieve_SSO_urls() {
        try {
            lastUpdate = new Date();
            log.info("retrieve_SSO_urls");
            Map<String,String> m  = retrieve_SSO_urls(urlGET(conf.federation_metadata_url));
            Map<String,String> m2 = retrieve_SSO_urls(urlGET(conf.federation_metadata_cru_url));
            if (m == null) return;
            if (m2 != null) m.putAll(m2);
            SSO_urls = m;
            Files.write(SSO_urls_backing_file.toPath(), new Gson().toJson(SSO_urls).getBytes());
        } catch (IOException e) {
            log.error("retrieve_SSO_urls failed", e);
        }
    }
    
    Map<String,String> retrieve_SSO_urls(InputStream in) {
        final Map<String,String> m = new HashMap<>();

        DefaultHandler handler = new DefaultHandler() {
            String entityID;

            public void startElement(String uri, String lName, String qName, Attributes attributes) {
                String name = qName.replaceFirst("^md:", "");
                if (name.equals("EntityDescriptor")) {
                    entityID = attributes.getValue("entityID");
                } else if (name.equals("SingleSignOnService")) {
                    if ("urn:mace:shibboleth:1.0:profiles:AuthnRequest".equals(attributes.getValue("Binding")))
                        m.put(entityID, attributes.getValue("Location"));
                }
            }

            public void endElement(String uri, String lName, String qName) {
                String name = qName.replaceFirst("^md:", "");
                if (name.equals("EntityDescriptor")) {
                    entityID = null;
                }
            }
        };

        try {
            SAXParserFactory.newInstance().newSAXParser().parse(in, handler);
            return m;
        } catch (javax.xml.parsers.ParserConfigurationException | SAXException | IOException e) {
            log.error("retrieve_SSO_urls failed", e);
            return null;
        }
    }

    static String[] getShibHeader(HttpServletRequest request, String name) {
        String value = request.getHeader(name);
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
