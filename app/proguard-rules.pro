# Add project specific ProGuard rules here.
-keepattributes *Annotation*
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep llama.cpp JNI
-keep class com.mundovivo.llm.LlamaNativeBridge { *; }
