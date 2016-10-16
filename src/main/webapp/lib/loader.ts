var loadBandeauJs = function(params) {
    if (pE.wanted_uid)
        params.push("uid=" + encodeURIComponent(pE.wanted_uid));
    params.push("app=" + (args.currentAppIds || [args.current]).join(","));

    var angle = window.orientation || '';
    var res = (angle == 90 || angle == -90) && navigator.userAgent.match(/Android.*Chrome/) ? screen.height + 'x' + screen.width : screen.width + 'x' + screen.height;
    res += ',' + (window.devicePixelRatio || 1).toFixed(2) + ',' + angle;
    
    params.push("res=" + res);
    if (pE.PARAMS) params.push('if_none_match=' + pE.PARAMS.hash); // pE.PARAMS is null when called from loader.ts
    if (pE.loadTime) params.push("time=" + pE.loadTime);
    params.push("callback=window.prolongation_ENT.main");
    h.loadScript(args.layout_url || pE.CONF.layout_url, params);
};

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
