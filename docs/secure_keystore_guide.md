# Android Keystore 安全設定指南

您詢問關於上架 Play Store 時，`keystore` (金鑰庫) 檔案應該放在哪裡才不會被偷。以下是透過 `local.properties` 進行保護的最佳實踐。

## 1. Keystore 檔案該放哪裡？
**絕對不要** 放在 `android/app/src/...` 裡面，以防不小心被 Git 紀錄。
**建議做法**：放在專案外部的安全資料夾，或是放在 `android/` 根目錄下，但**必須確保它被 Git 忽略**。

我已經更新了您的 `.gitignore` 檔案，現在它會自動忽略所有 `.jks` 或 `.keystore` 結尾的檔案。因此，您可以放心地將您的 `release.jks` 檔案放在 `/Users/jack/Downloads/NoMoreScamTW/android/` 目錄下。

## 2. 如何設定密碼？ (不要寫在程式碼裡)
不要將密碼寫死在 `build.gradle.kts` 裡，因為那樣別人從 Git 上就看得到了。
請將密碼設定在 `local.properties` 檔案中。這個檔案預設就不會被上傳到 Git，非常安全。

請開啟 `/Users/jack/Downloads/NoMoreScamTW/android/local.properties`，並加入以下 4 行：

```properties
storeFile=/Users/jack/Downloads/NoMoreScamTW/android/release.jks
storePassword=您的金鑰庫密碼
keyAlias=您的金鑰別名
keyPassword=您的金鑰密碼
```

*   `storeFile`: 金鑰庫檔案的路徑 (如果是放在 android 目錄下，可以直接寫檔名)。
*   `storePassword` / `keyPassword`: 您當初建立金鑰時設定的密碼。

## 3. 如何打包 (Build)
設定完成後，您可以透過以下方式產生簽名的 Release APK/Bundle：
- **Android Studio**: Build > Generate Signed Bundle / APK
- **Command Line**: `./gradlew assembleRelease`

我已經幫您修改好 `build.gradle.kts`，它會自動讀取上述設定。如果 `local.properties` 裡沒有這些資訊，它只會導致無法簽名，但絕不會洩漏您的密碼。
