# Hilt
-keep @dagger.hilt.EntryPoint interface *
-keep @dagger.hilt.components.SingletonComponent interface *
-keep @dagger.hilt.InstallIn interface *

# Hilt Workers (Global safety)
-keep @androidx.hilt.work.HiltWorker class * extends androidx.work.ListenableWorker
-keepclassmembers class * extends androidx.work.ListenableWorker {
    @dagger.assisted.AssistedInject <init>(android.content.Context, androidx.work.WorkerParameters);
}

# WorkManager
-keep class * extends androidx.work.InputMerger {
    public <init>();
}
-keep class androidx.work.OverwritingInputMerger {
    public <init>();
}

# Jetpack AppFunctions
# @AppFunction methods are aggregated by the appfunctions KSP compiler into
# assets/app_functions_v2.xml and are discovered/invoked reflectively at runtime. If R8
# renames the enclosing classes or these methods, the platform indexer can no longer map
# that generated metadata back to real code, so the functions become unreadable after a
# minified (release) upload to Play.
#
# IMPORTANT: the annotation is androidx.appfunctions.AppFunction. The previous rule targeted
# androidx.appfunctions.service.AppFunction (wrong package), so it matched nothing and the
# release build shipped fully obfuscated functions — the cause of the Play rejection.
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations,AnnotationDefault,Signature,InnerClasses,EnclosingMethod

# Keep any class that declares an @AppFunction method, plus those methods (names intact).
-keepclasseswithmembers class * {
    @androidx.appfunctions.AppFunction <methods>;
}
-keepclassmembers class * {
    @androidx.appfunctions.AppFunction *;
}

# The @AppFunction holder class is registered by name via AppFunctionConfiguration
# and referenced by the generated metadata — keep it (and its members) verbatim.
-keep class com.jolabs.looplog.habit.appfunctions.** { *; }

# Keep the KSP-generated inventory/invoker/registry classes the runtime loads reflectively.
-keep class **_AppFunctionInventory_Impl { *; }
-keep class **_AppFunctionInvoker_Impl { *; }
-keep class androidx.appfunctions.service.internal.AggregatedAppFunctionInvoker* { *; }
-keep class androidx.appfunctions.service.internal.$AggregatedAppFunctionInvoker_Impl { *; }

# Kotlinx Serialization (Global safety)
-keep @kotlinx.serialization.Serializable class * { *; }
-keepclassmembers class * {
    @kotlinx.serialization.SerialName *;
}

# General Enum Safety
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Preservation of @Keep annotation
-keep @androidx.annotation.Keep class * { *; }
-keep @androidx.annotation.Keep class * {
    @androidx.annotation.Keep *;
}
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}

# HiltWorker Factory
-keep class * extends androidx.hilt.work.HiltWorkerFactory { *; }
-keep class **_HiltModules* { *; }
-keep class **_AssistedFactory { *; }
-keep class **_AssistedFactory_Impl { *; }

# Sentry
-keepattributes LineNumberTable,SourceFile
-keepattributes Annotation
-keep class io.sentry.** { *; }
-dontwarn io.sentry.**

# Maintain line numbers for crash reporting
-keepattributes SourceFile,LineNumberTable

# Play In-App Review / play-services-base reference an annotation not on the classpath.
-dontwarn com.google.android.gms.common.annotation.NoNullnessRewrite
