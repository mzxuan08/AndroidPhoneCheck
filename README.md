# AndroidPhoneCheck（安卓验机）

一款面向二手手机验机的纯 Android 端应用，支持 Android 9–16。检测在本机完成，不依赖云端服务。

## 主要功能

- 设备、CPU、内存、存储、电池和屏幕参数采集
- 沉浸式全屏颜色检测与多点触控轨迹测试
- 前后摄像头预览，以及清晰度、曝光、偏色、噪点、冻结和画面稳定度辅助判断
- 麦克风录音回放、实时波形、信噪比、削波、断音和 FFT 语音频段辅助判断
- 扬声器左右声道与 200–4000 Hz 扫频测试
- 光线、距离、加速度、陀螺仪、磁力计等传感器逐项引导测试
- Wi-Fi、蓝牙、NFC、定位、SIM 和移动网络状态
- 音量键、振动、闪光灯、生物识别、USB、存储和安全风险检查
- 离线保存本次验机结果和算法依据

## 隐私

相机帧只在内存中分析。录音保存在应用临时目录，退出音频测试后删除。应用不会上传照片、录音或设备信息。

## 构建

项目使用 Kotlin、Jetpack Compose、CameraX 和 Gradle。使用 JDK 17 与 Android SDK 36：

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
```

Debug APK 输出到 `app/build/outputs/apk/debug/app-debug.apk`。

## 说明

算法结果用于辅助筛查，不能替代维修仪器或人工判断，也不能证明零部件是否原装。不同厂商对系统信息和权限的开放程度不同，无法读取的项目会显示为不支持或权限受限。
