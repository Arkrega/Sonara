-repackageclasses 'o'
-allowaccessmodification
-renamesourcefileattribute S

-keepattributes SourceFile,LineNumberTable,RuntimeVisibleAnnotations,AnnotationDefault,Signature,InnerClasses,EnclosingMethod

-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keep class com.sonara.music.utils.potoken.** { *; }
-keepclassmembers class * {
    void resume(...);
    void resumeWithException(...);
}

-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-keepclassmembers class kotlin.coroutines.SafeContinuation {
    volatile <fields>;
}

-assumenosideeffects class kotlinx.coroutines.internal.MainDispatcherLoader {
    boolean FAST_SERVICE_LOADER_ENABLED return false;
}
-assumenosideeffects class kotlinx.coroutines.internal.FastServiceLoaderKt {
    boolean ANDROID_DETECTED return true;
}
-assumenosideeffects class kotlinx.coroutines.DebugKt {
    boolean getASSERTIONS_ENABLED() return false;
    boolean getDEBUG() return false;
    boolean getRECOVER_STACK_TRACES() return false;
}

-keep class com.sonara.music.utils.SonaraNativeCore { *; }
-keepclasseswithmembernames class com.sonara.music.utils.SonaraNativeCore {
    native <methods>;
}

-keep class * implements io.ktor.client.HttpClientEngineContainer { *; }
-keep class * implements io.ktor.serialization.kotlinx.KotlinxSerializationExtensionProvider { *; }
-keep class coil3.network.okhttp.internal.OkHttpNetworkFetcherServiceLoaderTarget { *; }

-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite {
    <fields>;
}

-keep class com.sonara.music.models.MediaMetadata { *; }
-keep class com.sonara.music.models.MediaMetadata$* { *; }
-keep class com.sonara.music.models.PersistQueue { *; }
-keep class com.sonara.music.models.PersistPlayerState { *; }
-keep class com.sonara.music.models.QueueData { *; }
-keep class com.sonara.music.models.QueueData$* { *; }
-keep class com.sonara.music.models.QueueType { *; }
-keep class com.sonara.music.models.QueueType$* { *; }
-keep class com.sonara.music.playback.queues.** { *; }
-keepclassmembers class * implements java.io.Serializable {
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
}

-keepclassmembers enum com.sonara.music.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    public static final <fields>;
}

-keep class com.sonara.music.cast.** { *; }
-keep class com.google.android.gms.cast.** { *; }
-keep class androidx.mediarouter.** { *; }

-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
}
-assumenosideeffects class timber.log.Timber {
    public static void v(...);
    public static void d(...);
}
-assumenosideeffects class timber.log.Timber$Forest {
    public void v(...);
    public void d(...);
}
-assumenosideeffects class timber.log.Timber$Tree {
    public void v(...);
    public void d(...);
}

-dontwarn javax.servlet.ServletContainerInitializer
-dontwarn org.bouncycastle.jsse.BCSSLParameters
-dontwarn org.bouncycastle.jsse.BCSSLSocket
-dontwarn org.bouncycastle.jsse.provider.BouncyCastleJsseProvider
-dontwarn org.conscrypt.Conscrypt$Version
-dontwarn org.conscrypt.Conscrypt
-dontwarn org.conscrypt.ConscryptHostnameVerifier
-dontwarn org.openjsse.javax.net.ssl.SSLParameters
-dontwarn org.openjsse.javax.net.ssl.SSLSocket
-dontwarn org.openjsse.net.ssl.OpenJSSE
-dontwarn org.slf4j.impl.StaticLoggerBinder
-dontwarn java.beans.BeanDescriptor
-dontwarn java.beans.BeanInfo
-dontwarn java.beans.IntrospectionException
-dontwarn java.beans.Introspector
-dontwarn java.beans.PropertyDescriptor
-dontwarn com.google.re2j.**
-dontwarn kotlin.reflect.**
-dontwarn io.ktor.**
