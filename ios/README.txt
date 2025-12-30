NoMoreScamTW iOS Extension
========================

This folder contains the source code for the Safari Web Extension.

To run this on an iOS device (iPhone/iPad):

1.  **Prerequisites**:
    *   A Mac computer running macOS.
    *   Xcode installed (free from the Mac App Store).
    *   An Apple ID (free developer account is sufficient for local testing).

2.  **Conversion Steps**:
    *   Open Terminal on your Mac.
    *   Run the following command:
        `xcrun safari-web-extension-converter /path/to/NoMoreScamTW`
        (Replace `/path/to/NoMoreScamTW` with the actual path to the folder containing `manifest.json` inside this directory).
    *   Xcode will launch and prompt you to create a new project.
    *   Select "Swift" as the language.
    *   Once the project is verified, you can select a simulator or a connected physical device to run the app.

3.  **Notes**:
    *   The `manifest.json` is based on Manifest V3, which is supported by Safari on iOS 15+.
    *   The `background.js` (Service Worker) is supported.
    *   The fraud database update logic runs in the background. Note that iOS Safari imposes stricter resource limits on background scripts compared to Chrome Desktop.
