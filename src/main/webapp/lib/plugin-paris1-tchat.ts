(function () {
    function debounce(func, wait) {
        var timeout;
        return function() {
            clearTimeout(timeout);
            timeout = setTimeout(function() {
                timeout = null;
                func();
            }, wait);
        };
    }
    
    function sessionStorageGet(field) {
        try {
            return sessionStorage.getItem("ProlongationENT-tchat:" + field);
        } catch (err) {
            return null;
        }
    }
    function sessionStorageSet(field, value) {
        try {
            sessionStorage.setItem("ProlongationENT-tchat:" + field, value);
        } catch (err) {}
    }
    
    function tchat() {
        var url = (pE.validApps["tchat-iframe"] || {}).url;
        if (!url || h.getCookie("pE-tchat") !== "yes") return '';

        function toggleTchat() {
            try {
                h.toggleClass(document.getElementById("pE-tchat"), "big-tchat");
            } catch (err) {}
        }

        var maximized = sessionStorageGet("maximized");
        if (maximized || maximized !== '' && pE.currentApp.fname === 'tchat') {
            setTimeout(toggleTchat);
            url += '#maximized';
        }

        window.addEventListener("message", function (event) {
            var data = event.data || '';
            if (data.match(/ProlongationENT:tchat:(maximize|minimize)/)) {
                toggleTchat();
                sessionStorageSet("maximized", data.match(/maximize/) || '');
            }
        }, false);

        var _elt;
        function elt() {
            return _elt || (_elt = document.getElementById("pE-tchat"));
        }
        var is_fixed = window.innerHeight < document.body.scrollHeight;

        function check(e) {
            if (!elt()) return;
            if (window.innerHeight + window.scrollY >= document.body.scrollHeight) {
                h.removeClass(elt(), 'fixed-tchat');
                is_fixed = false;
            } else if (!is_fixed) {
                elt().className += " fixed-tchat";
                is_fixed = true;
            }
        }
        var check_ = debounce(check, 20);
        setTimeout(function () {
            window.addEventListener('scroll', check_);
            window.addEventListener('resize', check_);
            if (window.MutationObserver) {
                new MutationObserver(check_).observe(document, { childList: true, subtree: true });
            }
        });
        
        return '<span><iframe id="pE-tchat" class="' + (is_fixed ? 'fixed-tchat' : '') + '" src="' + url + '"></iframe></span>';
    }
    
    var plugin = {
        computeFooterTchat: tchat
    };
    pE.plugins.push(plugin);   
})();
