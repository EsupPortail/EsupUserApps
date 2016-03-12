var h = {
mylog: function(s) {
      if (window['console'] !== undefined) console.log(s);
},

head: function () {
    return document.getElementsByTagName("head")[0];
},

now: function () {
    return Math.round(new Date().getTime() / 1000);
},

/* return true if class has been removed */
removeClass: function (elt, classToToggle) {
    var regex = new RegExp(classToToggle, 'g');
       
    var without = elt.className.replace(regex , '');
    if (elt.className === without) {
	return false;
    } else {
        elt.className = without;
	return true;
    }
},

/* return true if class has been added */
toggleClass: function (elt, classToToggle) {
    if (h.removeClass(elt, classToToggle)) {
	return false;
    } else {
        elt.className += ' ' + classToToggle;
	return true;
    }
},

insertAfter: function (e, newNode) {
    e.parentNode.insertBefore(newNode, e.nextSibling);
},

simpleQuerySelectorAll: function (selector) {
    if (document.querySelectorAll) 
	try {
            return document.querySelectorAll(selector);
	} catch (err) {
	    return [];
	}

    // IE
    window['__qsaels'] = [];
    var style = h.addCSS(selector + "{x:expression(window.__qsaels.push(this))}");
    window.scrollBy(0, 0); // force evaluation
    h.head().removeChild(style);
    return window['__qsaels'];
},

simpleQuerySelector: function (selector) {
    if (document.querySelector) 
	try {
            return document.querySelector(selector);
	} catch (err) {
	    return null;
	}
    else
	return h.simpleQuerySelectorAll(selector)[0];
},

getCookie: function (name) {
    var m = document.cookie.match(new RegExp(name + '=([^;]+)'));
    return m && m[1];
},
removeCookie: function (name, domain, path) {
    document.cookie = name + "=;expires=Thu, 01 Jan 1970 00:00:01 GMT" + (domain ? ";domain=" + domain : '') + (path ? ";path=" + path : '');
},

simpleContains: function(a, val) {
    var len = a.length;
    for(var i = 0; i < len; i++) {
        if(a[i] == val) return true;
    }
    return false;
},

simpleEach: function (a, fn) {
    var len = a.length;
    for(var i = 0; i < len; i++) {
	fn(a[i], i, a);
    }
},

simpleEachObject: function (o, fn) {
    for(var k in o) {
	fn(k, o[k], o);
    }
},

simpleFilter: function(a, fn) {
    var r = [];
    var len = a.length;
    for(var i = 0; i < len; i++) {
	if (fn(a[i])) r.push(a[i]);
    }
    return r;
},

simpleMap: function(a, fn) {
    var r = [];
    var len = a.length;
    for(var i = 0; i < len; i++) {
	r.push(fn(a[i]));
    }
    return r;
},

intersect: function(a1, a2) {
    return h.simpleFilter(a1, function (e1) {
	return h.simpleContains(a2, e1);
    });
},

escapeQuotes: function(s) {
    var str = s;
    if (str) {
	str=str.replace(/\'/g,'&#39;');
	str=str.replace(/\"/g,'&quot;');
    }
    return str;
},

template: function(s, map) {
    // ES6-like template
    return s.replace(/\${\s*([^}]*?)\s*}/g, function (_m, e) { return map[e] });
},

onIdOrBody_rec: function(id, f) {
    if (id && document.getElementById(id) || document.body)
	f();
    else
	setTimeout(function () { h.onIdOrBody_rec(id, f) }, 9);
},

onIdOrBody: function(id, f) {
    if (id && document.getElementById(id) || document.body) {
	f();
    } else if (document.addEventListener) {
	document.addEventListener('DOMContentLoaded', f);
    } else 
	h.onIdOrBody_rec(id, f);
},

onReady_rec: function(f) {
    if (document['attachEvent'] ? document.readyState === "complete" : document.readyState !== "loading")
	f();
    else
	setTimeout(function () { h.onReady_rec(f) }, 9);
},

onReady: function (f) {
    // IE10 and lower don't handle "interactive" properly
    if (document['attachEvent'] ? document.readyState === "complete" : document.readyState !== "loading") {
	f();
    } else if (document.addEventListener) {
	document.addEventListener('DOMContentLoaded', f);
    } else 
	h.onReady_rec(f);
},

set_div_innerHTML: function(div_id, content) {
    var elt = document.getElementById(div_id);
    if (!elt) {
	elt = document.createElement("div");
	elt.setAttribute("id", div_id);
	document.body.insertBefore(elt, document.body.firstChild);
    }
    elt.innerHTML = content;
},

loadCSS: function(url, media) {
    var elt = document.createElement("link");
    elt.setAttribute("rel", "stylesheet");
    elt.setAttribute("type", "text/css");
    elt.setAttribute("href", url);
    if (media) elt.setAttribute("media", media);
    h.head().appendChild(elt);
},

unloadCSS: function(url) {
    var elt = h.simpleQuerySelector('link[href="' + url + '"]');
    if (elt) elt.parentNode.removeChild(elt);
},

addCSS: function(css) {
    var elt = document.createElement('style');
    elt.setAttribute("type", 'text/css');
    if (elt['styleSheet'])
	elt['styleSheet'].cssText = css;
    else
	elt.appendChild(document.createTextNode(css));
    h.head().appendChild(elt);
    return elt;
},

loadScript: function(url) {
    var elt = document.createElement("script");
    elt.setAttribute("type", "text/javascript");
    elt.setAttribute("src", url);
    elt.setAttribute("async", "async");
    elt.setAttribute("charset", "utf-8"); // workaround IE ignoring Content-Type
    h.head().appendChild(elt);
},

};

var loadBandeauJs = function(params) {
    if (b_E.uid)
	params.push("uid=" + encodeURIComponent(b_E.uid));
    params.push("app=" + (b_E.currentAppIds ? b_E.currentAppIds : [b_E.current]).join(","));

    var angle = window.orientation || '';
    var res = (angle == 90 || angle == -90) && navigator.userAgent.match(/Android.*Chrome/) ? screen.height + 'x' + screen.width : screen.width + 'x' + screen.height;
    res += ',' + (window.devicePixelRatio || 1).toFixed(2) + ',' + angle;
    
    params.push("res=" + res);
    if (pE.PARAMS) params.push('if_none_match=' + pE.PARAMS.hash); // b_E.PARAMS is null when called from loader.ts
    if (pE.loadTime) params.push("time=" + pE.loadTime);
    h.loadScript(b_E.url + "/layout" + (params.length ? "?" + params.join('&') : ''));
};
