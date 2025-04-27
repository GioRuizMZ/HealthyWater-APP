package com.tonala.healthywather.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class DeleteDeviceService {

    private static final String TAG = "DeleteDeviceService";
    private static final Executor executor = Executors.newSingleThreadExecutor();

    public interface DeleteCallback {
        void onDeleteResult(boolean success);
    }

    public static void eliminarVariableEnSegundoPlano(Context context, final DeleteCallback callback) {
        if (!isNetworkAvailable(context)) {
            Log.e(TAG, "No hay conexión a Internet.");
            callback.onDeleteResult(false);
            return;
        }

        executor.execute(new Runnable() {
            @Override
            public void run() {
                String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("usuarios").child(uid).child("Dispositivos");

                databaseReference.removeValue()
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    Log.d(TAG, "Variable eliminada correctamente.");
                                    callback.onDeleteResult(true);
                                } else {
                                    Log.e(TAG, "Error al eliminar la variable.", task.getException());
                                    callback.onDeleteResult(false);
                                }
                            }
                        });
            }
        });
    }

    private static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}


