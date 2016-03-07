package prolongationENT;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

class ComputeLayout {
    MainConf conf;
    Groups groups;
    Ldap ldap;
    Log log = LogFactory.getLog(ComputeLayout.class);
    
    ComputeLayout(MainConf conf) {
        this.conf = conf;
        ldap = new Ldap(conf.ldap);
        groups = new Groups(conf.GROUPS);
    }

    Set<String> computeValidApps(String uid, boolean wantImpersonate) {
        return computeValidApps(getLdapPeopleInfo(uid), wantImpersonate);
    }

    Ldap.Attrs getLdapPeopleInfo(String uid) {
	return ldap.getLdapPeopleInfo(uid, compute_wanted_attributes());
    }
    
    Set<String> computeValidApps(Ldap.Attrs person, boolean wantImpersonate) {
        String user = person.get("uid").get(0);
        Map<String, Boolean> groupsCache = new HashMap<>();

        Set<String> r = new HashSet<>();
        for (String appId : conf.APPS.keySet()) {
            App app_ = conf.APPS.get(appId);
            ACLs app = wantImpersonate ? app_.admins : app_;
            if (app == null) continue;
            Boolean found = false;

            if (app.users != null) {
                if (app.users.contains(user))
                    found = true;
            }

            if (!found && app.groups != null) {
                for (String group : app.groups) {
                    if (groups.hasGroup(person, group, groupsCache))
                        found = true;
                }
            }
            if (found) r.add(appId);
        }
        return r;
    }
  
    private Set<String> compute_wanted_attributes() {
        Set<String> r = groups.needed_ldap_attributes();
        r.add("memberOf"); // hard code memberOf
	r.addAll(conf.wanted_user_attributes);
        return r;
  }
    
}
