# Node-Sicherheitsupdate vom 22. Oktober 2026

## Anlass und Ergebnis

Die [Node.js-Sicherheitsmeldung vom 21. Oktober 2026](https://nodejs.org/en/blog/vulnerability/october-2026-security-releases)
nennt unter anderem Fehler in Brotli, DNS und TLS-Prüfungen. Für die bereits
installierte Hauptversion 22 ist
[Node 22.23.4](https://nodejs.org/en/blog/release/v22.23.4) das zugehörige
Sicherheitsrelease.

Auf dem CoShift-Server wurde die NVM-Installation des Benutzers `deploy` von
22.18.0 auf **22.23.4** aktualisiert und der NVM-Standard auf diese konkrete
Version gesetzt. npm ist dort jetzt in Version **10.9.9** vorhanden. Die alte
Installation und ihr Downloadcache wurden nach erfolgreicher Prüfung entfernt.
Globale Zusatzwerkzeuge mussten nicht migriert werden: Zuvor waren ausschließlich
die mitgelieferten Werkzeuge npm und Corepack installiert.

## Änderungen am Projekt

- `Dockerfile.frontend`: Buildbasis von `node:20-alpine` auf
  `node:22.23.4-alpine` umgestellt, lokal und unter `/opt/coshift` auf dem Server.
- `CoShift/frontend/.nvmrc`: dieselbe Version für die Entwicklung eingetragen,
  ebenfalls lokal und auf dem Server.
- `README.md` und `AGENTS.md`: Versionsangaben und den Ablauf mit NVM und
  `npm ci` abgestimmt.
- npm-Abhängigkeiten, Lockdatei und Anwendungsquellcode wurden nicht verändert.

Im Frontend-Verzeichnis kann NVM die Version so bereitstellen und auswählen:

```bash
nvm install
nvm use
node --version
npm ci
npm run lint
npm run build
```

Für einen Docker-Build vom Repository-Wurzelordner aus:

```bash
docker build --pull -f Dockerfile.frontend -t coshift-frontend:node-security-check .
```

`--pull` lädt die aktuelle Basis für die festgelegte Version. Bei zukünftigen
Sicherheitsupdates Dockerfile, `.nvmrc` und Dokumentation gemeinsam anpassen.

## Durchführung und Prüfungen

Vor der Änderung wurden Dockerfile, NVM-Aliase und die Kennungen, Images und
Startzeitpunkte der produktiven CoShift-Container im privaten Serverordner
`/home/deploy/node-security-20261022-3gvbtn6x` gesichert. Dort liegen auch die
Build- und Lintprotokolle. Der serverseitige Git-Stand ist älter als der lokale
und enthält eigene Änderungen; diese wurden erhalten.

Erfolgreich geprüft:

- NVM-Installation mit SHA-256-Prüfsummenprüfung des Downloads von `nodejs.org`.
- Node-/npm-Version und NVM-Standard in einer neuen SSH-Verbindung mit geladener
  NVM-Umgebung; zusätzlicher Brotli-Komprimierungs-/Dekomprimierungs-Smoke-Test.
- Keine festen Verweise auf 22.18.0 in den geprüften lesbaren Systemdienst- und
  Cron-Konfigurationen; kein Benutzer-Crontab für `deploy` vorhanden.
- Isolierte Docker-Produktionsbuilds aus dem Server-Frontend und dem lokalen
  Frontend. Beide führten `npm ci` und `tsc -b && vite build` erfolgreich aus und
  erzeugten dasselbe Image:
  `sha256:ccde0cf42c8479000783940e05d91f42bf30d417ff6a3f207dceb8fa98426f98`.
- `npm run lint` erfolgreich in der isolierten Node-22.23.4-Buildstufe.
- Nginx-Konfiguration erfolgreich geprüft; im fertigen Auslieferungscontainer
  ist kein Node-Befehl vorhanden.
- HTTP-Prüfung von `/`, `/day/2026-10-22`, `/admin`, `/configure-shifts` und
  JavaScript-Asset am separaten Testport `127.0.0.1:18081` erfolgreich.
- Chromium: Loginformular, Wochen-/Tagesansicht, Personenverwaltung,
  Schichtverwaltung und Weiterleitung eines normalen Benutzers von `/admin`
  geprüft; keine unbehandelten JavaScript-Ausnahmen beobachtet. Die geschützten
  Ansichten verwendeten vollständig simulierte API-Antworten. Das prüft die
  erzeugte Oberfläche, nicht Anmeldung oder Berechtigungen am echten Backend.
- Die Startseiten von `me.philipplack.de`, `coshift.philipplack.de` und
  `kassa.philipplack.de` antworteten anschließend mit HTTP 200. Kennungen, Images
  und Startzeitpunkte beider produktiver CoShift-Container blieben unverändert.

Der Testcontainer hatte keine Verbindung zum produktiven Backend und keine
Nutzerdaten eingebunden. Testcontainer und temporäres Builder-Image wurden nach
Abschluss entfernt. Die beiden gesonderten Auslieferungsimage-Tags
`coshift-frontend:node-security-20261022-local` und
`coshift-frontend:node-security-20261022-server` bleiben zur Nachvollziehbarkeit
erhalten. Es erfolgte keine Veröffentlichung und kein Neustart der Anwendungen.

## Grenzen und bestehende Beobachtungen

Der Build meldet weiterhin einen JavaScript-Chunk über 500 kB. Beim direkten
Laden einer Adminroute wurde außerdem eine Weiterleitung zur Wochenansicht
beobachtet, bevor die Benutzerrolle vollständig geladen war. Die Adminansichten
wurden deshalb über Navigation nach abgeschlossener Initialisierung geprüft.
Anwendungsquellcode und dieses bestehende Verhalten waren nicht Teil des Updates.

Ein vollständiger Funktionstest gegen das echte Backend wurde ohne Testzugang
nicht durchgeführt. Auf dem lokalen Entwicklungsrechner wurde zunächst Node
24.16.0 vorgefunden. Anschließend wurden auf ausdrücklichen Auftrag im
Frontend-Verzeichnis auch lokal `nvm install` und `nvm use` ausgeführt:
Node 22.23.4 und npm 10.9.9 wurden einschließlich Download-Prüfsumme bestätigt.
Die Auswahl gilt für die jeweilige Shell; der globale NVM-Standard und die
bestehende Installation von 24.16.0 wurden dabei nicht geändert. In einer neuen
Frontend-Shell die Projektversion mit `nvm use` auswählen.
