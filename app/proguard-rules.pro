# Keep kotlinx.serialization metadata
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keep,includedescriptorclasses class com.example.deepseek.**$$serializer { *; }
-keepclassmembers class com.example.deepseek.** {
    *** Companion;
}
-keepclasseswithmembers class com.example.deepseek.** {
    kotlinx.serialization.KSerializer serializer(...);
}
