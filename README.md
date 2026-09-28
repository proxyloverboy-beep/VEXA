# VEXA

A mobile voice-assistant project for Android.

## Current build
- Hindi/English speech recognition using Android SpeechRecognizer
- Hindi voice responses using Android TextToSpeech
- Phone temperature monitoring with spoken heat warning
- Battery percentage command
- Voice commands to open Camera, WhatsApp, YouTube and Chrome
- Voice commands for Wi-Fi and Bluetooth settings
- Voice command to open the phone dialer
- Dark professional VEXA interface

## Example commands
- "VEXA phone kitna garam hai"
- "VEXA battery kitni hai"
- "VEXA camera kholo"
- "VEXA WhatsApp kholo"
- "VEXA YouTube kholo"
- "VEXA Wi-Fi settings kholo"
- "VEXA Bluetooth kholo"
- "VEXA call karo 9876543210"
- "VEXA iska matlab kya hai"

## Voice security
Speech recognition is not the same as speaker verification. This build does **not** claim that it can reliably distinguish the owner's voice from another person's voice. A proper owner-only voice lock needs speaker verification/voice embeddings and secure on-device storage.

## Security limits
VEXA will not bypass the Android lock screen. Commands that open other apps/settings use Android's normal security model.

## Build
Open this Android project in an Android-capable IDE/build environment and build the `app` module.
