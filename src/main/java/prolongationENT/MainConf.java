package prolongationENT;

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
    Map<String, Map<String, Object>> GROUPS;
    Map<String, App> APPS;
    Map<String, App> APPS_ATTRS;
    Map<String, List<String>> LAYOUT;
    
    AppsConf init() {
    	for (App app : APPS.values()) {
            if (app.inherit != null) {
                app.merge(APPS_ATTRS.get(app.inherit));
            }
        }
        return this;
    }
}

class MainConf extends AppsConf {	   
	Set<String> admins = new HashSet<>();
    String cas_base_url;
    String ent_base_url;
    String ent_base_url_guest;
    String _currentIdpId;
    String current_idpAuthnRequest_url;

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
    String bandeau_ENT_url;

    MainConf init() {
        if (cas_login_url == null) cas_login_url = cas_base_url + "/login";
        if (cas_logout_url == null) cas_logout_url = cas_base_url + "/logout";
        if (bandeau_ENT_url == null) bandeau_ENT_url = ent_base_url_guest + "/ProlongationENT";
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
