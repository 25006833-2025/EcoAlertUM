# EcoAlertUM

## Stack and structure

### Stack

| Layer | Technology |
|---|---|
| App | Native Android, Kotlin |
| UI | Jetpack Compose, Material 3 |
| Navigation | Navigation Compose (one `NavHost`, bottom navigation bar) |
| Backend | Firebase: Auth, Cloud Firestore, Cloud Storage, Cloud Functions, FCM (see [docs/architecture.md](docs/architecture.md)) |
| Build | Gradle (Kotlin DSL) with a version catalog in [gradle/libs.versions.toml](gradle/libs.versions.toml) |

Library versions live only in `gradle/libs.versions.toml`. To add a library, add it there and reference it from `app/build.gradle.kts` as `libs.<alias>`.

### Package layout

All code is under `app/src/main/java/com/ecoalertum/android/`:

```
MainActivity.kt          Entry point: applies the theme and shows EcoAlertApp
core/
  navigation/
    Routes.kt            Every route name (constants)
    TopLevelDestination.kt  The 5 bottom-bar tabs: route, label, icon
    DrawerDestination.kt The side-menu items that open a screen
    EcoAlertNavHost.kt   The navigation graph
  ui/
    EcoAlertApp.kt       Root: side menu + top bar + bottom bar + NavHost
    EcoAlertDrawer.kt    Material 3 side menu (layout from the wireframe)
    EcoAlertBottomBar.kt Material 3 NavigationBar
    PlaceholderScreen.kt Temporary "module name" screen
home/       HomeScreen.kt
alerts/     AlertsScreen.kt, AlertHistoryScreen.kt, AlertPreferencesScreen.kt
map/        MapScreen.kt, MyReportsScreen.kt, ModeratorQueueScreen.kt
assistant/  AssistantScreen.kt
learn/      LearnScreen.kt, ProgressScreen.kt
settings/   EmergencyContactsScreen.kt, AboutScreen.kt
ui/theme/   Colours, typography, theme (owned by UI/UX)
```

- **One package per module.** Build your module inside its own package and replace the body of its `XxxScreen` composable. Extra screens, view models and components for that module go in the same package.
- **`core` is for shared code only:** navigation, shared UI components and, later, the data service. Do not put module-specific code there.
- **Navigation:** the five tabs and the side-menu screens sit in a nested graph (`Routes.MAIN_GRAPH`). The top bar, side menu and bottom bar show only inside that graph, so a login screen can be added in front of it. The steps are in the comment at the top of `EcoAlertNavHost.kt`.
- **Strings** go in `res/values/strings.xml` (Malay later in `values-ms/strings.xml`). Do not hard-code UI text.
- **No API keys or secrets in the app.** AI and official-data calls go through Cloud Functions.

### How to run

1. Install Android Studio (recent stable) with JDK 17+ and Android SDK 35.
2. Clone the repo and open the project folder in Android Studio. Let Gradle sync.
3. Start an emulator (API 24 or higher) or connect a phone.
4. Press **Run**, or from a terminal in the project folder:
   ```
   .\gradlew.bat assembleDebug      # Windows: build the debug APK
   ./gradlew assembleDebug          # macOS / Linux
   ```
   The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

  
### AI Assistant Backend (Module 4 — Week 4)

The EcoAlert UM AI Assistant uses Firebase Cloud Functions as a secure backend proxy for the Google Gemini API. The Android application communicates with the `askAssistant` callable function instead of accessing Gemini directly.

**Backend implementation:**

- **Cloud Function:** `askAssistant` (`functions/src/assistant/askAssistant.ts`)
- **Gemini integration:** `functions/src/assistant/gemini.ts`
- **Prompt and safety rules:** `functions/src/assistant/prompts.ts`
- **Request validation:** `functions/src/assistant/validation.ts`
- **Fallback responses:** `functions/src/shared/fallback.ts`
- **AI model:** `gemini-3.8-flash`, using the Gemini Interactions API
- **Supported languages:** English (`en`) and Bahasa Melayu (`ms`)

The backend validates incoming requests, checks Firebase Authentication and verified UM student email addresses, retrieves relevant Firestore alert information, and generates AI explanations using Gemini.

When Gemini generation fails, the backend provides predefined safety guidance instead of generating unsupported hazard information.

#### Local Firebase Emulator Setup

The AI Assistant backend can be tested locally using the Firebase Emulator Suite.

1. Install dependencies and build the Cloud Functions:

   ```powershell
   cd functions
   npm install
   npm run build
   cd ..
   ```

2. Configure the Gemini API key locally in `functions/.secret.local`:

   ```text
   GEMINI_API_KEY=your_api_key_here
   ```

   Never commit this file or expose the API key in Android source code.

3. Start the Firebase Emulators from the project root:

   ```powershell
   firebase.cmd emulators:start
   ```

4. Open the Emulator UI at `http://127.0.0.1:4000/`.

5. For local Android emulator testing, connect the Firebase SDKs to:

   | Service | Android Emulator Host | Port |
   |---|---|---|
   | Authentication | `10.0.2.2` | `9099` |
   | Firestore | `10.0.2.2` | `8080` |
   | Cloud Functions | `10.0.2.2` | `5001` |

6. Create a verified test student account in the Authentication Emulator and a sample document at `alerts/alert_001` in the Firestore Emulator.

7. Test the callable function with a request such as:

   ```json
   {
     "mode": "explain",
     "language": "en",
     "alertId": "alert_001"
   }
   ```

   The request must be made through an authenticated Firebase Functions client.





### To Setup Local Firebase emulator
1. Open your PC terminal
2. Install Node.js from nodejs.org if you don’t have it. Check with
 ```powershell
   node -v
   ```
```powershell
# Download and install fnm:
winget install Schniz.fnm

# In lieu of restarting the shell
fnm env --use-on-cd --shell powershell | Out-String | Invoke-Expression

# Download and install Node.js:
fnm install --use 26

# Verify the Node.js version:
node -v # Should print "v26.13.1".

# Verify npm version:
npm -v # Should print "11.20.0".
```
Important to add this to your PowerShell profile once:

```powershell
if (!(Test-Path -Path $PROFILE)) { New-Item -ItemType File -Path $PROFILE -Force }
Add-Content -Path $PROFILE -Value 'fnm env --use-on-cd --shell powershell | Out-String | Invoke-Expression'
```
If you don't , you have to run the [fnm env...] line every time you open a new PowerShell window

3.Install Java, version 21 or newer. The Firestore emulator needs it. Check with 
```powershell
java -version.
```

4.Install the Firebase tool. Run 
```powershell
npm install -g firebase-tools.
```

5.Run this and sign in with the Google account that owns the project.
```powershell
firebase login
```

6.Open another terminal in the project root(locate where is the project file in your laptop/PC). That’s the folder containing firebase.json, the one with app/, docs/ and functions/ in it.

7.Pick the project. Run 
```powershell
firebase use ecoalertum
```
That’s the project ID from our google-services.json.

8.Start the emulators. Run 
```powershell
firebase emulators:start 
```

9.IMPORTANT TO KEEP FIREBASE EMULATOR ON AND DO NOT CLOSE IT OR ELSE IT WONT WORK 
<img width="1291" height="512" alt="image" src="https://github.com/user-attachments/assets/f6eace8c-900f-4056-9f08-0944a852fe7d" />

10.go to this link http://127.0.0.1:4000/auth , it will bring you to the firebase emulator suite
<img width="1917" height="912" alt="Screenshot 2026-11-17 211330" src="https://github.com/user-attachments/assets/61f02b35-cf10-4d24-978d-df4b03746f1b" />

click add user

11.fill in as shown

<img width="500" height="600" alt="Screenshot 2026-11-17 211341" src="https://github.com/user-attachments/assets/9643ca66-95fc-4641-9b43-28b16545fd73" />
<img width="500" height="600" alt="Screenshot 2026-11-17 211346" src="https://github.com/user-attachments/assets/3d8c77fc-b32f-4e1a-91ae-b1841a17abc6" />




#### Week 4 Testing Results

The following components were successfully tested using local Firebase Emulators:

- Firebase Authentication with a verified UM student test account.
- Firestore alert document retrieval.
- Android-to-Firebase callable function invocation.
- Rule-based fallback response when Gemini generation fails.

**Known limitations:**

- Gemini requests encountered HTTP `503` (service unavailable) and API quota limitations during integration testing.
- Some requests exceeded the Cloud Function's approximately 60-second timeout, resulting in an `INTERNAL` error instead of a fallback response.
- A successful live Gemini-generated response through the Android-to-Firebase integration has not yet been verified.
- Timeout handling and further live API testing remain future improvements.
- The temporary Android test code is not part of the completed AI Assistant user interface.

For detailed test results and safety constraints, refer to `docs/AI_USAGE_LOG.md` and `docs/AI_SAFETY_GUIDELINES.md` (adjust these links if the files are stored elsewhere).

### Branch rules

- `main` is protected. Nobody pushes to it directly.
- One branch per task, named `feature/<name>` (for example `feature/alerts-list`). Branch from an up-to-date `main`.
- Open a pull request into `main` when the task is done. At least one teammate reviews and approves it before merging.
- Make sure the app builds (`assembleDebug`) before asking for a review.
