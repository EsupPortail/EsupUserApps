var h = pE.helpers; // for type checking
h = {
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
    var regex = new RegExp("\\b" + classToToggle + "\\b", 'g');
       
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

prependChild: function (e, newNode) {
    e.insertBefore(newNode, e.firstChild);
},

insertAfter: function (e, newNode) {
    e.parentNode.insertBefore(newNode, e.nextSibling);
},

simpleQuerySelectorAll: function (selector) {
        try {
            return document.querySelectorAll(selector);
        } catch (err) {
            return [];
        }
},

simpleQuerySelector: function (selector) {
        try {
            return document.querySelector(selector);
        } catch (err) {
            return null;
        }
},

eltMatches: function (elt, selector) {
    var f = Element.prototype.matches || Element.prototype.msMatchesSelector || Element.prototype.webkitMatchesSelector;
    return f && f.call(elt, selector);
},
	
eltClosest: function (elt, selector) {
    if (Element.prototype['closest']) return elt.closest(selector);

    for (; elt && elt.nodeType === 1; elt = elt.parentElement) {
	if (h.eltMatches(elt, selector)) return elt;
    }
    return null;    
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
        if(a[i] === val) return true;
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

toJSON: function (o) {
    // workaround old prototype.js messing with JSON.stringify. Needed for nuxeo 5.8
    var _array_tojson = Array.prototype.toJSON;
    if (_array_tojson) delete Array.prototype.toJSON;
    var s = JSON.stringify(o);
    if (_array_tojson) Array.prototype.toJSON = _array_tojson;
    return s;
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
    return s.replace(/\${\s*([^{}]*?)\s*}/g, function (_m, e) { return map[e]; });
},

onIdOrBody: function(id, f) {
    if (id && document.getElementById(id) || document.body) {
        f();
    } else
        h.onReady(f);
},

onReady: function (f) {
    // IE10 and lower don't handle "interactive" properly
    if (document['attachEvent'] ? document.readyState === "complete" : document.readyState !== "loading") {
        f();
    } else if (document.addEventListener) {
        document.addEventListener('DOMContentLoaded', f);
    } else 
        setTimeout(function () { h.onReady(f); }, 9);
},

set_div_innerHTML: function(div_id, content) {
    var elt = document.getElementById(div_id);
    if (!elt) {
        elt = document.createElement("div");
        elt.setAttribute("id", div_id);
        h.prependChild(document.body, elt);
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

loadScript: function(url, params) {
    var elt = document.createElement("script");
    elt.setAttribute("type", "text/javascript");
    elt.setAttribute("src", url + (params && params.length ? "?" + params.join('&') : ''));
    elt.setAttribute("async", "async");
    elt.setAttribute("charset", "utf-8"); // workaround IE ignoring Content-Type
    h.head().appendChild(elt);
},

};

pE.helpers = h;
