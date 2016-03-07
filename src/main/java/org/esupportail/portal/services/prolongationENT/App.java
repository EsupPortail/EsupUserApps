package org.esupportail.portal.services.prolongationENT;

import java.util.Map;

class App extends ACLs {
    String text;
    String title;
    String description;
    String url;
    String shibbolethSPPrefix;
    boolean hideFromMobile = false;
    boolean hashelp = false;

    String inherit;
    String serviceRegex;
    ACLs admins;
    Cookies cookies = new Cookies();
    

    public String getTexte() {
        return text != null ? text : title;
    }

    public String getDescription() {
        return description != null ? description : "";
    }

    public void merge(App app) {
        if (app.serviceRegex != null) serviceRegex = app.serviceRegex;
        if (app.admins != null) admins = app.admins;
        if (app.cookies != null) cookies = app.cookies;
    }
    
    public Map<String, String> export() {
    	Map<String,String> r =
	    Utils.asMap("text", getTexte())
	    .add("title", title)
	    .add("description", getDescription())
	    .add("url", url);
    	if (shibbolethSPPrefix != null) r.put("shibbolethSPPrefix", shibbolethSPPrefix);
    	if (hashelp) r.put("hashelp", "" + hashelp);
    	return r;
    }
}
