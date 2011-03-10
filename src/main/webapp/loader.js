if (!window.bandeau_ENT_maybe_loaded) 
  (function () {
    window.bandeau_ENT_maybe_loaded = true;
    var mylog = function() {};
    if (window['console'] !== undefined) { mylog = function(s) { console.log(s); }; } 
    //else { mylog = function(s) { alert(s); }; }

    if (parent == window) load_bandeau_ENT();
    //if (parent == window) loadScript("https://bandeau-ent.univ-paris1.fr/piwik-affiliation.php");

    function loadScript(url) {
	var script = document.createElement("script");
	script.setAttribute("type", "text/javascript");
	script.setAttribute("src", url);
	script.setAttribute("async", "async");
	document.getElementsByTagName("head")[0].appendChild(script);
    }
      
    function load_bandeau_ENT() {
	var b_E = window.bandeau_ENT;
	if (!b_E.url) b_E.url = "https://uportal3-test.univ-paris1.fr/ProlongationENT";
	if (!b_E.localStorage_prefix) b_E.localStorage_prefix = "bandeau_ENT:";
	if (!b_E.localStorage_js_text_field) b_E.localStorage_js_text_field = "v5:js_text";
	var url = b_E.url + "/js";
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

