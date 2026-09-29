# VeilBrowse

> **Browse Freely. Stay Private.**

VeilBrowse is a modern, privacy-first Android web browser built with Kotlin and Jetpack Compose. It reduces device fingerprinting exposure, blocks intrusive ad and analytics trackers, supports manual proxy and system VPN verification, and enforces strict session data purging without collecting user telemetry.

---

## Features

- **Private Browser Engine**: Sandboxed Android WebView with geolocation blocked, passwords unpersisted, strict HTTPS enforcement, multi-tab switching, and bookmarks.
- **Tracker & Fingerprint Shield**: Intercepts known ad tracking, analytics, and social pixel networks in real-time with a live blocked counter and inspection sheet.
- **Connection & VPN Verification**: Probes network capabilities and verifies active VPN tunnels honestly without generating fake locations or spoofed IPs.
- **Proxy Gateway**: In-app configuration for HTTP and SOCKS5 proxies (e.g. Tor or local gateways) applied directly via Android's `ProxyController`.
- **Randomized Privacy Profiles**: Select between Standard, Generic Android, Generic Mobile, and Generic Desktop to strip device model identifiers.
- **Zero Telemetry**: VeilBrowse never collects IMEI, device serial numbers, contacts, phone numbers, installed apps, or remote browsing logs.

---

## System Requirements

To build and run VeilBrowse locally, ensure you have:

| Requirement | Supported Version |
| :--- | :--- |
| **Operating System** | macOS, Linux, or Windows 10/11 |
| **JDK (Java)** | **JDK 21** (Temurin or OpenJDK recommended) |
| **Android Studio** | **Android Studio Ladybug (2024.2.1)** or newer |
| **Android SDK** | Android 7.0 (API level 24) to Android 15 (API level 36) |
| **Build Tools** | Gradle 9.3.1 (included via Gradle Wrapper) |

---

## 1. Clone the Repository

Clone the project from GitHub using Git:

```bash
git clone https://github.com/YOUR_USERNAME/VeilBrowse.git
cd VeilBrowse
```

---

## 2. Open in Android Studio

1. Launch **Android Studio**.
2. Select **Open** (or **File > Open**).
3. Navigate to the cloned `VeilBrowse` folder and click **OK**.
4. Allow Android Studio to sync the project with Gradle files.
   - If prompted for a JDK location, ensure **JDK 21** is selected under **Settings > Build, Execution, Deployment > Build Tools > Gradle > Gradle JDK**.

---

## 3. How to Build the Android APK

You can build the debug APK using either **Android Studio** or the **Terminal / Command Prompt**.

### Method A: Command-Line (Terminal)

The project includes the Gradle wrapper (`gradlew` / `gradlew.bat`), so you do not need to install Gradle manually.

#### On macOS / Linux:
```bash
# Make gradlew executable (if not already done)
chmod +x ./gradlew

# Build the Debug APK
./gradlew assembleDebug
```

#### On Windows (PowerShell or Command Prompt):
```cmd
gradlew.bat assembleDebug
```

### Method B: Android Studio GUI

1. Open the project in Android Studio.
2. In the top menu bar, click **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
3. Wait for the build process to finish. A notification will pop up in the lower-right corner with a **locate** link to open the folder containing the APK.

---

## 4. APK Output Location

After a successful build, the generated APK file is located at:

```
app/build/outputs/apk/debug/app-debug.apk
```

You can install this APK directly onto an Android device or emulator:
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## 5. Automated Builds via GitHub Actions

This repository includes a ready-to-run GitHub Actions workflow located at `.github/workflows/android-build.yml`.

### How to download the APK from GitHub Actions:

1. Push your code to your GitHub repository:
   ```bash
   git add .
   git commit -m "Initial commit for VeilBrowse"
   git push origin main
   ```
2. Navigate to your GitHub repository in your web browser.
3. Click the **Actions** tab at the top.
4. Select the **Build VeilBrowse Android APK** workflow on the left sidebar.
5. Click on the most recent workflow run (corresponding to your commit).
6. Scroll down to the **Artifacts** section at the bottom of the page.
7. Click **`veilbrowse-debug-apk`** to download a ZIP containing the compiled `app-debug.apk`.

---

## 6. How to Create a GitHub Release

When you are ready to publish an official version:

1. On GitHub, go to your repository homepage.
2. In the right-hand column, click **Releases > Create a new release** (or **Draft a new release**).
3. Create a tag (e.g., `v1.0.0`) and enter a release title (e.g., `VeilBrowse v1.0.0`).
4. Drag and drop your compiled APK into the **Attach binaries by dropping them here** section.
5. Click **Publish release**.

---

## 7. Preparing a Signed Release APK / AAB

To build an optimized release APK or Google Play Android App Bundle (AAB):

1. Generate your production keystore in Android Studio via **Build > Generate Signed Bundle / APK**.
2. For automated CI/CD builds, export your keystore credentials as environment variables:
   - `KEYSTORE_PATH`: Absolute path to your `.jks` file
   - `STORE_PASSWORD`: Keystore password
   - `KEY_PASSWORD`: Key alias password
3. Run:
   ```bash
   # For release APK
   ./gradlew assembleRelease

   # For Google Play App Bundle (AAB)
   ./gradlew bundleRelease
   ```
4. Output locations:
   - Release APK: `app/build/outputs/apk/release/`
   - Release AAB: `app/build/outputs/bundle/release/`

> **Security Note:** Never commit your keystore (`*.jks`), private keys, or credentials into Git. They are ignored by `.gitignore`.

---

## Architecture & Project Structure

```
VeilBrowse/
├── .github/workflows/
│   └── android-build.yml       # Automated GitHub Actions APK build & artifact upload
├── app/
│   ├── build.gradle.kts        # App-level dependencies, plugins, and compile SDK setup
│   ├── proguard-rules.pro      # ProGuard optimization rules
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml   # App declarations and network permissions
│       │   ├── java/com/example/
│       │   │   ├── MainActivity.kt   # Root Edge-to-Edge Navigation Scaffold
│       │   │   ├── ui/theme/         # Dark Privacy Cyber ColorScheme & Typography
│       │   │   └── veilbrowse/
│       │   │       ├── data/model/   # Tabs, Trackers, Diagnostics, Settings data classes
│       │   │       ├── data/db/      # Room database & DAOs
│       │   │       ├── data/repository/ # Network diagnostic probe & Settings storage
│       │   │       ├── engine/       # WebClient, ChromeClient, TrackerBlocker, UA cloaking
│       │   │       ├── ui/           # Dashboard, Browser, Privacy, Connection, Settings screens
│       │   │       └── viewmodel/    # State management for Browser, Network, and Settings
│       │   └── res/                  # Adaptive launcher icons, drawables, strings, themes
│       └── test/                     # Local JVM and Robolectric unit tests
├── gradle/
│   ├── libs.versions.toml      # Centralized Gradle version catalog
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── build.gradle.kts            # Root project build configuration
├── settings.gradle.kts         # Module includes and repository resolution
├── gradlew                     # Gradle wrapper Unix script
├── gradlew.bat                 # Gradle wrapper Windows batch script
└── .gitignore                  # Git ignore rules for build artifacts and keystores
```

---

## License

This project is licensed under the Apache 2.0 License.
