package prolongationENT;

import java.util.HashSet;
import java.util.Set;


class MainConf {	   

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

    void init() {
        cas_login_url = cas_base_url + "/login";
        cas_logout_url = cas_base_url + "/logout";
        bandeau_ENT_url = ent_base_url_guest + "/ProlongationENT";
    }

}
