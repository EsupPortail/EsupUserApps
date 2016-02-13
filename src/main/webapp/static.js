(function () {

'use strict';
var CONF = undefined;

var DATA = undefined;

var CSS = undefined;

var PARAMS = undefined;


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

function bandeau_ENT_Account_toggleOpen() {
    toggleClass(document.getElementById('portalPageBarAccount'), 'open');
    var isOpen = toggleClass(document.getElementById('portalPageBarAccountInner'), 'open');

    if (isOpen) {
	var close = function() {
	    removeClass(document.getElementById('portalPageBarAccount'), 'open');
	    removeClass(document.getElementById('portalPageBarAccountInner'), 'open');
	    document.removeEventListener("click", close, false);
	};
	setTimeout(function () { document.addEventListener("click", close, false); }, 0);
    }
    return false;
}

function bandeau_ENT_Menu_toggle() {
    if (b_E.quirks && simpleContains(b_E.quirks, 'global-menuClosed-class'))
	toggleClass(document.body, 'bandeau_ENT_menuClosed');
    return toggleClass(document.getElementById('bandeau_ENT_Inner'), 'menuClosed');
}

function bandeau_ENT_Menu_toggleAndStore() {
    var b = bandeau_ENT_Menu_toggle();
    if (window.localStorage) localStorageSet("menuClosed", b ? "true" : "false");

    return false;
}

function installToggleMenu(hide) {
    var hideByDefault = b_E.hide_menu;
    var toggleMenu = document.getElementById('bandeau_ENT_portalPageBarToggleMenu');
    if (toggleMenu) {
	toggleMenu.onclick = bandeau_ENT_Menu_toggleAndStore;
	var savedState = window.localStorage && localStorageGet("menuClosed");
	if (hide || savedState === "true" || savedState !== "false" && hideByDefault)
	    bandeau_ENT_Menu_toggle();
    }
}

function computeHeader() {
    var app_logout_url = CONF.ent_logout_url;
    var logout_url = app_logout_url; //CONF.bandeau_ENT_url + '/logout?service=' + encodeURIComponent(app_logout_url);
    return replaceAll(DATA.bandeauHeader, "<%logout_url%>", logout_url);
}

function computeLink(appId, app) {
    var a = "<a title='" + escapeQuotes(app.description) + "' href='" + app.url + "'>" + escapeQuotes(app.text) + "</a>";
    return "<li class='bandeau_ENT_Menu_Entry_" + appId + "'>" + a + "</li>";
}

function computeMenu(currentAppId) {
    var li_list = simpleMap(DATA.layout, function (tab) {
	var sub_li_list = simpleMap(tab.apps, function(appId) {
	    return computeLink(appId, DATA.apps[appId]);
	});
    
	var className = simpleContains(tab.apps, currentAppId) ? "activeTab" : "inactiveTab";
	return "<li class='" + className + "' onclick=''><span>" + escapeQuotes(tab.title) + "</span><ul>" + sub_li_list.join("\n") + "</ul></li>";
    });

    var toggleMenuSpacer = "<div class='toggleMenuSpacer'></div>\n";

    return "<ul class='bandeau_ENT_Menu'>\n" + toggleMenuSpacer + li_list.join("\n") + "\n</ul>";
}

function getValidAppIds() {
    var l = [];
    simpleEach(DATA.layout, function (tab) {
	l.push.apply(l, tab.apps);
    });
    return l;
}

function computeBestCurrentAppId() {
    if (b_E.current) {
	// easy case
	return b_E.current;
    } else if (b_E.currentAppIds) {
	// multi ids for this app, hopefully only one id is allowed for this user...
	// this is useful for apps appearing with different titles based on user affiliation
	var validApps = getValidAppIds();
	var currentAppIds = intersect(b_E.currentAppIds, validApps);
	if (currentAppIds.length > 1) {
	    mylog("multiple appIds (" + currentAppIds + ") for this user, choosing first");
	}
	return currentAppIds[0];
    } else {
	return undefined;
    }
}

function computeHelp(currentAppId) {
    var app = DATA.apps[currentAppId];
    if (app && app.hashelp) {
	var href = "http://esup-data.univ-paris1.fr/esup/aide/canal/" + currentAppId + ".html";
	var onclick = "window.open('','form_help','toolbar=no,location=no,directories=no,status=no,menubar=no,resizable=yes,scrollbars=yes,copyhistory=no,alwaysRaised,width=600,height=400')";
	var a = "<a href='"+  href + "' onclick=\"" + onclick + "\" target='form_help' title=\"Voir l'aide du canal\"><span>Aide</span></a>";
	return "<div class='bandeau_ENT_Help'>" + a + "</div>";
    } else {
	return '';
    }
}

function computeTitlebar(currentAppId) {
    var app = DATA.apps[currentAppId];
    if (app && app.title && !b_E.no_titlebar)
	return "<div class='bandeau_ENT_Titlebar'><a href='" + app.url + "'>" + escapeQuotes(app.title) + "</a></div>";
    else
	return '';
}
function bandeau_div_id() {
    return b_E.div_id || (b_E.div_is_uid && DATA.person.id) || 'bandeau_ENT';
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

function loadSpecificCss() {
    if (window['cssToLoadIfInsideIframe']) {
	var v = window['cssToLoadIfInsideIframe'];
	if (typeof v === "string")
	    loadCSS(v);
    }
}

function unloadSpecificCss() {
    if (window['cssToLoadIfInsideIframe']) {
	var v = window['cssToLoadIfInsideIframe'];
	if (typeof v === "string")
	    unloadCSS(v);
    }
}

function find_DOM_elt(elt_spec) {
    if (typeof elt_spec === "string") {
	return simpleQuerySelector(elt_spec);
    } else if (typeof elt_spec === "boolean") {
	return elt_spec;
    } else if (elt_spec.selector) {
	return simpleQuerySelector(elt_spec.selector);
    } else if (elt_spec.fn) {
	return elt_spec.fn(simpleQuerySelector);
    } else {
	mylog("ignoring invalid DOM elt spec " + elt_spec);
	return undefined;
    }
}

function logout_DOM_elt() {
    if (b_E.logout)
	return find_DOM_elt(b_E.logout);
}

function isLogged() {
    if (b_E.login)
	return !find_DOM_elt(b_E.login);
    return b_E.is_logged && find_DOM_elt(b_E.is_logged);
}

function simulateClickElt(elt) {
    if (elt.href && elt.getAttribute('href') !== '#')  // for JSF (esup-annuaire2)
	document.location = elt.href;
    else if (elt.tagName === "FORM")
	elt.submit();
    else
	elt.click();
}

function asyncLogout() {
    removeSessionStorageCache();
    if (CONF.cas_impersonate) removeCookie(CONF.cas_impersonate.cookie_name, CONF.cas_impersonate.cookie_domain, '/');
    loadScript(CONF.bandeau_ENT_url + '/logout?callback=window.bandeau_ENT_onAsyncLogout');
    return false;
}
window.bandeau_ENT_onAsyncLogout = function() {
    var elt = logout_DOM_elt();
    if (elt) {
	simulateClickElt(elt);
    } else {
	document.location = CONF.ent_logout_url;
    }
}
function installLogout() {
    var logout_buttons = "#bandeau_ENT_Inner .portalPageBarLogout, #bandeau_ENT_Inner .portalPageBarAccountLogout";
    simpleEach(simpleQuerySelectorAll(logout_buttons),
	       function (elt) { 
		   elt.onclick = asyncLogout;
	       });
}

function _accountLink(text, link_spec) {
    var a = document.createElement("a");
    a.innerHTML = escapeQuotes(text);
    if (link_spec.href) {
	a.setAttribute('href', link_spec.href);
    } else {
	a.setAttribute('href', '#');
	a.onclick = function () { simulateClickElt(find_DOM_elt(link_spec)); };
    }
    return a;
}

function installAccountLinks(currentAppId) {
    var app = DATA.apps[currentAppId];
    var appLinks_li = simpleQuerySelector('.portalPageBarAccountAppLinks');
    if (app && app.title) {
	appLinks_li.innerHTML = escapeQuotes(app.title);
	toggleClass(appLinks_li, 'portalPageBarAccountSeparator');
    }
    simpleEachObject(b_E.account_links, function (text, link_spec) {
	var sub_li = document.createElement("li");
	sub_li.appendChild(_accountLink(text, link_spec));
	insertAfter(appLinks_li, sub_li);
    });
}

function installFooter() {
    var id = 'bandeau_ENT_Footer';
    var elt = document.getElementById(id);
    if (!elt) {
	elt = document.createElement("div");
	elt.setAttribute("id", id);
	document.body.appendChild(elt);
    }
    elt.innerHTML =
	'<a href="https://esup.univ-paris1.fr/contacts">Nous contacter</a>' + '|' +
	'<a href="https://esup.univ-paris1.fr/mentions">Mentions légales</a>';
}

function installBandeau() {
    mylog("installBandeau");

    loadSpecificCss();

    if (typeof CSS != 'undefined') 
	addCSS(CSS.base);
    else
	loadCSS(CONF.bandeau_ENT_url + "/main.css");

    var widthForNiceMenu = 800;
    // testing min-width is not enough: in case of a non-mobile comptabile page, the width will be big.
    // also testing min-device-width will help
    var conditionForNiceMenu = '(min-width: ' + widthForNiceMenu + 'px) and (min-device-width: ' + widthForNiceMenu + 'px)';
    var smallMenu = window.matchMedia ? !window.matchMedia(conditionForNiceMenu).matches : screen.width < widthForNiceMenu;
    if (!smallMenu) {
	// on IE7&IE8, we do want to include the desktop CSS
	// but since media queries fail, we need to give them a simpler media
	var handleMediaQuery = "getElementsByClassName" in document; // not having getElementsByClassName is a good sign of not having media queries... (IE7 and IE8)
	var condition = handleMediaQuery ? conditionForNiceMenu : 'screen';

	if (typeof CSS != 'undefined') 
	    addCSS("@media " + condition + " { \n" + CSS.desktop + "}\n");
	else
	    loadCSS(CONF.bandeau_ENT_url + "/desktop.css", condition);
    }

    var header = computeHeader();
    var menu = computeMenu(currentAppId);
    var help = computeHelp(currentAppId);
    var titlebar = computeTitlebar(currentAppId);
    var clear = "<p style='clear: both; height: 13px; margin: 0'></p>";
    var menu_ = "<div class='bandeau_ENT_Menu_'>" + menu + clear + "</div>";
    var bandeau_html = "\n\n<div id='bandeau_ENT_Inner' class='menuOpen'>" + header + menu_ + titlebar + help + "</div>" + "\n\n";
    onIdOrBody(bandeau_div_id(), function () { 
	set_div_innerHTML(bandeau_div_id(), bandeau_html);

	if (!b_E.showSearch) {
	    var searchElt = document.getElementById("portalPageBarSearch");
	    if (searchElt) searchElt.innerHTML = '';
        }

	var barAccount = document.getElementById('portalPageBarAccount');
	if (barAccount) barAccount.onclick = bandeau_ENT_Account_toggleOpen;

	if (DATA.realUserId || b_E.uid && getImpersonateCookie()) getCanImpersonate();

	onReady(function () {
	    if (b_E.account_links) installAccountLinks(currentAppId);
	    installLogout();
	    if (!b_E.no_footer) installFooter();
	});
	installToggleMenu(smallMenu);

	if (smallMenu && document.body.scrollTop === 0) {
	    var bandeau = document.getElementById(bandeau_div_id());

	    setTimeout(function() { 
		mylog("scrolling to " + bandeau.clientHeight);
		window.scrollTo(0, bandeau.clientHeight); 
	    }, 0);
	}
	if (b_E.quirks && simpleContains(b_E.quirks, 'window-resize'))
	     setTimeout(triggerWindowResize, 0);

	if (b_E.onload) b_E.onload(DATA, PARAMS, CONF);
    });

}

function triggerWindowResize() {
    var evt = document.createEvent('UIEvents');
    evt.initUIEvent('resize', true, false,window,0);
    window.dispatchEvent(evt);
}

function getCanImpersonate() {
    if (!CONF.cas_impersonate) return;
    var uid = DATA.realUserId || DATA.person.id;
    loadScript(CONF.cas_impersonate.check_url + "&callback=window.bandeau_ENT_onCanImpersonate&uid=" + uid);
}
window.bandeau_ENT_onCanImpersonate = function(canImpersonate) {
    if (DATA.realUserId) {	
	disableMenuItemsUserCannotImpersonate(canImpersonate);
    }
    if (b_E.uid) {
	detectImpersonationPbs(canImpersonate);
    }
};

function disableMenuItemsUserCannotImpersonate(canImpersonate) {
    simpleEach(getValidAppIds(), function (id) {
	if (!simpleContains(canImpersonate, id)) {
	    var elt = simpleQuerySelector(".bandeau_ENT_Menu_Entry_" + id);
	    if (elt) toggleClass(elt, "bandeau_ENT_Menu_Entry__Forbidden");
	}
    });
}

function detectImpersonationPbs(canImpersonate) {
    // application tells us what uid is logged, yeepee
    if (DATA.realUserId) {
	// app is impersonated
	if (!getImpersonateCookie()) 
	    impersonationPb("you are still impersonated");
	else if (getImpersonateCookie() !== b_E.uid)
	    impersonationPb("you are still impersonated as " + b_E.uid);
    } else {
	// app is not impersonated
	if (getImpersonateCookie() && currentAppShouldBeImpersonated(canImpersonate))
	    impersonationPb("you are not impersonated");
    }
}
function impersonationPb(err) {
    alert(err + ", please logout and log again.");
    //var barAccount = document.getElementById('portalPageBarAccount');
    //barAccount.innerHTML = 'warning: ' + err;
}
function currentAppShouldBeImpersonated(canImpersonate) {
    var appIds = b_E.current ? [b_E.current] : b_E.currentAppIds;
    return intersect(appIds, canImpersonate).length > 0;
}

function getImpersonateCookie() {
    return CONF.cas_impersonate && getCookie(CONF.cas_impersonate.cookie_name);
}

function mayInstallBandeau() {
    if (b_E.prevHash !== PARAMS.hash) {
	b_E.prevHash = PARAMS.hash;
	installBandeau();
    }
}

function localStorageGet(field) {
    try {
	return localStorage.getItem(b_E.localStorage_prefix + field);
    } catch (err) {
	return null;
    }
}
function localStorageSet(field, value) {
    try {
	localStorage.setItem(b_E.localStorage_prefix + field, value);
    } catch (err) {}
}
function sessionStorageGet(field) {
    try {
	return sessionStorage.getItem(b_E.localStorage_prefix + field);
    } catch (err) {
	return null;
    }
}
function sessionStorageSet(field, value) {
    try {
	sessionStorage.setItem(b_E.localStorage_prefix + field, value);
    } catch (err) {}
}
function setSessionStorageCache() {
    sessionStorageSet(b_E.localStorage_js_text_field, b_E.js_text);
    sessionStorageSet("url", b_E.url);
    sessionStorageSet(currentAppId + ":time", now());

    // for old Prolongation, cleanup our mess
    if (window.localStorage) {
	simpleEachObject(localStorage, function (field) {
	    if (field.match(b_E.localStorage_prefix)) localStorage.removeItem(field);
	});
    }
}
function removeSessionStorageCache() {
    if (window.sessionStorage) {
	mylog("removing cached bandeau from sessionStorage");
	sessionStorageSet(b_E.localStorage_js_text_field, '');
    }
}

function loadBandeauJs(params) {
    if (b_E.uid)
	params.push("uid=" + encodeURIComponent(b_E.uid));
    params.push("app=" + (b_E.currentAppIds ? b_E.currentAppIds : [b_E.current]).join(","));

    var angle = window.orientation || '';
    var res = (angle == 90 || angle == -90) && navigator.userAgent.match(/Android.*Chrome/) ? screen.height + 'x' + screen.width : screen.width + 'x' + screen.height;
    res += ',' + (window.devicePixelRatio || 1).toFixed(2) + ',' + angle;
    
    params.push("res=" + res);
    if (b_E.loadTime) params.push("time=" + b_E.loadTime);
    loadScript(b_E.url + "/js" + (params.length ? "?" + params.join('&') : ''));
}

function detectReload($time) {
    var $prev = sessionStorageGet('detectReload');
    if ($prev && $prev != $time) {
	mylog("reload detected, updating bandeau softly");
	loadBandeauJs(['if_none_match=' + PARAMS.hash]);
    }
    sessionStorageSet('detectReload', $time);
}

function mayUpdate() {
    if (notFromLocalStorage) {
	if (window.sessionStorage) {
	    mylog("caching bandeau in sessionStorage (" + b_E.localStorage_prefix + " " + b_E.localStorage_js_text_field + ")");
	    setSessionStorageCache();
	}
	if (PARAMS.is_old) {
	    mylog("server said bandeau is old, forcing full bandeau update");
	    loadBandeauJs(['noCache=1']);
	}
    } else {
	var age = now() - sessionStorageGet(currentAppId + ":time");
	if (age > CONF.time_before_checking_browser_cache_is_up_to_date) {
	    mylog("cached bandeau is old (" + age + "s), updating it softly");
            sessionStorageSet(currentAppId + ":time", now()); // the new bandeau will update "time", but only if bandeau has changed!
	    loadBandeauJs(['if_none_match=' + PARAMS.hash]);
	} else {
	    // if user used "reload", the cached version of detectReload will change
	    window.bandeau_ENT_detectReload = detectReload;
	    loadScript(CONF.bandeau_ENT_url + "/detectReload");
	}
    }
}

/*var loadTime = now();*/
var b_E = window.bandeau_ENT;
var currentAppId = computeBestCurrentAppId();
var notFromLocalStorage = b_E.notFromLocalStorage;
b_E.notFromLocalStorage = false;

if (!b_E.is_logged)
    b_E.is_logged = b_E.logout;

if (currentAppId === "aleph") {
    delete b_E.logout;
    b_E.is_logged = { fn: function(find) { var e = find("span#meconnecter"); return e && e.innerHTML === "Consulter mon compte"; } };
    b_E.account_links = { "Mon compte lecteur": { fn: function(find) { return find('#compte').parentNode } } };
}
if (currentAppId === "domino") {
    window.cssToLoadIfInsideIframe = "https://esup-data.univ-paris1.fr/esup/canal/css/domino.css"; 
}
if (currentAppId === "HyperPlanning-ens") {
    if (DATA.apps[currentAppId] && !DATA.apps[currentAppId].title)
	DATA.apps[currentAppId].title = "Mon emploi du temps";
}

if (!notFromLocalStorage && b_E.url !== sessionStorageGet('url')) {
    mylog("not using bandeau from sessionStorage which was computed for " + sessionStorageGet('url') + " whereas " + b_E.url + " is wanted");
    return "invalid";
} else if (!DATA.person.id) {
    // disabled for now

    if (notFromLocalStorage) {
	unloadSpecificCss();
	onIdOrBody(bandeau_div_id(), function () { 
	    set_div_innerHTML(bandeau_div_id(), '');
	});
	removeSessionStorageCache();
    } else {
	// checking wether we are logged in now
	loadBandeauJs([]);
    }
} else if ((b_E.is_logged || b_E.login) && !isLogged()) {
    onReady(function () {
	    if (isLogged()) mayInstallBandeau();
	    mayUpdate();
    });
} else {
    mayInstallBandeau();
    mayUpdate();
}

// things seem ok, cached js_text can be kept
return "OK";

})();
