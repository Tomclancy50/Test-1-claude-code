# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class com.familywifisync.** {
    *** Companion;
}
-keepclasseswithmembers class com.familywifisync.** {
    kotlinx.serialization.KSerializer serializer(...);
}
