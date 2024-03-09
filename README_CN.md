# CatSpy

[English](README.md) | 简体中文

[![Build status](https://github.com/Gegenbauer/CatSpy/workflows/Build%20Artifacts/badge.svg)](https://github.com/Gegenbauer/CatSpy/actions/workflows/build_artifacts.yml?query=workflow%3ABuild)
![GitHub contributors](https://img.shields.io/github/contributors/Gegenbauer/CatSpy)
![GitHub all releases](https://img.shields.io/github/downloads/Gegenbauer/CatSpy/total)
![GitHub release (latest by SemVer)](https://img.shields.io/github/downloads/Gegenbauer/CatSpy/latest/total)
[![Latest release](https://img.shields.io/github/release/Gegenbauer/CatSpy.svg)](https://github.com/Gegenbauer/CatSpy/releases/latest)
[![License](http://img.shields.io/:license-apache-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)

**CatSpy** - 一个用于浏览安卓日志的工具。

**主要特性：**
- 针对日志的各个部分进行过滤，例如 message，tag，pid，tid，log level 等
- 导入本地文件日志和查看已连接设备实时日志
- 随输入内容变化的实时过滤
- 多标签页，同时查看多个日志
- 日志分页显示，提高渲染速度
- 可以为日志行添加书签，方便查看和过滤
- 接入 FlatLaf，提供多种主题供切换
- 支持多语言（韩语、简体中文、英语）

## 相对于其他 Android 日志查看工具的优势
| 优势                     | CatSpy                                                       | Android Studio                               | 文本阅读器（Notepad++, sublime, fleet） |
| ------------------------ | ------------------------------------------------------------ |----------------------------------------------| --------------------------------------- |
| 内存占用                 | 相比 Android Studio 更轻量，占用内存更小，但由于 JVM 的特性，相对于一些纯粹的文本阅读器，占用内存还是更大的 | \                                            | \                                       |
| 过滤方式                 | 支持 message，tag，pid，tid，log level 等单独过滤搜索。文本类过滤器支持正向匹配和反向匹配，支持条件以或的形式叠加 | 特有的过滤语法，创建过滤器的自由度更高                          | 只支持文本匹配搜索                      |
| 是否支持读取设备实时日志 | 支持读取实时设备日志，并支持包名过滤                         | 支持                                           | \                                       |
| 实时日志有效期           | 抓取到的实时日志只要不手动清空会一直保留，随时可以对抓取到的所有日志进行过滤 | 实时日志经常在切换设备后消失，或者由于其他不知名原因消失，经常导致需要重复复现来抓取日志 | \                                       |
| 实时日志是否支持导出     | 可导出实时日志，且支持导出的日志大小取决于运行该软件的设备内存。 | 可导出日志，但导出的文件只能通过导入到 Android Studio 查看，可读性较低  | \                                       |
| 导入文件日志             | 支持导入，而且文件日志大小的限制也取决于运行该软件的设备内存。 | 可导入文件日志，只支持导入通过 Android Studio 导出的实时日志文件     | 可导入文件日志                          |

## 局限性
只支持导入标准 logcat 格式的日志文件，不支持导入其他格式的日志文件
即日志文件的格式应该是这样的
```bash
03-02 11:36:21.389   466   466 D BootAnimation: /product/media/bootanimation.zip is loaded successfully
```
后续将支持支持自定义解析规则

## 界面
### 日志
![log_main_interface.png](pic%2Flog_main_interface.png)

### 主题设置
![theme_configuration.png](pic%2Ftheme_configuration.png)

## 下载
最新版本: [![Latest release](https://img.shields.io/github/release/Gegenbauer/CatSpy.svg)](https://github.com/Gegenbauer/CatSpy/releases/latest)

## 启动应用
### 使用安装包安装后打开
#### macOS
在 macOS 上，由于应用未签名，可能会提示无法打开，此时按如下操作打开

1. 打开 dmg 文件，将应用拖到应用程序文件夹
2. launchpad 中找到应用，打开应用
3. 关闭弹出的警告框
  
  ![macOS_open_warning.png](pic%2FmacOS_open_warning.png)
4. 打开系统设置 -> 隐私与安全 -> 找到安全 -> 点击打开
  
  ![macOS_grant_open_permission.png](pic%2FmacOS_grant_open_permission.png)
5. 再次打开应用

#### Linux
安装好 deb 后，可以在应用菜单中找到应用图标，点击打开应用

#### Windows
安装好 msi 后，可以在应用菜单中找到应用图标，点击打开应用
但需要注意，安装目录最好不要放在 C 盘，因为 C 盘有可能会有权限问题

### 使用 jar 包运行
```bash
java -jar CatSpy-${version}.jar
```

## 构建和运行
### 环境
- JDK17 及以上
```bash
git clone git@github.com:Gegenbauer/CatSpy.git
cd CatSpy
./gradlew :app:packageDeb # 构建 deb 包
./gradlew :app:packageMsi # 构建 msi 包
./gradlew :app:packageDmg # 构建 dmg 包
./gradlew :app:run #运行
```

## 支持
项目最初基于[lognote](https://github.com/cdcsgit/lognote)，对其进行了完全重构，对日志加载和渲染流程进行了优化，
功能上增加了构建各平台产物的能力，增加了软件更新功能，增加了日志分页显示功能。
项目结构上根据层级划分为了多个模块，并增加了缓存、上下文、服务、数据绑定，日志等模块

另外还参考了以下项目

[darklaf](https://github.com/weisJ/darklaf) 

[jadx](https://github.com/skylot/jadx)

![JetBrains Logo (Main) logo](https://resources.jetbrains.com/storage/products/company/brand/logos/jb_beam.svg)
