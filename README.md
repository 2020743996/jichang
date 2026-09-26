# 鸡场

原生 Android 订阅与节点管理器，面向 Mihomo 配置。应用只管理数据并生成 Mihomo YAML，不内置代理内核，也不接管 Android VPN。

## 当前功能

- 本机添加和刷新 HTTP(S) 订阅；在设备上解析 Base64、Mihomo/Clash YAML、常见节点分享 URI 与基础 Surge 节点行。
- 概览、资源、配置三个主入口；模板库收纳在配置内。可创建、复制、切换、重命名和删除多个配置。
- 订阅和节点库共享；每份配置独立选择订阅、节点、规则、地区分组和导出选项。
- 节点支持搜索、来源/协议/启用状态筛选、批量启停和编辑。
- 规则页分为规则、策略组和规则集三个分区，支持搜索、类型筛选、编辑、排序和删除；以可视化表单编辑 Mihomo 路由类型、GEOSITE、自定义模板、规则集提供者、组合条件与子规则；MATCH 兜底固定最后。
- 自动按地区筛选节点并生成策略组；导出可内嵌节点或引用 Mihomo 订阅。
- 配置页可预览、下载或局域网分享当前生成的 Mihomo YAML。
- 模板页可从 HTTP(S) 链接下载 Mihomo YAML 并保存到本机模板库；可以预览、重命名、导出或基于模板创建可编辑配置。模板中的 DNS、TUN 及其他未托管顶层字段会在生成时保留，远程 URL 不会保存。
- 局域网分享使用当前配置与导出选项，支持随时停止；悬浮导航与快捷操作使用 LiquidGlass Compose 材质的局部背景采样、折射、高光及可调透明度，配置正文保持清晰可读。
- 应用图标为原创“小鸡啄米”自适应图标，配色独立于应用主题。
- 所有数据通过 Room 保存在本机，不启用云同步或第三方转换服务。

目前 URI 解析覆盖 SS、VMess、VLESS、Trojan、Hysteria、Hysteria 2、TUIC、AnyTLS、SOCKS5 和 HTTP。YAML 输入保留节点字段；无法识别的行会计入跳过数。配置生成器仅输出一份 Mihomo 格式，不按 Clash 系 App 分流。

## 获取应用

请从 GitHub Releases 下载并安装 Android APK。本仓库发布应用源码与安装包，不附带构建脚本。

鸡场 0.6.0 起要求 Android 13（API 33）或更高版本，以启用 Android 图形着色语言（AGSL）玻璃渲染。

本项目纳入 [LiquidGlass](https://github.com/Abdullajon1881/LiquidGlass) 的 Core 与 Compose 源码（上游提交 `72ad05c49628ea2270116b2943629cd81c0cc496`），按 Apache-2.0 许可证分发；许可文本与来源说明见 [`third_party`](third_party/README.md)。

## 局域网分享

每次开启分享都会生成新的随机路径令牌；链接只绑定当前局域网接口，停止分享或系统关闭服务后失效。链接本身是访问凭据，请只发给可信对象。传输为 HTTP，适用于可信局域网；不应在公共 Wi-Fi 上分享。
