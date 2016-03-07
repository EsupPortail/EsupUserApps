package org.esupportail.portal.services.prolongationENT;

import java.util.List;
import java.util.LinkedList;


class Cookies {
    static String[] default_names = new String[] { "JSESSIONID", "PHPSESSID" };
    static String[] default_name_prefixes = new String[] { "_shibsession_" };

    String path;
    List<String> names;
    List<String> name_prefixes;

    String path() {
        return path != null ? path : "/";
    }

    List<String> names() {
        List<String> r = new LinkedList<>();
        if (names != null) r.addAll(names);
        for (String name : default_names) r.add(name);
        return r;
    }
    
    List<String> name_prefixes() {
        List<String> r = new LinkedList<>();
        if (name_prefixes != null) r.addAll(name_prefixes);
        for (String prefix : default_name_prefixes) r.add(prefix);
        return r;
    }
}
