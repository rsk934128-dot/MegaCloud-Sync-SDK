# 🛡️ AppPulse - Cloud Storage & Security Vault for Android

AppPulse হলো একটি অত্যাধুনিক এবং সুরক্ষিত অ্যান্ড্রয়েড অ্যাপ্লিকেশন যা Jetpack Compose, Firebase Authentication, Cloud Storage Vault এবং APK Security Scanner সমন্বয়ে তৈরি।

---

## 📥 সরাসরি APK ডাউনলোড করার নিয়ম (Download APK)

### পদ্ধতি ১: GitHub Releases থেকে ডাউনলোড (সরাসরি ইনস্টল)
১. গিটহাব রিপোজিটরির ডানপাশের **Releases** সেকশনে যান।
২. সর্বশেষ রিলিজ (যেমন: `v1.0.0`) নির্বাচন করুন।
৩. **Assets** সেকশন থেকে `AppPulse-v1.0.apk` ফাইলটিতে ক্লিক করে সরাসরি ফোনে ডাউনলোড করুন।
৪. আপনার অ্যান্ড্রয়েড ফোনে ফাইলটি ওপেন করে **Install** বাটনে চাপুন।

### পদ্ধতি ২: GitHub Actions আর্টফ্যাক্টস থেকে ডাউনলোড
১. গিটহাবের **Actions** ট্যাবে ক্লিক করুন।
২. **Build & Release Android APK** ওয়ার্কফ্লোটি সিলেক্ট করুন।
৩. সর্বশেষ সফল বিল্ডে ক্লিক করুন।
৪. পেজের নিচে **Artifacts** সেকশনে থাকা `AppPulse-Android-APK` জিপ ফাইলটি ডাউনলোড করে এক্সট্রাক্ট করুন।

---

## 🚀 গিটহাবে নতুন রিলিজ ও এপিকে তৈরি করার নিয়ম (Triggering a New Release)

### গিটহাব Actions দিয়ে ১-ক্লিকে রিলিজ তৈরি:
১. রিপোজিটরির **Actions** ট্যাবে যান।
২. বামপাশে **Build & Release Android APK** সিলেক্ট করুন।
৩. ডানপাশে থাকা **Run workflow** বাটনে ক্লিক করুন।
৪. রিলিজ ট্যাগ (যেমন: `v1.0.1`) দিয়ে **Run workflow** নিশ্চিত করুন।
৫. ২-৩ মিনিটের মধ্যে স্বয়ংক্রিয়ভাবে বিল্ড হয়ে **Releases** পেজে এপিকে ফাইল যুক্ত হয়ে যাবে।

### গিট ট্যাগ দিয়ে রিলিজ তৈরি:
```bash
git tag v1.0.0
git push origin v1.0.0
```
ট্যাগ পুশ করলেই গিটহাব অ্যাকশন স্বয়ংক্রিয়ভাবে APK কম্পাইল করে রিলিজ প্রকাশ করবে।

---

## 🛠️ লোকাল ডিভাইসে বিল্ড করার নিয়ম (Local Build)

যদি নিজের কম্পিউটারে সোর্স কোড থেকে APK তৈরি করতে চান:

```bash
# রিপোজিটরি ক্লোন করুন
git clone <your-repo-url>
cd <repo-folder>

# পরিবেশ প্রস্তুত করুন
chmod +x gradlew
base64 -d debug.keystore.base64 > debug.keystore

# APK তৈরি করুন
./gradlew assembleDebug
```
বিল্ড সম্পন্ন হলে এপিকে ফাইলটি পাওয়া যাবে:  
`app/build/outputs/apk/debug/app-debug.apk`

---

## ✨ অ্যাপের প্রধান ফিচারসমূহ
- 🔐 **Google Sign-In & Auth:** বিশ্বস্ত গুগল ক্রেডেনশিয়াল ও ফায়ারবেস অথেন্টিকেশন।
- 📁 **Cloud & Local Storage Vault:** SAF সিস্টেম ফাইল পিকার, অফলাইন ক্যাশিং ও প্রিভিউ।
- 🎙️ **Transparent Audio Memo Vault:** স্বচ্ছ ভয়েস রেকর্ডিং ও ইন-অ্যাপ প্লেয়ার।
- 🛡️ **APK Threat Scanner:** প্যাকেজ পারমিশন ও সিকিউরিটি অ্যানালাইসিস।
- 🎨 **Modern Cyber & OLED Dark Theme:** ডায়নামিক কালার এবং ম্যাটেরিয়াল ডিজাইন ৩।
