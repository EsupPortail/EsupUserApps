package prolongationENT;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.LogFactory;


class Stats {           
    MainConf conf = null;

    static String request_last_time_attr_prefix = "request_last_time_";
    static String visit_id_attr = "visit_id";    
    static String global_visit_nb_attr = "global_visit_nb";
    static String app_visit_nb_attr_prefix = "app_visit_nb_";

    org.apache.commons.logging.Log log = LogFactory.getLog(Stats.class);
    
    Stats(MainConf conf) {
        this.conf = conf;
    }
    
    void log(HttpServletRequest request, String userId, Set<String> userChannels) {
        HttpSession session = request.getSession(false);
        String app = request.getParameter("app");
        if (app == null) return;
        String[] app_ = app.split(",");
        List<String> apps = intersect(app_, userChannels);
        app = apps.isEmpty() ? app_[0] : apps.get(0);
        
        log.info(
                 "[" + new java.util.Date() + "] " +
                 "[IP:" + request.getRemoteAddr() + "] " +
                 "[ID:" + userId + "] " +
                 "[APP:" + app + "] " +
                 "[URL:" + request.getHeader("Referer") + "] " +
                 "[USER-AGENT:" + request.getHeader("User-Agent") +"] " +
                 "[RES:" + request.getParameter("res") +"] " +
                 "[VISIT:" + get_visit_id(session) + ":" + get_app_visit_nb(session, app) +"] " +
                 "[LOAD-TIME:" + request.getParameter("time") + "]");
            
    }


    boolean isNewVisit(HttpSession session, String app) {
        String attr = request_last_time_attr_prefix + app;
        Long last = (Long) session.getAttribute(attr);
        long current = System.currentTimeMillis();

        session.setAttribute(attr, current);

        long max_inactive = conf.visit_max_inactive * 1000;
        return last == null || current - last > max_inactive;
    }

    Long counter(HttpSession session, String attr) {
        Long nb = (Long) session.getAttribute(attr);
        nb = (nb == null ? 0 : nb) + 1;
        session.setAttribute(attr, nb);
        return nb;
    }

    String get_visit_id(HttpSession session) {
        String id = (String) session.getAttribute(visit_id_attr);
        if (isNewVisit(session, "") || id == null) {
            id = UUID.randomUUID().toString();
            session.setAttribute(visit_id_attr, id);
        }
        return id;
    }

    Long get_app_visit_nb(HttpSession session, String app) {
        String attr = app_visit_nb_attr_prefix + app;
        Long nb = (Long) session.getAttribute(attr);
        if (isNewVisit(session, app) || nb == null) {
            nb = counter(session, global_visit_nb_attr);
            session.setAttribute(attr, nb);
        }
        return nb;
    }

    List<String> intersect(String[] l1, Set<String> l2) {
        List<String> r = new ArrayList<>();
        for (String e : l1)
            if (l2.contains(e))
                r.add(e);
        return r;            
    }

}
