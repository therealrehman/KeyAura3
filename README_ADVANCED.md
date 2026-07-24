# KeyAura – Advanced Keyboard

This update transforms KeyAura into a feature-rich, modern keyboard with:

## New Features
- **Gesture Typing**: Swipe across keys for fast typing (togglable)
- **Cursor Control**: Swipe on spacebar to move cursor left/right
- **Text Expansion**: Define shortcuts (e.g., "omw" → "On my way!")
- **Number Row Toggle**: Show/hide numbers above keyboard
- **Keyboard Height Adjust**: Slider in settings (30%–50% of screen)
- **Key Popup**: Large character preview on key press
- **Smart Backspace**: Swipe left on backspace to delete entire word
- **Polished UI**: Gradient background, smooth animations, modern icons

## Settings
- All new features are configurable from the Settings screen.
- Categories: General, Typing, Appearance, Themes.

## Performance
- Optimized dictionary (trie) for fast suggestion
- Lazy loading of assets
- Smooth 60fps rendering

## Build & Install
```bash
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

Enjoy the new KeyAura!

