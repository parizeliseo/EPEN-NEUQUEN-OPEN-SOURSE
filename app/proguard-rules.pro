# Lectura de PDF (PDFBox y BouncyCastle): se usan por reflexión y recursos internos.
-keep class com.tom_roush.** { *; }
-keep class org.bouncycastle.** { *; }
-dontwarn com.tom_roush.**
-dontwarn org.bouncycastle.**
-dontwarn com.gemalto.jp2.**

# Puente con la página del portal (métodos llamados desde JavaScript).
-keepclassmembers class ar.pariz.epen.PortalActivity$Puente {
    @android.webkit.JavascriptInterface <methods>;
}
-keepattributes JavascriptInterface

# Trabajo en segundo plano (WorkManager lo crea por reflexión).
-keep class ar.pariz.epen.RevisionWorker { <init>(...); }
