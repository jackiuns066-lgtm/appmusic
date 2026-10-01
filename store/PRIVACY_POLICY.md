# سیاست حفظ حریم خصوصی — برنامهٔ Novo (نوین‌وب)

**آخرین به‌روزرسانی:** مهر ۱۴۰۵ / September 2026
**نام برنامه:** Novo Music Player
**شناسهٔ بسته:** `ir.webnovo.novo`
**سازنده:** تیم نوین‌وب — <https://webnovo.ir>

## خلاصه در یک خط
Novo یک پخش‌کنندهٔ موسیقی **کاملاً آفلاین** است؛ هیچ اطلاعاتی از شما جمع‌آوری، ذخیره یا ارسال نمی‌شود.

## چه اطلاعاتی جمع‌آوری می‌کنیم؟
**هیچ.** برنامه هیچ حساب کاربری، ثبت‌نام، شمارهٔ تلفن، ایمیل، موقعیت مکانی، مخاطبین،
شناسهٔ تبلیغاتی یا شناسهٔ دستگاه را جمع‌آوری نمی‌کند.

## چه مجوزهایی می‌گیرد و چرا؟
| مجوز | دلیل |
|---|---|
| خواندن فایل‌های صوتی (`READ_MEDIA_AUDIO` / `READ_EXTERNAL_STORAGE`) | فقط برای پیدا کردن و پخش کردن آهنگ‌های موجود روی خود گوشی شما |
| اعلان (`POST_NOTIFICATIONS`) | فقط برای نمایش کنترل‌های پخش (قبلی، پخش/توقف، بعدی، نوار زمان) روی اعلان و صفحهٔ قفل |
| سرویس پیش‌زمینهٔ پخش موسیقی (`FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`) | تا موسیقی هنگام بسته‌شدن صفحه یا جابه‌جایی بین برنامه‌ها ادامه پیدا کند |
| `WAKE_LOCK` و `MODIFY_AUDIO_SETTINGS` | جلوگیری از خواب‌رفتن گوشی هنگام پخش و تنظیم افکت‌های صوتی (اکولایزر، باس‌بوست) |

## اینترنت
برنامه **مجوز اینترنت (`INTERNET`) ندارد**. حتی اگر بخواهیم هم نمی‌توانیم هیچ داده‌ای را
بیرون از گوشی ارسال کنیم. تنها استثنا، لینک‌هایی است که خودتان با انگشت لمس می‌کنید
(وب‌سایت نوین‌وب، صفحهٔ امتیاز دادن، دانلود نسخهٔ جدید)؛ این‌ها در **مرورگر یا فروشگاه**
باز می‌شوند، نه داخل برنامه.

## داده‌هایی که روی گوشی ذخیره می‌شوند
- فهرست آهنگ‌ها، پلی‌لیست‌ها، علاقه‌مندی‌ها و شمارندهٔ پخش‌ها: در پایگاه دادهٔ محلی (Room) داخل خود برنامه.
- تنظیمات (زبان، رنگ تم، حالت اکولایزر): در حافظهٔ محلی برنامه.
این داده‌ها **هرگز** از گوشی خارج نمی‌شوند و با حذف برنامه پاک می‌شوند.

## تبلیغات و خرید درون‌برنامه‌ای
برنامه هیچ تبلیغی ندارد، هیچ خرید درون‌برنامه‌ای ندارد و هیچ داده‌ای برای تبلیغات هدفمند
به شرکت‌های دیگر نمی‌دهد.

## کودکان
برنامه برای همهٔ سنین مناسب است و چون هیچ داده‌ای جمع نمی‌کند، اطلاعاتی از کودکان نیز ذخیره نمی‌شود.

## تغییرات این سیاست
اگر روزی قابلیتی اضافه شد که به اینترنت نیاز داشته باشد (مثل دریافت متن آهنگ)، این سیاست
به‌روزرسانی می‌شود و نسخهٔ تازه با انتشار بعدی برنامه در دسترس قرار می‌گیرد.

## تماس
ایمیل: <support@webnovo.ir> · وب‌سایت: <https://webnovo.ir>

---

# Privacy Policy — Novo Music Player

**Publisher:** Novin Web team — <https://webnovo.ir>
**Package:** `ir.webnovo.novo`

Novo is a fully offline music player. It collects **no** data whatsoever: no accounts, no
email, no phone number, no location, no contacts, no advertising identifiers.

- The media permissions are used only to find and play audio files stored on your device.
- The notification permission is used only to show playback controls in the notification
  shade and on the lock screen.
- The app does **not** request the `INTERNET` permission, so it cannot transmit any data.
  The only outbound links (our website, the store rating page, the download page) open in
  your browser or store app because you tapped them.
- Library data (tracks, playlists, favourites, play counts, settings) is stored locally in
  the app's own database and is deleted when you uninstall the app.
- No ads, no in-app purchases, no third-party analytics.

Contact: <support@webnovo.ir>
