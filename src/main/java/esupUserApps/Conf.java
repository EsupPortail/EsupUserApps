package esupUserApps;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

class Conf {

static class Auth {
    Ldap.LdapConf ldap;
}

// NB: we would want Conf.Main extends Conf.Apps, Conf.Auth.
// be we can't do it in java, so we cheat!
static class Apps extends Conf.Auth {
    Map<String, Map<String, Object>> GROUPS = new HashMap<>();
    Map<String, App> APPS = new HashMap<>();
    Map<String, App> APPS_ATTRS;
    Map<String, List<String>> LAYOUT;
    Map<String,String> url_vars;
    
    Conf.Apps init() {
        for (App app : APPS.values()) {
            if (app.inherit != null) {
                for (String base: app.inherit) {
                    App baseApp = APPS_ATTRS.get(base);
                    if (baseApp == null) throw new RuntimeException("invalid inherit " + base + " in app " + app.title);
                    app.merge(baseApp);
                }
            }
            app.init(url_vars);
        }
        return this;
    }
}

static class Main extends Conf.Apps {
    Set<String> admins = new HashSet<>();
    String cas_base_url;
    String EsupUserApps_url;
    String current_idpId;
    String current_idpAuthnRequest_url;

    Set<String> wanted_user_attributes;
    int visit_max_inactive = 1800; // 30 min

    static class Cas_impersonate {
        String cookie_name;
        String cookie_domain;
    };
    Cas_impersonate cas_impersonate;

    Shibboleth.Conf shibboleth;

    TopAppsAgimus.GlobalConf topApps;
    
    boolean isCasSingleSignOutWorking;
    boolean disableServerCache;

    // below have valid default values
    String cas_login_url;
    String cas_logout_url;

    Conf.Main init() {
        if (cas_base_url == null) throw new RuntimeException("config.json must set cas_base_url");
        if (cas_login_url == null) cas_login_url = cas_base_url + "/login";
        if (cas_logout_url == null) cas_logout_url = cas_base_url + "/logout";
        return this;
    }

    void merge(Conf.Apps conf) {
        GROUPS = conf.GROUPS;
        APPS = conf.APPS;
        LAYOUT = conf.LAYOUT;
    }
    void merge(Conf.Auth conf) {
        ldap = conf.ldap;
    }    
    void merge(Shibboleth.Conf conf) {
        if (conf.federation_metadata_url != null)
            shibboleth = conf;
    }
}

}
