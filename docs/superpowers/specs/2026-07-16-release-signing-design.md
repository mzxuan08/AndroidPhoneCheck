# AndroidPhoneCheck 1.0 发布签名设计

## 目标

为 AndroidPhoneCheck 生成独立的长期发布签名，构建可安装的 1.0.0 release APK，并发布到 GitHub。

## 密钥与密码

- 使用 JDK `keytool` 生成 RSA 4096 位、有效期 10,000 天的 JKS 密钥库。
- 密钥别名固定为 `androidphonecheck`。
- 密钥库位于项目 F 盘的 `.signing/androidphonecheck-release.jks`。
- 自动生成随机强密码，写入 `.signing/keystore.properties`，不在终端或提交记录中显示。
- `.signing/` 整体加入 `.gitignore`，密钥、密码和证书私密材料不得上传 GitHub。
- 用户需要自行备份整个 `.signing` 目录；丢失后无法使用相同签名升级已安装应用。

## Gradle 配置

`app/build.gradle.kts` 在本地签名配置存在时读取 storeFile、storePassword、keyAlias 和 keyPassword，并把该配置用于 release 构建。配置缺失时给出明确错误，不回退为调试签名。

## 构建与发布

- 运行单元测试、release APK 构建和 lint。
- 使用 Android SDK 工具校验 APK 签名和证书摘要。
- 将签名 APK 命名为 `AndroidPhoneCheck-1.0.0.apk`。
- 提交安全的 Gradle 与 `.gitignore` 改动并推送 GitHub。
- 创建或更新 `v1.0.0` GitHub Release，上传签名 APK；不上传 `.signing` 目录。

## 验收

- Git 工作区不追踪任何密钥或密码文件。
- release APK 签名校验成功，版本为 1.0.0 / versionCode 8。
- GitHub Release 公开可访问并包含签名 APK。
