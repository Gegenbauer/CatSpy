-keep class me.gegenbauer.catspy.** { *; }
-keep class javax.swing.** { *; }
-keep class com.formdev.flatlaf.** { *; }
-keep class io.netty.** { *; }
-keep class io.netty.util.internal.logging.** { *; }
-keep class org.slf4j.** { *; }
-keep class ch.qos.logback.** { *; }
-keep class org.apache.logging.log4j.** { *; }
-keep class org.fife.ui.autocomplete.** { *; }
-keep class kotlinx.coroutines.swing.** { *; }
-keep class com.github.weisj.** { *; }

# Gson uses generic type information stored in a class file when working with
# fields. Proguard removes such information by default, keep it.
-keepattributes Signature

# This is also needed for R8 in compat mode since multiple
# optimizations will remove the generic signature such as class
# merging and argument removal. See:
# https://r8.googlesource.com/r8/+/refs/heads/main/compatibility-faq.md#troubleshooting-gson-gson
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# Optional. For using GSON @Expose annotation
-keepattributes AnnotationDefault,RuntimeVisibleAnnotations
-keep class com.google.gson.reflect.TypeToken { <fields>; }
-keepclassmembers class **$TypeAdapterFactory { <fields>; }

-keep class io.vertx.core.** { *; }
-keepdirectories META-INF/
-keepdirectories META-INF.services/
-ignorewarnings
-keepattributes *Annotation*