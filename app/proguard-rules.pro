# Portal Ani — R8 / ProGuard rules for release builds.
# Keep this file minimal: rely on library consumer rules where possible.

# Readable stack traces in Play Console / logcat.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# BuildConfig (AniList OAuth credentials injected at compile time).
-keep class com.portal.portalani.BuildConfig { *; }

# Manifest-declared components (DreamService screensaver, OAuth, boot receiver).
-keep class com.portal.portalani.MainActivity { *; }
-keep class com.portal.portalani.AnimeDreamService { *; }
-keep class com.portal.portalani.AniListOAuthActivity { *; }
-keep class com.portal.portalani.BootReceiver { *; }
-keep class com.portal.portalani.PortalAniApplication { *; }

# WorkManager periodic screensaver guard.
-keep class com.portal.portalani.ScreensaverGuardWorker { *; }

# org.json parsing (AniList GraphQL responses, cache serialization).
-keep class org.json.** { *; }

# OkHttp / Okio (consumer rules ship with the library; silence optional platform warnings).
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# Coil image loading.
-dontwarn coil.**

# Compose runtime keeps @Composable implementations via default R8 rules when referenced from UI.
