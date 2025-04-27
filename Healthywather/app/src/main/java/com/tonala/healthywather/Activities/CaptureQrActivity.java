package com.tonala.healthywather.Activities;

import com.journeyapps.barcodescanner.CaptureActivity;

public class CaptureQrActivity extends CaptureActivity {
    @Override
    public void onBackPressed() {
        finish();
        super.onBackPressed();
    }
}