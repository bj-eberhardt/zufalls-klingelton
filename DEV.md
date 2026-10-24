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

## Architekturhinweise

- `TrackRepository` persistiert den Track-Pool.
- `PlaybackStore` persistiert Aktivierung, Modus, aktuellen Track und Historie.
- `RingtoneCoordinator` enthält die fachliche Wechsel- und Wiederherstellungslogik.
- `RingtoneController` kapselt `RingtoneManager.setActualDefaultRingtoneUri`.
- `ReceiverWorkRunner` trennt Receiver-Lifecycle und fachliche Arbeit.
- Compose-Oberflächen reagieren auf den zentralen UI-State des `MainViewModel`.
