(function () {
    
    var menus = ['pE-buttons', 'pE-account'];
    function toggleMenu(id, id2) {
        var isOpen = h.toggleClass(document.getElementById(id), 'pE-menu-is-open');
        if (id2) h.toggleClass(document.getElementById(id2), 'pE-menu-is-open');

        // close other menus
        h.simpleEach(menus, function (otherId) {
            if (id !== otherId) h.removeClass(document.getElementById(otherId), 'pE-menu-is-open');
        });
        
        if (isOpen) {
            var close = function() {
                h.removeClass(document.getElementById(id), 'pE-menu-is-open');
                if (id2) h.removeClass(document.getElementById(id2), 'pE-menu-is-open');
                document.removeEventListener("click", close, false);
            };
            setTimeout(function () { document.addEventListener("click", close, false); }, 0);
        }
        return false;
    }

    function moreButtons_toggleMenu() {
        return toggleMenu('pE-buttons', 'pE-openMoreButtons');
    }

    function account_toggleMenu() {
        return toggleMenu('pE-account', 'pE-photo');
    }
    
    function personAttr(attrName) {
        var v = pE.DATA.userAttrs && pE.DATA.userAttrs[attrName];
        return v && v[0];
    }
    
    function themeUrl() {
        return pE.CONF.prolongationENT_url + "/" + pE.CONF.theme;
    }

    function relogUrl(app) {
        return app.url.replace(/^(https?:\/\/[^\/]*).*/, "$1") + "/ProlongationENT/redirect?relog&impersonate&id=" + app.fname;
    }
    
    function computeLink(app) {
        // for uportal4 layout compatibility:
        if (!app.url.match(/^http/)) app.url = pE.CONF.uportal_base_url + app.url.replace(/\/detached\//, "/max/");
        
        var url = app.url;
        var classes = ['pE-button'];
        if (pE.DATA.canImpersonate) {
            url = relogUrl(app);
            if (!h.simpleContains(pE.DATA.canImpersonate, app.fname)) {
                classes.push('pE-button-forbidden');
            }
        }
        var a = "<a title='" + h.escapeQuotes(app.description) + "' href='" + url + "' data-fname='" + app.fname + "'>" +
            "<img src='" + themeUrl() + "/icon/" + simplifyFname(app.fname) + ".svg'><br>" +
          h.escapeQuotes(app.shortText || app.text || app.title) + "</a>";
        return "<div class='" + classes.join(' ') + "'>" + a + "</div>";
    }

    // in our Agimus, we simplify the fnames, we must handle this
    function simplifyFname(k) {
        k = k.replace(/^C([A-Z])/, "$1").replace(/-(etu|ens|gest|pers|teacher|default)$/, '');
        return k.toLowerCase();
    }
    
    function computeMenu(currentApp) {
        // we must normalize
        var validApps = {};
        h.simpleEachObject(pE.validApps, function (k, app) { 
            validApps[simplifyFname(k)] = app;
        });
        var list = [];
        h.simpleEach(pE.DATA.topApps, function (fname) {
            fname = simplifyFname(fname);
            var app = validApps[fname];
            delete validApps[fname];
            if (app) list.push(computeLink(app));
        });
        return list;
    }
 
    function computeHeader() {
        var app = pE.currentApp;
        var appLinks = computeMenu(app);
        var topApps = appLinks.slice(0, pE.DATA.canImpersonate ? 99 : 12).join("<!--\n-->");

        var html_elt = document.getElementsByTagName("html")[0];
        if (!args.no_footer) h.toggleClass(html_elt, 'pE-sticky-footer');

        // NB: to simplify, do not use browser cache for the photo if impersonated
        var photo_version = !pE.DATA.realUserId && personAttr('modifyTimestamp');

        return h.template(pE.TEMPLATES.header, {
            appTitle: app.url ? "<a href='" + app.url + "'><span class='pE-title-app-short'>" + h.escapeQuotes(app.shortText || app.text || app.title) + "</span><span class='pE-title-app-long'>" + h.escapeQuotes(app.title) + "</span></a>" : "Application non autorisée",
            topApps: topApps,
            photoUrl: "https://userphoto-test.univ-paris1.fr/?uid=" + pE.DATA.user + (photo_version ? "&v=" + photo_version : ''),
            themeUrl: themeUrl(),
            logout_url: pE.CONF.ent_logout_url,
            userDetails: personAttr("displayName") || personAttr("mail"),
            accountAnchorClass: pE.validApps["CCompte-pers"] || pE.validApps["CCompte-etu"] ? '' : 'pE-hide',
            pagePersoClass: pE.validApps["page-perso"] ? '' : 'pE-hide',
        });
    }

    function computeHelp(app) {
        if (app && app.hashelp) {
            var href = "http://esup-data.univ-paris1.fr/esup/aide/canal/" + app.fname + ".html";
            var onclick = "window.open('','form_help','toolbar=no,location=no,directories=no,status=no,menubar=no,resizable=yes,scrollbars=yes,copyhistory=no,alwaysRaised,width=600,height=400')";
            var inner = "<span>Aide " + (app.shortText || app.text || app.title) + "</span> <img src='" + themeUrl() + "/help.svg'>";
            return "<a href='" + href + "' onclick=\"" + onclick + "\" target='form_help' title=\"Voir l'aide du canal\">" + inner + "</a>";
        } else {
            return '';
        }
    }
    
    function computeFooter() {
        var app = pE.currentApp;
        return h.template(pE.TEMPLATES.footer, {
            themeUrl: themeUrl(),
            helpUrl: (pE.validApps['gun-etu'] || { url: "https://ent.univ-paris1.fr/gun-pers-flipbook" }).url,
            appHelp: computeHelp(app),
        });
    }

    function server_log(params) {
        var l = [];
        h.simpleEachObject(params, function (k, v) {
            l.push(k + "=" + encodeURIComponent(v));
        });
        h.loadScript(pE.CONF.prolongationENT_url + "/log?" + l.join("&"));
    }

    function log_button_click(event) {
        var container = this;
        function eltFname(elt) {
            return elt && elt.getAttribute('data-fname');
        }
        var fname = eltFname(h.eltClosest(event.target, "[data-fname]"));
        if (fname) {
            var index = 1 + h.simpleMap(container.querySelectorAll("[data-fname]"), eltFname).indexOf(fname);
            server_log({ user: pE.DATA.user, app: fname, index: index });
        }
    }
    
    var plugin = {
        computeHeader: computeHeader,
        computeFooter: computeFooter,
        logout_buttons: function () { return ".pE-accountLogout" },
        
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
                args.extra_css = "https://esup-data.univ-paris1.fr/esup/canal/css/domino.css"; 
            }
            if (pE.currentApp.fname === "HyperPlanning-ens") {
                if (pE.currentApp.title)
                    pE.currentApp.title = "Mon emploi du temps";
            }
        }, 
        
        post_header_add: function() {
            var account = document.getElementById('pE-photo');
            if (account) account.onclick = account_toggleMenu;

            var open_menu = document.getElementById('pE-openMoreButtons');
            if (open_menu) open_menu.onclick = moreButtons_toggleMenu;

            var buttons = document.getElementById('pE-buttons');
            if (buttons) buttons.onmousedown = log_button_click;

            h.simpleEach(h.simpleQuerySelectorAll('#pE-header .pE-button img'), function (elt) {
                elt['onerror'] = function () {
                    var src = this.src.replace(/[^\/]*\.png/, "default.png");
                    if (src !== this.src) this.src = src;
                    console.log(this, this.src);
                };
            });
        },        
    };

    pE.plugins.push(plugin);
    
})();
