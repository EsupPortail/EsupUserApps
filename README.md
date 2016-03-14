ProlongationENT
===========

Web widget prolonging uportal/ENT into apps

Configuration
-------------

* create ```src/main/webapp/WEB-INF/config.json``` similar to ```config-example.json```
* deploy using ```mvn package``` (or modify ```build.properties``` and use ```ant deploy```)
* test the result with ```https://ent.univ.fr/ProlongationENT/test/```

### intégration dans une application

When it works, simply add the following to applications:

```html
<script> window.prolongation_ENT_args = { current: 'xxx' } </script>
<script src="https://ent.univ.fr/ProlongationENT/loader.js"></script>
```

You can do it using apache:

```apache
RequestHeader unset  Accept-Encoding
AddOutputFilterByType SUBSTITUTE text/html
Substitute "s|<body>|<body> <script>window.prolongation_ENT_args = { current: 'xxx' }; </script><script src='https://ent.univ.fr/ProlongationENT/loader.js'></script>|"
```

### ```window.prolongation_ENT_args``` options

* current, currentAppIds
* no_titlebar
* hide_menu

* logout: used to find the logout button. bandeau's logout will trigger a click on app's logout button
* login
* is_logged

* ping_to_increase_session_timeout
* quirks

NB: for the full list, see ```interface prolongation_ENT_args``` in ```lib/defs.d.ts```

### reverse proxy caching

It is recommanded to use apache (or nginx or ...) to cache loader.js since it is a single point of failure.
In apache, using:

```apache
CacheEnable disk /ProlongationENT
```

will ensure the latest cached version when the servlet is down.

Themes
-------------------

Themes are composed of CSS, images, HTML templates (cf ```webapp/theme-simple```).
If you need javascript to animate HTML templates, you can also write ```webapp/lib/theme-xxx.ts```.

If you write a new theme, or need help writing it, please contact Pascal Rigaux or esup-utilisateurs@esup-portail.org (french list).

Technical details
-------------------

### all urls

#### ```/loader.js```

Combines configuration, CSS, templates and javascript code.
If a cached layout is found in ```sessionStorage```, it will use it.
Otherwise it loads it (layout_url).

A neat feature is "background" update: the sessionStorage layout is used, but if it is old, an updated version is requested from server and the bandeau is updated if there is a change.

#### ```/purgeCache```

The configuration files and ```/loader.js``` are computed on startup (or webapp reload). If you modify a file, call ```/purgeCache``` to take changes into account.

#### ```/layout```

In JSONP mode, it tries to authenticate the user and then computes its "layout". Configure ```config-apps.json``` first!

#### ```/detectReload```

Helper url to detect if the user wants to reload the page.

When detected, code in ```lib/main.ts``` will trigger an update of the bandeau (it will first display the sessionStorage version, but will asap try to get updated version)

#### ```/redirect```

Redirects to an application using its code (aka fname). Not really useful anymore...

#### ```/logout```

#### ```/canImpersonate```

To be used as the ```CAN_IMPERSONATE_URL``` in https://github.com/prigaux/impersonate-cas-apache-wrapper.

### javascript code

All the javascript code is in ```src/main/web/lib/*.ts```. It really is javascript code, though it is also typechecked using typescript.
