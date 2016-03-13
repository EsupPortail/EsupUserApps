pE.plugins = [];

pE.callPlugins = function(event) {
  h.simpleEach(pE.plugins, function (plugin) {
      if (plugin[event]) plugin[event]();
  });
};
