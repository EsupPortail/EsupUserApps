pE.main = function (DATA, PARAMS, fromLocalStorage) {

var CONF = pE.CONF;
pE.PARAMS = PARAMS;
    

function bandeau_ENT_Account_toggleOpen() {
    h.toggleClass(document.getElementById('portalPageBarAccount'), 'open');
    var isOpen = h.toggleClass(document.getElementById('portalPageBarAccountInner'), 'open');

    if (isOpen) {
	var close = function() {
	    h.removeClass(document.getElementById('portalPageBarAccount'), 'open');
	    h.removeClass(document.getElementById('portalPageBarAccountInner'), 'open');
	    document.removeEventListener("click", close, false);
	};
	setTimeout(function () { document.addEventListener("click", close, false); }, 0);
    }
    return false;
}

function bandeau_ENT_Menu_toggle() {
    if (b_E.quirks && h.simpleContains(b_E.quirks, 'global-menuClosed-class'))
	h.toggleClass(document.body, 'bandeau_ENT_menuClosed');
    return h.toggleClass(document.getElementById('bandeau_ENT_Inner'), 'menuClosed');
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

function personAttr(attrName) {
    var v = DATA.userAttrs && DATA.userAttrs[attrName];
    return v && v[0];
}
 
function computeHeader() {
    var login = personAttr('supannAliasLogin') || DATA.user;
    return h.template(pE.TEMPLATES.header, {
        logout_url: CONF.ent_logout_url,
        userName: personAttr("displayName") || personAttr("mail") || DATA.user,
        userDetails: personAttr("displayName") ? personAttr("mail") + " (" + login + ")" : login,
    });
}

function relogUrl(app) {
    return app.url.replace(/^(https?:\/\/[^\/]*).*/, "$1") + "/ProlongationENT/redirect?relog&impersonate&id=" + app.fname;
}
function computeLink(app) {
    // for uportal4 layout compatibility:
    if (!app.url.match(/^http/)) app.url = CONF.ent_base_url + app.url.replace(/\/detached\//, "/max/");
    
    var url = app.url;
    var classes = '';
    if (DATA.canImpersonate) {
	url = relogUrl(app);
    	if (!h.simpleContains(DATA.canImpersonate, app.fname)) {
	    classes = "class='bandeau_ENT_Menu_Entry__Forbidden'";
	}
    }
    var a = "<a title='" + h.escapeQuotes(app.description) + "' href='" + url + "'>" + h.escapeQuotes(app.text || app.title) + "</a>";
    return "<li " + classes + ">" + a + "</li>";
}

function computeMenu(currentApp) {
    var li_list = h.simpleMap(DATA.layout.folders, function (tab) {
	var sub_li_list = h.simpleMap(tab.portlets, function(app) {
	    return computeLink(app);
	});
    
	var className = h.simpleContains(tab.portlets, currentApp) ? "activeTab" : "inactiveTab";
	return "<li class='" + className + "' onclick=''><span>" + h.escapeQuotes(tab.title) + "</span><ul>" + sub_li_list.join("\n") + "</ul></li>";
    });

    var toggleMenuSpacer = "<div class='toggleMenuSpacer'></div>\n";

    return "<ul class='bandeau_ENT_Menu'>\n" + toggleMenuSpacer + li_list.join("\n") + "\n</ul>";
}

function getValidApps() {
    var l = [];
    h.simpleEach(DATA.layout.folders, function (tab) {
	l.push.apply(l, tab.portlets);
    });
    return l;
}

function computeBestCurrentAppId() {
  var ids = b_E.current ? [b_E.current] : b_E.currentAppIds;
  if (!ids) return;
	// multi ids for this app, hopefully only one id is allowed for this user...
	// this is useful for apps appearing with different titles based on user affiliation
	var validApps = getValidApps();
	var currentApps = h.simpleFilter(validApps, function (app) { 
    return h.simpleContains(ids, app.fname);
  });
	if (currentApps.length > 1) {
	    h.mylog("multiple appIds (" + currentApps + ") for this user, choosing first");
	}
	return currentApps[0];
}

function computeHelp(app) {
    if (app && app.hashelp) {
	var href = "http://esup-data.univ-paris1.fr/esup/aide/canal/" + app.fname + ".html";
	var onclick = "window.open('','form_help','toolbar=no,location=no,directories=no,status=no,menubar=no,resizable=yes,scrollbars=yes,copyhistory=no,alwaysRaised,width=600,height=400')";
	var a = "<a href='"+  href + "' onclick=\"" + onclick + "\" target='form_help' title=\"Voir l'aide du canal\"><span>Aide</span></a>";
	return "<div class='bandeau_ENT_Help'>" + a + "</div>";
    } else {
	return '';
    }
}

function computeTitlebar(app) {
    if (app && app.title && !b_E.no_titlebar)
	return "<div class='bandeau_ENT_Titlebar'><a href='" + app.url + "'>" + h.escapeQuotes(app.title) + "</a></div>";
    else
	return '';
}
function bandeau_div_id() {
    return b_E.div_id || (b_E.div_is_uid && DATA.user) || 'bandeau_ENT';
}

function loadSpecificCss() {
    if (window.cssToLoadIfInsideIframe) {
	var v = window.cssToLoadIfInsideIframe;
	if (typeof v === "string")
	    h.loadCSS(v, null);
    }
}

function unloadSpecificCss() {
    if (window.cssToLoadIfInsideIframe) {
	var v = window.cssToLoadIfInsideIframe;
	if (typeof v === "string")
	    h.unloadCSS(v);
    }
}

function find_DOM_elt(elt_spec) {
    if (typeof elt_spec === "string") {
	return h.simpleQuerySelector(elt_spec);
    } else if (typeof elt_spec === "boolean") {
	return elt_spec;
    } else if (elt_spec.selector) {
	return h.simpleQuerySelector(elt_spec.selector);
    } else if (elt_spec.fn) {
	return elt_spec.fn(h.simpleQuerySelector);
    } else {
	h.mylog("ignoring invalid DOM elt spec " + elt_spec);
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
	document.location.href = elt.href;
    else if (elt.tagName === "FORM")
	elt.submit();
    else
	elt.click();
}

function asyncLogout() {
    removeSessionStorageCache();
    if (CONF.cas_impersonate) h.removeCookie(CONF.cas_impersonate.cookie_name, CONF.cas_impersonate.cookie_domain, '/');
    h.loadScript(CONF.bandeau_ENT_url + '/logout?callback=' + 'window.prolongation_ENT.onAsyncLogout');
    return false;
}
pE.onAsyncLogout = function() {
    var elt = logout_DOM_elt();
    if (elt) {
	simulateClickElt(elt);
    } else {
	document.location.href = CONF.ent_logout_url;
    }
};
function installLogout() {
    var logout_buttons = "#bandeau_ENT_Inner .portalPageBarLogout, #bandeau_ENT_Inner .portalPageBarAccountLogout";
    h.simpleEach(h.simpleQuerySelectorAll(logout_buttons),
	       function (elt) { 
		   elt['onclick'] = asyncLogout;
	       });
}

function _accountLink(text, link_spec) {
    var a = document.createElement("a");
    a.innerHTML = h.escapeQuotes(text);
    if (link_spec.href) {
	a.setAttribute('href', link_spec.href);
    } else {
	a.setAttribute('href', '#');
	a.onclick = function () { simulateClickElt(find_DOM_elt(link_spec)); };
    }
    return a;
}

function installAccountLinks(app) {
    var appLinks_li = h.simpleQuerySelector('.portalPageBarAccountAppLinks');
    if (app && app.title) {
	appLinks_li['innerHTML'] = h.escapeQuotes(app.title);
	h.toggleClass(appLinks_li, 'portalPageBarAccountSeparator');
    }
    h.simpleEachObject(b_E.account_links, function (text, link_spec) {
	var sub_li = document.createElement("li");
	sub_li.appendChild(_accountLink(text, link_spec));
	h.insertAfter(appLinks_li, sub_li);
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
    elt.innerHTML = pE.TEMPLATES.footer;
}

var currentApp;

function installBandeau() {
    h.mylog("installBandeau");

    loadSpecificCss();

    if (pE.CSS) 
	h.addCSS(pE.CSS.base);
    else
	h.loadCSS(CONF.bandeau_ENT_url + "/" + CONF.theme + "/main.css", null);

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

	if (pE.CSS) 
	    h.addCSS("@media " + condition + " { \n" + pE.CSS.desktop + "}\n");
	else
	    h.loadCSS(CONF.bandeau_ENT_url + "/" + CONF.theme + "/desktop.css", condition);
    }

    var header = computeHeader();
    var menu = computeMenu(currentApp);
    var help = computeHelp(currentApp);
    var titlebar = computeTitlebar(currentApp);
    var clear = "<p style='clear: both; height: 13px; margin: 0'></p>";
    var menu_ = "<div class='bandeau_ENT_Menu_'>" + menu + clear + "</div>";
    var bandeau_html = "\n\n<div id='bandeau_ENT_Inner' class='menuOpen'>" + header + menu_ + titlebar + help + "</div>" + "\n\n";
    h.onIdOrBody(bandeau_div_id(), function () {
	h.set_div_innerHTML(bandeau_div_id(), bandeau_html);

	if (!b_E.showSearch) {
	    var searchElt = document.getElementById("portalPageBarSearch");
	    if (searchElt) searchElt.innerHTML = '';
        }

	var barAccount = document.getElementById('portalPageBarAccount');
	if (barAccount) barAccount.onclick = bandeau_ENT_Account_toggleOpen;

        if (CONF.cas_impersonate && !b_E.uid) detectImpersonationPbs();

	h.onReady(function () {
	    if (b_E.account_links) installAccountLinks(currentApp);
	    installLogout();
	    if (!b_E.no_footer) installFooter();
	});
	installToggleMenu(smallMenu);

	if (smallMenu && document.body.scrollTop === 0) {
	    var bandeau = document.getElementById(bandeau_div_id());

	    setTimeout(function() { 
		h.mylog("scrolling to " + bandeau.clientHeight);
		window.scrollTo(0, bandeau.clientHeight); 
	    }, 0);
	}
	if (b_E.quirks && h.simpleContains(b_E.quirks, 'window-resize'))
	     setTimeout(triggerWindowResize, 0);

	if (b_E.onload) b_E.onload(DATA, pE.PARAMS, CONF);
    });

}

function triggerWindowResize() {
    var evt = document.createEvent('UIEvents');
    evt.initUIEvent('resize', true, false,window,0);
    window.dispatchEvent(evt);
}

function detectImpersonationPbs() {
    var want = h.getCookie(CONF.cas_impersonate.cookie_name);
    // NB: explicit check with "!=" since we do not want null !== undefined
    if (want != pE.wanted_uid && (pE.wanted_uid || h.simpleContains(DATA.canImpersonate, currentApp.fname))) {
        var msg = "Vous êtes encore identifié sous l'utilisateur " + DATA.user + ". Acceptez vous de perdre la session actuelle ?";
	if (window.confirm(msg)) {
	    document.location.href = relogUrl(currentApp);
	}
    }
}

function localStorageGet(field) {
    try {
	return localStorage.getItem(pE.localStorage_prefix + field);
    } catch (err) {
	return null;
    }
}
function localStorageSet(field, value) {
    try {
	localStorage.setItem(pE.localStorage_prefix + field, value);
    } catch (err) {}
}
function sessionStorageGet(field) {
    try {
	return sessionStorage.getItem(pE.localStorage_prefix + field);
    } catch (err) {
	return null;
    }
}
function sessionStorageSet(field, value) {
    try {
	sessionStorage.setItem(pE.localStorage_prefix + field, value);
    } catch (err) {}
}
function setSessionStorageCache(js_text) {
    sessionStorageSet(pE.localStorage_js_text_field, js_text);
    sessionStorageSet("url", b_E.url);
    sessionStorageSet(currentApp.fname + ":time", h.now());

    // for old Prolongation, cleanup our mess
    if (window.localStorage) {
	h.simpleEachObject(localStorage, function (field) {
	    if (field.match(pE.localStorage_prefix)) localStorage.removeItem(field);
	});
    }
}
function removeSessionStorageCache() {
    if (window.sessionStorage) {
	h.mylog("removing cached bandeau from sessionStorage");
	sessionStorageSet(pE.localStorage_js_text_field, '');
    }
}

function detectReload($time) {
    var $prev = sessionStorageGet('detectReload');
    if ($prev && $prev != $time) {
	h.mylog("reload detected, updating bandeau softly");
	loadBandeauJs([]);
    }
    sessionStorageSet('detectReload', $time);
}

function mayUpdate() {
    if (!fromLocalStorage) {
	if (window.sessionStorage) {
	    h.mylog("caching bandeau in sessionStorage (" + pE.localStorage_prefix + " " + pE.localStorage_js_text_field + ")");
            var js_text =
                "window.prolongation_ENT.main(\n" + JSON.stringify(DATA) + ",\n\n" + JSON.stringify(pE.PARAMS) + "\n\n, true);\n";
	    setSessionStorageCache(js_text);
	}
	if (PARAMS && PARAMS.is_old) {
	    h.mylog("server said bandeau is old, forcing full bandeau update");
	    loadBandeauJs(['noCache=1']);
	}
    } else {
	var age = h.now() - sessionStorageGet(currentApp.fname + ":time");
	if (age > CONF.time_before_checking_browser_cache_is_up_to_date) {
	    h.mylog("cached bandeau is old (" + age + "s), updating it softly");
            sessionStorageSet(currentApp.fname + ":time", h.now()); // the new bandeau will update "time", but only if bandeau has changed!
	    loadBandeauJs([]);
	} else {
	    // if user used "reload", the cached version of detectReload will change
	    pE.detectReload = detectReload;
	    b_E['detectReload'] = detectReload; // needed for migration
	    h.loadScript(CONF.bandeau_ENT_url + "/detectReload");
	}
    }
}

currentApp = computeBestCurrentAppId() || {};

if (!b_E.is_logged)
    b_E.is_logged = b_E.logout;

if (currentApp.fname === "aleph") {
    delete b_E.logout;
    b_E.is_logged = { fn: function(find) { var e = find("span#meconnecter"); return e && e.innerHTML === "Consulter mon compte"; } };
    b_E.account_links = { "Mon compte lecteur": { fn: function(find) { return find('#compte').parentNode } } };
}
if (currentApp.fname === "domino") {
    window.cssToLoadIfInsideIframe = "https://esup-data.univ-paris1.fr/esup/canal/css/domino.css"; 
}
if (currentApp.fname === "HyperPlanning-ens") {
    if (currentApp.title)
	currentApp.title = "Mon emploi du temps";
}

if (fromLocalStorage && b_E.url !== sessionStorageGet('url')) {
    h.mylog("not using bandeau from sessionStorage which was computed for " + sessionStorageGet('url') + " whereas " + b_E.url + " is wanted");
    return "invalid";
} else if ((b_E.is_logged || b_E.login) && !isLogged()) {
    h.onReady(function () {
	    if (isLogged()) installBandeau();
	    mayUpdate();
    });
} else {
    installBandeau();
    mayUpdate();
}

// things seem ok, cached js_text can be kept
return "OK";

};
