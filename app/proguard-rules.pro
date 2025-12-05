# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Keep Room database classes
-keep class * extends androidx.room.RoomDatabase {
    *;
}

# Keep ViewModel classes
-keep class * extends androidx.lifecycle.ViewModel {
    *;
}

# Keep data classes
-keepclassmembers class * {
    @androidx.room.* *;
}

# Keep LiveData
-keep class androidx.lifecycle.LiveData { *; }
-keep class androidx.lifecycle.MutableLiveData { *; }

# Keep Gson serialized classes
-keep class com.rpsms.org.models.** { *; }

# Keep BroadcastReceiver
-keep class com.rpsms.org.SmsReceiver { *; }

# Keep Service
-keep class com.rpsms.org.SmsBackupService { *; }

# Keep notification classes
-keep class * extends android.app.Notification { *; }
-keep class * extends android.app.NotificationChannel { *; }

# Keep permission classes
-keep class * extends android.content.BroadcastReceiver { *; }
-keep class * extends android.app.Service { *; }
-keep class * extends android.content.ContentProvider { *; }

# Remove logging in release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}

# Keep generic signatures for Room
-keepattributes Signature

# Keep annotations
-keepattributes *Annotation*

# Keep inner classes
-keepclassmembers class ** {
    @androidx.room.* *;
}

# Keep enum entries
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep serializable classes
-keepnames class * implements java.io.Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# Keep resource class names
-keepclassmembers class **.R$* {
    public static <fields>;
}

# For navigation components
-keep class androidx.navigation.** { *; }
-keep class * extends androidx.navigation.NavDirections { *; }

# For view binding
-keep class * implements androidx.viewbinding.ViewBinding { *; }
