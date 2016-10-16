function loader() {
    if (pE.maybe_loaded) return; 
    if (parent !== window) return; // never in iframe

    pE.maybe_loaded = true;
    
    var navigationStart = window.performance && window.performance.timing && window.performance.timing.navigationStart;
    pE.loadTime = navigationStart && (new Date().getTime() - navigationStart);

    pE.wanted_uid = args.uid || h.getCookie("CAS_IMPERSONATED");
    pE.localStorage_prefix = "bandeau_ENT:" + (pE.wanted_uid ? pE.wanted_uid + ":" : '');
    pE.localStorage_js_text_field = "v6:js_text";
    
    var storageName = pE.localStorage_prefix + pE.localStorage_js_text_field;
    try {
        if (window.sessionStorage && !pE.CONF.disableLocalStorage && sessionStorage.getItem(storageName)) {
            h.mylog("loading bandeau from sessionStorage (" + storageName + ")");
            var val = eval(sessionStorage.getItem(storageName));
            if (val === "OK") return;
            else throw (new Error("invalid return value '" + val + "'"));
        }
    } catch (err) {
        h.mylog(err);
        try {
            sessionStorage.setItem(storageName, '');
        } catch (err) { }
    }

    loadBandeauJs([]);
}
if (args) loader();
