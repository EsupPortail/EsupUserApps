package org.esupportail.portal.services;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

class ProlongationENTApp {
    String text;
    String title;
    String description;
    String url;
    String shibbolethSPPrefix;
    Set<String> users;
    Set<String> groups;
    boolean hideFromMobile = false;
    boolean hashelp = false;

    public String getTexte() {
        return text != null ? text : title;
    }

    public String getDescription() {
        return description != null ? description : "";
    }
    
    public Map<String, String> export() {
    	Map<String,String> r = new HashMap<String,String>();
    	r.put("text", getTexte());
    	r.put("title", title);
    	r.put("description", getDescription());
    	r.put("url", url);
    	if (shibbolethSPPrefix != null) r.put("shibbolethSPPrefix", shibbolethSPPrefix);
    	if (hashelp) r.put("hashelp", "" + hashelp);
    	return r;
    }
}
