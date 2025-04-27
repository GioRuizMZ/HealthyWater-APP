package com.tonala.healthywather.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;
import android.view.Menu;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.tonala.healthywather.R;

import java.util.Map;

public class MessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";
    private SharedPreferences sharedPreferences;
    private String tokenGuardado;

    @Override
    public void onCreate() {
        super.onCreate();

        sharedPreferences = getSharedPreferences("user_data", Context.MODE_PRIVATE);
        tokenGuardado = sharedPreferences.getString("fcmToken", null);

        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(new OnCompleteListener<String>() {
                    @Override
                    public void onComplete(@NonNull Task<String> task) {
                        if (task.isSuccessful() && task.getResult() != null) {
                            String token = task.getResult();
                            validarYActualizarToken(token);
                        } else {
                            Log.e(TAG, "Error al obtener el token: ", task.getException());
                        }
                    }
                });
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        if (remoteMessage.getNotification() != null) {
            // Manejar notificación cuando la app está en segundo plano
            sendNotification(remoteMessage.getNotification().getTitle(), remoteMessage.getNotification().getBody());
        } else if (remoteMessage.getData().size() > 0) {
            // Manejar mensaje de datos cuando la app está en primer plano
            handleDataMessage(remoteMessage.getData());
        }
    }

    @Override
    public void onNewToken(String token) {
        Log.d(TAG, "Refreshed token: " + token);
        validarYActualizarToken(token);
    }

    private void validarYActualizarToken(String token) {
        if (tokenGuardado == null || !tokenGuardado.equals(token)) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("fcmToken", token);
            editor.apply();
            updateTokenInDatabase(token);
        }
    }

    private void updateTokenInDatabase(String token) {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("usuarios").child(uid).child("fcmtoken");

        databaseReference.setValue(token)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        // Token actualizado exitosamente
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Error al actualizar el token en la base de datos", e);
                    }
                });
    }

    private void handleDataMessage(Map<String, String> data) {
        String title = data.get("title");
        String body = data.get("body");
        sendNotification(title, body);
    }

    private void sendNotification(String title, String messageBody) {
        Intent intent = new Intent(this, Menu.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
        );

        String channelId = getString(R.string.default_notification_channel_id);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.logosinletras)
                .setContentTitle(title)
                .setContentText(messageBody)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "Channel human readable title", NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify(0, notificationBuilder.build());
    }
}




