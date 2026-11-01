# Retrofit service interfaces are used through reflection.
-keep,allowobfuscation,allowshrinking interface com.pelypets.todolist.data.remote.api.**

# Credential Manager loads the Play Services provider reflectively.
-if class androidx.credentials.CredentialManager
-keep class androidx.credentials.playservices.** { *; }
