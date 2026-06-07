# Keep kotlinx.serialization generated serializers for the shared models.
-keepclassmembers class com.familywifisync.shared.model.** {
    *** Companion;
}
-keepclasseswithmembers class com.familywifisync.shared.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}
