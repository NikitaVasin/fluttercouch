## Flutter wrapper
 -keep class io.flutter.app.** { *; }
 -keep class io.flutter.plugin.** { *; }
 -keep class io.flutter.util.** { *; }
 -keep class io.flutter.view.** { *; }
 -keep class io.flutter.** { *; }
 -keep class io.flutter.plugins.** { *; }
 -keep class com.google.firebase.** { *; } 
 -dontwarn io.flutter.embedding.**
 -ignorewarnings
 -keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
 -keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
 -keepclassmembernames class kotlinx.** {
     volatile <fields>;
 }
 -keep class kotlinx.coroutines.android.AndroidDispatcherFactory {*;}
 -keep class com.couchbase.lite.ConnectionStatus { <init>(...); }
 -keep class com.couchbase.lite.LiteCoreException { static <methods>; }
 -keep class com.couchbase.lite.internal.core.C4* {
     static <methods>;
     <fields>;
     <init>(...);
}