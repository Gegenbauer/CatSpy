# CatSpy

English | [简体中文](README_CN.md)

[![Build status](https://github.com/Gegenbauer/CatSpy/workflows/Build%20Artifacts/badge.svg)](https://github.com/Gegenbauer/CatSpy/actions/workflows/build_artifacts.yml?query=workflow%3ABuild)
![GitHub contributors](https://img.shields.io/github/contributors/Gegenbauer/CatSpy)
![GitHub all releases](https://img.shields.io/github/downloads/Gegenbauer/CatSpy/total)
![GitHub release (latest by SemVer)](https://img.shields.io/github/downloads/Gegenbauer/CatSpy/latest/total)
[![Latest release](https://img.shields.io/github/release/Gegenbauer/CatSpy.svg)](https://github.com/Gegenbauer/CatSpy/releases/latest)
[![License](http://img.shields.io/:license-apache-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)

**CatSpy** - A tool for browsing logs.

**Main Features:**
- Can filter logs based on various parts
- Import local file logs and view real-time logs of connected devices
- Real-time filtering with changing input content
- Multiple tabs
- Paginated display of logs to improve rendering speed
- Can add bookmarks to logs and filter logs added to bookmarks
- Integrated with FlatLaf, providing multiple themes to switch
- Supports multiple languages (Korean, Simplified Chinese, English)

## Comparison of Advantages: CatSpy vs Other Android Log Viewing Tools
| Advantage                  | CatSpy                                                       | Android Studio                                               | Text Readers (Notepad++, sublime, fleet) |
| -------------------------- | ------------------------------------------------------------ | ------------------------------------------------------------ | --------------------------------------- |
| Memory Usage               | Lighter than Android Studio, uses less memory, but due to the characteristics of JVM, it uses more memory than some pure text readers | \                                                            | \                                       |
| Filtering Method           | Supports individual filtering search for message, tag, pid, tid, log level. Text filters support forward matching and reverse matching, and conditions can be added in an OR manner | Unique filtering syntax, higher freedom in creating filters                       | Only supports text matching search                      |
| Support for Reading Real-time Device Logs | Supports reading real-time device logs and supports package name filtering                         | Supported                                                         | \                                       |
| Validity of Real-time Logs           | The captured real-time logs will be retained as long as they are not manually cleared, and all captured logs can be filtered at any time | Real-time logs often disappear after switching devices, or disappear for other unknown reasons, often leading to the need to reproduce logs | \                                       |
| Real-time Log Export     | Can export real-time logs, and the size of the exported logs depends on the memory of the device running the software. | The logs can be exported, but the exported files can only be viewed by importing into Android Studio, resulting in lower readability. | \                                       |
| Import File Logs             | Supports import, and the size limit of the file log also depends on the memory of the device running the software. | File logs can be imported, but only those exported from Android Studio in real-time are supported.                            | Can import file logs                          |

## Interface
### Log main interface
![log_main_interface.png](pic%2Flog_main_interface.png)

### Theme Settings
![theme_configuration.png](pic%2Ftheme_configuration.png)

## Download
latest release: [![Latest release](https://img.shields.io/github/release/Gegenbauer/CatSpy.svg)](https://github.com/Gegenbauer/CatSpy/releases/latest)

## Build && Run
### Environment
- JDK17 or higher
```bash
git clone git@github.com:Gegenbauer/CatSpy.git
cd CatSpy
./gradlew :app:packageDeb # build deb artifact
./gradlew :app:packageMsi # build msi artifact
./gradlew :app:packageDmg # build dmg artifact
./gradlew :app:run #run
```

## Download

## References
The project was initially based on [lognote](https://github.com/cdcsgit/lognote), and was completely refactored, with optimizations made to the log loading and rendering process,
Added the ability to build artifacts for various platforms, added software update functionality, added paginated display of logs.
The project structure is divided into multiple modules according to the level, and added modules such as cache, context, service, data binding, and logs

**Also referred to the following projects**

[darklaf](https://github.com/weisJ/darklaf)

[jadx](https://github.com/skylot/jadx)