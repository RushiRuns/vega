# ProGuard rules for Vega

# Keep Room database classes
-keep class androidx.room.** { *; }
-keepattributes Exceptions

# Keep Hilt-generated classes
-keep class com.google.dagger.hilt.** { *; }
-keep class dagger.** { *; }

# Keep Kotlin metadata
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepattributes Signature

# Keep coroutine code
-keep class kotlin.coroutines.** { *; }

# Keep Model classes
-keep class com.vega.data.database.** { *; }

# Remove logging in release builds (optional)
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}
