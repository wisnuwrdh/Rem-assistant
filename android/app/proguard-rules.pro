# Keep kotlinx.serialization & Live2D JavascriptInterface methods
-keepattributes *Annotation*
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keepclassmembers class com.ecosystemku.assistant.data.network.** {
    *;
}
