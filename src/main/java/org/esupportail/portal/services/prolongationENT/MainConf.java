package org.esupportail.portal.services.prolongationENT;

import java.util.HashSet;
import java.util.Set;


public class MainConf {	   

    public Set<String> admins = new HashSet<>();
    public String cas_base_url;
    public String ent_base_url;
    public String ent_base_url_guest;
    public String _currentIdpId;
    public String current_idpAuthnRequest_url;

    public Set<String> apps_no_bandeau = new HashSet<>();
    public Set<String> url_bandeau_compatible = new HashSet<>();

    public Set<String> wanted_user_attributes;
    public int visit_max_inactive = 1800; // 30 min
    public int time_before_checking_browser_cache_is_up_to_date = 60;

    public class Cas_impersonate {
        public String cookie_name;
        public String cookie_domain;
    };
    public Cas_impersonate cas_impersonate;
    
    public boolean isCasSingleSignOutWorking;
    public boolean disableLocalStorage;

    // below have valid default values
    public String cas_login_url;
    public String cas_logout_url;
    public String bandeau_ENT_url;

    public void init() {
        cas_login_url = cas_base_url + "/login";
        cas_logout_url = cas_base_url + "/logout";
        bandeau_ENT_url = ent_base_url_guest + "/ProlongationENT";
    }

}
