# ajLeaderboards 2.11.0-liso.1

Der vorherige Start auf Folia 26.2 / Java 27 scheiterte beim SlimJar-Self-Attach:
`Could not self-attach to current VM using external process`, anschließend
`Injectable is null` und fehlender `OkHttpClient$Builder`.

HikariCP 3.4.5, H2 2.1.214 und OkHttp 4.12.0 einschließlich ihrer transitiven
Bibliotheken werden jetzt beim Build eingebettet und in den Plugin-Namensraum
verschoben. Der SlimJar-Lader und sein Logger entfallen. Die bisher verwendeten
Bibliotheksversionen und Datenbankformate bleiben gleich. SLF4J und SQLite kommen
weiterhin vom Server. JDBC-Serviceeinträge werden zusammengeführt; reflektiv
geladene Klassen und Ressourcen werden nicht durch Minimierung entfernt.

## Prüfung

Build mit JDK 21 und dem vorhandenen Gradle-Wrapper 9.3.1:

```text
./gradlew check shadowJar --no-daemon --console=plain
```

- 58 bestehende Unit-Tests erfolgreich.
- `verifyBundledRuntime` lädt die **fertige JAR** in einem isolierten Classloader.
  Nur die vom Server bereitgestellte SLF4J-API wird zusätzlich freigegeben.
- H2-SQL-Schreib-/Lesevorgang, JDBC-ServiceLoader, Hikari-Verbindungspool,
  OkHttp-Client sowie die Public-Suffix-Ressource einschließlich Kotlin/Okio geprüft.
- Keine SlimJar-Klassen in der fertigen JAR, keine Agent-Installation im Startpfad.
- Bestehender Null-Fallback der Zahlenformatierung ergänzt; der Zeitstempeltest
  verwendet einen festen Offset statt der Zeitzone des Build-Rechners.
- Keine Commands, Aliase oder Berechtigungen verändert.

Das erfolgreiche lokale Ergebnis ersetzt keinen Live-Test unter Java 27. Dessen
Ergebnis wird separat im Installationsnachweis festgehalten.

## Installation

1. Server geordnet stoppen. Kein PlugMan-/Plugin-Reload.
2. Bisherige ajLeaderboards-JAR durch Umbenennen mit Endung `.before-liso1` sichern.
3. `ajLeaderboards-2.11.0-liso.1.jar` nach `plugins/` hochladen; nur eine aktive
   ajLeaderboards-JAR behalten.
4. `plugins/ajLeaderboards/` samt Datenbanken und Konfiguration unverändert lassen.
   Alte `libs/`-Downloads sind nicht mehr erforderlich, müssen aber nicht gelöscht werden.
5. Server starten, Enable-Log und vorhandene Boards/Placeholder prüfen.

Rückweg: Server stoppen, neue JAR aus der aktiven Plugin-Auswahl nehmen und gesicherte
alte JAR zurückbenennen. Keine Datenbanken zurücksetzen.

Quellen zur Paketierung:
[Shadow Service-Dateien](https://gradleup.com/shadow/configuration/merging/),
[Grenzen der Minimierung](https://gradleup.com/shadow/configuration/minimizing/).
