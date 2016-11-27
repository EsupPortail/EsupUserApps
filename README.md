ProlongationENT
===========


Shibboleth
-------------------

For shibbolethized applications, the simpler is to integrate CAS-isified ProlongationENT.
Your users will get ProlongationENT, whereas other users will have nothing.

You can also use shibbolethized ProlongationENT:
* configure ```config-shibboleth.json```, create a new random key for ```proxyKeys```
* in the apache of your application, do:

```apache
SSLProxyEngine on
<Location /ProlongationENT>
  ProxyPass https://ent.univ.fr/ProlongationENT
</Location>
<Location /ProlongationENT/layout> 
  AuthType shibboleth
  ShibRequireSession Off
  require shibboleth
  ShibUseHeaders On
  RequestHeader set ProlongationENT-Proxy-Key XXXrandomXXX
</Location>
```

* in the application:

```html
<script> window.prolongation_ENT_args = { current: 'xxx', layout_url: '/ProlongationENT/layout' } </script>
<script src="https://ent.univ.fr/ProlongationENT/loader.js"></script>
```



Technical details
-------------------

### all urls

#### ```/purgeCache```

The configuration files and ```/loader.js``` are computed on startup (or webapp reload). If you modify a file, call ```/purgeCache``` to take changes into account (when debugging, you can use config option ```disableServerCache```)

#### ```/layout```

It tries to authenticate the user (using CAS) and then computes its "layout". Configure ```config-apps.json``` first!

It is mostly compatible with uPortal's ```/layout.json```

#### ```/detectReload```

Helper url to detect if the user wants to reload the page.

When detected, code in ```lib/main.ts``` will trigger an update of the bandeau (it will first display the sessionStorage version, but will asap try to get updated version)

#### ```/redirect```

Redirects to an application using its code (aka fname). Not really useful anymore...

#### ```/logout```

#### ```/canImpersonate```

To be used as the ```CAN_IMPERSONATE_URL``` in https://github.com/prigaux/impersonate-cas-apache-wrapper.
