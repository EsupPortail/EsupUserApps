EsupUserApps implements a web-service ``canImpersonate`` to be used with [impersonate-cas-apache-wrapper](https://github.com/prigaux/impersonate-cas-apache-wrapper).

It also adds a few helpers:

impersonate.html
----------------

This page:
* sets ``CAS_IMPERSONATE`` domain cookie
* lists the applications the user is allowed to impersonate
  * with links to ``https://app/EsupUserApps/redirect?id=appId&relog&impersonate`` to help relog on applications to switch user.

https://app/EsupUserApps/redirect?relog&impersonate
--------------------------

This page:
* will try to remove application session cookies
  * by default it will try to remove ``JSESSIONID``, ``PHPSESSID`` and ``_shibsession_``* with ``Path=/``
  * you can customize for each application in config-apps.json, for example: ``"cookies": { "path": "/idp", "names": [ "_idp_session" ] }``
* will set ``CAS_IMPERSONATED`` local cookie to ``CAS_IMPERSONATE`` value
* redirect to the application

This relies on proxying https://app/EsupUserApps/redirect urls to EsupUserApps. Example for Apache:
```apache
ProxyPass /EsupUserApps https://ent.univ-ville.fr/EsupUserApps
```

ProlongationENT
---------------

The application will display ProlongationENT.

If the user is allowed to impersonate ``app``, ProlongationENT will check if global cookie ``CAS_IMPERSONATE`` and local cookie ``CAS_IMPERSONATED`` are the same.

If they are different, ProlongationENT will
* warn the user the application session is not impersonated,
* propose to relog with url ``https://app/EsupUserApps/redirect?id=appId&relog&impersonate``