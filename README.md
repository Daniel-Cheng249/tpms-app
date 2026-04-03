# TPMS 胎压监测 App

蓝牙胎压监测系统，支持 Bosch SMP290 传感器。

## 功能

- BLE 扫描和连接
- 4轮胎压/温度/电量实时显示
- 设备绑定管理

## 技术栈

- Kotlin + Jetpack Compose
- MVVM 架构
- DataStore 持久化

## 开发环境

- Android Studio 8.2.0
- JDK 17
- minSdk 26, compileSdk 34

## 快速开始

```bash
# 克隆项目
git clone https://github.com/Daniel-Cheng249/tpms-app.git

# 用 Android Studio 打开并构建
```

## 更新代码

双击 `push.bat` 或运行：

```bash
git add . && git commit -m "描述" && git push
```

## 项目结构

```
app/src/main/java/com/tpms/monitor/
├── ble/           # BLE 扫描和连接
├── data/          # 数据模型和存储
├── ui/            # Compose UI
└── MainActivity.kt
```
