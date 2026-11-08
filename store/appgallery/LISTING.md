# KeyAura - Huawei AppGallery listing (v1.1.10)

Copy each block into the AppGallery Connect form. Limits are AppGallery's.

## App name (max 30)
KeyAura Keyboard

## Brief introduction (max 80)
Animated keyboard with themes, Urdu layout, clipboard and a built-in game.

## Full introduction (max 8000)
KeyAura is a free keyboard for Android with smooth animated themes, a full Urdu layout and a clipboard history.

Typing features
- Full QWERTY keyboard with numbers, shift, backspace and a clear Enter key
- Native Urdu script keyboard (اردو layout)
- Emoji panel with search and recent emojis
- Clipboard history panel with a 2-column grid
- Voice typing with the microphone key
- Swipe gesture tunes with musical notes

Look and feel
- Animated themes with per-key colour bursts
- 8 solid themes: Midnight, Forest, Wine, Slate, Royal, White, Grey, Black
- Custom photo background from your gallery
- Portrait and landscape layouts

Fun
- Birdy Bird mini-game built into the keyboard

Free to use
- All typing features are free
- Some animated themes, swipe tunes and the mini-game unlock for 12 hours after you watch a short rewarded video ad. If no video is available, a 1-hour unlock is given while you are online.
- A banner ad is shown on the main screen. Ads are shown by Start.io (StartApp).
- You choose whether ads are personalized the first time you open the app.

Privacy
- KeyAura does not store what you type or send it anywhere. Typing is handled on your device.
- Ads are shown by Start.io, which may collect device identifiers and ad interaction data. See the privacy policy in the app (About > Privacy).

## New features (max 1000)
- Animated themes with per-key colour bursts
- Urdu layout, clipboard history grid, emoji search
- Birdy Bird mini-game
- Landscape keyboard kept at a comfortable size
- Ads consent question on first open

## Category and rating
- Category: Tools (or Input method, whichever AppGallery lists for keyboards; check the list)
- Age rating: everyone. The app has ads, so answer the content-rating questions as "contains ads".

## Ads disclosure (must be in the listing)
This app contains ads. Do not write "no ads" or "ad-free" anywhere: not in the description, not in screenshots, not in the update notes.

## Permissions (AppGallery asks for a reason for each sensitive one)
- RECORD_AUDIO: used only for the microphone key (voice typing). Audio is handled by the device speech service when you tap the mic key.
- AD_ID: used by the ad network to show ads, and to limit personalized ads when the user chooses that.
- INTERNET and ACCESS_NETWORK_STATE: to show ads and to check if the device is online before a rewarded unlock.
- VIBRATE: key vibration when enabled in settings.

## Privacy policy
- Must be a live URL (Blogger or your site). Use the text in the app: About > Privacy.
- Data safety / data declaration must match the app: it shows ads (device identifiers, ad interaction), and it does not collect typed text.

## Screenshots (8, at least 3, 800x450 or 450x800 at minimum)
1. Keyboard in portrait with an animated theme
2. Urdu layout
3. Clipboard panel (colour cards)
4. Emoji panel
5. Themes screen with locked and unlocked themes
6. Birdy Bird game
7. Landscape keyboard
8. Settings or the ads consent screen
Do not show any text that says "no ads".

## Before you press Submit
- [ ] Package name is com.oklabs.keyaura on the AppGallery app (matches build.gradle)
- [ ] Signing key: the same key you use in GitHub secrets (SHA-256 added to AppGallery Connect)
- [ ] agconnect-services.json in app/ if Huawei Ads Kit is used
- [ ] Privacy policy URL live and matches the app text
- [ ] Listing does not say "no ads"
- [ ] "Zero keystroke logging" claim verified (see note below)
- [ ] Tested on a real Huawei phone: portrait, landscape, rewarded unlock, offline popup

## Note on claims
The About page says typed text is never stored or sent. Check the code path that handles typed text (suggestions, clipboard, speech) before publishing this sentence. If anything is stored or sent, change the line.
