# Sicherheitsupdates produktiv veröffentlicht – 22. Oktober 2026

Die nach der [Klärung der vier Prioritäten](Klaerung-1-bis-4-2026-10-22.md)
freigegebenen Updates wurden am **22.10.2026 zwischen 18:14 und 18:16 MESZ**
veröffentlicht. Die abschließende Betriebsprüfung erfolgte um 18:18 MESZ.
Die [maschinellen Nachweise](Deployment-2026-10-22.json) enthalten die tatsächlich
laufenden Image-IDs, Versionen, Portbindungen und HTTP-Prüfungen.

## Veröffentlichter Stand

| Dienst | Produktiver Stand |
| --- | --- |
| Kassa | FastAPI 0.159.1, Starlette 1.9.0, python-multipart 0.0.32, AnyIO 4.15.1, pip 26.2.2; Template-Aufruf angepasst. |
| CoShift-Backend | Spring Boot 4.1.2, Framework 7.0.10, Security 7.1.2, Tomcat 11.0.27, Jackson 3.1.7 und die bestehende zusätzliche Jackson-2-Abhängigkeit 2.21.7. |
| Java-Laufzeit | Temurin 21.0.13+9 auf Alpine 3.24.3. |
| CoShift-Frontend | Nginx 1.30.6 auf Alpine 3.24.3; bestehende statische Dateien unverändert. |
| Host-Kernel | Weiterhin 6.8.0-147-generic; kein HWE-Wechsel und kein Neustart. |

Die veröffentlichten Images sind genau die vorher isoliert geprüften Kandidaten:

- Backend: `coshift-backend:security-20261022`, Image-ID
  `sha256:c8cd7b276874e96499daa5c58a7cb7ec017c7e0c7a54307be3235cf5e4343433`.
- Frontend: `coshift-frontend:security-20261022`, Image-ID
  `sha256:1ee0cb2543e3b9d364085f986bd76b5cfb8913a0ffb0945930caf4e754f0cee0`.

Die produktive Compose-Datei benennt diese lokalen Tags ausdrücklich mit
`pull_policy: never`. Beide Dienste behalten die vorherigen Loopback-Bindungen
und die Neustartregel `unless-stopped`. Die vorhandenen Builddefinitionen wurden
zusammen mit POM, MVC-Test und Dockerfiles nachgeführt.

**Der Server verwendet weiterhin seinen älteren JSON-Anwendungsstand.** Die
PostgreSQL-/JPA-Migration des lokalen Repositorys wurde nicht veröffentlicht.
Auch der lokale JPA-Stand ist nun auf Boot 4.1.2 aktualisiert und besteht seine
27 Tests ohne den PostgreSQL benötigenden Kontexttest. Die CRLF-Zeilenenden der
betroffenen bestehenden Dateien sind erhalten.

## Ausführung und Prüfungen

Für Kassa wurde eine neue Umgebung unter
`/opt/kassa/.venvs/security-20261022T161152Z` aufgebaut und vorab geprüft.
`/opt/kassa/.venv` zeigt nun auf diese Umgebung. Der systemd-Dienst läuft weiterhin
als `deploy` mit unveränderter Unit; ein SIGTERM an seinen bisherigen Prozess
löste den vorgesehenen automatischen Neustart aus. Die alte Umgebung bleibt
unter `/opt/kassa/.venvs/pre-security-20261022T161152Z` erhalten.

Produktiv bestanden:

- Kassa-Startseite intern und über HTTPS: 200, Sicherheitsheader vorhanden.
- Beide Kassa-Upload-Endpunkte mit synthetischer `.txt`-Datei: 400.
- Kassa mit 1.001 kleinen URL-encoded-Feldern: 400 durch das neue Parserlimit.
  Der Testkörper blieb unter 10 KiB; kein Last- oder Ausfalltest.
- CoShift-Startseite und SPA-Unterseiten: 200, identischer Inhalts-Hash wie vor
  dem Update.
- CoShift-API ohne Anmeldung, intern und öffentlich: 401 mit `no-store`,
  `nosniff` und `DENY`.
- Betroffene CA-Wurzel: fehlt in beiden produktiven OS-Bundles und im
  produktiven Java-Truststore; Vergleich des vollständigen DER-Zertifikats.
- Beide CoShift-Container laufen ohne automatischen Neustart seit dem Wechsel;
  Kassa ist aktiv. Die persönliche Webseite `me.philipplack.de` liefert weiterhin
  200. Keine temporären Testcontainer verbleiben.

Erfolgreiche Basic-Anmeldung, Profil-/Schicht-API und Adminsperre mit einer
synthetischen USER-Identität wurden vorher mit denselben Images isoliert geprüft.
Eine erfolgreiche Anmeldung mit einem echten produktiven Konto wurde mangels
vorliegender Zugangsdaten nicht zusätzlich durchgeführt. Es wurde kein
Testkonto in produktiven Daten angelegt und kein echter Kassenexport verarbeitet.

## Sicherungen und Rückfall

Private Sicherungen und Ausführungsprotokolle liegen unter
`/home/deploy/coshift-security-deploy-20261022T161152Z` (Verzeichniszugriff 0700).
Es gibt Sicherungen der ursprünglichen Containerkonfiguration, Build- und
Compose-Dateien, Kassa-Quellen und -Konfiguration sowie der CoShift-JSON-Daten.
Nach dem Stoppen des alten Backends wurde zusätzlich eine konsistente
`coshift-data-quiescent.tar.gz` erstellt. Der anschließende Vergleich aller
Datendateien ergab **keine Inhaltsänderung** durch die Veröffentlichung.

Die alten Images bleiben mit den Tags
`coshift-backend:rollback-20261022T161152Z` und
`coshift-frontend:rollback-20261022T161152Z` erhalten. Die Deployment-Skripte im
privaten Sicherungsordner enthalten automatische Rückfallpfade; diese wurden
nicht benötigt. Ein späterer Rückfall auf die alten Images erfordert das
Wiederherstellen der gesicherten Compose-/Builddateien und Quellen, erneutes
Erzeugen der beiden Container sowie die Bereitschaftsprüfung. JSON-Daten dabei
nicht blind aus der Sicherung überschreiben: Nach der Veröffentlichung können
bereits neue legitime Buchungen hinzugekommen sein.

Für Kassa besteht der Rückfall aus Wiederherstellen der gesicherten zwei
Quell-/Requirementsdateien, Rücktausch der alten virtuellen Umgebung nach
`/opt/kassa/.venv` und erneutem Dienststart über seine Neustartregel.

## Grenzen und verbleibende Befunde

Die vorangegangenen Scans der nun veröffentlichten Images und des identischen
Kassa-Paketstacks ergaben **keine Treffer** bei den erkannten Paketarten und dem
Datenbankstand **22.10.2026, 13:15 UTC**. Die manuell eingesetzte Java-JRE wurde
separat über Herstellerarchiv, SHA-256 und tatsächliche Laufzeitversion geprüft.
Die Nullzahl ist keine Aussage über unbekannte Lücken oder über die separat
inventarisierten npm-/Browserabhängigkeiten.

Die [Kernel-Triage](Kernel-Triage-2026-10-22.csv), die noch ausstehenden
npm-Aktualisierungen und die bestehende Frage der vollständigen serverseitigen
Methodenautorisierung bleiben bestehen. Der Host wurde entsprechend der
vorherigen Bewertung nicht auf einen anderen Kernel umgestellt.
