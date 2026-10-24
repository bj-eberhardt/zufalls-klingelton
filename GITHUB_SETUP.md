# GitHub-Setup für Android-Releases

Dieses Projekt nutzt GitHub Actions, um nach einem gemergten Pull Request auf `master` automatisch die App-Version zu erhöhen, eine signierte Android-App zu bauen und ein GitHub Release zu erstellen.

## Workflow

Der Workflow liegt in `.github/workflows/bump-version-on-merge.yml` und startet bei:

- einem gemergten Pull Request nach `master`
- einem manuellen Start über **Actions → Version erhöhen und Android Release erstellen → Run workflow**

Der Ablauf:

1. `versionCode` wird um 1 erhöht.
2. Die Patch-Version von `versionName` wird erhöht, zum Beispiel `1.0.0` → `1.0.1`.
3. Die Änderung wird als automatischer Commit nach `master` gepusht.
4. Ein Tag wie `v1.0.1` wird erstellt und gepusht.
5. Die Unit-Tests laufen.
6. Eine signierte APK und ein signiertes AAB werden gebaut.
7. Das GitHub Release für den neuen Tag wird mit beiden Dateien angelegt.

Der automatische Versions-Commit enthält `[skip ci]`, damit kein weiterer Release-Lauf ausgelöst wird.

## Erforderliche GitHub-Secrets

Unter **Settings → Secrets and variables → Actions → New repository secret** müssen diese vier Secrets angelegt werden:

| Secret | Inhalt |
| --- | --- |
| `ZK_STORE_FILE_B64` | Base64-Inhalt des Release-Keystores (`.jks`) |
| `ZK_STORE_PASSWORD` | Passwort des Keystores |
| `ZK_KEY_ALIAS` | Alias des verwendeten Signaturschlüssels |
| `ZK_KEY_PASSWORD` | Passwort des Signaturschlüssels |

Der Keystore darf nicht committed werden. `keystore/`, `*.jks` und `*.keystore` sind in `.gitignore` ausgeschlossen. Die Action schreibt den Keystore nur temporär in den Runner und entfernt ihn nach dem Build auch bei einem Fehler.

### Alias des Signaturschlüssels ermitteln

`ZK_KEY_ALIAS` ist nicht der Dateiname des Keystores, sondern der Alias des darin enthaltenen Schlüssels. Für den vorhandenen Keystore kann der Alias so angezeigt werden:

```powershell
keytool -list -v -keystore .\keystore\google_play
```

Nach Eingabe des Keystore-Passworts in der Ausgabe nach dieser Zeile suchen:

```text
Alias name: key0
```

Den angezeigten Wert als `ZK_KEY_ALIAS` eintragen. Bei einem neu erzeugten oder standardmäßig angelegten Keystore lautet der Alias häufig `key0`; maßgeblich ist aber immer der tatsächlich angezeigte Wert. `ZK_KEY_PASSWORD` ist das Passwort dieses Alias-Schlüssels und kann vom Keystore-Passwort abweichen.

### Base64-Keystore erzeugen

Der vorhandene Keystore liegt lokal unter `keystore/google_play` und hat bewusst keine Dateiendung. Unter PowerShell kann er so für das Secret kodiert werden:

```powershell
$keystorePath = Join-Path (Get-Location) "keystore\google_play"
[Convert]::ToBase64String([IO.File]::ReadAllBytes($keystorePath))
```

Die komplette Ausgabe als Wert von `ZK_STORE_FILE_B64` eintragen. Den Keystore selbst niemals in den Quellcode oder in ein öffentliches Repository laden.

## Repository-Berechtigungen

Unter **Settings → Actions → General** muss der Workflow Schreibzugriff auf Repository-Inhalte haben. Der Workflow fordert dies bereits explizit an:

```yaml
permissions:
  contents: write
```

Damit darf der automatisch bereitgestellte `GITHUB_TOKEN` den Versions-Commit, den Tag und das Release erstellen.

## Signaturkonfiguration

`app/build.gradle.kts` liest während des CI-Builds die temporäre Datei `keystore.properties`. Lokal kann eine solche Datei ebenfalls verwendet werden, sie darf aber nicht committed werden.

Ohne `keystore.properties` bleibt der lokale Debug-Build möglich. Für `assembleRelease` wird in der Action die Datei aus den Secrets erzeugt und die Release-Variante signiert.

## Release manuell starten

1. Repository auf GitHub öffnen.
2. **Actions** auswählen.
3. **Version erhöhen und Android Release erstellen** öffnen.
4. **Run workflow** anklicken.
5. Den Branch `master` auswählen.

Der manuelle Start führt denselben Versionsschritt wie ein gemergter Pull Request aus. Deshalb nicht mehrfach starten, wenn keine weitere Version erzeugt werden soll.

## Ergebnis

Im GitHub-Release erscheinen:

- `app-release.apk` für die direkte Installation auf Android-Testgeräten
- `app-release.aab` für die weitere Verwendung im Google Play Console-Workflow

Der Play-Store-Upload selbst ist bewusst nicht Bestandteil dieser Action. Er kann später mit einem separaten, geschützten Deployment-Schritt ergänzt werden.

## Fehlerdiagnose

- **Fehlendes Secret:** Der Workflow bricht vor dem Signieren mit dem Namen des fehlenden Secrets ab.
- **Tag existiert bereits:** `versionName` wurde bereits veröffentlicht; den Versionsstand prüfen und nicht denselben Release-Tag überschreiben.
- **Keystore-Fehler:** Base64-Wert, Alias und beide Passwörter müssen exakt zum Keystore gehören.
- **Release wird nicht erstellt:** Repository-Berechtigung `contents: write` und den vollständigen Workflow-Log prüfen.
