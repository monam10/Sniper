# SnapLoad 📥

<p align="center">
  <img src="screenshots/logo.png" alt="SnapLoad Logo" width="120"/>
</p>

<p align="center">
  <b>تحميل مقاطع الفيديو والصوت بجودة عالية من أشهر المنصات</b><br/>
  تطبيق Android مفتوح المصدر، مجاني 100%، بدون إعلانات
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Android-7.0%2B-brightgreen"/>
  <img src="https://img.shields.io/badge/Kotlin-1.9-blue"/>
  <img src="https://img.shields.io/badge/Material%20You-3-orange"/>
  <img src="https://img.shields.io/badge/License-MIT-lightgrey"/>
</p>

---

## 📱 لقطات الشاشة

| الرئيسية | اختيار الجودة | التحميلات | الإعدادات |
|-----------|---------------|-----------|-----------|
| ![Home](screenshots/home.png) | ![Quality](screenshots/quality.png) | ![Downloads](screenshots/downloads.png) | ![Settings](screenshots/settings.png) |

---

## 🌍 المنصات المدعومة

| المنصة | الأيقونة | الدعم |
|--------|----------|-------|
| YouTube | 🔴 | فيديو + صوت + تنسيقات متعددة |
| Instagram | 🟣 | Reels + Posts + Stories |
| TikTok | ⚫ | فيديوهات + بدون علامة مائية |
| Twitter / X | 🔵 | فيديوهات التغريدات |
| Facebook | 🔵 | Watch + Reels |
| Dailymotion | 🔵 | فيديو + صوت |
| Vimeo | 🔵 | جودة عالية |
| Reddit | 🟠 | فيديوهات وGIF |
| Pinterest | 🔴 | فيديوهات |
| SoundCloud | 🟠 | صوت فقط |
| Twitch | 🟣 | Clips + VODs |
| Bilibili | 🔵 | فيديو + صوت |
| OK.ru | 🟠 | فيديو |
| VK | 🔵 | فيديو |

---

## ✨ المميزات

- ✅ تحميل فيديو وصوت بجودات متعددة (4K، 1080p، 720p...)
- ✅ رصد الحافظة تلقائياً — الصق الرابط وسيتعرف عليه
- ✅ واجهة عربية RTL + إنجليزية
- ✅ وضع داكن / فاتح / تلقائي (Material You)
- ✅ إشعارات ذكية مع شريط تقدم
- ✅ سجل التحميلات مع بحث وفلترة
- ✅ مشاركة الملفات المحمّلة
- ✅ مشاركة من أي تطبيق (Share Sheet)
- ✅ قفل التطبيق ببصمة الإصبع أو PIN
- ✅ ودجت للشاشة الرئيسية
- ✅ بدون إعلانات، بدون تسجيل

---

## 🏗️ البنية التقنية

```
SnapLoad/
├── app/                          # تطبيق Android (Kotlin)
│   └── src/main/java/com/snapload/app/
│       ├── data/                 # Layer: Data
│       │   ├── db/               # Room Database
│       │   ├── model/            # Data classes
│       │   ├── network/          # Retrofit + OkHttp
│       │   └── repository/       # Repositories
│       ├── ui/                   # Layer: UI
│       │   ├── home/             # HomeFragment + ViewModel
│       │   ├── downloads/        # DownloadsFragment + ViewModel + Adapter
│       │   ├── quality/          # QualityBottomSheet + Adapter
│       │   ├── settings/         # SettingsFragment
│       │   ├── share/            # ShareHandlerActivity
│       │   └── lock/             # AppLock (Biometric/PIN)
│       ├── service/              # DownloadService + DownloadWorker
│       ├── notifications/        # NotificationHelper
│       ├── clipboard/            # ClipboardMonitor
│       ├── history/              # HistoryManager
│       ├── storage/              # StorageManager
│       ├── network/cache/        # NetworkCacheInterceptor
│       ├── widget/               # DownloadWidget
│       └── utils/                # Constants, Extensions, etc.
└── server/                       # Python Flask API (Render.com)
    ├── main.py
    ├── requirements.txt
    └── render.yaml
```

### المكتبات المستخدمة

| المكتبة | الإصدار | الغرض |
|---------|---------|-------|
| Retrofit2 | 2.9.0 | HTTP Client |
| OkHttp3 | 4.12.0 | Network Layer |
| Glide | 4.16.0 | تحميل الصور |
| Room | 2.6.1 | قاعدة البيانات المحلية |
| WorkManager | 2.9.0 | المهام في الخلفية |
| Navigation Component | 2.7.7 | التنقل بين الشاشات |
| Coroutines | 1.7.3 | البرمجة غير المتزامنة |
| yt-dlp | Latest | استخراج روابط الفيديو (السيرفر) |

---

## 🚀 تثبيت التطبيق

### طريقة 1: تنزيل APK مباشرة
1. اذهب إلى [Releases](../../releases)
2. حمّل آخر ملف `snapload-release.apk`
3. فعّل "تثبيت من مصادر غير معروفة" في إعدادات Android
4. ثبّت الملف

### طريقة 2: البناء من المصدر
```bash
git clone https://github.com/yourusername/SnapLoad.git
cd SnapLoad

# بناء Debug APK
./gradlew assembleDebug

# APK في:
# app/build/outputs/apk/debug/app-debug.apk
```

---

## 🖥️ بناء وتشغيل السيرفر

### محلياً للاختبار:
```bash
cd server
chmod +x setup.sh
./setup.sh
```

### على Render.com (مجاني):
انظر [تعليمات النشر الكاملة](DEPLOYMENT.md)

---

## 🔧 متطلبات التطوير

- Android Studio Hedgehog 2023.1.1+
- JDK 17+
- Android SDK 34
- Python 3.11+ (للسيرفر)

---

## 🤝 المساهمة

1. Fork المستودع
2. أنشئ branch جديد: `git checkout -b feature/amazing-feature`
3. اعمل التعديلات وأضفها: `git commit -m 'Add amazing feature'`
4. ادفع للـ branch: `git push origin feature/amazing-feature`
5. افتح Pull Request

---

## 📄 الترخيص

هذا المشروع مرخص تحت رخصة MIT — انظر ملف [LICENSE](LICENSE) للتفاصيل.

---

## ⚠️ إخلاء المسؤولية

هذا التطبيق مخصص للاستخدام الشخصي فقط. يُرجى احترام حقوق النشر وشروط الاستخدام لكل منصة. المطور غير مسؤول عن أي استخدام غير قانوني.

---

<p align="center">صُنع بـ ❤️ لمجتمع المصادر المفتوحة العربي</p>
