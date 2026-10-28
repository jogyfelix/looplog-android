# LiteRT LM (Large Model) rules to prevent JNI lookup failures
-keep class com.google.ai.edge.litertlm.** { *; }

# Keep Play AI Delivery classes which are used for model downloading
-keep class com.google.android.play.core.aipacks.** { *; }
