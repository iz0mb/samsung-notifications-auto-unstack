# Keep the accessibility service so the system can bind to it by name.
-keep class com.autounstack.app.NotificationExpandService { *; }

# Keep the main activity entry point.
-keep class com.autounstack.app.MainActivity { *; }

# Keep the preferences manager (accessed by name via SharedPreferences key constants).
-keep class com.autounstack.app.PreferencesManager { *; }
