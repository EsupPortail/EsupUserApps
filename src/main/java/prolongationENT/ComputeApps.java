package prolongationENT;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

class ComputeApps {
    MainConf conf;
    Groups groups;
    Ldap ldap;
    Log log = LogFactory.getLog(ComputeApps.class);
    
    ComputeApps(MainConf conf) {
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
    
    Set<String> canImpersonate(String uid, String service) {
        Set<String> appIds = computeValidApps(uid, true);
        if (service != null) {
            // cleanup url
	    service = service.replace(":443/", "/");
            for (String appId : new HashSet<>(appIds)) {
                App app = conf.APPS.get(appId);
                boolean keep = app.serviceRegex != null && service.matches(app.serviceRegex);
                if (!keep) appIds.remove(appId);
            }
        }
        return appIds;
    }
  
    private Set<String> compute_wanted_attributes() {
        Set<String> r = groups.needed_ldap_attributes();
        r.add("memberOf"); // hard code memberOf
	r.addAll(conf.wanted_user_attributes);
        return r;
  }
    
}
