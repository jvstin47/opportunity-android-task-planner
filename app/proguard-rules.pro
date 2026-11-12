# Room and local entities
-keep class com.opportunity.app.data.local.** { *; }
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# API interfaces
-keep interface com.opportunity.app.sync.SyncApi { *; }
-keep interface com.opportunity.app.ai.AnthropicApi { *; }
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**

# Glance widgets & receivers
-keep class com.opportunity.app.widget.** { *; }

# kotlinx.serialization
-keepclassmembers class kotlinx.serialization.internal.** { *; }
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}
-keepclassmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
