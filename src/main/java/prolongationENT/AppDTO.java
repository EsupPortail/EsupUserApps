package prolongationENT;

import static prolongationENT.Utils.firstNonNull;

class AppDTO {
    String fname;
    String text;
    String title;
    String description;
    String url;
    Boolean hashelp;
        
    public AppDTO(String fname, App app, String url) {
        this.fname = fname;
        text = app.text;
        title = app.title;
        description = firstNonNull(app.description, "");
        this.url = url;
        if (app.hashelp) hashelp = app.hashelp;
    }
}
