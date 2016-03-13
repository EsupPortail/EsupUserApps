(function () {

function installAccountLinks(app) {
    var appLinks_li = h.simpleQuerySelector('.portalPageBarAccountAppLinks');
    if (app && app.title) {
	appLinks_li['innerHTML'] = h.escapeQuotes(app.title);
	h.toggleClass(appLinks_li, 'portalPageBarAccountSeparator');
    }
    h.simpleEachObject(b_E.account_links, function (text, link_spec) {
	var sub_li = document.createElement("li");
	sub_li.appendChild(pE.accountLink(text, link_spec));
	h.insertAfter(appLinks_li, sub_li);
    });
}

    if (b_E.account_links) {
        h.onReady(function() {
	    installAccountLinks(pE.currentApp);
        });
    }

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
	    if (window.localStorage) pE.localStorageSet("menuClosed", b ? "true" : "false");

	    return false;
	}

	function installToggleMenu(hide) {
	    var hideByDefault = b_E.hide_menu;
	    var toggleMenu = document.getElementById('bandeau_ENT_portalPageBarToggleMenu');
	    if (toggleMenu) {
		toggleMenu.onclick = bandeau_ENT_Menu_toggleAndStore;
		var savedState = window.localStorage && pE.localStorageGet("menuClosed");
		if (hide || savedState === "true" || savedState !== "false" && hideByDefault)
		    bandeau_ENT_Menu_toggle();
	    }
	}

  var plugin = { 
		post_header_add: function() {
			var barAccount = document.getElementById('portalPageBarAccount');
			if (barAccount) barAccount.onclick = bandeau_ENT_Account_toggleOpen;
			installToggleMenu(pE.width_xs);
		},
	};
	pE.plugins.push(plugin);
})();
