# eLauncher

eLauncher is an extremely lightweight and minimal launcher for Android, based on NoLauncher and inspired by [OLauncher Light](https://github.com/tanujnotes/Ultra/), and OLauncher in general. It is even more barebones than OLauncher Light, and aims to provide only the most basic features.

eLauncher favours easy readibility on eInk/ePaper devices, such as the Onyx Boox Note series, and the Bigme HiBreak.

## Features

- Extremely lightweight: only 1010KB
- eInk friendly: uses a light theme by default, fix text size and weight
- Fuzzy Search: search for apps by typing their name
- Bottom search bar in app drawer

- Homescreen and app drawer: swipe up on homescreen to enter the app drawer
- Long press an app field on the homescreen to assign an app, app can be renamed
- Type to search in app drawer, if only one result is left, it is automatically launched (like OLauncher)
- Gestures: swipe down for notification center, left/right swipe to launch any app (configurable in Settings), double tap to open the original launcher
- Customizable swipe gestures: pick any app for left and right swipe gestures via Settings
- Hold on empty space to change the number of apps on homescreen

## apk size differences with OLauncher Light

This might have been done on purpose, but OLauncher Light uses long deprecated APIs, like ListView to achieve its impressive 23 KB apk size. eLauncher uses RecyclerView, which is much better for performance and memory usage, and also uses many other newer APIs. Thus, the APK size is much larger than with OLauncher Light, but still really small — ~1 MB.

## Download

You can download the apk file directly from the releases tab and install it manually.

## Contributing

Feel free to contribute if you found a bug or have a way to make the code more efficient or minimal, but please don't add massive new features. If you feel like adding a lot of customization options, widgets, etc. please start your own fork, as the scope of this project is to be as (reasonably) barebones of a launcher as possible.

## Testing (Cucumber BDD + coverage gate)

Behavior specs live in Gherkin features (`app/src/test/resources/features/`) and run on the JVM via Robolectric — no emulator needed:

```sh
./gradlew check                      # runs the BDD suite, unit tests, and the coverage gate
./gradlew jacocoUnitTestReport       # HTML/XML coverage report
```

- Feature files describe every launcher behavior (drawer, fuzzy search, homescreen slots, gestures, settings, first-launch onboarding, dark mode, last-used-app, vendor shims).
- Step definitions are in `app/src/test/java/me/pompel/elauncher/bdd/steps/`.
- `check` fails the build when line coverage of the app source drops below **80%** (`jacocoTestCoverageVerification`).
- CI (`.github/workflows/ci.yml`) enforces the gate on every push and pull request.

## Android 4.4 / API 19 e-readers

This fork is adjusted for Android 4.4.2 (API 19) e-readers such as the Tolino
Epos 2. The minimum SDK remains 19. HOME registration, alphabetically sorted
applications, launching, home slots and basic fuzzy search remain available.
On API 19 there is no usage-access request, usage highlighting or last-used-app
slot, and no system-bar color API calls. Drawer transitions and list animations
are disabled for e-ink. The search drawable uses explicit black/white color
resources, compatible with KitKat. Usage statistics and system-bar colors on
API 21+ are isolated behind SDK guards; Bigme integration remains vendor/API gated.

Build with `./gradlew assembleDebug`; the APK is
`app/build/outputs/apk/debug/app-debug.apk`. Install with
`adb install -r app/build/outputs/apk/debug/app-debug.apk`, then select eLauncher
as the default HOME app. Physical Tolino testing is still required, including
returning HOME after reboot; a successful build alone does not prove device behavior.
