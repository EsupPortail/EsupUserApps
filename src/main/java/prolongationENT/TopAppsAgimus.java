package prolongationENT;


// This module expects a wrapper in front of elasticsearch.
//
// Suggestion (Apache):
//
//  RewriteEngine On
//  # add "||" to the endDate if needed
//  RewriteRule ^/topApps/([",\w]*)/([",\w]*)/(\w+),([\d-]+)$ /topApps/$1/$2/$3,$4||
//  # the real rewriting
//  RewriteRule ^/topApps/([",\w]*)/(NONE)?/(\w+),([\w|-]+)$   http://localhost:9200/logstash-ent-pro-*/_search?source={query:{bool:{filter:[{terms:{"eduPersonAffiliation.raw":[$1]}},{range:{"@timestamp":{"gte":"$4-$3","lte":"$4"}}}]}},aggs:{Services:{terms:{field:"service.raw",size:25,order:{Sessions:"desc"}},aggs:{Sessions:{cardinality:{field:"appli_session_id.raw"}}}}},size:0} [P]
//  RewriteRule ^/topApps/([",\w]*)/([",\w]*)/(\w+),([\w|-]+)$ http://localhost:9200/logstash-ent-pro-*/_search?source={query:{bool:{filter:[{terms:{"eduPersonAffiliation.raw":[$1]}},{terms:{"supannEntiteAffectation.raw":[$2]}},{range:{"@timestamp":{"gte":"$4-$3","lte":"$4"}}}]}},aggs:{Services:{terms:{field:"service.raw",size:25,order:{Sessions:"desc"}},aggs:{Sessions:{cardinality:{field:"appli_session_id.raw"}}}}},size:0} [P]


import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gson.Gson;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import static prolongationENT.Utils.*;

class TopAppsAgimus {
    
    Log log = LogFactory.getLog(TopAppsAgimus.class);

    SimpleDateFormat dateFormat_MMdd = new SimpleDateFormat("MM-dd");
    SimpleDateFormat dateFormat_yyyy = new SimpleDateFormat("yyyy");
    
    Cache<List<String>> cache;
    Conf conf;

    static class CommonConf {
        String elasticSearchWrapperUrl;
        Set<String> blacklist;
    }
    
    static class GlobalConf extends CommonConf {
        Conf stable;
        Conf latest;

        GlobalConf init() {
            if (stable != null) stable.merge(this);
            if (latest != null) latest.merge(this);
            return this;
        }
    }

    static class Conf extends CommonConf {
        // using elasticsearch time units
        String cacheLifetime;
        String interval;
        Integer minAnswers;

        // expected format "MM-dd". eg: "08-01" is first of august)
        // must be ordered. eg: ["08-01", "01-01"]
        List<String> moduloDates;

        void merge(CommonConf conf) {
            if (elasticSearchWrapperUrl == null) elasticSearchWrapperUrl = conf.elasticSearchWrapperUrl;
            if (blacklist == null) blacklist = conf.blacklist;
        }
    }
    
    static class ElasticSearchResult {
        static class Bucket {
            String key;
        }
        static class Aggregation {
            List<Bucket> buckets;
        }
        static class Aggregations {
            Aggregation Services;
        }
        Aggregations aggregations;
    }

    TopAppsAgimus(Conf conf) {
        this.conf = conf;
        cache = new Cache<>(toSeconds(conf.cacheLifetime));
    }
    
    String endDate() {
        if (conf.moduloDates == null) return "now";

        Date now = new Date();
        String MMdd = dateFormat_MMdd.format(now);
        String yyyy = dateFormat_yyyy.format(now);
        for (String moduloDate : conf.moduloDates) {
            if (moduloDate.compareTo(MMdd) <= 0) return yyyy + "-" + moduloDate;
        }
        // take first in previous year
        return "" + (Integer.parseInt(yyyy) - 1) + "-" + conf.moduloDates.get(0);
    }
    
    List<String> get(Ldap.Attrs attrs) {
        List<String> r = get(encodeParams(attrs.get("eduPersonPrimaryAffiliation"),
                                          attrs.get("supannEntiteAffectation")));
        if (conf.minAnswers != null && r != null && r.size() < conf.minAnswers) {
            r.addAll(get(encodeParams(attrs.get("eduPersonPrimaryAffiliation"), null)));
        }
        return r;
    }

    List<String> get(String urlPath) {
        List<String> r = cache.get(urlPath);
        if (r == null) {
            r = get_no_cache(urlPath);
            cache.put(urlPath, r);
        }
        return r;
    }
    
    List<String> get_no_cache(String urlPath) {
        try {
            String url = conf.elasticSearchWrapperUrl + urlPath;
            return simplifyResult(new Gson().fromJson(new InputStreamReader(urlGET(url)), ElasticSearchResult.class));
        } catch (IOException e) {
            log.error(e);
            return null;
        }
    }

    List<String> simplifyResult(ElasticSearchResult result) {
        if (result == null || result.aggregations == null || result.aggregations.Services == null || result.aggregations.Services.buckets == null) return null;
        List<String> r = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (ElasticSearchResult.Bucket bucket : result.aggregations.Services.buckets)
            if (!seen.contains(bucket.key.toLowerCase()) &&
                (conf.blacklist == null || !conf.blacklist.contains(bucket.key))) {
                seen.add(bucket.key.toLowerCase());
                r.add(bucket.key);
            }
        return r;
    }

    String encodeParams(List<String> affiliation, List<String> affectation) {
        StringBuilder sb = new StringBuilder();
        encodeParams(sb.append("/"), affiliation);
        encodeParams(sb.append("/"), affectation);
        sb.append("/").append(conf.interval).append(",").append(endDate());
        return sb.toString();
    }
    
    StringBuilder encodeParams(StringBuilder sb, List<String> params) {
        if (params == null) return sb;
        boolean beginning = true;
        for (String param : params) {
            if (!beginning) sb.append(",");
            beginning = false;
            sb.append(urlencode("\"")).append(urlencode(param)).append(urlencode("\""));
        }
        return sb;
    }

    String removeLastChar(String s) {
        return s.substring(0, s.length()-1);
    }
    
    int toSeconds(String timeWithUnit) {
        if ("".equals(timeWithUnit))
            throw new RuntimeException("invalid time with unit");

        int lastChar = timeWithUnit.length() - 1;
        int nb = Integer.parseInt(timeWithUnit.substring(0, lastChar));
        switch (timeWithUnit.charAt(lastChar)) {
        case 's': return nb;
        case 'm': return nb * 60;
        case 'h': return nb * 60 * 60;
        case 'd': return nb * 60 * 60 * 24;
        case 'w': return nb * 60 * 60 * 24 * 7;
        case 'M': return nb * 60 * 60 * 24 * 30;
        case 'y': return nb * 60 * 60 * 24 * 365;
        }
        throw new RuntimeException("invalid time with unit " + timeWithUnit);
    }
}
