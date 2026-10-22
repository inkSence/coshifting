# Klärung der vier CVE-Prioritäten – 22. Oktober 2026

**Fortschreibung:** Die hier beschriebenen Kandidaten wurden anschließend
freigegeben und produktiv veröffentlicht. Aktueller Stand und Nachweise:
[Deployment vom 22.10.2026](Deployment-2026-10-22.md).

Die offenen Punkte aus dem [CVE-Bericht](CVE-Report-2026-10-22.md) wurden durch
Quellprüfung, Herstellerabgleich und isolierte Tests konkretisiert. Kassa und
CoShift benötigen Updates. Dafür liegen getestete Kandidaten vor. Die laufenden
Anwendungen wurden nicht aktualisiert, Produktionsdaten nicht verändert und
der Host nicht neu gestartet.

## 1. Kassa: Öffentliche Parserpfade bestätigt

Beide öffentlichen Endpunkte `/api/process` und `/upload` rufen den
URL-encoded-Formularparser **vor** der Validierung der erforderlichen Datei auf.
Die anschließende Antwort `422` verhindert den vorherigen Parseraufwand nicht.
Die Prüfung erfolgte an einer Quellkopie mit gemockten Repositories und Use Cases,
ohne Requests an den produktiven Uploaddienst.

| Prüffall | Installierter Stack | Aktualisierter Kandidat |
| --- | --- | --- |
| URL-encoded ohne Pflichtdatei, beide Endpunkte | Parser aufgerufen, danach 422 | Parser aufgerufen, danach 422 |
| 1.001 URL-encoded-Felder | 422 erst nach dem Parsen | 400 durch Parserlimit |
| URL-encoded-Feld knapp über 1 MiB | 422 erst nach dem Parsen | 400 durch Parserlimit |
| Multipart-Datei mit 102 Part-Headern | 200 im gemockten Test | 400 durch Parserlimit |
| Regulärer CSV-Upload, beide Endpunkte | 200 | 200 |
| Falsche Dateiendung, beide Endpunkte | 400 | 400 |
| Startseite mit Template | 200 | 200 nach Template-Anpassung |

Die Testkörper blieben unter dem Nginx-Limit von 5 MiB. Es wurden weder
CPU-Erschöpfung noch Ausfälle provoziert. Die Bedingungen für
[CVE-2026-42561](https://github.com/Kludex/python-multipart/security/advisories/GHSA-pp6c-gr5w-3c5g),
[CVE-2026-53539](https://github.com/Kludex/python-multipart/security/advisories/GHSA-5rvq-cxj2-64vf)
und [CVE-2026-54283](https://github.com/Kludex/starlette/security/advisories/GHSA-82w8-qh3p-5jfq)
sind damit auf dem HTTP-Verarbeitungspfad vorhanden. Das quadratische
Laufzeitverhalten der zweiten CVE wurde nicht zusätzlich vermessen.

Der kompatibel aufgelöste Kandidat verwendet FastAPI **0.159.1**, Starlette
**1.9.0**, python-multipart **0.0.32**, AnyIO **4.15.1**, Uvicorn **0.54.0** und
pip **26.2.2**. `pip check` besteht. Der erneute Trivy-Abgleich der installierten
Pakete einschließlich pip liefert **0 Treffer** beim unten genannten Datenstand.

Der erste Test mit neuen Bibliotheken fand einen echten Kompatibilitätsfehler:
Die alte positionale `TemplateResponse`-Signatur funktioniert nicht mehr.
Mit expliziten Argumenten `request=`, `name=` und `context=` bestehen alle zehn
HTTP-/Parserprüffälle einschließlich der bestehenden Sicherheitsheader.
Die fachliche CSV-Abgleichlogik war dabei gemockt; das ist kein vollständiger
Ende-zu-Ende-Test mit echten Kassenexporten.

Die KEV-Meldung
[CVE-2026-48710](https://github.com/Kludex/starlette/security/advisories/GHSA-86qp-5c8j-p5mr)
setzt sicherheitsrelevante Entscheidungen anhand von `request.url` voraus.
Eine solche Zugriffskontrolle wurde im Kassa-Quellstand nicht gefunden.
Der neue Stack enthält den Fix dennoch. Die besonderen Optionen für die
Dateischreib-Lücke CVE-2026-24486 wurden ebenfalls nicht gefunden. Für AnyIOs
IDNA-TLS-Lücke wurde kein entsprechender ausgehender TLS-Aufruf gefunden.

Vorbereitet: [Paketversionen](prepared-2026-10-22/kassa-requirements.lock),
[Template-Korrektur](prepared-2026-10-22/kassa-template.patch) und
[isolierter Test](prepared-2026-10-22/kassa_parser_probe.py).
Der Test verändert beim Import die Repository-/Use-Case-Funktionen durch Mocks;
ihn ausschließlich gegen eine separate Quellkopie ausführen, wie geschehen:
`KASSA_EXPECT_FIXED=1 <test-venv>/bin/python kassa_parser_probe.py <quellkopie>`.

**Entscheidung:** Kassa zuerst auf den geprüften Paketstack samt Template-Fix
umstellen. Der öffentlich erreichbare Parserpfad macht dies dringlicher als
Versionsmeldungen zu ungenutzten Funktionen.

## 2. CoShift: Zusammenpassendes Boot-4-Update geprüft

Ein Update nur auf die letzte in Maven Central verfügbare Version **3.5.16**
reicht nicht: Deren BOM verwaltet Framework 6.2.19, Security 6.5.11,
Tomcat 10.1.55 und Jackson 2.21.4. Damit bleiben gegenüber den aktuellen
Herstellermeldungen Lücken offen. Diese Versionskombination wurde aus dem
[veröffentlichten BOM](https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-dependencies/3.5.16/spring-boot-dependencies-3.5.16.pom)
gelesen, nicht aus einer alten Projektseite abgeleitet.

Der geprüfte Kandidat **Spring Boot 4.1.2** verwaltet zusammen:

| Komponente | Kandidat |
| --- | --- |
| Spring Framework | 7.0.10 |
| Spring Security | 7.1.2 |
| Tomcat | 11.0.27 |
| Jackson 3 | 3.1.7 |
| Jackson 2 für die explizite bestehende Abhängigkeit | 2.21.7 |

Quelle ist das [Boot-4.1.2-BOM](https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-dependencies/4.1.2/spring-boot-dependencies-4.1.2.pom).
Zusätzlich wurde die [Tomcat-Herstellerliste](https://tomcat.apache.org/security-11.html)
gegen die gewählte 11er-Version geprüft.

Die Produktionsquellen unterscheiden sich vom lokalen JPA-Stand. Daher wurden
**zwei getrennte Quellkopien** getestet:

- Lokaler JPA-Stand: Kompilierung und 27 Tests bestanden. Der PostgreSQL
  benötigende Kontexttest wurde nicht ausgeführt.
- Tatsächlicher älterer JSON-Serverstand: Kompilierung, 23 Tests sowie der
  zusätzliche Spring-Kontexttest bestanden. Ein ausführbares Boot-4-JAR wurde
  gebaut, ohne die lokale JPA-Migration hineinzunehmen.

Erforderliche Testanpassungen: `MockBean` durch `MockitoBean` ersetzen,
`WebMvcTest` aus dem neuen Paket importieren und
`spring-boot-starter-webmvc-test` aufnehmen. Die 53 lokalen beziehungsweise
49 serverseitigen Produktionsquelldateien kompilierten ohne API-Anpassung.
Die lokale MVC-Prüfung kontrolliert zusätzlich Cache- und Content-Type-Header.

[CVE-2026-22732](https://spring.io/security/cve-2026-22732/) ist im alten
Security-Stand versionsseitig bestätigt. Die lesend geprüften produktiven
401-Antworten enthielten die erwarteten Sicherheits- und No-Cache-Header;
ein konkreter Headerausfall wurde dort nicht reproduziert. Das ist kein
Ausschluss der bedingten Lücke. Der Kandidat enthält den Herstellerfix.
Die pauschalen Tomcat- und Jackson-RCE-Behauptungen bleiben unzutreffend:
Tomcat-Realms, AJP, HTTP/2 und polymorphes Jackson-Typing wurden nicht als
entsprechende aktive Anwendungsfunktionen gefunden. JSON-Requestverarbeitung
ist dagegen vorhanden; Parser-/DoS-Fixes bleiben relevant.

Die SBOM des lokalen Boot-4-Kandidaten liefert beim erneuten Scannerabgleich
**0 Bibliothekentreffer**. Dies bewertet bekannte Paketlücken und ersetzt
keine Prüfung der fachlichen Autorisierung. Die bereits dokumentierte fehlende
Aktivierung von Method Security ist eine separate bestehende Baustelle.

Auch der kombinierte Test mit dem neuen JSON-Backend-JAR, Java 21.0.13 und
Nginx 1.30.6 besteht in einem temporären internen Docker-Netz: anonymes API-GET
liefert 401, die synthetische Basic-Anmeldung ermöglicht Schicht- und Profil-GETs
mit 200, ein Aufruf unter `/api/admin/` mit der USER-Rolle liefert 403, die SPA
liefert 200. Alle vier API-Antworten enthalten `no-store`, `nosniff` und `DENY`.
Dabei waren ausschließlich synthetische JSON-Daten schreibgeschützt eingebunden;
das Testnetz hatte keinen ausgehenden Netzzugang. Der Scan des vollständigen
neuen Backend-Images liefert **0 Treffer** bei den erkannten OS- und
Anwendungspaketen. Die Testcontainer und das Testnetz wurden entfernt.

Vorbereitet: [Patch für den lokalen Stand](prepared-2026-10-22/backend-boot4.patch)
und [Patch für den älteren Serverstand](prepared-2026-10-22/backend-server-boot4.patch).
Die beiden Patches gelten für unterschiedliche Ausgangsstände und dürfen nicht
beide auf dieselbe Quellkopie angewendet werden. Zeilenenden wurden für die
Patchdarstellung normalisiert; die echten Anwendungsdateien sind unverändert.

**Entscheidung:** Boot 4.1.2 als gemeinsam getesteten Aktualisierungsstand
verwenden. Die Veröffentlichung des JSON-Serverstands getrennt von der
PostgreSQL-Migration behandeln.

## 3. Laufzeitimages und Java-Truststore

Ein neues Frontend-Image mit Nginx **1.30.6** und Alpine **3.24.3** wurde aus
den bereits ausgelieferten statischen Dateien und der bestehenden
Nginx-Konfiguration gebaut. Damit wurde bei diesem Test ausschließlich die
Laufzeitbasis geändert. `nginx -t` und SPA-Aufrufe für `/`, `/day/2026-10-22`,
`/admin` und `/configure-shifts` bestanden. Der OS-Scan liefert **0 Treffer**;
die separat inventarisierten npm-/Browserabhängigkeiten werden dadurch nicht
aktualisiert. [Nginx-Herstellerreferenz](https://nginx.org/en/security_advisories.html).

Wichtiger Fallstrick beim Backend: Der tatsächlich neu heruntergeladene Tag
`eclipse-temurin:21-jre-alpine` enthielt **21.0.12.1+1** und weiterhin die
betroffene Java-Vertrauenswurzel. Der spezifische Tag
`21.0.13_9-jre-alpine` war noch nicht verfügbar. Die offizielle
[Temurin-Veröffentlichung 21.0.13+9](https://github.com/adoptium/temurin21-binaries/releases/tag/jdk-21.0.13%2B9)
war hingegen bereits vorhanden.

Für den Test wurde deshalb die offizielle Alpine-JRE **21.0.13+9** heruntergeladen,
gegen ihre veröffentlichte SHA-256-Datei geprüft und in ein separates Image auf
der aktuellen Alpine-Basis eingesetzt. Die alte JRE wurde darin vollständig
ersetzt. `java -version` bestätigt 21.0.13+9. Dies ist ein expliziter Kandidat,
kein stilles Überschreiben eines produktiven Images.

Die Zertifikatsprüfung verglich das vollständige DER-Zertifikat, nicht nur
Dateinamen oder Aliasnamen. Die laut
[Ubuntu-Sicherheitsmeldung USN-8944-1](https://ubuntu.com/security/notices/USN-8944-1)
entfernte Wurzel `emSign ECC Root CA - G3` hat den SHA-256-Fingerabdruck
`86a1ecba089c4a8d3bbe2734c612ba341d813e043cf9e8a862cd5c57a36bbe6b`.

| Truststore | Betroffene Wurzel enthalten? |
| --- | --- |
| Ubuntu-Host, aktives OS-Bundle | Nein |
| Laufendes Frontend, aktives OS-Bundle | Ja |
| Laufendes Backend, aktives OS-Bundle | Ja |
| Laufendes Backend, Java-cacerts | Ja |
| Neues Frontend, aktives OS-Bundle | Nein |
| Neuer Backend-Laufzeitkandidat, aktives OS-Bundle | Nein |
| Neuer Backend-Laufzeitkandidat, Java-cacerts | Nein |

Beim Kandidaten mit **unverändertem alten JAR** sank der Scan auf 76
Bibliothekentreffer; OS-Treffer blieben keine. Das bestätigt zusätzlich, dass
neue Basisimages allein die Spring-/Tomcat-/Jackson-Lücken nicht beseitigen.
Die manuell eingesetzte JRE wird durch den OS-Paketscan nicht vollständig
inventarisiert; ihre Version und Herkunft wurden gesondert geprüft.

Vorbereitet: [Frontend-Basispatch](prepared-2026-10-22/frontend-base.patch)
und [Backend-Laufzeit-Dockerfile](prepared-2026-10-22/Dockerfile.backend-runtime-candidate).
Letzteres erwartet im Buildkontext das fertig gebaute `app.jar` und
`OpenJDK21U-jre_x64_alpine-linux_hotspot_21.0.13_9.tar.gz` aus der oben verlinkten
offiziellen Veröffentlichung. Die geprüfte Downloadsumme steht im Dockerfile;
das Archiv und JAR werden nicht im Repository abgelegt.
Das vorbereitete Laufzeit-Dockerfile gilt für die AMD64-Architektur des Servers.

**Entscheidung:** Beide Images zusammen mit dem Bibliotheksupdate erneuern.
Ein bloßes `docker build --pull` genügt wegen des nachlaufenden Temurin-Tags
aktuell nicht. OS- und Java-Truststore nach dem endgültigen Build erneut prüfen.

## 4. Kernel: Konkrete Funktionsprüfung statt pauschaler CVE-Zählung

Die vollständige [Triage der 14 kritischen Kandidaten](Kernel-Triage-2026-10-22.csv)
enthält Voraussetzungen, Laufzeitbefunde und Ubuntu-Status für GA und HWE.

- **CVE-2026-63825 ausgeschlossen:** `CONFIG_GCOV_KERNEL` ist deaktiviert.
  Die für diesen Fehler erforderliche Instrumentierung ist im laufenden Kernel
  nicht vorhanden. [Ubuntu-Referenz](https://ubuntu.com/security/CVE-2026-63825).
- **CVE-2026-64523 bedingt:** Kernel-Handshake ist fest einkompiliert
  (`CONFIG_NET_HANDSHAKE=y`), daher wäre ein fehlendes Modul kein Ausschluss.
  `tlshd` und NFS-Dienste sind inaktiv; ein aktiver Verbraucher wurde nicht
  gefunden. Gewöhnliches HTTPS in Nginx belegt diesen Kernelpfad nicht.
- **CVE-2026-68426 bedingt:** `xfrm_user` und `xfrm_algo` sind geladen,
  ESP/IPComp-Module dagegen nicht; alle XFRM-Fehlerzähler waren null.
  Die direkte Policy-/State-Abfrage war mangels Berechtigung nicht möglich.
  Ein aktiver IPsec-Pfad ist nicht nachgewiesen; ein vollständiger Ausschluss
  lässt sich aus diesen Befunden nicht ableiten.
- Die weiteren elf betreffen NFS/RDMA, Ceph, ksmbd, Soft-RoCE, GUE,
  NVMe-Target und verschlüsseltes TIPC. Die jeweiligen Module waren nicht
  geladen; passende Dateisystem-Mounts beziehungsweise aktive Serverdienste
  wurden nicht gefunden. Damit fehlt aktuell ein beobachteter Dienstpfad.
  Lokal auslösbares Modulnachladen oder neue Namespaces sind dadurch nicht
  grundsätzlich ausgeschlossen. Unprivilegierte User-Namespaces sind erlaubt,
  mit aktivierter AppArmor-Einschränkung.

Installiert sind `linux-virtual` und `linux-image-virtual` in **6.8.0-147.147**.
Der GA-Aktualisierungspfad ist damit über Metapakete vorhanden. Die APT-Listen
stammten vom Untersuchungstag. Eine Installation wurde nur simuliert:

| Alternative | Simulation / Herstellerstatus |
| --- | --- |
| Aktuelle GA-Linie | Weiterhin 6.8.0-147.147; `linux-virtual` würde nichts neu installieren. |
| `linux-virtual-hwe-24.04 --no-install-recommends` | 7.0.0-38.38~24.04.4, elf neue Pakete, keine Entfernung. |
| Drei erste kritische CVEs in HWE | Laut Ubuntu bereits behoben. |
| Übrige elf kritische CVEs in HWE | Laut Ubuntu weiterhin verwundbar; teils Fixes für kommende 7.0.0-39 beziehungsweise -42 in Arbeit. Diese sind kein verfügbarer Fix. |

Auch der verfügbare HWE-Kernel beseitigt somit nicht alle kritischen Kandidaten.
Die zwei im ersten Bericht erwähnten Kernel-KEVs sind im laufenden GA-147
bereits behoben; die Treffer dazu stammen nur aus alten 134er-Paketen.

**Entscheidung:** Kein blindes HWE-Upgrade allein wegen der großen Trefferzahl.
Zuerst die bestätigten Anwendungspfade aktualisieren. Die kommende GA-Version
148 anhand der tatsächlich veröffentlichten Fixes prüfen und reguläre
Kernelwartung mit geplantem Neustart durchführen. Für die weiteren 2.929
Kernelkandidaten wurde keine vollständige manuelle Funktionsprüfung behauptet.
Alte Kernelpakete wurden nicht entfernt.

## Nachweise und verbleibender Veröffentlichungsumfang

Die privaten Rohdaten liegen auf dem Server unter
`/home/deploy/coshift-cve-20261022-_pr2y7oh/followup`, lokale Kopien unter
`/tmp/coshift-cve-20261022/followup`. Die Scannerprüfungen verwendeten dieselbe
Trivy-Datenbank vom **22.10.2026, 13:15 UTC** wie die Erstinventur.
Ein Ergebnis mit null Treffern ist auf diesen Datenstand und die jeweiligen
erkannten Paketarten begrenzt.
Die kompakten Ergebnisse stehen zusätzlich in
[Validierung und Image-IDs](Validierung-2026-10-22.json).

Auf dem Server liegen die getrennten Tags
`coshift-frontend:cve-20261022-candidate`,
`coshift-backend:cve-20261022-runtime-candidate` (altes JAR) und
`coshift-backend:cve-20261022-complete-candidate` (aktualisierter JSON-Stand).
Die produktiven Tags und Container wurden nicht ersetzt. Vor einer späteren
Veröffentlichung sind die konkreten Image-IDs festzuhalten, Rückfallimages und
JSON-Daten zu sichern sowie die produktive Compose-Konfiguration gezielt
anzupassen. Kassa benötigt einen Wechsel der virtuellen Umgebung samt
Template-Korrektur und Dienstneustart. Diese Veröffentlichungsschritte sind
nicht Teil der hier ausgeführten Klärung.
