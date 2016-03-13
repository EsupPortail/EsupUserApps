package prolongationENT;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

class AuthConf {
	Ldap.LdapConf ldap;
}

// NB: we would want MainConf extends AppsConf, AuthConf.
// be we can't do it in java, so we cheat!
class AppsConf extends AuthConf {
    Map<String, Map<String, Object>> GROUPS = new HashMap<>();
    Map<String, App> APPS = new HashMap<>();
    Map<String, App> APPS_ATTRS;
    Map<String, List<String>> LAYOUT;
    
    AppsConf init() {
    	for (App app : APPS.values()) {
            if (app.inherit != null) {
                app.merge(APPS_ATTRS.get(app.inherit));
            }
            app.init();
        }
        return this;
    }
}

class MainConf extends AppsConf {	   
	Set<String> admins = new HashSet<>();
    String cas_base_url;
    String uportal_base_url;
    String uportal_base_url_guest;
    String layout_url;
    String _currentIdpId;
    String current_idpAuthnRequest_url;
    String theme = "theme-simple";

    Set<String> apps_no_bandeau = new HashSet<>();
    Set<String> url_bandeau_compatible = new HashSet<>();

    Set<String> wanted_user_attributes;
    int visit_max_inactive = 1800; // 30 min
    int time_before_checking_browser_cache_is_up_to_date = 60;

    class Cas_impersonate {
        String cookie_name;
        String cookie_domain;
    };
    Cas_impersonate cas_impersonate;
    
    boolean isCasSingleSignOutWorking;
    boolean disableLocalStorage;

    // below have valid default values
    String cas_login_url;
    String cas_logout_url;
    String prolongationENT_url;
    String ent_logout_url;

    MainConf init() {
        if (cas_login_url == null) cas_login_url = cas_base_url + "/login";
        if (cas_logout_url == null) cas_logout_url = cas_base_url + "/logout";
        if (uportal_base_url_guest == null) uportal_base_url_guest = uportal_base_url;
        if (prolongationENT_url == null) prolongationENT_url = uportal_base_url_guest + "/ProlongationENT";
        if (layout_url == null) layout_url = prolongationENT_url + "/layout";
	if (ent_logout_url == null) ent_logout_url = Utils.via_CAS(cas_logout_url, uportal_base_url + "/Logout"); // nb: esup logout may not logout of CAS if user was not logged in esup portail, so forcing CAS logout in case
        return this;
    }

    void merge(AppsConf conf) {
    	GROUPS = conf.GROUPS;
    	APPS = conf.APPS;
    	LAYOUT = conf.LAYOUT;
    }
    void merge(AuthConf conf) {
    	ldap = conf.ldap;
    }    
}
