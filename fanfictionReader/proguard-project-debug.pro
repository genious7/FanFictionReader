-keeppackagenames org.jsoup.nodes

# Generates useful stacktraces
# See http://proguard.sourceforge.net/manual/examples.html#stacktrace
-renamesourcefileattribute SourceFile
-keepattributes LineNumberTable, SourceFile

# Removes some warnings
-keepattributes **

# Preserve Android support libraries` classes and interfaces
# Fix bug on Samsung, Wiko (and other) devices running Android 4.2
# See also: https://code.google.com/p/android/issues/detail?id=78377
-keep class !android.support.v7.view.menu.**, android.support.v7.** {*;}
-keep interface android.support.v7.** { *; }
-dontwarn android.support.v7.**

-keep class !android.support.design.internal.NavigationMenu,!android.support.design.internal.NavigationMenuPresenter,!android.support.design.internal.NavigationSubMenu, android.support.design.** {*;}
-keep interface android.support.design.** { *; }
-dontwarn android.support.design.**

# Do not obfuscate crashlytics code
-keep class com.crashlytics.** { *; }
-keep class com.crashlytics.android.**

# Do not obfuscate Serializable cookie since it uses reflection
-keep class com.spicymango.fanfictionreader.util.AndroidCookieStore { *; }
-keep class com.spicymango.fanfictionreader.util.AndroidCookieStore$SerializableHttpCookie { *; }
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

# Remove debugging log statements
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

-dontobfuscate