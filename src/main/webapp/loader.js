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
	document.getElementsByTagName("head")[0].appendChild(script);
    }
      
    function load_bandeau_ENT() {
	var b_E = window.bandeau_ENT;
	if (!b_E) return;
	b_E.url = "https://esup-test.univ-paris1.fr/ProlongationENT";

	
	if (!b_E.localStorage_prefix) b_E.localStorage_prefix = "bandeau_ENT:" + (b_E.uid ? b_E.uid + ":" : '');
	if (!b_E.localStorage_js_text_field) b_E.localStorage_js_text_field = "v5:js_text";
	var url = b_E.url + "/js?app=" +
	    (b_E.currentAppIds ? b_E.currentAppIds : [b_E.current]).join(",") +
	    (b_E.uid ? "&uid=" + b_E.uid : '');
	
	var localStorageName = b_E.localStorage_prefix + b_E.localStorage_js_text_field;
	try {
	    if (window.localStorage && localStorage.getItem(localStorageName)) {
		mylog("loading bandeau from localStorage (" + localStorageName + ")");
		var val = eval(localStorage.getItem(localStorageName));
		if (val === "OK") return;
		else throw (new Error("invalid return value '" + val + "'"));
	    }
	} catch (err) {
	    mylog("load_bandeau_ENT: " + err.message);
	    try {
		localStorage.setItem(localStorageName, '');
	    } catch (err) { }
	}

	loadScript(url);
    }

})();

