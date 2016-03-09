(function () {

var mylog = function() {};
if (window['console'] !== undefined) { mylog = function(s) { console.log(s); }; } 

function head() {
    return document.getElementsByTagName("head")[0];
}

function now() {
    return Math.round(new Date().getTime() / 1000);
}

/* return true if class has been removed */
function removeClass(elt, classToToggle) {
    var regex = new RegExp(classToToggle, 'g');
       
    var without = elt.className.replace(regex , '');
    if (elt.className === without) {
	return false;
    } else {
        elt.className = without;
	return true;
    }
}

/* return true if class has been added */
function toggleClass(elt, classToToggle) {
    if (removeClass(elt, classToToggle)) {
	return false;
    } else {
        elt.className += ' ' + classToToggle;
	return true;
    }
}

function insertAfter(e, newNode) {
    e.parentNode.insertBefore(newNode, e.nextSibling);
}

function simpleQuerySelectorAll(selector) {
    if (document.querySelectorAll) 
	try {
            return document.querySelectorAll(selector);
	} catch (err) {
	    return [];
	}

    // IE
    window.__qsaels = [];
    var style = addCSS(selector + "{x:expression(window.__qsaels.push(this))}");
    window.scrollBy(0, 0); // force evaluation
    head().removeChild(style);
    return window.__qsaels;
}

function simpleQuerySelector(selector) {
    if (document.querySelector) 
	try {
            return document.querySelector(selector);
	} catch (err) {
	    return null;
	}
    else
	return simpleQuerySelectorAll(selector)[0];
}

function getCookie(name) {
    var m = document.cookie.match(new RegExp(name + '=([^;]+)'));
    return m && m[1];
}
function removeCookie(name, domain, path) {
    document.cookie = name + "=;expires=Thu, 01 Jan 1970 00:00:01 GMT" + (domain ? ";domain=" + domain : '') + (path ? ";path=" + path : '');
}

function simpleContains(a, val) {
    var len = a.length;
    for(var i = 0; i < len; i++) {
        if(a[i] == val) return true;
    }
    return false;
}

function simpleEach(a, fn) {
    var len = a.length;
    for(var i = 0; i < len; i++) {
	fn(a[i], i, a);
    }
}

function simpleEachObject(o, fn) {
    for(var k in o) {
	fn(k, o[k], o);
    }
}

function simpleFilter(a, fn) {
    var r = [];
    var len = a.length;
    for(var i = 0; i < len; i++) {
	if (fn(a[i])) r.push(a[i]);
    }
    return r;
}

function simpleMap(a, fn) {
    var r = [];
    var len = a.length;
    for(var i = 0; i < len; i++) {
	r.push(fn(a[i]));
    }
    return r;
}

function intersect(a1, a2) {
    return simpleFilter(a1, function (e1) {
	return simpleContains(a2, e1);
    });
}

function escapeQuotes(s) {
    var str = s;
    if (str) {
	str=str.replace(/\'/g,'&#39;');
	str=str.replace(/\"/g,'&quot;');
    }
    return str;
}

function replaceAll(s, target, replacement) {
    return s.split(target).join(replacement);
}

function onIdOrBody_rec(id, f) {
    if (id && document.getElementById(id) || document.body)
	f();
    else
	setTimeout(function () { onIdOrBody_rec(id, f) }, 9);
}

function onIdOrBody(id, f) {
    if (id && document.getElementById(id) || document.body) {
	f();
    } else if (document.addEventListener) {
	document.addEventListener('DOMContentLoaded', f);
    } else 
	onIdOrBody_rec(id, f);
}

function onReady_rec(f) {
    if (document.attachEvent ? document.readyState === "complete" : document.readyState !== "loading")
	f();
    else
	setTimeout(function () { onReady_rec(f) }, 9);
}

function onReady(f) {
    // IE10 and lower don't handle "interactive" properly
    if (document.attachEvent ? document.readyState === "complete" : document.readyState !== "loading") {
	f();
    } else if (document.addEventListener) {
	document.addEventListener('DOMContentLoaded', f);
    } else 
	onReady_rec(f);
}

function set_div_innerHTML(div_id, content) {
    var elt = document.getElementById(div_id);
    if (!elt) {
	elt = document.createElement("div");
	elt.setAttribute("id", div_id);
	document.body.insertBefore(elt, document.body.firstChild);
    }
    elt.innerHTML = content;
}

function loadCSS (url, media) {
    var elt = document.createElement("link");
    elt.setAttribute("rel", "stylesheet");
    elt.setAttribute("type", "text/css");
    elt.setAttribute("href", url);
    if (media) elt.setAttribute("media", media);
    head().appendChild(elt);
};

function unloadCSS(url) {
    var elt = simpleQuerySelector('link[href="' + url + '"]');
    if (elt) elt.parentNode.removeChild(elt);
}

function addCSS(css) {
    var elt = document.createElement('style');
    elt.setAttribute("type", 'text/css');
    if (elt.styleSheet)
	elt.styleSheet.cssText = css;
    else
	elt.appendChild(document.createTextNode(css));
    head().appendChild(elt);
    return elt;
}

function loadScript (url) {
    var elt = document.createElement("script");
    elt.setAttribute("type", "text/javascript");
    elt.setAttribute("src", url);
    elt.setAttribute("async", "async");
    elt.setAttribute("charset", "utf-8"); // workaround IE ignoring Content-Type
    head().appendChild(elt);
}
