package esupUserApps;


import java.util.Collections;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// the syntax of GROUPS is a subset of mongodb query, cf https://docs.mongodb.com/manual/reference/operator/query/
class Groups {   
    Map<String, Query> GROUPS;
    Set<String> needed_ldap_attributes;
    Logger log = LoggerFactory.getLogger(Groups.class);

    Groups(Map<String, Map<String, Object>> GROUPS) {
        needed_ldap_attributes = new HashSet<>();
        this.GROUPS = prepareQueries(GROUPS, needed_ldap_attributes);
    }
    
    Set<String> needed_ldap_attributes() {
        return needed_ldap_attributes;
    }
    
    boolean hasGroup(Map<String, List<String>> person, String name) {
        if (!GROUPS.containsKey(name)) {
            return hasPlainLdapGroup(person, name);
        } else {
            boolean b = GROUPS.get(name).test(person);
            log.info("hasGroup " + name  + " " + b);
            return b;
        }
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


    static abstract class Tester<P> {
        protected P p;
        abstract boolean test(String v);
        boolean test(List<String> l) {
            if (l == null) return test((String) null);
            for (String v : l) {
                if (test(v)) return true;
            }
            return false;
        }
    }
    static class TesterEq extends Tester<String> {
        TesterEq(String p) { this.p = p; }
        boolean test(String v) { return p == null ? v == null : p.equals(v); } // https://docs.mongodb.com/manual/tutorial/query-for-null-fields/
    }
    static class TesterRegex extends Tester<Pattern> {
        TesterRegex(String p) { this.p = Pattern.compile(p, Pattern.DOTALL | Pattern.MULTILINE); }
        boolean test(String v) { return v != null && p.matcher(v).matches(); }
    }
    static class TesterIn extends Tester<Set<String>> {
        TesterIn(List<String> p) { this.p = new HashSet<String>(p); }
        boolean test(String v) { return p.contains(v); }
    }
    static class TesterNot extends Tester<Tester<?>> {
        TesterNot(Tester<?> p) { this.p = p; }
        boolean test(String v) { return !p.test(v); }
        boolean test(List<String> l) { return !p.test(l); }
    }
    
    static interface Query {
        boolean test(Map<String, List<String>> doc);
    }
    static class QueryAttr implements Query {
        enum Mapper { Joinln }
        Mapper mapper;
        String attr;
        List<Tester<?>> l;
        QueryAttr(String attr, Mapper mapper, List<Tester<?>> l) {
            this.attr = attr; this.mapper = mapper; this.l = l;
        }
        public boolean test(Map<String, List<String>> doc) {
            List<String> doc_l = doc.get(attr);
            if (mapper == Mapper.Joinln) {
                doc_l = Collections.singletonList(doc_l == null ? "" : String.join("\n", doc_l));
            }
            for (Tester<?> t : l) {
                if (!t.test(doc_l)) return false;
            }
            return true;
        }
    }
    static class QueryAnd implements Query {
        List<Query> qs;
        QueryAnd(List<Query> qs) { this.qs = qs; }
        public boolean test(Map<String, List<String>> doc) {
            for (Query q: qs) {
                if (!q.test(doc)) return false;
            }
            return true;
        }
    }   
    static class QueryOr implements Query {
        List<Query> qs;
        QueryOr(List<Query> qs) { this.qs = qs; }
        public boolean test(Map<String, List<String>> doc) {
            for (Query q: qs) {
                if (q.test(doc)) return true;
            }
            return false;
        }
    }   
    
    private static Map<String, Query> prepareQueries(Map<String, Map<String, Object>> m, Set<String> attrs) {
        Map<String, Query> r = new HashMap<>();
        for (String name : m.keySet()) {
            r.put(name, prepareQuery(m.get(name), attrs));
        }
        return r;
    }

    private static Query prepareQuery(Map<String, Object> query, Set<String> attrs) {
        List<Query> r = new LinkedList<>();
        for (String key: query.keySet()) {
            if (key.equals("$and")) {
                r.add(new QueryAnd(prepareQueryList(query.get(key), attrs)));
            } else if (key.equals("$or")) {
                r.add(new QueryOr(prepareQueryList(query.get(key), attrs)));
            } else {
                QueryAttr qA = prepareQueryAttr(key, query.get(key));
                attrs.add(qA.attr);
                r.add(qA);
            }
        }
        return r.size() == 1
            ? r.get(0) // small optimisation for exactly one test
            : new QueryAnd(r);
    }

    private static QueryAttr prepareQueryAttr(String key, Object tester) {
        String attr = key;
        QueryAttr.Mapper mapper = null;
        if (key.contains("(")) {
            String attr_to_join = Utils.removeSuffixOrNull(key, ".joinln()");
            if (attr_to_join != null) {
                attr = attr_to_join;
                mapper = QueryAttr.Mapper.Joinln;
            } else {
                throw new RuntimeException("invalid attribute " + key + ". Only allowed extension is xxx.joinln()");
            }
        }
        return new QueryAttr(attr, mapper, prepareTesters(tester));
    }

    @SuppressWarnings("unchecked")
    private static List<Query> prepareQueryList(Object l, Set<String> attrs) {
        List<Query> r = new LinkedList<>();
        for (Object query : (List<?>) l) {
            r.add(prepareQuery((Map<String, Object>) query, attrs));
        }
        return r;
    }

    @SuppressWarnings("unchecked")
    private static List<Tester<?>> prepareTesters(Object tester) {
        List<Tester<?>> r = new LinkedList<>();
        if (tester instanceof String) {
            r.add(new TesterEq((String) tester));
        } else {
            Map<String, Object> tester_ = (Map<String, Object>) tester;
            for (String op : tester_.keySet()) {
                r.add(prepareTester(op, tester_.get(op)));
            }
        }
        return r;        
    }

    @SuppressWarnings({ "unchecked" })
    private static Tester<?> prepareTester(String op, Object p) {
        switch (op) {
        case "$regex": return new TesterRegex((String) p);
        case "$eq":    return new TesterEq((String) p);
        case "$ne":    return new TesterNot(new TesterEq((String) p));
        case "$in":    return new TesterIn((List<String>) p);
        case "$nin":   return new TesterNot(new TesterIn((List<String>) p));
        case "$exists": throw new RuntimeException("use { \"$ne\": null } instead of { $exists: true }   and  { \"$eq\": null } instead of { $exists: false }. You can also use $in / $nin with null value.");
        default: throw new RuntimeException("unknown operator " + op);
        }
    }
    
}
