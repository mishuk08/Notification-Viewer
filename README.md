# 📱 All Notification Viewer
 
> A simple and powerful Android app to view, manage, and track all your notifications in one place.
 
---
 
## 👨‍💻 Developer
 
**Mishuk Babu Shoukhin**
Android App Developer | Content Creator | Digital Entrepreneur
 
---
 
## 📖 About the App
 
**All Notification Viewer** is an Android utility app that logs and displays all incoming notifications from every app installed on your device. Never miss an important notification again — even if you accidentally dismissed it!
 
---
 
## ✨ Features
 
- 🔔 View all incoming notifications in real-time
- 📂 Organize notifications by app
- 🗑️ Delete individual or all notifications
- 🔍 Search through notification history
- 🕒 Timestamp for each notification
- 📵 Works even in Do Not Disturb mode
- 🌙 Dark mode support
- 🔒 100% offline — no internet required
---
 
## 📲 How to Use
 
1. Install the app on your Android device
2. Grant **Notification Access** permission when prompted
3. All incoming notifications will be automatically saved
4. Open the app anytime to browse your full notification history
---
 
## 🛠️ Tech Stack
 
| Technology | Details |
|---|---|
| Language | Java / Kotlin |
| Platform | Android (API 21+) |
| Database | SQLite / Room |
| Architecture | MVVM |
| UI | XML Layouts + Material Design |
 
---
 
## 📋 Requirements
 
- Android 5.0 (Lollipop) or higher
- Notification Access Permission (required)
- Storage Permission (optional, for export)
---
 
## 🚀 Installation
 
### From Source
 
```bash
# Clone the repository
git clone https://github.com/mishukbabushoukhin/all-notification-viewer.git
 
# Open in Android Studio
# Build → Generate Signed APK
```
 
### Direct APK
 
Download the latest APK from the [Releases](https://github.com/mishukbabushoukhin/all-notification-viewer/releases) section.
 
---
 
## 📁 Project Structure
 
```
AllNotificationViewer/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   └── com.mishuk.notificationviewer/
│   │   │   │       ├── MainActivity.java
│   │   │   │       ├── NotificationService.java
│   │   │   │       ├── NotificationAdapter.java
│   │   │   │       └── DatabaseHelper.java
│   │   │   ├── res/
│   │   │   │   ├── layout/
│   │   │   │   ├── drawable/
│   │   │   │   └── values/
│   │   │   └── AndroidManifest.xml
├── README.md
└── build.gradle
```
 
---
 
## 🔐 Permissions
 
```xml
<uses-permission android:name="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
```
 
---
 
## 📸 Screenshots
 
> _Screenshots will be added soon._
 
---
 
## 🤝 Contributing
 
Contributions are welcome! Feel free to open issues or submit pull requests.
 
1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request
---
 
## 📄 License
 
```
MIT License
 
Copyright (c) 2025 Mishuk Babu Shoukhin
 
Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software.
```
 
---
 
## 📬 Contact
 
- **Developer:** Mishuk Babu Shoukhin
- **GitHub:** [github.com/mishukbabushoukhin](https://github.com/mishuk08)
- **Facebook:** [facebook.com/mishukbabushoukhin](https://facebook.com/mishuk008)
---
 
<p align="center">Made with ❤️ by <strong>Mishuk Babu Shoukhin</strong></p>
