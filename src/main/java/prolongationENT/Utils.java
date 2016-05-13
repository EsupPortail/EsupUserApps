package prolongationENT;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.net.URL;
import java.net.HttpURLConnection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.xml.bind.annotation.adapters.HexBinaryAdapter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jasig.cas.client.util.AbstractCasFilter;
import org.jasig.cas.client.validation.Assertion;

class Utils {
    
    static <V> MapBuilder<V> asMap(String key, V value) {
        return new MapBuilder<V>().add(key, value);
    }

    @SuppressWarnings("serial")
    static class MapBuilder<V> extends HashMap<String, V> {
        MapBuilder<V> add(String key, V value) {
            this.put(key, value);
            return this;
        }
    }

    static class CacheEntry<V> {
        Date date;
        V val;

        CacheEntry(V val) {
            this.val = val;
            this.date = new Date();
        }
    }

    static class Cache<V> {
        ConcurrentHashMap<String, CacheEntry<V>> map;
        int lifetime; // in seconds

        Cache(int lifetime) {
            map = new ConcurrentHashMap<>();
            this.lifetime = lifetime;
        }
        V get(String key) {
            CacheEntry<V> e = map.get(key);
            if (e == null)
                return null;
            else if (lifetime <= 0 || delta_ms(e.date, new Date()) < lifetime * 1000)
                return e.val;
            else {
                map.remove(key);
                return null;
            }
        }
        void put(String key, V value) {
            map.put(key, new CacheEntry<>(value));
        }
    }

    static boolean isEmpty(String s) {
        return s == null || s.isEmpty();
    }
    
    @SafeVarargs
    static <T> T firstNonNull(T... values) {
        for (T value : values) {
            if (value != null) return value;
        }
        return null;
    }
    
    static String firstCommonElt(Iterable<String> l1, Set<String> l2) {
        for (String e : l1) if (l2.contains(e)) return e;
        return null;
    }

    private static Log log() {
        return log(Utils.class);
    }
    
    static Log log(Class<?> clazz) {
        return LogFactory.getLog(clazz);
    }
    
    static String removePrefixOrNull(String s, String prefix) {
        return s.startsWith(prefix) ? s.substring(prefix.length()) : null;
    }

    static String via_CAS(String cas_login_url, String href) {
        return cas_login_url + "?service="  + urlencode(href);
    }
    
    static URL toURL(String url) {
        try {
            return new URL(url);
        } catch (java.net.MalformedURLException e) {
            log().error(e, e);
            return null;
        }
    }

    static String url2host(String url) {
        URL url_ = toURL(url);
        return url_ != null ? url_.getHost() : null;
    }

    static String urlencode(String s) {
        try {
            return java.net.URLEncoder.encode(s, "UTF-8");
        }
        catch (java.io.UnsupportedEncodingException uee) {
            return s;
        }
    }

    static String urldecode(String s) {
        try {
            return java.net.URLDecoder.decode(s, "UTF-8");
        }
        catch (java.io.UnsupportedEncodingException uee) {
            return s;
        }
    }
  
    static String json_encode(Object o) {
        Gson gson = new GsonBuilder().disableHtmlEscaping().create();
        return gson.toJson(o);
    }

    static String computeMD5(String s) {
        try {
            //System.out.println("computing digest of " + file);
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(s.getBytes());
            return (new HexBinaryAdapter()).marshal(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    static boolean hasParameter(HttpServletRequest request, String attrName) {
        return request.getParameter(attrName) != null;
    }

    // wrapper around request.getCookies() which never returns null
    static Cookie[] getCookies(HttpServletRequest request) {
        Cookie[] l = request.getCookies();
        return l != null ? l : new Cookie[] {};
    }
    
    static String getCookie(HttpServletRequest request, String name) {
        for(Cookie c : getCookies(request)) { 
            if (c.getName().equals(name)) return c.getValue();
        }  
        return null;
    }
    static Cookie newCookie(String name, String val, String path) {
        Cookie c = new Cookie(name, val);
        c.setPath(path);
        if (val == null) c.setMaxAge(0);
        return c;
    }

    static String file_get_contents(ServletContext sc, String file) {
        try {
            InputStream in = sc.getResourceAsStream("/" + file);
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
    static String file_get_contents(HttpServletRequest request, String file) {
        return file_get_contents(request.getSession().getServletContext(), file);
    }

    static String file_get_contents(File file) throws IOException {
        return new String(Files.readAllBytes(file.toPath()), "UTF-8");
    }
    
    static InputStream urlGET(String url) throws IOException {
        return urlGET(toURL(url));
    }   

    static InputStream urlGET(URL url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.connect();
        return conn.getInputStream();
    }
    
    // inspired from java-cas-client code
    static String get_CAS_userId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Assertion assertion = (Assertion) (session == null ? request
                                           .getAttribute(AbstractCasFilter.CONST_CAS_ASSERTION) : session
                                           .getAttribute(AbstractCasFilter.CONST_CAS_ASSERTION));
        return assertion == null ? null : assertion.getPrincipal().getName();
    }
    
    static void session_invalidate(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) return;
        try {
            session.invalidate();
        } catch (IllegalStateException ise) {
            // IllegalStateException indicates session was already invalidated.
            // This is fine.  LogoutServlet is looking to guarantee the logged out session is invalid;
            // it need not insist that it be the one to perform the invalidating.
        }
    }
    
    static long now() {
        return System.currentTimeMillis() / 1000L;
    }   

    static void bad_request(HttpServletResponse response, String msg) {
        log().info(msg);
        try {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, msg);
        } catch (IOException e) {
            log().error(e);
        }
    }

    static void respond_js(HttpServletResponse response, long cacheMaxAge, String js) throws IOException {
        response.setHeader("Cache-Control", "max-age=" + cacheMaxAge);
        respond_js(response, js);
    }
    static void respond_js(HttpServletResponse response, String js) throws IOException {
        response.setContentType("application/javascript; charset=utf8");
        response.getWriter().write(js);
    }
    static void respond_json(HttpServletResponse response, Object json) throws IOException {
        response.setContentType("application/json; charset=utf8");
        response.getWriter().write(json_encode(json));
    }
    static void respond_json_or_jsonp(HttpServletRequest request, HttpServletResponse response, Object json) throws IOException {
        String callback = request.getParameter("callback");
        if (callback == null)
            respond_json(response, json);
        else
            respond_js(response, callback + "(" + json_encode(json) + ")");      
    }

    static void addFilter(ServletContext sc, String name, Class<? extends Filter> clazz, Map<String,String> params, String... urls) {
        FilterRegistration.Dynamic o = sc.addFilter(name, clazz);
        if (params != null) o.setInitParameters(params);
        o.addMappingForUrlPatterns(null, true, urls);
    }
        
    static void addServlet(ServletContext sc, String name, Class<? extends Servlet> clazz, Map<String,String> params, String... urls) {
        ServletRegistration.Dynamic o = sc.addServlet(name, clazz);
        if (params != null) o.setInitParameters(params);
        o.addMapping(urls);
    }

    static MapBuilder<Object> objectFieldsToMap(Object o, String... fieldNames) {
        MapBuilder<Object> map = new MapBuilder<>();
        for (String name : fieldNames) {
            try {
                map.put(name, o.getClass().getDeclaredField(name).get(o));
            } catch (NoSuchFieldException | IllegalAccessException e) {
                log().error(e);
            }
        }
        return map;
    }

    static Long delta_ms(Date d1, Date d2) {
        return d1 == null || d2 == null ? null : d2.getTime() - d1.getTime();
    }
}
