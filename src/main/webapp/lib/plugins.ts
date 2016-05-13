pE.plugins = [];

pE.callPlugins = function(event) {
    var v;
    h.simpleEach(pE.plugins, function (plugin) {
        if (plugin[event]) v = plugin[event]();
    });
    return v;
};
