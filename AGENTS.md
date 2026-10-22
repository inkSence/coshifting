# Hinweise für die Arbeit an CoShift

## Projekt und Geltungsbereich

Diese Datei gilt für das gesamte Repository. Kommunikation und neue
Arbeitsdokumentation standardmäßig auf Deutsch verfassen. Vorhandene englische
Bezeichner und den Stil der bearbeiteten Dateien beibehalten.

CoShift verwaltet die Arbeitsschichten eines genossenschaftlichen Supermarkts.
Mitglieder können Schichten ansehen und buchen sowie ihr Zeitkonto einsehen;
Admins verwalten Personen und Schichten. Das Repository enthält Backend,
Frontend, Docker-Konfiguration und Demo-Datenbankinitialisierung. Es ist vom
separaten Projekt für die persönliche Website unabhängig.

Produktiv läuft weiterhin der ältere JSON-Serverstand. Die
[Sicherheitsveröffentlichung vom 22.10.2026](CoShift/doc/security/Deployment-2026-10-22.md)
aktualisierte dessen Bibliotheken und Laufzeitimages; sie veröffentlichte die
lokale PostgreSQL-/JPA-Migration nicht. Diese Ausgangsstände bei späteren
Deployments getrennt behandeln.

## Orientierung und Architektur

| Pfad | Zweck |
| --- | --- |
| `CoShift/pom.xml` | Backend: Java 21, Spring Boot 4.1.2, Spring Security, JPA, PostgreSQL und Gson. |
| `CoShift/mvnw` | Maven-Wrapper; konfiguriert für Maven 3.9.9. |
| `CoShift/src/main/java/org/coshift/a_domain/` | Domänenobjekte für Schichten, Personen und Zeitkonten. |
| `CoShift/src/main/java/org/coshift/b_application/` | Use Cases, Ports und `UseCaseInteractor` als Einstieg für Adapter. |
| `CoShift/src/main/java/org/coshift/c_adapters/` | REST-Controller, DTOs, Mapper, Security, Präsentation und Repository-Adapter. |
| `CoShift/src/main/java/org/coshift/d_frameworks/` | Spring-Verdrahtung, JPA-Entities und Gson-Dateizugriffe. |
| `CoShift/src/main/resources/` | Standardkonfiguration und Profil `local`. |
| `CoShift/src/test/java/` | JUnit-/Mockito-Tests, MVC-Tests und Spring-Kontexttest. |
| `CoShift/frontend/` | Eigenständiges npm-Projekt: React 19, TypeScript 5.8, Vite 6 und ESLint 9. |
| `CoShift/frontend/src/app/` | React-Einstieg, Provider und Routing. |
| `CoShift/frontend/src/features/auth/` | Anmeldung und Authentifizierungskontext. |
| `CoShift/frontend/src/pages/` | Wochen-/Tagesansicht, Personen- und Schichtverwaltung, API-Hook. |
| `CoShift/frontend/src/layout/` | Gemeinsames Layout und Admin-Routenschutz. |
| `CoShift/data/` | Versionierte JSON-Dateien für Schichten, Zeitkonten und ältere Personendaten. |
| `db/init/` | SQL-Schema und Seed-Daten für die Demo-Datenbank. |
| `Dockerfile.backend` | Maven-Build, anschließend Java-21-JRE als Laufzeit. |
| `Dockerfile.frontend` | Node/Vite-Build, anschließend statische Auslieferung mit Nginx. |
| `CoShift/docker/nginx/default.conf` | SPA-Fallback und Weiterleitung von `/api/` an das Backend. |
| `docker-compose.yml` | Lokale Containerkonfiguration mit PostgreSQL, Backend und Frontend. |
| `docker-compose.demo.yml` | Demo-Konfiguration mit separatem PostgreSQL-Volume und SQL-Initialisierung. |

Die vier Backend-Schichten folgen Clean/Hexagonal Architecture. Fachlogik gehört
in Domäne und Use Cases; Spring, HTTP, JPA und Gson bleiben in den äußeren
Schichten. Vorhandene Ports und Mapper nutzen, statt Framework-Typen in den Kern
einzuführen. Controller übersetzen HTTP in Anwendungsaufrufe.

## Persistenz und Daten

- Personen werden standardmäßig über `PersonJpaRepository` (`@Primary`) in
  PostgreSQL gespeichert. Der ältere `PersonJsonRepository` trägt das Profil
  `json`; dieses Profil allein deaktiviert die JPA-Konfiguration nicht.
- Schichten verwenden weiterhin `data/shifts25/shifts.json`, Zeitkonten
  `data/persons/timeAccounts.json`. Diese Pfade sind relativ zum Arbeitsverzeichnis
  des Backend-Prozesses. Das Backend daher lokal aus `CoShift/` starten.
- `ShiftJpaEntity` ist derzeit keine aktive JPA-Entity. Historische Kommentare und
  Notizen nicht als Beleg für eine abgeschlossene Datenmigration behandeln.
- Personen-IDs und Zeitkonto-IDs verbinden Datenbank und JSON-Dateien. Bei
  Änderungen an Persistenz oder Demo-Daten deren Konsistenz berücksichtigen.
- `application.yml` verwendet PostgreSQL auf `localhost:5432`, Datenbank
  `coshifting`, mit `ddl-auto: validate`. Das Schema muss bereits existieren.
  `application-local.yml` setzt `ddl-auto: update`; nur bewusst gegen eine
  geeignete Entwicklungsdatenbank aktivieren.
- Versionskontrollierte Daten und Zugangsdaten nicht für Tests umschreiben oder
  in Ausgaben kopieren. Tests für Dateizugriffe sollen temporäre Dateien verwenden;
  `TimeAccountJsonRepositoryTest` zeigt ein Beispiel mit `@TempDir`.

## Frontend und API

- Material UI/Emotion liefern die Oberfläche, TanStack Query verwaltet
  Serverdaten, Day.js verarbeitet Datumswerte. Vorhandene Hooks und Komponenten
  erweitern und relevante Queries nach Mutationen invalidieren.
- Routen: `/` für die Wochenansicht, `/day/:date` für einen Tag sowie `/admin`
  und `/configure-shifts` für Adminansichten. Ohne Authentifizierung erscheint
  das Loginformular.
- `useApi` in `src/pages/api.ts` ergänzt den Authorization-Header und behandelt
  Antworten ohne Inhalt (`204`). Authentifizierung erfolgt derzeit über HTTP Basic;
  der Header wird im `sessionStorage` gehalten. Passwörter und Header nicht loggen.
- Die Aufrufe verwenden aktuell relative `/api/...`-Pfade. Vite leitet diese
  lokal an `http://localhost:8080` weiter; im Frontend-Container übernimmt Nginx
  die Weiterleitung an `backend:8080` und erhält das `/api/`-Präfix.
- `VITE_API_URL` ist in Konfigurationen vorhanden, wird im aktuellen
  Frontend-Quellcode aber nicht ausgewertet. Eine Änderung dieser Variable allein
  ändert die tatsächlichen API-Aufrufe daher nicht.
- Admin-Routenschutz im Browser ersetzt keine serverseitige Autorisierung.
  Bei Berechtigungsänderungen `SpringConfig`, Controller und Security-Tests
  gemeinsam prüfen. `@PreAuthorize` ist vorhanden, eine Aktivierung über
  `@EnableMethodSecurity` ist im analysierten Quellstand nicht vorhanden.

## Lokal arbeiten

Für das Backend Java 21 verwenden. Der Wrapper benötigt beim ersten Aufruf
gegebenenfalls Netzwerkzugriff; alternativ ist ein installiertes Maven nutzbar.
Frontend-Befehle ausschließlich im npm-Projekt `CoShift/frontend/` ausführen.
Die zusätzliche Lockdatei direkt unter `CoShift/` ist kein Frontend-Einstieg.

Backend nach Bereitstellung einer passenden Entwicklungsdatenbank:

```bash
cd CoShift
./mvnw spring-boot:run
```

Backend-Port: `8080`. Für bewusst gewählte automatische Schemaanpassung an einer
Entwicklungsdatenbank: `./mvnw spring-boot:run -Dspring-boot.run.profiles=local`.

Frontend in einem zweiten Terminal, vom Repository-Wurzelordner aus:

```bash
cd CoShift/frontend
npm ci
npm run dev
```

Vite verwendet normalerweise Port `5173`. `npm ci` nutzt die vorhandene Lockdatei;
Abhängigkeitsupdates nur im Rahmen der jeweiligen Aufgabe durchführen. Bei
Node-Änderungen eine unterstützte, gepatchte Version und die `engines` der
Abhängigkeiten prüfen. `CoShift/frontend/.nvmrc` und die Buildstufe im
Frontend-Dockerfile verwenden Node 22.23.4; bei Updates beide Stellen sowie die
Versionsangabe in `README.md` gemeinsam pflegen.

## Prüfungen

Passend zur Änderung prüfen und Ergebnisse einschließlich Einschränkungen nennen:

- Backend, aus `CoShift/`: `./mvnw test`. Der enthaltene
  `CoShiftApplicationTests` lädt den Spring-Kontext und benötigt eine erreichbare
  PostgreSQL-Datenbank mit passendem Schema; kein isoliertes Testprofil ist
  eingerichtet. Gegen eine getrennte Testdatenbank ausführen.
- Einzelne Tests lassen sich gezielt ausführen, zum Beispiel
  `./mvnw -Dtest=ShiftTest,ConfigureShiftUseCaseTest,PersonJpaMapperTest test`.
  Neue Fachlogik möglichst ohne Spring-Kontext testen; HTTP-/Security-Verhalten
  über geeignete MVC-Tests prüfen. Unter Boot 4 `MockitoBean` und
  `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest` verwenden;
  die MVC-Tests benötigen `spring-boot-starter-webmvc-test`.
- Frontend, aus `CoShift/frontend/`: `npm run lint` und `npm run build`.
  Der Build führt `tsc -b` und Vite aus. Es gibt derzeit kein npm-Testskript oder
  konfiguriertes Frontend-Testframework.
- Bei UI-Änderungen betroffene Ansichten einschließlich Anmeldung, Rollen,
  Schichtbuchung und Zeitkonto prüfen, soweit relevant und mit Testdaten möglich.
- Für reine Dokumentationsänderungen genügen Inhalts-, Pfad- und Diff-Prüfungen;
  Anwendung oder Datenbank müssen dafür nicht gestartet werden.
- Bestehende Fehler von neu verursachten Fehlern unterscheiden. Keine
  unbeteiligten Reparaturen oder umfangreichen Neuformatierungen durchführen.

## Docker und Demo

Docker-Builds verwenden den Repository-Wurzelordner als Kontext. Der Backend-Build
überspringt Tests mit `-DskipTests`; ein erfolgreiches Image ersetzt daher keinen
Testlauf. Node wird nur beim Frontend-Build gebraucht; das fertige Image enthält
Nginx und die erzeugten Dateien. Die Laufzeitbasis ist auf Nginx 1.30.6
und einen geprüften Digest gepinnt. Das Backend verwendet Temurin 21.0.13+9
(AMD64): Solange das offizielle Docker-Tag nachläuft, wird das offizielle
JRE-Archiv mit festem SHA-256 im Dockerfile eingesetzt. Bei Updates Version,
Downloadsumme und Basisdigest gemeinsam prüfen.

Vor Start oder Änderung von Compose diese Besonderheiten berücksichtigen:

- Die Standarddatei veröffentlicht PostgreSQL auf `5432`, Backend auf `8080`
  und Frontend auf `8081`. Für das Backend fehlt dort eine Überschreibung der
  JDBC-Adresse: `localhost` im Container bezeichnet nicht den Dienst `db`.
- Die Demo setzt die JDBC-Adresse auf `db:5432`, veröffentlicht PostgreSQL auf
  `5433` und verwendet `demo_pgdata` sowie `db/init/`. Initialisierungsskripte
  laufen nur bei einer frischen PostgreSQL-Datenablage.
- Beide Compose-Dateien binden **denselben** Ordner `./CoShift/data` nach
  `/app/data` ein. Die Demo isoliert damit nur die Datenbank, nicht die JSON-Daten.
  Für schreibende Versuche auch die JSON-Daten ausdrücklich isolieren.
- Beide Varianten verwenden Backend-Port `8080` und Frontend-Port `8081` und
  können so nicht gleichzeitig unverändert laufen. `depends_on` garantiert
  außerdem keine Bereitschaft der Datenbank.
- Keine Daten-Volumes löschen oder Produktionsdaten für Tests verändern.
  Deployment-Konfigurationen auf einem Server können vom Repository abweichen;
  eine lokale Codeänderung allein ist kein Veröffentlichungsauftrag.

## Arbeitsweise und Dokumentation

Vor Änderungen den Git-Zustand prüfen, vorhandene Nutzeränderungen erhalten und
nur aufgabenbezogene Dateien committen. Generierte Verzeichnisse wie `target/`,
`node_modules/` und `dist/` nicht versionieren. Bestehende Zeilenenden und
Formatierung in bearbeiteten Dateien möglichst erhalten.

`README.md`, `CoShift/doc/` und `CoShift/.junie/guidelines.md` liefern Kontext,
enthalten aber auch Entwürfe oder ältere Angaben. Für das tatsächliche Verhalten
Quellcode, Builddateien und aktuelle Konfigurationen prüfen. Verhaltensänderungen
mit der zugehörigen Dokumentation abstimmen. Bekannte Altstände nur innerhalb
eines passenden Auftrags ändern.
