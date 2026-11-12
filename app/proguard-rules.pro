# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Retrofit
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions

# OkHttp
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**

# Gson
-keep class com.google.gson.** { *; }
-keepattributes *Annotation*
-keep class sun.misc.Unsafe { *; }
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keep @interface com.google.gson.annotations.SerializedName

# Glide
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep public class * extends com.bumptech.glide.module.LibraryGlideModule
-keep class com.bumptech.glide.GeneratedAppGlideModuleImpl
-keep @interface com.bumptech.glide.annotation.GlideModule
-keep @interface com.bumptech.glide.annotation.GlideOption
-keep @interface com.bumptech.glide.annotation.GlideType

# Keep data models
-keep class com.svd.svdagencies.data.model.** { *; }
-keep class com.svd.svdagencies.data.api.** { *; }

# Keeping all classes in the data package just in case
-keep class com.svd.svdagencies.data.** { *; }

# AndroidX and standard rules
-keepattributes SourceFile,LineNumberTable
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# Location models are serialized by Gson and passed to the bundled map.
-keep class com.svd.svdagencies.utils.AgentPosition { *; }
-keep class com.svd.svdagencies.utils.AgentPositions { *; }
-keep class com.svd.svdagencies.utils.LocationRequestState { *; }
-keep class com.svd.svdagencies.utils.LocationRequestEnvelope { *; }
-keep class com.svd.svdagencies.utils.PublicTrackingShare { *; }
