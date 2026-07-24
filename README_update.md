# KeyAura Phase 1 Update

This ZIP contains all files changed or added in Phase 1.

## How to apply

1. Extract the ZIP
2. Copy the contents into your existing KeyAura3 project root, **overwriting** when prompted
3. Sync/rebuild the project

## Changes summary

- Added **SettingsActivity** – central hub for About, Contact, Privacy, Version, and Themes
- Added **ContactUsActivity** – email composition with pre-filled address
- Added **PrivacyPolicyActivity** – placeholder privacy policy
- Updated **AboutActivity** – uses `@string/about_content` with OKLabs branding
- Updated **MainActivity** – added Settings button, menu now opens Settings
- Updated **AndroidManifest.xml** – registered new activities
- Updated **strings.xml** – all text externalized, new strings added
- Added **Coming Soon** placeholders for future themes (Neon Pulse, Ocean Waves, Firestorm)
- Marked **Master Animation** theme as **Free**

## Build

```bash
./gradlew assembleDebug
```

All changes are production-ready and follow Material Design 3 dark theme.

