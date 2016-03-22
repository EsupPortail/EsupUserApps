package prolongationENT;

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
    
    Conf.Apps init() {
        for (App app : APPS.values()) {
            if (app.inherit != null) {
                for (String base: app.inherit) {
                    app.merge(APPS_ATTRS.get(base));
                }
            }
            app.init();
        }
        return this;
    }
}

static class Main extends Conf.Apps {
    Set<String> admins = new HashSet<>();
    String cas_base_url;
    String uportal_base_url;
    String layout_url;
    String _currentIdpId;
    String current_idpAuthnRequest_url;
    String theme = "theme-simple";

    Set<String> wanted_user_attributes;
    int visit_max_inactive = 1800; // 30 min
    int time_before_checking_browser_cache_is_up_to_date = 60;

    static class Cas_impersonate {
        String cookie_name;
        String cookie_domain;
    };
    Cas_impersonate cas_impersonate;

    Shibboleth.Conf shibboleth;
    
    boolean isCasSingleSignOutWorking;
    boolean disableLocalStorage;
    boolean disableServerCache;
    boolean disableCSSInlining;

    // below have valid default values
    String cas_login_url;
    String cas_logout_url;
    String prolongationENT_url;
    String ent_logout_url;

    Conf.Main init() {
        if (cas_base_url == null) throw new RuntimeException("config.json must set cas_base_url");
        if (cas_login_url == null) cas_login_url = cas_base_url + "/login";
        if (cas_logout_url == null) cas_logout_url = cas_base_url + "/logout";
        if (prolongationENT_url == null) prolongationENT_url = uportal_base_url + "/ProlongationENT";
        if (layout_url == null) layout_url = prolongationENT_url + "/layout";
        if (ent_logout_url == null) ent_logout_url = Utils.via_CAS(cas_logout_url, uportal_base_url + "/Logout"); // nb: esup logout may not logout of CAS if user was not logged in esup portail, so forcing CAS logout in case
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
