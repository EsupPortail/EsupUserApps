(function () {
    
 
function computeHeader() {
    var login = pE.personAttr('supannAliasLogin') || pE.DATA.user;
    return h.template(pE.TEMPLATES.header, {
        logout_url: pE.CONF.ent_logout_url,
        userName: pE.personAttr("displayName") || pE.personAttr("mail") || pE.DATA.user,
        userDetails: pE.personAttr("displayName") ? pE.personAttr("mail") + " (" + login + ")" : login,
    });
}

function relogUrl(app) {
    return app.url.replace(/^(https?:\/\/[^\/]*).*/, "$1") + "/ProlongationENT/redirect?relog&impersonate&id=" + app.fname;
}
function computeLink(app) {
    // for uportal4 layout compatibility:
    if (!app.url.match(/^http/)) app.url = pE.CONF.uportal_base_url + app.url.replace(/\/detached\//, "/max/");
    
    var url = app.url;
    var classes = '';
    if (pE.DATA.canImpersonate) {
        url = relogUrl(app);
        if (!h.simpleContains(pE.DATA.canImpersonate, app.fname)) {
            classes = "class='bandeau_ENT_Menu_Entry__Forbidden'";
        }
    }
    var a = "<a title='" + h.escapeQuotes(app.description) + "' href='" + url + "'>" + h.escapeQuotes(app.text || app.title) + "</a>";
    return "<li " + classes + ">" + a + "</li>";
}

function computeMenu(currentApp) {
    var li_list = h.simpleMap(pE.DATA.layout.folders, function (tab) {
        if (tab.title === "__hidden__") return '';
        var sub_li_list = h.simpleMap(tab.portlets, function(app) {
            return computeLink(app);
        });
        
        var className = h.simpleContains(tab.portlets, currentApp) ? "activeTab" : "inactiveTab";
        return "<li class='" + className + "' onclick=''><span>" + h.escapeQuotes(tab.title) + "</span><ul>" + sub_li_list.join("\n") + "</ul></li>";
    });
    
    var toggleMenuSpacer = "<div class='toggleMenuSpacer'></div>\n";
    
    return "<ul class='bandeau_ENT_Menu'>\n" + toggleMenuSpacer + li_list.join("\n") + "\n</ul>";
}

function computeTitlebar(app) {
    if (app && app.title && !args.no_titlebar)
        return "<div class='bandeau_ENT_Titlebar'><a href='" + app.url + "'>" + h.escapeQuotes(app.title) + "</a></div>";
    else
        return '';
}

function computeHelp(app) {
    if (app && app.hashelp) {
        var href = "https://ent.univ-paris1.fr/assets/aide/canal/" + app.fname + ".html";
        var onclick = "window.open('','form_help','toolbar=no,location=no,directories=no,status=no,menubar=no,resizable=yes,scrollbars=yes,copyhistory=no,alwaysRaised,width=600,height=400')";
        var a = "<a href='"+  href + "' onclick=\"" + onclick + "\" target='form_help' title=\"Voir l'aide du canal\"><span>Aide</span></a>";
        return "<div class='bandeau_ENT_Help'>" + a + "</div>";
    } else {
        return '';
    }
}

function mayLoadDesktopCCSS() {
    var widthForNiceMenu = 800;
    // testing min-width is not enough: in case of a non-mobile comptabile page, the width will be big.
    // also testing min-device-width will help
    var conditionForNiceMenu = '(min-width: ' + widthForNiceMenu + 'px) and (min-device-width: ' + widthForNiceMenu + 'px)';
    var width_xs = window.matchMedia ? !window.matchMedia(conditionForNiceMenu).matches : screen.width < widthForNiceMenu;
    if (!width_xs) {
        // on IE7&IE8, we do want to include the desktop CSS
        // but since media queries fail, we need to give them a simpler media
        var handleMediaQuery = "getElementsByClassName" in document; // not having getElementsByClassName is a good sign of not having media queries... (IE7 and IE8)
        var condition = handleMediaQuery ? conditionForNiceMenu : 'screen';
        
        if (pE.CSS) 
            h.addCSS("@media " + condition + " { \n" + pE.CSS.desktop + "}\n");
        else
            h.loadCSS(pE.CONF.prolongationENT_url + "/" + pE.CONF.theme + "/desktop.css", condition);
    }
}
    
    function computeFullHeader() {
        mayLoadDesktopCCSS();
        
        var header = computeHeader();
        var menu = computeMenu(pE.currentApp);
        var help = computeHelp(pE.currentApp);
        var titlebar = computeTitlebar(pE.currentApp);
        var clear = "<p class='bandeau_ENT_Menu_Clear'></p>";
        var menu_ = "<div class='bandeau_ENT_Menu_'>" + menu + clear + "</div>";
        return "\n\n<div id='bandeau_ENT_Inner' class='menuOpen'>" + header + menu_ + titlebar + help + "</div>" + "\n\n";
    }
    
    var plugin = {
        computeHeader: computeFullHeader,
        logout_buttons: function () { return "#bandeau_ENT_Inner .portalPageBarLogout, #bandeau_ENT_Inner .portalPageBarAccountLogout"; },
    };

    pE.plugins.push(plugin);
    
})();
