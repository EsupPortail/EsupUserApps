package org.esupportail.portal.services.prolongationENT;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Set;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.xml.bind.annotation.adapters.HexBinaryAdapter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class Utils {
    
    public static <V> MapBuilder<V> asMap(String key, V value) {
        return new MapBuilder<V>().add(key, value);
    }

    public static class MapBuilder<V> extends HashMap<String, V> {
        public MapBuilder<V> add(String key, V value) {
            this.put(key, value);
            return this;
        }
    }

    public static String firstCommonElt(Iterable<String> l1, Set<String> l2) {
        for (String e : l1) if (l2.contains(e)) return e;
        return null;
    }

    private static Log log() {
	return log(Utils.class);
    }
    
    public static Log log(Class<?> clazz) {
	return LogFactory.getLog(clazz);
    }
    
    public static String removePrefixOrNull(String s, String prefix) {
	return s.startsWith(prefix) ? s.substring(prefix.length()) : null;
    }
    
    public static URL toURL(String url) {
	try {
	    return new URL(url);
	} catch (java.net.MalformedURLException e) {
	    log().error(e, e);
	    return null;
	}
    }

    public static String urlencode(String s) {
	try {
	    return java.net.URLEncoder.encode(s, "UTF-8");
	}
	catch (java.io.UnsupportedEncodingException uee) {
	    return s;
	}
    }

    public static String urldecode(String s) {
       try {
           return java.net.URLDecoder.decode(s, "UTF-8");
       }
       catch (java.io.UnsupportedEncodingException uee) {
           return s;
       }
    }
  
    public static String json_encode(Object o) {
        Gson gson = new GsonBuilder().disableHtmlEscaping().create();
        return gson.toJson(o);
    }

    public static String computeMD5(String s) {
	try {
	    //System.out.println("computing digest of " + file);
	    MessageDigest md = MessageDigest.getInstance("MD5");
	    byte[] digest = md.digest(s.getBytes());
	    return (new HexBinaryAdapter()).marshal(digest);
	} catch (NoSuchAlgorithmException e) {
	    throw new RuntimeException(e);
	}
    }

    public static boolean hasParameter(HttpServletRequest request, String attrName) {
	return request.getParameter(attrName) != null;
    }
    
    public static String getCookie(HttpServletRequest request, String name) {
	for(Cookie c : request.getCookies()) { 
            if (c.getName().equals(name)) return c.getValue();
        }  
	return null;
    }
    public static Cookie newCookie(String name, String val, String path) {
        Cookie c = new Cookie(name, val);
        c.setPath(path);
        if (val == null) c.setMaxAge(0);
        return c;
    }

    public static InputStream file_get_stream(HttpServletRequest request, String file) {
	return request.getSession().getServletContext().getResourceAsStream(file);
    }

    public static String file_get_contents(HttpServletRequest request, String file) {
	try {
            InputStream in = file_get_stream(request, "/" + file);
            if (in == null) {
                log().error("error reading file " + file);
                return null;
            }
	    return IOUtils.toString(in, "UTF-8");
	} catch (IOException e) {
	    log().error("error reading file " + file, e);
	    return null;
	}
    }
    
}
