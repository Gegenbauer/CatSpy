# CatSpy

English | [简体中文](README_CN.md)

[![Build status](https://github.com/Gegenbauer/CatSpy/workflows/Build%20Artifacts/badge.svg)](https://github.com/Gegenbauer/CatSpy/actions/workflows/build_artifacts.yml?query=workflow%3ABuild)
![GitHub contributors](https://img.shields.io/github/contributors/Gegenbauer/CatSpy)
![GitHub all releases](https://img.shields.io/github/downloads/Gegenbauer/CatSpy/total)
![GitHub release (latest by SemVer)](https://img.shields.io/github/downloads/Gegenbauer/CatSpy/latest/total)
[![Latest release](https://img.shields.io/github/release/Gegenbauer/CatSpy.svg)](https://github.com/Gegenbauer/CatSpy/releases/latest)
[![License](http://img.shields.io/:license-apache-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)

**CatSpy** - A tool designed for log browsing.

**Key Features:**
- Filter various parts of the log, such as message, tag, pid, tid, log level, etc.
- Customize log types, log parsers, and log color schemes
- Import local file logs and view real-time logs from connected Android devices
- Real-time filtering as you type
- Multiple tabs to view multiple logs simultaneously
- Record recently viewed file logs for easy access
- Paginated log display to improve rendering speed
- Add bookmarks to log lines for easy viewing and filtering
- Integrate with FlatLaf to provide multiple themes for switching
- Support multiple languages (Korean, Simplified Chinese, English)

## Advantages Compared to Other Android Log Viewing Tools
| Advantage                              | CatSpy                                                       | Android Studio                               | Text Readers (Notepad++, sublime, fleet) |
|----------------------------------------| ------------------------------------------------------------ |----------------------------------------------| --------------------------------------- |
| Memory Usage                           | Lighter than Android Studio, consuming less memory. However, due to JVM characteristics, it may still consume more memory compared to some pure text readers. | \                                            | \                                       |
| Filtering Method                       | Supports individual filtering for message, tag, pid, tid, log level, etc. Text filters support both forward and reverse matching, with conditions combinable using OR logic. | Unique filtering syntax in Android Studio provides greater flexibility in filter creation.                       | Only supports text matching search                      |
| Real-time Device Log Reading Support   | Enables real-time device log reading and supports package name filtering.                         | Supported                                           | \                                       |
| The validity period of Real-time logs. | Captured real-time logs persist until manually cleared, allowing filtering at any time. | Real-time logs often disappear after device switching or due to other unknown reasons, often requiring log reproduction. | \                                       |
| Real-time Log Export                   | Allows exporting real-time logs, with exported file size dependent on the device's memory. | Logs can be exported, but the exported files can only be viewed by importing into Android Studio, resulting in reduced readability.  | \                                       |
| File Log Import                        | Supports importing file logs, with size limitations based on the device's memory. | File logs can be imported, but only those exported from Android Studio in real-time are supported.                            | File logs can be imported                          |

## Interface
### Home Page
![home_page.png](pic%2Fhome_page.png)

### Log Main Interface
![log_main_interface.png](pic%2Flog_main_interface.png)

### Log Customization Panel
![log_customization_dialog.png](pic%2Flog_customization_dialog.png)

### Theme Settings
![theme_configuration.png](pic%2Ftheme_configuration.png)

## Download
Latest Release: [![Latest release](https://img.shields.io/github/release/Gegenbauer/CatSpy.svg)](https://github.com/Gegenbauer/CatSpy/releases/latest)

## Launching the Application
### Running After Installation from Packages
#### macOS
On macOS, due to the application being unsigned, you may encounter an error indicating it cannot be opened. In such cases, follow these steps to open it:

1. Open the dmg file and drag the application to the Applications folder.
2. Find the application in the launchpad and open it.
3. Dismiss the warning box that appears.
   
   ![macOS_open_warning.png](pic%2FmacOS_open_warning.png)
4. Navigate to System Preferences -> Privacy & Security -> Security and click "Open".
   
   ![macOS_grant_open_permission.png](pic%2FmacOS_grant_open_permission.png)
5. Attempt to open the application again.

#### Linux
After installing the .deb package, you can find the application icon in the application menu. Click on it to open the application.

#### Windows
After installing the .msi package, you can find the application icon in the application menu. Click on it to open the application. 

However, please note that it's advisable not to install the application in the C drive as it may encounter permission issues.

### Running with JAR File
```bash
java -jar CatSpy-${version}.jar
```

## Build and Run
### Environment
- JDK17 or higher
```bash
git clone git@github.com:Gegenbauer/CatSpy.git
cd CatSpy
./gradlew :app:packageDeb # Build deb artifact
./gradlew :app:packageMsi # Build msi artifact
./gradlew :app:packageDmg # Build dmg artifact
./gradlew :app:run # Run
```

## Support
The project was initially based on [lognote](https://github.com/cdcsgit/lognote) and underwent complete refactoring, with optimizations made to the log loading and rendering processes. It introduced the ability to build artifacts for various platforms, software update functionality, and paginated log display. The project structure is divided into multiple modules according to hierarchy, including cache, context, service, data binding, and logs modules.

Additionally, the project referenced the following:

[darklaf](https://github.com/weisJ/darklaf)

[jadx](https://github.com/skylot/jadx)

![JetBrains Logo (Main) logo](https://resources.jetbrains.com/storage/products/company/brand/logos/jb_beam.svg)