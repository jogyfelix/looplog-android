# Glance ActionCallback implementations are instantiated by the Glance runtime via
# reflection (Class.forName(...).getDeclaredConstructor().newInstance()). The library's
# own consumer rule (-keep public class * extends ActionCallback) preserves the class
# name but not its no-arg constructor, so R8 still strips <init>() as "unused" and the
# reflective instantiation fails with NoSuchMethodException at runtime.
-keep class * implements androidx.glance.appwidget.action.ActionCallback {
    public <init>();
}
