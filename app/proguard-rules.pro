# Keep Room entities and DAOs
-keep class com.ankitsaini.securevault.data.** { *; }
-keepclassmembers class com.ankitsaini.securevault.data.** { *; }

# Keep Compose components
-dontwarn androidx.compose.**
-keep class androidx.compose.** { *; }

# Keep Kotlin coroutines
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# Keep Biometric API classes
-keep class androidx.biometric.** { *; }

# Keep CameraX classes
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# Keep accessibility service
-keep class com.ankitsaini.securevault.services.** { *; }

# Keep data classes
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}
