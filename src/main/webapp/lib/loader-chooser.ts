(function () {
    var loader_js_url = "%s";
    var jsHash = "%s";
    var cookieName = "%s";

    function getCookie(name) {
        var m = document.cookie.match(new RegExp(name + '=([^;]+)'));
        return m && m[1];
    }
    var theme = getCookie(cookieName);
    document.write("<script src='" + loader_js_url + "?" + (theme ? "theme=" + theme : "v=" + jsHash) + "'></script>");
})();
