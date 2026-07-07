# GitHub Actions 自定义签名配置

为避免每次编译的 APK 签名不一致导致覆盖安装失败，工作流现在使用固定的 release 签名构建。

## 前置准备

### 1. 生成签名密钥（如已有请跳过）

```bash
keytool -genkey -v -keystore my-release-key.keystore -alias my-key-alias -keyalg RSA -keysize 2048 -validity 10000
```

记录以下信息：
- **Keystore 密码**
- **Key 别名 (Alias)**
- **Key 密码**

### 2. 将 Keystore 转为 Base64

```bash
base64 my-release-key.keystore > keystore.base64
```

复制 `keystore.base64` 文件内容，然后到 GitHub 仓库设置中创建 Secrets。

### 3. 在 GitHub 仓库中设置 Secrets

进入仓库 **Settings → Secrets and variables → Actions → Secrets**，添加以下 4 个 Secrets：

| Secret 名称 | 说明 |
|-------------|------|
| `KEYSTORE_BASE64` | Keystore 文件的 Base64 编码内容 |
| `KEYSTORE_PASSWORD` | Keystore 密码 |
| `KEY_ALIAS` | 签名别名 |
| `KEY_PASSWORD` | Key 密码 |

## 工作流行为

- 使用 `./gradlew assembleRelease` 构建 **Release APK**
- `debug` 构建类型也复用同一签名配置，方便本地调试与 CI 产物签名一致
- 构建产物路径：`app/build/outputs/apk/release/*`
