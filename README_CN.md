# CatSpy

[![Build status](https://github.com/Gegenbauer/CatSpy/workflows/Build/badge.svg)](https://github.com/Gegenbauer/CatSpy/actions?query=workflow%3ABuild)
![GitHub contributors](https://img.shields.io/github/contributors/Gegenbauer/CatSpy)
![GitHub all releases](https://img.shields.io/github/downloads/Gegenbauer/CatSpy/total)
![GitHub release (latest by SemVer)](https://img.shields.io/github/downloads/Gegenbauer/CatSpy/latest/total)
![Latest release](https://img.shields.io/github/release/Gegenbauer/CatSpy.svg)
[![License](http://img.shields.io/:license-apache-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)

**CatSpy** - 一个用于浏览日志的工具。

**主要特性：**
- 能针对日志的各个部分进行过滤
- 导入本地文件日志和查看已连接设备实时日志
- 随输入内容变化的实时过滤
- 多标签页
- 日志分页显示，提高渲染速度
- 可以为日志添加书签，并过滤显示添加到书签的日志
- 接入 FlatLaf，提供多种主题供切换
- 支持多语言（韩语、简体中文、英语）

**待实现的特性**
- 扩展日志格式，支持自定义日志格式及过滤器

## 界面
### 日志
![log_main_interface.png](pic%2Flog_main_interface.png)

### 主题设置
![theme_configuration.png](pic%2Ftheme_configuration.png)

## 参考
项目最初基于[lognote](https://github.com/cdcsgit/lognote)，对其进行了完全重构，对日志加载和渲染流程进行了优化，
功能上增加了构建各平台产物的能力，增加了软件更新功能，增加了日志分页显示功能。
项目结构上根据层级划分为了多个模块，并增加了缓存、上下文、服务、数据绑定，日志等模块
另外还参考了以下项目

[darklaf](https://github.com/weisJ/darklaf)

[jadx](https://github.com/skylot/jadx)