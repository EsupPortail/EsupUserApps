package org.esupportail.portal.services.prolongationENT;

import java.net.URL;
import java.util.HashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class Utils {
    
    public static <V> MapBuilder<V> asMap(String key, V value) {
        return new MapBuilder<V>().add(key, value);
    }

    public static class MapBuilder<V> extends HashMap<String, V> {
        public MapBuilder<V> add(String key, V value) {
            this.put(key, value);
            return this;
        }
    }
    
}
