# occupies 6.7mb
# CatSpy Start
-keep class me.gegenbauer.catspy.view.combobox.FilterComboBox { *; }
-keep class me.gegenbauer.catspy.view.combobox.FilterComboBoxModel { *; }
# CatSpy End

# occupies 9.3mb
# Netty Start
-keepclassmembernames class io.netty.buffer.AbstractByteBufAllocator {
    *;
}
-keepclassmembernames class io.netty.buffer.AdvancedLeakAwareByteBuf {
    *;
}
-keep public class io.netty.util.ReferenceCountUtil {
    *;
}
# Netty End

# Slf4j Start
-keep class org.slf4j.** { *; }
# Slf4j End

# occupies 2.2mb
# AutoComplete Start
-keepclassmembers class org.fife.ui.autocomplete.AutoCompletion {
    private AutoCompletePopupWindow popupWindow;
}
# AutoComplete End

# occupies 3.4mb
-keep class ch.qos.logback.** { *; }
# occupies 8mb
-keep class org.apache.logging.log4j.** { *; }

# occupies 4.9mb
# KotlinX Start
-keep class kotlinx.coroutines.** { *; }
# KotlinX End

# occupies 19mb
# Kotlin Start
# Keep Kotlin lambda classes and methods
-keep class **$Lambda$** { *; }
# Keep Kotlin metadata
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
# Keep essential Kotlin classes
-keep class kotlin.Metadata { *; }
# Keep directories
-keepdirectories META-INF/
-keepdirectories META-INF.services/
# Kotlin End

# occupies 13mb
# FlatLaf Start
-keep class com.formdev.flatlaf.** { *; }
# FlatLaf End

# occupies 11mb
# Darklaf Start
-keep class com.github.weisj.** { *; }
# Darklaf End

# occupies 5.7mb
# Vertx Start
-keep class io.vertx.core.** { *; }
# Vertx End

# Gson Start
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
# Gson End

# occupies 260kb
# Okio Start
-keep class okio.** { *; }
# Okio End

# Commons Start
# Ignore warnings
-ignorewarnings
# Keep attributes
-keepattributes *Annotation*
# Commons End