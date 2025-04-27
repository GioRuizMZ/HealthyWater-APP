plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.tonala.healthywather"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.tonala.healthywather"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            /*isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )*/
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.12.0")
    implementation ("com.google.code.gson:gson:2.8.9")
    implementation ("com.google.android.gms:play-services-location:21.2.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("androidx.compose.animation:animation-graphics-android:1.6.7")
    //FIREBASE
    implementation(platform("com.google.firebase:firebase-bom:32.3.1"))
    implementation ("com.google.android.gms:play-services-auth:21.1.1")
    implementation ("com.google.firebase:firebase-messaging")
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.firebase:firebase-database:21.0.0")
    //MATERIAL DESIGN
    implementation ("com.google.android.material:material:1.12.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation ("com.airbnb.android:lottie:6.3.0")
    implementation ("com.github.bumptech.glide:glide:4.16.0")
    implementation ("com.github.PhilJay:MPAndroidChart:v3.1.0")
    implementation ("com.google.zxing:core:3.4.1")
    implementation("io.github.thanosfisherman.wifiutils:wifiutils:1.6.6")
    implementation ("com.journeyapps:zxing-android-embedded:4.3.0")
    annotationProcessor ("com.github.bumptech.glide:compiler:4.16.0")
    implementation ("com.github.yalantis:ucrop:2.2.5")
    implementation("androidx.navigation:navigation-fragment:2.7.7")
    implementation("androidx.navigation:navigation-ui:2.7.7")
    implementation ("com.github.yalantis:ucrop:2.2.8-native")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}