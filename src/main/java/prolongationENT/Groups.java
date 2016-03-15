package prolongationENT;


import java.util.Collections;
import java.util.List;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


class Groups {
    Map<String, Map<String, List<Pattern>>> GROUPS;
    Log log = LogFactory.getLog(Ldap.class);

    Groups(Map<String, Map<String, Object>> GROUPS) {
        this.GROUPS = prepareRegexes(GROUPS);
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static Map<String, Map<String, List<Pattern>>> prepareRegexes(Map<String, Map<String, Object>> m) {
        for (Map<String,Object> attr2regexes : m.values()) {
            for (String attr : attr2regexes.keySet()) {
                Object regexes = attr2regexes.get(attr);
                List<Pattern> p = new LinkedList<>();
                if (regexes instanceof String) {
                    regexes = Collections.singletonList(regexes);
                }
                for (String regex : (List<String>) regexes) {
                    p.add(Pattern.compile(regex));
                }
                attr2regexes.put(attr, p);
            }
        }
        // down casting to precise type since we've done the job now
        return (Map) m;
    }

    Set<String> needed_ldap_attributes() {
        Set<String> r = new HashSet<>();
        for (Map<String, List<Pattern>> attr2regexes : GROUPS.values()) {
            r.addAll(attr2regexes.keySet());
        }
        return r;
    }
    
    boolean hasGroup(Map<String, List<String>> person, String name) {
        if (!GROUPS.containsKey(name)) {
            return hasPlainLdapGroup(person, name);
        }
            
        for (Entry<String, List<Pattern>> attr_regexes : GROUPS.get(name).entrySet()) {
            String attr = attr_regexes.getKey();
                
            List<String> attrValues = person.get(attr);
            if (attrValues == null) {
                log.warn("missing attribute " + attr);
                return false;
            }
            for (Pattern regex : attr_regexes.getValue()) {
                boolean okOne = false;
                for (String v : attrValues) {
                    if (regex.matcher(v).matches()) {
                        okOne = true;
                        break;
                    }
                }
                if (!okOne) return false;
            }
        }
        return true;
    }

    boolean hasPlainLdapGroup(Map<String, List<String>> person, String name) {
        // check memberOf
        List<String> vals = person.get("memberOf");
        if (vals != null) {
            for (String val : vals)
                if (val.startsWith("cn=" + name + ",")) return true;
        }
        return false;
    }

    boolean hasGroup(Ldap.Attrs person, String name, Map<String, Boolean> cache) {
        Boolean r = cache.get(name);
        if (r == null) {
            r = hasGroup(person, name);
            cache.put(name, r);
        }
        return r;
    }
}
