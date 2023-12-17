# CatSpy

[![Build status](https://github.com/Gegenbauer/CatSpy/workflows/Build/badge.svg)](https://github.com/Gegenbauer/CatSpy/actions?query=workflow%3ABuild)
![GitHub contributors](https://img.shields.io/github/contributors/Gegenbauer/CatSpy)
![GitHub all releases](https://img.shields.io/github/downloads/Gegenbauer/CatSpy/total)
![GitHub release (latest by SemVer)](https://img.shields.io/github/downloads/Gegenbauer/CatSpy/latest/total)
![Latest release](https://img.shields.io/github/release/Gegenbauer/CatSpy.svg)
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

**Features to be implemented**
- Extend log format, support custom log format and filter

## Interface
### Log main interface
![log_main_interface.png](pic%2Flog_main_interface.png)

### Theme Settings
![theme_configuration.png](pic%2Ftheme_configuration.png)

## References
The project was initially based on [lognote](https://github.com/cdcsgit/lognote), and was completely refactored, with optimizations made to the log loading and rendering process,
Added the ability to build artifacts for various platforms, added software update functionality, added paginated display of logs.
The project structure is divided into multiple modules according to the level, and added modules such as cache, context, service, data binding, and logs

**Also referred to the following projects**

[darklaf](https://github.com/weisJ/darklaf)

[jadx](https://github.com/skylot/jadx)