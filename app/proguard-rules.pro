# WhatsSchedule Pro ProGuard Rules

# ---- Gson ----
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.whatsschedule.app.model.** { *; }
-keep class com.google.gson.** { *; }

# ---- Kotlin ----
-dontwarn kotlin.**
-keep class kotlin.Metadata { *; }
-keepclassmembers class **$WhenMappings { *; }

# ---- Keep receivers (referenced from manifest) ----
-keep class com.whatsschedule.app.alarm.** { *; }

# ---- Strip logs in release ----
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}
