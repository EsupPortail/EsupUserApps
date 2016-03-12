if (window.bandeau_ENT && !window.bandeau_ENT.maybe_loaded) 
(function () {
    var b_E = window.bandeau_ENT;
    var h = b_E.helpers;
    
    b_E.maybe_loaded = true;

    if (parent == window) load_bandeau_ENT();

    function load_bandeau_ENT() {
	     b_E.url = b_E.CONF.bandeau_ENT_url;

        var navigationStart = window.performance && window.performance.timing && window.performance.timing.navigationStart;
        b_E.loadTime = navigationStart && (new Date().getTime() - navigationStart);

        if (b_E.uid)
            b_E.forced_uid = true;
        else 
	    b_E.uid = h.getCookie("CAS_IMPERSONATED");
	if (!b_E.localStorage_prefix) b_E.localStorage_prefix = "bandeau_ENT:" + (b_E.uid ? b_E.uid + ":" : '');
	if (!b_E.localStorage_js_text_field) b_E.localStorage_js_text_field = "v6:js_text";
	
	var storageName = b_E.localStorage_prefix + b_E.localStorage_js_text_field;
	try {
	    if (window.sessionStorage && !pE.CONF.disableLocalStorage && sessionStorage.getItem(storageName)) {
		h.mylog("loading bandeau from sessionStorage (" + storageName + ")");
		var val = eval(sessionStorage.getItem(storageName));
		if (val === "OK") return;
		else throw (new Error("invalid return value '" + val + "'"));
	    }
	} catch (err) {
	    h.mylog("load_bandeau_ENT: " + err.message);
	    try {
		sessionStorage.setItem(storageName, '');
	    } catch (err) { }
	}

  	loadBandeauJs([]);
    }

})();
