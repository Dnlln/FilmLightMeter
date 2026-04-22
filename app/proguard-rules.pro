# FilmLightMeter ProGuard rules
-keep class androidx.camera.** { *; }
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}
