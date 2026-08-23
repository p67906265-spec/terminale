# Regole ProGuard per Terminale

# SSHJ / EdDSA: classe JDK opzionale non presente su Android.
-dontwarn sun.security.x509.X509Key
-keep class org.bouncycastle.jce.provider.BouncyCastleProvider { *; }
