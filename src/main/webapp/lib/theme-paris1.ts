(function () {

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
        if (args.quirks && h.simpleContains(args.quirks, 'global-menuClosed-class'))
            h.toggleClass(document.body, 'bandeau_ENT_menuClosed');
        return h.toggleClass(document.getElementById('bandeau_ENT_Inner'), 'menuClosed');
    }

    function bandeau_ENT_Menu_toggleAndStore() {
        var b = bandeau_ENT_Menu_toggle();
        if (window.localStorage) pE.localStorageSet("menuClosed", b ? "true" : "false");

        return false;
    }

    function installToggleMenu(hide) {
        var hideByDefault = args.hide_menu;
        var toggleMenu = document.getElementById('bandeau_ENT_portalPageBarToggleMenu');
        if (toggleMenu) {
            toggleMenu.onclick = bandeau_ENT_Menu_toggleAndStore;
            var savedState = window.localStorage && pE.localStorageGet("menuClosed");
            if (hide || savedState === "true" || savedState !== "false" && hideByDefault)
                bandeau_ENT_Menu_toggle();
        }
    }

    function mayHideAccountLink() {
        if (pE.validApps["CCompte-pers"] || pE.validApps["CCompte-etu"]) return;
        
        var elt = h.simpleQuerySelector('.portalPageBarAccountAnchor');
        if (elt) elt.remove();
    }
    
    var plugin = {
        
        post_compute_currentApp: function () {      
            if (window['cssToLoadIfInsideIframe']) {
                // migrate to new syntax
                args.extra_css = window['cssToLoadIfInsideIframe'];
            }

            if (pE.currentApp.fname === "aleph") {
                delete args.logout;
                args.is_logged = { fn: function(find) { var e = find("span#meconnecter"); return e && e.innerHTML === "Consulter mon compte"; } };
            }
            if (pE.currentApp.fname === "domino") {
                args.extra_css = "https://ent.univ-paris1.fr/assets/canal/css/domino.css"; 
            }
            if (pE.currentApp.fname === "HyperPlanning-ens") {
                if (pE.currentApp.title)
                    pE.currentApp.title = "Mon emploi du temps";
            }
        }, 
        
        post_header_add: function() {
            var barAccount = document.getElementById('portalPageBarAccount');
            if (barAccount) barAccount.onclick = bandeau_ENT_Account_toggleOpen;
            installToggleMenu(pE.width_xs);
            mayHideAccountLink();
        },
    };

    pE.plugins.push(plugin);
    
})();
