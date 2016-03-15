package prolongationENT;

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
        description = app.description != null ? app.description : "";
        this.url = url;
        if (app.hashelp) hashelp = app.hashelp;
    }
}
