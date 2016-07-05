(function () {
    function convert(layout) {
        var folders = h.simpleMap(layout.navigation.tabs, function (tab) {
            var portlets = [];
            function convertContent(l) {
                h.simpleEach(l, function (e) {
                    if (e.fname) portlets.push(e);
                    if (e.content) convertContent(e.content);
                });
            }
            convertContent(tab.content);
            return { title: tab.name, portlets: portlets };
        });
        return { folders: folders };
    }
    
    var plugin = {
        main: function() {
            if (pE.DATA.layout && pE.DATA.layout.navigation) {
                pE.DATA.layout = convert(pE.DATA.layout);
            }
        }
    };
    pE.plugins.push(plugin);   
})();
