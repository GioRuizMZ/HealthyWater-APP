package com.tonala.healthywather.utils;
import android.app.Application;
import androidx.appcompat.app.AppCompatDelegate;
public class ChangeThemes extends Application{
    public void onCreate() {
        super.onCreate();
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
    }
}
