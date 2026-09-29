# 鸡场

Android 上的 **Clash 系列配置管理工具**。在手机上整理节点、订阅和分流规则，生成 Clash 系列 YAML 配置。

**[下载 Android APK](https://github.com/not-power/jichang/releases/latest)** · 支持 Android 13 及以上

## 主要功能

- **资源管理**：添加订阅、导入节点链接或扫码导入，保存和复用配置模板。
- **规则编辑**：管理分流规则、策略组、规则集与子规则；支持搜索、排序、批量操作和配置校验。
- **Clash 配置生成**：设置常用参数，也可编辑高级 YAML；导出前可校验并预览生成结果。
- **分享配置**：下载 YAML，或在局域网内通过链接和二维码分享。

## 界面

| 概览 | 资源 | 规则 | 分享 |
| :--: | :--: | :--: | :--: |
| <img src="app/screenshots/overview.png" width="180" alt="概览页面"> | <img src="app/screenshots/resources.png" width="180" alt="资源页面"> | <img src="app/screenshots/rules.png" width="180" alt="规则页面"> | <img src="app/screenshots/share.png" width="180" alt="分享页面"> |

## 快速上手

1. 在「资源」添加订阅、节点或模板。
2. 在「规则」调整分流规则并检查配置。
3. 在「分享」预览、下载 Clash YAML，或开启局域网分享。

当前以 **Mihomo（Clash.Meta）** 的配置格式为适配目标；其他 Clash 系列内核对协议和规则的支持可能不同。鸡场只负责生成配置，不内置代理内核或 VPN。扫码导入时会向系统申请相机权限。

项目采用 [MIT 许可证](LICENSE)。
