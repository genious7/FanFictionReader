-keeppackagenames org.jsoup.nodes

# Generates useful stacktraces
# See http://proguard.sourceforge.net/manual/examples.html#stacktrace
-renamesourcefileattribute SourceFile
-keepattributes LineNumberTable, SourceFile

# Removes some warnings
-keepattributes Signature, InnerClasses

# Fix bug on Samsung, Wiko (and other) devices running Android 4.2
# See also: https://code.google.com/p/android/issues/detail?id=78377
-keep class !android.support.v7.view.menu.**{*;}
-keep class !android.support.design.internal.NavigationMenu{*;}
-keep class !android.support.design.internal.NavigationMenuPresenter{*;}
-keep class !android.support.design.internal.NavigationSubMenu{*;}

# Preserve Android support libraries` classes and interfaces
-dontwarn android.support.v7.**
-keep class android.support.v7.** { *; }
-keep interface android.support.v7.** { *; }

-dontwarn android.support.design.**
-keep class android.support.design.** { *; }
-keep interface android.support.design.** { *; }

# Do not obfuscate crashlytics code
-keep class com.crashlytics.** { *; }
-keep class com.crashlytics.android.**

# Remove debugging log statements
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}