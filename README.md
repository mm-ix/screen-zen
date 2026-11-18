# ScreenZen（屏息）

Android 应用拦截 / 屏幕时间管理 App。

## 功能

- **拦截清单**：勾选需要限制的应用；命中后由无障碍服务检测到前台应用切换，先盖上一层全屏「呼吸倒计时」遮罩，数到 0 才放行
- **拦截总开关**：一键开启/关闭，关闭时所有应用一律放行
- **统计**：今日与累计「打开 / 放弃」次数、近 7 天柱状图、按 App 统计、今日使用时长（基于 UsageStats）
- **设置**：自定义呼吸倒计时秒数（0 秒 = 不出现遮罩）
- **数据导出**：CSV（Excel 可打开）/ `.db` 数据库备份
- **权限引导**：无障碍服务、悬浮窗、使用情况访问、电池优化等逐项检测与跳转

## 技术栈

- Java 原生 + Gradle，包名 `com.screenzen.app`
- minSdk 26 / targetSdk 34，Material 3 + viewBinding
- SQLite（`DbHelper`）本地存储拦截清单、打开事件与统计

## 主要模块

```
app/src/main/java/com/screenzen/app/
├── data/       # 数据库、DAO、数据模型、设置
├── export/     # CSV / DB 导出
├── service/    # 无障碍服务、悬浮遮罩、保活、开机广播
├── ui/         # 主页、拦截清单、设置、权限引导、图表/呼吸动画控件
├── usage/      # 使用时长统计
└── util/       # 包名、权限、时间工具
```

## 构建

```bash
JAVA_HOME="D:/Tools/SDK/JDK" ./gradlew assembleDebug --offline
```

产物：`app/build/outputs/apk/debug/app-debug.apk`。

> 本机 `PATH` 中没有 `java`，必须显式设置 `JAVA_HOME`；依赖已缓存在本地，使用 `--offline` 构建。

## 测试

- 真机：一加 8
- 目前只出 debug 包，无 release 签名
