if (!window.bandeau_ENT_maybe_loaded) 
  (function () {
    window.bandeau_ENT_maybe_loaded = true;
    var mylog = function() {};
    if (window['console'] !== undefined) { mylog = function(s) { console.log(s); }; } 
    //else { mylog = function(s) { alert(s); }; }

    if (parent == window) load_bandeau_ENT();

    function loadScript(url) {
	var script = document.createElement("script");
	script.setAttribute("type", "text/javascript");
	script.setAttribute("src", url);
	script.setAttribute("async", "async");
	script.setAttribute("charset", "utf-8"); // workaround IE ignoring Content-Type
	document.getElementsByTagName("head")[0].appendChild(script);
    }
      
    function load_bandeau_ENT() {
	var b_E = window.bandeau_ENT;
	if (!b_E) return;
	b_E.url = "https://ent-test.univ-paris1.fr/ProlongationENT";

        var navigationStart = window.performance && window.performance.timing && window.performance.timing.navigationStart;
        b_E.loadTime = navigationStart && (new Date() - navigationStart);
	
	if (!b_E.localStorage_prefix) b_E.localStorage_prefix = "bandeau_ENT:" + (b_E.uid ? b_E.uid + ":" : '');
	if (!b_E.localStorage_js_text_field) b_E.localStorage_js_text_field = "v5:js_text";
	
	var storageName = b_E.localStorage_prefix + b_E.localStorage_js_text_field;
	try {
	    if (window.sessionStorage && sessionStorage.getItem(storageName)) {
		mylog("loading bandeau from sessionStorage (" + storageName + ")");
		var val = eval(sessionStorage.getItem(storageName));
		if (val === "OK") return;
		else throw (new Error("invalid return value '" + val + "'"));
	    }
	} catch (err) {
	    mylog("load_bandeau_ENT: " + err.message);
	    try {
		sessionStorage.setItem(storageName, '');
	    } catch (err) { }
	}

        var angle = window.orientation || '';
        var res = (angle == 90 || angle == -90) && navigator.userAgent.match(/Android.*Chrome/) ? screen.height + 'x' + screen.width : screen.width + 'x' + screen.height;
        res += ',' + (window.devicePixelRatio || 1).toFixed(2) + ',' + angle;

	var url = b_E.url + "/js?app=" +
	    (b_E.currentAppIds ? b_E.currentAppIds : [b_E.current]).join(",") +
            "&res=" + res +
            (b_E.loadTime ? "&time=" + b_E.loadTime : "") +
	    (b_E.uid ? "&uid=" + b_E.uid : '');
	loadScript(url);
    }

})();

