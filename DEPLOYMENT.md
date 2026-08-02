# 🚀 دليل النشر الكامل — SnapLoad

## الجزء الأول: نشر السيرفر على Render.com (مجاني)

### المتطلبات
- حساب GitHub (مجاني)
- حساب Render.com (مجاني)

### الخطوات

#### 1. رفع الكود على GitHub
```bash
# إنشاء مستودع جديد على GitHub
git init
git add .
git commit -m "Initial commit: SnapLoad server"
git branch -M main
git remote add origin https://github.com/yourusername/snapload-server.git
git push -u origin main
```

#### 2. إنشاء حساب Render.com
1. اذهب إلى [render.com](https://render.com)
2. اضغط "Get Started for Free"
3. سجّل بحساب GitHub
4. اربط حسابك مع GitHub

#### 3. إنشاء Web Service جديد
1. من لوحة تحكم Render، اضغط **New → Web Service**
2. اختر مستودع `snapload-server`
3. اضغط **Connect**

#### 4. إعداد الـ Service
```
Name:          snapload-api
Region:        Frankfurt (EU Central) أو Oregon (US West)
Branch:        main
Runtime:       Python 3
Build Command: pip install --upgrade pip && pip install -r requirements.txt && yt-dlp -U
Start Command: gunicorn main:app --bind 0.0.0.0:$PORT --workers 2 --timeout 120
Plan:          Free
```

#### 5. Environment Variables
أضف هذه المتغيرات في قسم "Environment":
```
PYTHON_VERSION = 3.11.0
```

#### 6. تفعيل Auto-Deploy
- في إعدادات الـ Service، فعّل **Auto-Deploy: Yes**
- سيُنشر السيرفر تلقائياً عند كل push على `main`

#### 7. انتظر الـ Build
- الـ Build يأخذ 2-5 دقائق
- انتظر حتى تظهر الرسالة: **Your service is live 🎉**
- احفظ الرابط: `https://snapload-api.onrender.com`

#### 8. اختبر السيرفر
```bash
curl https://snapload-api.onrender.com/ping
# النتيجة المتوقعة: {"status": "ok", "message": "SnapLoad API is running 🚀"}
```

---

## الجزء الثاني: تفعيل UptimeRobot (مجاني) — إبقاء السيرفر يقظاً 24/7

الخطة المجانية في Render.com تُوقف السيرفر بعد 15 دقيقة من الخمول.
UptimeRobot يضربه كل 5 دقائق ليبقى يقظاً.

### الخطوات

1. **إنشاء حساب** على [uptimerobot.com](https://uptimerobot.com) (مجاني)

2. **إضافة Monitor جديد:**
   - اضغط **+ Add New Monitor**
   - النوع: **HTTP(s)**
   - Friendly Name: `SnapLoad API`
   - URL: `https://snapload-api.onrender.com/ping`
   - Monitoring Interval: **5 minutes**
   - اضغط **Create Monitor**

3. **إعداد تنبيهات البريد الإلكتروني:**
   - اذهب إلى **My Settings → Alert Contacts**
   - اضغط **Add Alert Contact**
   - النوع: **E-mail**
   - أدخل بريدك الإلكتروني
   - احفظ

4. **ربط التنبيه بالـ Monitor:**
   - ارجع للـ Monitor
   - في قسم **Alert Contacts**: اختر البريد الذي أضفته
   - احفظ

> **ملاحظة:** الخطة المجانية في UptimeRobot تتيح 50 monitor و5 دقائق interval — كافٍ تماماً.

---

## الجزء الثالث: بناء APK للإصدار

### 1. توليد Keystore (مرة واحدة فقط)
```bash
keytool -genkey -v \
  -keystore app/snapload.keystore \
  -alias snapload \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000

# ستُطلب منك:
# - كلمة مرور الـ keystore
# - اسمك، المنظمة، المدينة، الدولة
# - كلمة مرور الـ key (يمكن نفس كلمة مرور الـ keystore)
```

> ⚠️ **مهم جداً:** احتفظ بنسخة من `snapload.keystore` وكلمة المرور في مكان آمن.
> فقدان الـ keystore يعني عدم القدرة على تحديث التطبيق على Google Play.

### 2. إعداد متغيرات البيئة
```bash
export KEYSTORE_PASS="your_keystore_password"
export KEY_PASS="your_key_password"
```

### 3. بناء Release APK
```bash
./gradlew assembleRelease
# APK في: app/build/outputs/apk/release/app-release.apk
```

### 4. أو AAB لـ Google Play (مستحسن)
```bash
./gradlew bundleRelease
# AAB في: app/build/outputs/bundle/release/app-release.aab
```

### 5. تحقق من الحجم والتوقيع
```bash
# التحقق من التوقيع
jarsigner -verify -verbose app/build/outputs/apk/release/app-release.apk

# فحص الحجم
ls -lh app/build/outputs/apk/release/
```

---

## الجزء الرابع: إعداد GitHub Actions Secrets

لكي يعمل CI/CD ويبني APK تلقائياً:

1. اذهب إلى مستودعك على GitHub
2. **Settings → Secrets and variables → Actions**
3. اضغط **New repository secret** وأضف:

| الاسم | القيمة |
|-------|--------|
| `KEYSTORE_BASE64` | `base64 app/snapload.keystore` (شغّل هذا الأمر وانسخ الناتج) |
| `KEYSTORE_PASS` | كلمة مرور الـ keystore |
| `KEY_PASS` | كلمة مرور الـ key |

```bash
# توليد KEYSTORE_BASE64
base64 app/snapload.keystore
# أو على macOS:
base64 -i app/snapload.keystore
```

---

## تحديث رابط السيرفر في التطبيق

بعد الحصول على رابط Render.com، غيّره في `app/build.gradle.kts`:

```kotlin
buildConfigField(
    "String",
    "API_BASE_URL",
    "\"https://YOUR-APP-NAME.onrender.com\""
)
```

ثم أعد بناء التطبيق.
