package prolongationENT;

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
    

    String getTexte() {
        return text != null ? text : title;
    }

    String getDescription() {
        return description != null ? description : "";
    }

    void merge(App app) {
        if (app.serviceRegex != null) serviceRegex = app.serviceRegex;
        if (app.admins != null) admins = app.admins;
        if (app.cookies != null) cookies = app.cookies;
    }
    
    Map<String, String> export() {
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
