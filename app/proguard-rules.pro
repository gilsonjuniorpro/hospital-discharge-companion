# Keep Kotlin serialization classes for typed Compose navigation routes
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.hospitaldischargecompanion.core.navigation.**$$serializer { *; }
-keepclassmembers class com.hospitaldischargecompanion.core.navigation.** {
    *** Companion;
}
