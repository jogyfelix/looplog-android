# Jetpack Glance Action Callbacks
# These are instantiated by the Glance library using reflection.
-keep class * implements androidx.glance.appwidget.action.ActionCallback {
    public <init>();
}

# Hilt Workers
# Ensures Hilt can instantiate workers with @HiltWorker and @AssistedInject
-keep @androidx.hilt.work.HiltWorker class * extends androidx.work.ListenableWorker
-keepclassmembers class * extends androidx.work.ListenableWorker {
    @dagger.assisted.AssistedInject <init>(android.content.Context, androidx.work.WorkerParameters);
}

# Kotlinx Serialization
# Preserves DTOs and their fields for JSON serialization/deserialization
-keep @kotlinx.serialization.Serializable class * { *; }
-keepclassmembers class * {
    @kotlinx.serialization.SerialName *;
}

# Enums used in serialization (like HabitStatus)
# R8 often removes or renames enum constants if not explicitly kept when using valueOf()
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
-keep enum com.jolabs.looplog.model.HabitStatus { *; }

# Hilt EntryPoints
# EntryPointAccessors uses reflection to find these interfaces
-keep @dagger.hilt.EntryPoint interface *
