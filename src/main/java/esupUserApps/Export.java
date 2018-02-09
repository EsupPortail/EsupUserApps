package esupUserApps;

import java.util.Collection;

import static esupUserApps.Utils.firstNonNull;

class Export {
    static class App {
        String fname;
        String text;
        String shortText;
        String title;
        String description;
        String url;
        Integer position;
        Collection<String> tags;
        Boolean hashelp;
        Boolean hide;
        
        public App(String fname, esupUserApps.App app, String url) {
            this.fname = fname;
            text = app.text;
            shortText = app.shortText;
            title = app.title;
            description = firstNonNull(app.description, "");
            this.url = url;
            if (app.hashelp) hashelp = app.hashelp;
            if (app.hide) hide = app.hide;
            if (app.tags != null) tags = app.tags;
            if (app.position != null) position = app.position;
        }
    }
    
    static class Layout {
        public Layout(String title, Collection<App> portlets) {
            this.title = title;
            this.portlets = portlets;
        }
        String title;
        Collection<App> portlets;
    }
    
}
