package prolongationENT;

import java.util.Collection;

import static prolongationENT.Utils.firstNonNull;

class Export {
    static class App {
        String fname;
        String text;
        String shortText;
        String title;
        String description;
        String url;
        Collection<String> tags;
        Boolean hashelp;
        
        public App(String fname, prolongationENT.App app, String url) {
            this.fname = fname;
            text = app.text;
            shortText = app.shortText;
            title = app.title;
            description = firstNonNull(app.description, "");
            this.url = url;
            if (app.hashelp) hashelp = app.hashelp;
            if (app.tags != null) tags = app.tags;
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
