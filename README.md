# RoutePilot / 路线助手

[下载最新版 APK](downloads/RoutePilot-1.4.0.apk) · [使用方法](docs/使用方法.md) · [版本说明](docs/版本说明.md)

当前版本：1.4.0（versionCode 5）。与之前交付版本使用相同开发签名，可直接覆盖安装。

### 1.4.0 阅读确认、地图与定位状态

- 首次使用时，必须在免责声明正文滑到末尾，才可勾选「理解免责声明和使用须知」并进入。确认保存在本机，之后启动直接进入地图；主界面仍可重新查看说明。清除应用数据或卸载后需要重新确认。
- 增加全屏地图、中心准星选点、比例尺、中心坐标显示和最高 22 级缩放。超过地图源原生级别只放大已有瓦片，不增加实际地物细节。天地图道路／卫星影像仍需自备有效 Key。
- 底部固定显示开发者选项和本应用设置入口。使用标准安卓入口并提供后备入口；系统未开放对应页面时仍需手动查找，未逐品牌验证。
- 增加「预定位」：在路线起点发送静止位置，不累计里程与时间，点击「继续」开始移动。增加已发送位置组数，不能据此推断第三方已接受。
- 自检区分未提供精度字段与 0 m，标记自检前的缓存位置；明确自检结束后保留结果的数据年龄会继续增加。模拟位置回调不代表真实 GNSS 卫星状态。

启动页门槛、地图源和界面数据检查通过；APK 编译、签名验证通过。未进行荣耀 MagicOS 11 / Android 17 真机或运动世界 7.4.0 的运行验证；不保证解决第三方 GPS 弱提示。

### 1.3.0 启动说明

新增本地 `startup.html` / `startup.css` / `startup.js`，展示经确认的免责声明原文（`assets/disclaimer.txt`）和八部分使用方法。启动时经阅读确认后才进入地图主界面；主界面新增重新查看入口。界面重建保留当前活动的确认状态，状态按本版本绑定；初始启动页不请求在线地图。若在未确认的启动页选择退出，会同时停止仍在运行的模拟服务。

“影梭位置格式兼容（实验）”的界面名称统一调整为“卫星位置兼容”；实现与默认关闭状态保持不变，相关提示继续说明不增强真实卫星信号。历史版本说明中的旧名称仅用于记录历史。

320 / 412 px 浏览器预览、页签切换、确认控制、返回主界面和重新查看入口验证通过；未进行安卓真机启动与退出测试。

### 1.2.0 定位兼容与自检

修正 GPS 与 network 测试 provider 的属性；加入默认关闭的影梭格式兼容模式（100 ms 更新、模拟精度字段 1 / 2 m、GPS 附加 satellites=7），加入 30 秒前台系统回调自检。附加数字不代表实际卫星数，也不生成 GNSS 回调。参考来源和 GPLv3 许可见 NOTICE.md、LICENSE。

标准模式每秒更新；兼容模式每 100 ms 更新，因此耗电和系统调用次数可能增加。两种模式均保留 Android 模拟位置标记。自检只观察本应用收到的回调，不代表运动世界接受位置；离开本页面会结束自检。

本版编译、签名、数据契约检查通过；未进行 Android 真机及运动世界测试。

新增 `LocationDiagnostics.java`：监听本应用收到的位置 / GNSS 回调，不虚构测量结果。

数据契约测试：`node tests/ui-contract.test.cjs`。

### 1.1.0 地图更新

- 地图源下拉菜单和重新加载按钮。
- 自动模式无 Key 时先尝试 OSM France / HOT，再尝试标准 OSM；有有效格式的 Key 时先尝试天地图道路图。
- 首次加载或重新请求的地图 12 秒无成功响应时切换下一源；每轮只尝试有限次，避免无限重试。
- 可手动选择天地图道路／卫星图，叠加对应中文注记。必须提供自己有权使用的地图服务 Key。
- Key 仅保存在本机的应用配置中，并在请求天地图时发送给天地图；不进入 GPX、模拟服务启动参数、状态提示或交付源码。
- 安卓端增加连接诊断；日志提示只传递主机名和错误码，不传递含 Key 的请求 URL。
- 界面预览已确认 HOT 备用底图在开发电脑网络上加载；未验证用户手机网络，未以有效 Key 验证天地图请求。

原生 Java + 本地 WebView 界面的安卓路线模拟定位应用。最低 Android 8.0 / API 26，目标 API 36，无 CPU 架构专属库。荣耀 Magic 8 Pro 及其他机型的实际运行表现未验证。

## 功能

- OpenStreetMap 地图点击选点、WGS84 坐标输入和路线预览。
- 单程／闭环循环、固定速度、目标距离停止。
- 前台定位服务、通知栏暂停／继续／停止、运行期间部分唤醒锁。
- GPX 导入导出和本机配置保存。
- 使用公开的 `LocationManager.addTestProvider` / `setTestProviderLocation`。保留系统模拟位置标记。

此应用不与运动世界直接通信，不包含运动世界账号流程，不保证模拟位置被其他应用接受或计入成绩。

## 构建方式一：Android Studio

打开本目录作为 Gradle 工程。安装 Android SDK Platform 36，使用支持当前 Android Gradle Plugin 的 Gradle／JDK 环境。Gradle 配置使用 AGP 8.10.1，Java 源码兼容级别为 8；建议使用 JDK 21。

本源码包未附带 Gradle Wrapper，也未验证 Gradle 构建；本次交付 APK 使用下述直接构建方式完成。

## 构建方式二：不使用 Gradle（本次已验证）

需要 Python 3.8+、JDK 21、Android SDK Platform 36 和 SDK Build Tools 36.0.0。工具可通过 Android Studio 的 SDK Manager 安装；使用官方工具时遵守相应许可。

```powershell
python build.py --jdk "C:\path\to\jdk-21" --platform "C:\path\to\Android\Sdk\platforms\android-36\android.jar" --tools "C:\path\to\Android\Sdk\build-tools\36.0.0"
```

也可通过 `JAVA_HOME` 指定 JDK 并省略 `--jdk`。构建会依次调用 aapt2、javac、D8、zipalign 和 apksigner；APK 输出到项目目录的上一级，文件名为 `RoutePilot-1.4.0.apk`。

默认中间文件及开发密钥保存在 `build-local/`，可通过 `--work-dir` 指定其他路径。脚本生成的是开发签名包，密码是公开的开发用默认值，不应作为生产发布密钥。本源码包不含本次 APK 的私有签名密钥。

## 路线引擎测试

```powershell
javac -encoding UTF-8 -d build-tests app/src/main/java/cn/routepilot/app/RouteEngine.java tests/RouteEngineTest.java
java -cp build-tests RouteEngineTest
```

## 主要源码

- `MainActivity.java`：本地页面、权限、开发者设置、真实定位和 GPX 文件交互。
- `RouteService.java`：前台服务、模拟 GPS／网络位置、生命周期和通知控制。
- `RouteEngine.java`：纯 Java 距离和路线插值。
- `assets/index.html` / `app.js`：移动端界面、地图选点、配置和状态展示。

## 已知边界

- 未进行安卓设备安装及运行测试；Android 8.0+ 是编译目标范围，不是逐机型认证。
- 其他应用可以识别模拟位置；没有隐藏该标记的功能。
- 只注入 GPS / network 测试 provider；不同设备／应用的位置融合行为可能不同。
- 进程被结束后不自动重启或恢复路线，避免无人知晓地继续模拟；真实位置可能需要重新获取。
- 暂停保留运行服务、唤醒锁和当前位置；停止才释放。循环且目标为 0 时需手动停止。
- GPX 多段轨迹会按文件顺序相连。纬度限制为 ±85°，以匹配在线地图可显示范围。
- 所有在线底图都需要联网；离线坐标／GPX 仍可用。未内置地名搜索或道路路线规划。

## 第三方资源

Leaflet 1.9.4（BSD-2-Clause），许可见 `app/src/main/assets/LEAFLET-LICENSE.txt`。Leaflet 已内置于 APK，加载地图无需从 CDN 下载脚本。

在线地图 © [OpenStreetMap contributors](https://www.openstreetmap.org/copyright)，遵循 [OSM 瓦片使用政策](https://operations.osmfoundation.org/policies/tiles/)。没有瓦片批量下载或离线地图预取功能。

备用地图由 [OSM France](https://www.openstreetmap.fr/usage/) 托管，HOT 风格；当前免费、非营利、低流量个人使用符合其所列使用条件。商业分发或规模化使用前需换用获授权的相应服务。所有服务均不保证可用。

天地图接入参照其[官方地图服务文档](https://lbs.tianditu.gov.cn/server/MapService.html)，使用 HTTPS WMTS Web Mercator（`_w`）接口。Key 的类型、权限、配额和应用限制以天地图控制台要求为准；不要混用其他平台或无权限的 Key。

地图源控制测试：`node tests/map-controller.test.cjs`。本次验证覆盖超时、有限次切换、缺少 Key、切换后的旧事件隔离、部分加载、清理和署名；APK 构建、签名验证通过。安卓真机与运动世界有效性仍未验证。
