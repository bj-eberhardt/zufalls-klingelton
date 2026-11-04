# 🛠️ Development Guide

Technische Informationen für die lokale Entwicklung und Qualitätssicherung von **Zufalls-Klingelton**.

## Voraussetzungen

Die Einrichtung der GitHub-Release-Automation ist in [GITHUB_SETUP.md](GITHUB_SETUP.md) beschrieben.

- Android Studio mit aktuellem Android SDK
- JDK 17
- Android SDK Platform 36
- Android-Gerät oder Emulator für Instrumentationstests
- Windows: `gradlew.bat`; macOS/Linux: `./gradlew`

Die App nutzt Kotlin, Jetpack Compose und Material 3. Das Mindest-SDK ist Android 8.0 (API 26).

## Projekt bauen

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
.\gradlew.bat assembleAndroidTest
```

## Automatisierte lokale Gerätetest-Matrix

Die Datei `app/build.gradle.kts` definiert zwei lokale Gradle Managed Devices
(Pixel 2 mit API 35 und 36). Gradle erstellt, startet und entfernt die
Emulatoren für den jeweiligen Testlauf automatisch. Für die lokale Matrix
Für die lokale Matrix verwenden wir API 35 und 36, weil ältere Managed-
Device-Images auf diesem Rechner nicht zuverlässig booten. API 26 und API 32
können separat auf vorbereiteten lokalen Emulatoren getestet werden.

Für Windows ist in `gradle.properties` das stabile Software-Rendering
`swiftshader_indirect` aktiviert. Das verhindert, dass der Emulator die auf
manchen Installationen nicht unterstützte Option `auto-no-window` auswählt.
API 35 und 36 verwenden `x86_64`, passend zu den
jeweiligen System-Images. Die Matrix startet die Emulatoren absichtlich
nacheinander, um Snapshot- und Speicherprobleme durch parallele Emulatorstarts
auf lokalen Windows-Rechnern zu vermeiden.

Nur Unit-Tests:

```powershell
.\gradlew.bat testDebugUnitTest
```

Ein einzelnes Managed Device:

```powershell
.\gradlew.bat pixel2Api35DebugAndroidTest
```

Die komplette lokale Matrix läuft über den projektspezifischen Sammel-Task:

```powershell
.\gradlew.bat :app:localPhoneMatrixTest
```

Diese Tasks werden nicht von der GitHub-Actions-CI aufgerufen. Die CI führt
weiterhin nur `testDebugUnitTest` aus; für die Managed Devices ist kein
Emulator-Setup in der CI erforderlich.

Die Runtime-Berechtigungen können in einem End-to-End-Test über die echten
Android-Systemdialoge geprüft und per UI-Automation bestätigt werden. Die
Systemklingelton-Tests benötigen zusätzlich den besonderen
`WRITE_SETTINGS`-Zugriff. Dieser ist kein normaler Runtime-Dialog und muss auf
dem dedizierten lokalen Testemulator vor dem Systemtest über `appops` gesetzt
werden. Dadurch bleiben die fachlichen Tests deterministisch und verändern
nicht versehentlich ein echtes Gerät.

## Berechtigungen

Die App benötigt:

- `READ_MEDIA_AUDIO` ab Android 13 beziehungsweise `READ_EXTERNAL_STORAGE` bis Android 12
- `READ_PHONE_STATE` für den Wechsel nach einem eingehenden Anruf
- `WRITE_SETTINGS` für das Setzen des Systemklingeltons

`WRITE_SETTINGS` wird über die Android-Systemeinstellungen beziehungsweise die App-Op `android:write_settings` erteilt.

## Instrumentationstests

Die Tests verändern den globalen Systemklingelton des Testgeräts temporär. Deshalb ausschließlich einen dedizierten Emulator oder ein ausdrücklich freigegebenes Testgerät verwenden.

### Emulator vorbereiten

API 35/36:

```bash
adb shell pm grant de.emailspeicher.zufallsklingelton android.permission.READ_MEDIA_AUDIO
adb shell pm grant de.emailspeicher.zufallsklingelton android.permission.READ_PHONE_STATE
adb shell appops set de.emailspeicher.zufallsklingelton android:write_settings allow
```

API 32:

```bash
adb shell pm grant de.emailspeicher.zufallsklingelton android.permission.READ_EXTERNAL_STORAGE
adb shell pm grant de.emailspeicher.zufallsklingelton android.permission.READ_PHONE_STATE
adb shell appops set de.emailspeicher.zufallsklingelton android:write_settings allow
```

### Ausführen

Standardlauf über Gradle:

```bash
./gradlew connectedDebugAndroidTest
```

Windows:

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

Alternativ in Android Studio eine Testklasse aus `app/src/androidTest` auf einem API-32- oder API-35/36-Emulator starten.

Bei manchen Emulator-Images wird `WRITE_SETTINGS` durch die erneute APK-Installation zurückgesetzt. Dann zuerst die APKs installieren, anschließend die App-Op setzen und den Runner direkt starten:

```bash
adb shell am instrument -w -r de.emailspeicher.zufallsklingelton.test/androidx.test.runner.AndroidJUnitRunner
```

Fehlendes `WRITE_SETTINGS` wird nicht stillschweigend übersprungen, sondern als konkreter Setup-Fehler gemeldet.

## Testabdeckung

- `RingtoneSystemRule`: Berechtigungen, Original-Klingelton, Preferences und MediaStore-Cleanup
- `MediaStoreFixture`: kurze WAV-Fixtures im MediaStore
- `RingtoneCoordinatorInstrumentationTest`: Aktivierung, Tagesmodus, Anrufmodus, Löschen, Leeren und nicht erreichbare URIs
- `ReceiverInstrumentationTest`: Boot-, Datum- und Telefonstatus-Receiver sowie Exactly-once-Verhalten bei `RINGING → IDLE`
- Compose-Tests für Setup und Track-Pool

Die Fixture erzeugt den kurzen WAV-Ton deterministisch im Testcode; es ist kein persönliches Musikstück im Repository erforderlich.

## Geschützte System-Broadcasts

`BOOT_COMPLETED`, `DATE_CHANGED` und `PHONE_STATE` sind auf aktuellen Android-Versionen geschützte System-Broadcasts. Sie können aus einem normalen Testprozess nicht glaubwürdig per `sendBroadcast()` oder `am broadcast` gefälscht werden.

Die Instrumentationstests führen deshalb die echten Receiver-Klassen mit echten Intents und dem produktiven `goAsync()`-Runner-Seam aus. Persistenz, asynchrone Verarbeitung und tatsächliche System-Klingeltonänderung bleiben dabei real. Die Zustellung im Produkt erfolgt weiterhin durch Android über die Manifest-Receiver.

Ein echter Reboot beziehungsweise eine echte Telefonie ist für einen separaten Geräteakzeptanztest erforderlich.

## Echte UI- und Plattformtests

`PermissionAndPickerE2eTest` bedient lokal echte Runtime-Berechtigungsdialoge,
den `WRITE_SETTINGS`-Bildschirm und DocumentsUI auf API 35 und 36:

```powershell
.\gradlew.bat :app:localPermissionE2eTest
```

`localPlatformAcceptanceTest` benötigt genau einen verbundenen Emulator und
führt echte Emulator-Telefonie sowie einen echten Reboot aus:

```powershell
.\gradlew.bat :app:localPlatformAcceptanceTest
```

Der Acceptance-Task bricht bei echten Geräten oder mehreren verbundenen
Geräten bewusst ab und wird nicht von der CI ausgeführt.

### Manuelle lokale Testausführung

Für die vollständige lokale Prüfung werden diese drei Abläufe verwendet:

1. Schnelle fachliche Instrumentationstests auf API 35 und 36:

   ```powershell
   .\gradlew.bat :app:localPhoneMatrixTest
   ```

2. Echte Berechtigungsdialoge, `WRITE_SETTINGS` und DocumentsUI:

   ```powershell
   .\gradlew.bat :app:localPermissionE2eTest
   ```

3. Echter Emulator-Reboot und Emulator-Telefonie. Dafür muss genau ein
   Emulator per `adb devices` verbunden sein:

   ```powershell
   .\gradlew.bat :app:localPlatformAcceptanceTest
   ```

Für die maximale Abdeckung alle drei Tasks ausführen. Der zweite Task nutzt
   ebenfalls die lokalen Managed Devices und kann deshalb bereits vorhandene
   Matrixtests erneut ausführen; bei einem schnellen reinen Regressionstest
   genügt der erste Task.

## Architekturhinweise

- `TrackRepository` persistiert den Track-Pool.
- `PlaybackStore` persistiert Aktivierung, Modus, aktuellen Track und Historie.
- `RingtoneCoordinator` enthält die fachliche Wechsel- und Wiederherstellungslogik.
- `RingtoneController` kapselt `RingtoneManager.setActualDefaultRingtoneUri`.
- `ReceiverWorkRunner` trennt Receiver-Lifecycle und fachliche Arbeit.
- Compose-Oberflächen reagieren auf den zentralen UI-State des `MainViewModel`.
