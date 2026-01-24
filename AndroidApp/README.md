# CLTDIY Android App

This is the Android port of the CLTDIY iOS app.

## How to Build

1. Open this folder (`AndroidApp`) in **Android Studio**.
2. Sync Project with Gradle Files.
3. Build and Run on an Emulator or Device.

## Features

- **AI Chat**: Uses OpenAI (ChatGPT) and Google Gemini.
- **Speech**: Voice input (SpeechRecognizer) and Output (TextToSpeech).
- **DIY Helper**: Take photos of DIY problems to ask AI.
- **Real Estate Analysis**: Property value analysis helper.
- **Web Content**: Browsing specific local information.

## API Keys

You need to set your OpenAI and Gemini API keys in the app settings (Gear icon) to use the AI features.

## Testing Speech on Simulator (Error 7 Troubleshooting)

If you encounter "No speech match found (Error 7)" on the Android Emulator, please try the following:

1. **Verify Microphone Access**:
   - In the Emulator toolbar, click the **Three Dots (...)** -> **Microphone**.
   - Ensure the microphone is toggled **ON** and your computer's mic is selected.
2. **Google Speech Services**:
   - Simulators often do not include the necessary Google Speech Services or have them disabled.
   - Go to **Settings -> Google -> Settings for Google apps -> Search, Assistant & Voice -> Voice**. Ensure "Offline speech recognition" includes Chinese (simplified).
3. **Use Text Fallback**:
   - We have added a **Text Input field** at the bottom of the "AI 对话" screen. This is the recommended way to test AI logic if voice input is unavailable on your simulator.
4. **Physical Device**:
   - For a full experience with voice, running the app on a **Physical Android Device** is highly recommended.
