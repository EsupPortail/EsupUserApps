# Découverte de EsupUserApps et la ProlongationENT #

Prérequis
---------

* Git, Java, Maven
* serveur LDAP
* serveur CAS


Découverte de EsupUserApps
----------

```sh
git clone https://github.com/EsupPortail/EsupUserApps
cd EsupUserApps/src/main/webapp/WEB-INF/
cp config-example.json config.json
cp config-auth-example.json config-auth.json 
cp config-apps-example.json config-apps.json 
```
Modifiez `config.json`, par exemple :
```json
     "cas_base_url": "https://cas.univ-paris1.fr/cas",
     "EsupUserApps_url": "http://localhost:8080",
```
Modifiez `config-auth.json`, par exemple :
```json
      "url": "ldap://ldap-test",
      "bindDN": "", "bindPasswd": "",
      "peopleDN": "ou=people,dc=univ-paris1,dc=fr",
```

Ensuite lancez la webapp :
```sh
cd ../../../..
mvn jetty:run
```

Testez http://localhost:8080/layout

- si vous avez l'erreur `{ error: "Unauthorized" }`, autentifiez vous sur votre CAS, et recommencez.
- conseil, utilisez l'extension [JSONView](https://jsonview.com/) (disponible pour Firefox & Chromium) pour voir le JSON avec mise en forme.


Découverte de ProlongationENT
-----------------------------

```sh
git clone https://github.com/EsupPortail/ProlongationENT
cd ProlongationENT/src/main/webapp/WEB-INF/
cp config-example.json config.json
```
Modifiez `config.json`, par exemple :
```js
     //"layout_url": "https://ent.univ.fr/layout.jsp",
     "esupUserApps_url": "http://localhost:8080",
```
Ensuite lancez la webapp :
```sh
cd ../../../..
mvn jetty:run -Djetty.port=8081
```

Testez http://localhost:8081/test/


Ajouter la ProlongationENT sur une application
----------------------------------------------

Juste après le `<body>` d'une application, ajoutez :
```html
  <script> window.prolongation_ENT_args = { current: "monAppli" }; </script>
  <script src="http://localhost:8081/loader.js"></script>
```

Testez votre application, vous devriez voir le bandeau en haut.

Ensuite :
- ajoutez votre application dans `EsupUserApps/src/main/webapp/WEB-INF/config-apps.json`, en utilisant le même identifiant `monAppli`. 
- redémarrez la servlet EsupUserApps
- vérifiez que l'application est visible dans le bandeau (si vous avez les droits de voir cette application)

:tada: plutôt que de redémarrer la servlet EsupUserApps, faites une requête sur http://localhost:8080/purgeCache . Cela fonctionne pour toutes les modifications dans les fichiers `config*.json`

Découverte de ProlongationENT-home
----------------------------------

ProlongationENT-home est une application purement html et javascript.

Nous pouvons par exemple mettre ces fichiers statiques dans le répertoire `webapp` de la ProlongationENT :

```sh
cd ProlongationENT/src/main/webapp/
git clone https://github.com/EsupPortail/ProlongationENT-home
```
Modifiez `ProlongationENT-home/index.html` :
```html
     <script src="http://localhost:8081/loader.js"></script>
```

Testez http://localhost:8081/ProlongationENT-home/


Découverte de EsupUserApps admin
--------------------------------

La modification de `config-apps.json` se fait actuellement via un éditeur texte. Nous travaillons sur un éditeur web. Il existe par contre une page simple permettant de voir cette configuration avec une mise en forme plus lisible.

Modifiez `EsupUserApps/src/main/webapp/admin/config-apps.html` :
```html
<script src="http://localhost:8081/loader.js"></script>
```

Puis testez http://localhost:8080/admin/config-apps.html .
