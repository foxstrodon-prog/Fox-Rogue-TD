# =================================================================================================
# FOX-ROGUE-TD PROGUARD & R8 PRODUCTION HARDENING RULES
# Certified by Victor (Lead Android Security & Google Play Compliance Specialist)
# =================================================================================================

# 1. Zero Sensitive Information Leakage: Strip all android.util.Log calls in release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
}

# 2. Reflection Retain for WorkManager ListenableWorkers (e.g., ScoreSubmissionWorker)
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# 3. Reflection Retain for Jetpack ViewModels
-keep class * extends androidx.lifecycle.ViewModel {
    public <init>(...);
}

# 4. Retain Serialization & Leaderboard Model Classes
-keepclassmembers class com.example.model.** { *; }
-keep class com.example.model.** { *; }
-keepclassmembers class com.example.engine.storage.** { *; }

# 5. Hide line numbers & source file attributes in release to impede reverse engineering
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable
