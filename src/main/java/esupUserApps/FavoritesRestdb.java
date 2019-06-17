package esupUserApps;


import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import com.google.gson.Gson;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static esupUserApps.Utils.*;

class FavoritesRestdb {
    
    Logger log = LoggerFactory.getLogger(FavoritesRestdb.class);

    Cache<List<String>> cache;
    Conf conf;

    static class Conf {
        String restdbUrl;
        String cacheLifetime;
        String requestTimeout;
    }
    
    static class RestdbResult {
        List<String> list;
    }

    FavoritesRestdb(Conf conf) {
        this.conf = conf;
        cache = new Cache<>(toSeconds(conf.cacheLifetime));
    }

    List<String> get(String userId) {
        List<String> r = cache.get(userId);
        if (r == null) {
            r = get_no_cache(userId);
            cache.put(userId, r);
        }
        return r;
    }
    
    List<String> get_no_cache(String userId) {
        String url = conf.restdbUrl + "/" + userId + "/list";
        try {
            return simplifyResult(new Gson().fromJson(new InputStreamReader(urlGET(url, conf.requestTimeout)), RestdbResult.class));
        } catch (IOException e) {
            log.error("error accessing " + url, e);
            return null;
        }
    }

    private List<String> simplifyResult(RestdbResult result) {
        return result != null ? result.list : null;
    }

    void purgeUserCache(String userId) {
        cache.remove(userId);
    }
}
