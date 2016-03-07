package prolongationENT;

class AppDTO {
    String text;
    String title;
    String description;
    String url;
    Boolean hashelp;
	
    public AppDTO(App app, String url) {
	    text = app.text != null ? app.text : app.title;
	    title = app.title;
	    description = app.description != null ? app.description : "";
    	this.url = url;
    	if (app.hashelp) hashelp = app.hashelp;
	}
}
