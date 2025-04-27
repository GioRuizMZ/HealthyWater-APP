package com.tonala.healthywather.utils;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class UploadDeviceService {

    private static final String TAG = "UploadDeviceService";
    private static final Executor executor = Executors.newSingleThreadExecutor();

    public interface UploadCallback {
        void onUploadResult(boolean success);
    }

    public static void subirVariableEnSegundoPlano(final String valor, final UploadCallback callback) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("usuarios").child(uid).child("Dispositivos");

                databaseReference.setValue(valor)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    callback.onUploadResult(true);
                                } else {
                                    callback.onUploadResult(false);
                                }
                            }
                        });
            }
        });
    }
}

