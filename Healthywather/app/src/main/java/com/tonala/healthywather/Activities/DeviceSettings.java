package com.tonala.healthywather.Activities;

import android.Manifest;
import android.animation.Animator;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.tonala.healthywather.R;
import com.tonala.healthywather.utils.UploadDeviceService;

import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DeviceSettings extends AppCompatActivity {
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int MY_PERMISSIONS_REQUEST_BLUETOOTH_CONNECT = 789;
    private TextInputEditText edtCodigo;
    private TextInputLayout cajaCodigo;
    private String deviceAddress;
    private TextView texto_bienvenida, texto_cancelarQr, texto_escaneo;
    private ConstraintLayout opciones_configurarcion, conectando_a_dispositivo;
    private LottieAnimationView sucessanim, loadinganim, erroranim;
    private BluetoothSocket bluetoothSocket;
    boolean en_busqueda = false;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private ImageButton btn_back_config_disp;
    private Button iniciar_scaneo_wf;
    private Button iniciar_scaneo_bt;
    private CardView card_reintentar, card_reintentarscan;
    private Boolean ConfiguracionBT = false;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configurar_dispositivo);

        btn_back_config_disp = findViewById(R.id.btn_back_config_disp);
        texto_bienvenida = findViewById(R.id.bienvenida);
        texto_cancelarQr = findViewById(R.id.txt_cancelarescaneo);
        texto_escaneo = findViewById(R.id.txt_escaneo);
        opciones_configurarcion = findViewById(R.id.opciones);
        conectando_a_dispositivo = findViewById(R.id.conectando);
        sucessanim = findViewById(R.id.okanim);
        sucessanim.setVisibility(View.GONE);
        loadinganim = findViewById(R.id.loadinganim);
        erroranim = findViewById(R.id.erroranim);
        conectando_a_dispositivo.setVisibility(View.GONE);
        iniciar_scaneo_wf = findViewById(R.id.btn_qr_wf);
        iniciar_scaneo_bt = findViewById(R.id.btn_qr_bt);
        Button reintentar = findViewById(R.id.btn_reintentar);
        Button reintentarscan = findViewById(R.id.btn_reintentar_scan);
        Button Codigo = findViewById(R.id.btn_Codigo);
        card_reintentar = findViewById(R.id.card_reintentar);
        card_reintentarscan = findViewById(R.id.card_reintentar_scan);
        texto_bienvenida.setVisibility(View.GONE);
        opciones_configurarcion.setVisibility(View.GONE);
        erroranim.setVisibility(View.GONE);
        animacion();

        iniciar_scaneo_wf.setOnClickListener(v -> checarpermisos());
        iniciar_scaneo_bt.setOnClickListener(v -> {
            checarpermisos();
            ConfiguracionBT = true;
        });
        Codigo.setOnClickListener(v -> IniciarMSGCodigo());
        btn_back_config_disp.setOnClickListener(v -> DeviceSettings.this.finish());
        reintentar.setOnClickListener(v -> {
            texto_escaneo.setText("Conectando a tu Healtywather®...");
            loadinganim.setVisibility(View.VISIBLE);
            erroranim.setVisibility(View.GONE);
            card_reintentar.setVisibility(View.GONE);
            new BluetoothConnectTask(deviceAddress).execute();
        });
        reintentarscan.setOnClickListener(v -> {
            ViewPropertyAnimator animator = btn_back_config_disp.animate()
                    .alpha(0.0f)
                    .setDuration(300);
            ViewPropertyAnimator animator2 = card_reintentarscan.animate()
                    .alpha(0.0f)
                    .setDuration(300);
            ViewPropertyAnimator animator3 = texto_cancelarQr.animate()
                    .alpha(0.0f)
                    .setDuration(300);
            animator.setListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(@NonNull Animator animation) {
                }

                @Override
                public void onAnimationEnd(@NonNull Animator animation) {
                    texto_cancelarQr.setVisibility(View.GONE);
                    btn_back_config_disp.setVisibility(View.GONE);
                    card_reintentarscan.setVisibility(View.GONE);
                    LanzarEscanerQR();
                }

                @Override
                public void onAnimationCancel(@NonNull Animator animation) {
                }

                @Override
                public void onAnimationRepeat(@NonNull Animator animation) {
                }
            });
        });
    }
    private void IniciarMSGCodigo() {
        View mensaje_Codigo = getLayoutInflater().inflate(R.layout.mensaje_agregarcodigo, null);
        Dialog dialogo_codigo;

        edtCodigo = mensaje_Codigo.findViewById(R.id.txtinputcodigo);
        cajaCodigo = mensaje_Codigo.findViewById(R.id.editcodigo);

        ConstraintLayout layout_ok = mensaje_Codigo.findViewById(R.id.layout_ok);
        ConstraintLayout layout_error = mensaje_Codigo.findViewById(R.id.layout_error);
        ConstraintLayout layout_loading = mensaje_Codigo.findViewById(R.id.layout_loading);
        ConstraintLayout layout_ingresar = mensaje_Codigo.findViewById(R.id.layout_ingresarCodigo);

        Button Aceptarok = mensaje_Codigo.findViewById(R.id.btn_Aceptar);
        Button AceptarError = mensaje_Codigo.findViewById(R.id.btn_AceptarError);
        Button Conectar = mensaje_Codigo.findViewById(R.id.btn_ConectarCodigo);
        ImageButton Back = mensaje_Codigo.findViewById(R.id.btn_backCodigo);

        dialogo_codigo = new Dialog(this, R.style.DialogTheme);
        dialogo_codigo.setContentView(mensaje_Codigo);
        dialogo_codigo.getWindow().setLayout(1000, 1200);
        dialogo_codigo.setCancelable(false);
        ConstraintLayout dialogContainer = dialogo_codigo.findViewById(R.id.layout_codigo);
        dialogContainer.setBackground(ContextCompat.getDrawable(this, R.drawable.fondo_mensaje));

        Aceptarok.setOnClickListener(v -> {
            dialogo_codigo.cancel();
            startActivity(new Intent(this, MenuuActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK));
            this.finish();
        });
        AceptarError.setOnClickListener(v -> dialogo_codigo.cancel());
        Back.setOnClickListener(v -> dialogo_codigo.cancel());

        Conectar.setOnClickListener(v -> {
            String codigo = edtCodigo.getText().toString().trim();
            if (codigo.length() == 8) {
                // Ocultar el teclado cuando se cumple la condición
                ocultarTeclado(mensaje_Codigo);
                cajaCodigo.setErrorEnabled(false);
                layout_ingresar.animate()
                        .alpha(0.0f)
                        .setDuration(300)
                        .withEndAction(() -> {
                            layout_ingresar.setVisibility(View.GONE);
                            layout_loading.setVisibility(View.VISIBLE);
                            layout_loading.setAlpha(0.0f);
                            layout_loading.animate()
                                    .setDuration(300)
                                    .alpha(1.0f)
                                    .withEndAction(() -> BuscarDispositivo(codigo, dispositivoEncontrado -> {
                                        if (dispositivoEncontrado) {
                                            layout_loading.animate()
                                                    .setDuration(300)
                                                    .alpha(0.0f)
                                                    .withEndAction(() -> {
                                                        layout_loading.setVisibility(View.GONE);
                                                        layout_ok.setVisibility(View.VISIBLE);
                                                        layout_ok.setAlpha(0.0f);
                                                        layout_ok.animate()
                                                                .setDuration(300)
                                                                .alpha(1.0f);
                                                    });
                                        } else {
                                            layout_loading.animate()
                                                    .setDuration(300)
                                                    .alpha(0.0f)
                                                    .withEndAction(() -> {
                                                        layout_loading.setVisibility(View.GONE);
                                                        layout_error.setVisibility(View.VISIBLE);
                                                        layout_error.setAlpha(0.0f);
                                                        layout_error.animate()
                                                                .setDuration(300)
                                                                .alpha(1.0f);
                                                    });
                                        }
                                    }));
                        });
            } else {
                cajaCodigo.setError("El código debe tener 8 caracteres");
            }
        });

        dialogo_codigo.show();
    }
    private void ocultarTeclado(View vista) {
        View currentFocus = vista.findFocus();
        if (currentFocus != null) {
            InputMethodManager imm = (InputMethodManager) vista.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            }
        }
    }
    private void BuscarDispositivo(String codigo, FirebaseCallback callback) {
        executorService.execute(() -> {
            StringBuilder sb = new StringBuilder();

            for (int i = 0; i < codigo.length(); i++) {
                char c = codigo.charAt(i);
                if (i < 2) {
                    sb.append((char) (c - 5));
                } else if (i < 5) {
                    sb.append((char) (c - 4));
                } else {
                    sb.append((char) (c - 3));
                }
            }
            String codigoDesencodeado = sb.toString().replaceAll("X*$", "");
            DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Dispositivos");
            databaseReference.child(codigoDesencodeado).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    boolean dispositivoEncontrado = dataSnapshot.exists();
                    if (dispositivoEncontrado) {
                        String ID = codigoDesencodeado.substring(2);
                        SharedPreferences sharedPreferences = getSharedPreferences("user_data", Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString("ID_dispositivo", ID);
                        editor.putString("Nombre_dispositivo", "Mi Healthywater");
                        editor.apply();
                        UploadDeviceService.subirVariableEnSegundoPlano("ID" + ID, success -> {
                            if (success) {
                                handler.post(() -> callback.onCallback(dispositivoEncontrado));
                            }
                            else {
                                handler.post(() -> callback.onCallback(false));
                            }
                        });
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    handler.post(() -> callback.onCallback(false));
                }
            });
        });
    }
    public interface FirebaseCallback {
        void onCallback(boolean dispositivoEncontrado);
    }
    private void animacion() {
        en_busqueda = false;
        iniciar_scaneo_bt.setEnabled(false);
        iniciar_scaneo_wf.setEnabled(false);
        btn_back_config_disp.setTranslationX(-200);
        btn_back_config_disp.setAlpha(0.0f);
        btn_back_config_disp.setVisibility(View.VISIBLE);

        texto_bienvenida.setTranslationX(-200);
        texto_bienvenida.setAlpha(0.0f);
        texto_bienvenida.setVisibility(View.VISIBLE);

        opciones_configurarcion.setTranslationY(300);
        opciones_configurarcion.setAlpha(0.0f);
        opciones_configurarcion.setVisibility(View.VISIBLE);

        ViewPropertyAnimator animatort = texto_bienvenida.animate()
                .translationX(0)
                .alpha(1.0f)
                .setDuration(1000);

        ViewPropertyAnimator animatorb = btn_back_config_disp.animate()
                .translationX(0)
                .alpha(1.0f)
                .setDuration(1000);

        ViewPropertyAnimator animatorc = opciones_configurarcion.animate()
                .translationY(0)
                .alpha(1.0f)
                .setDuration(1000);

        animatort.start();
        animatorb.start();
        animatorc.start();

        animatorc.setListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(@NonNull Animator animation) {
            }

            @Override
            public void onAnimationEnd(@NonNull Animator animation) {
                iniciar_scaneo_bt.setEnabled(true);
                iniciar_scaneo_wf.setEnabled(true);
            }

            @Override
            public void onAnimationCancel(@NonNull Animator animation) {
            }

            @Override
            public void onAnimationRepeat(@NonNull Animator animation) {
            }
        });
    }

    private void checarpermisos() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "El dispositivo no soporta Bluetooth", Toast.LENGTH_SHORT).show();
        } else if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else if (ContextCompat.checkSelfPermission(DeviceSettings.this, Manifest.permission.BLUETOOTH)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(DeviceSettings.this,
                    new String[]{Manifest.permission.BLUETOOTH},
                    MY_PERMISSIONS_REQUEST_BLUETOOTH_CONNECT);
        } else {
            desapareceranim();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_PERMISSIONS_REQUEST_BLUETOOTH_CONNECT) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                desapareceranim();
            } else {
                Toast.makeText(this, "Permiso de conexión Bluetooth denegado", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void desapareceranim() {
        texto_bienvenida.setTranslationX(0);
        texto_bienvenida.setAlpha(1.0f);
        opciones_configurarcion.setTranslationX(0);
        opciones_configurarcion.setAlpha(1.0f);
        btn_back_config_disp.setTranslationX(0);
        btn_back_config_disp.setAlpha(1.0f);
        ViewPropertyAnimator animatort = texto_bienvenida.animate()
                .translationX(-300)
                .alpha(0.0f)
                .setDuration(1000);
        ViewPropertyAnimator animatorb = btn_back_config_disp.animate()
                .translationX(-300)
                .alpha(0.0f)
                .setDuration(1000);
        ViewPropertyAnimator animatorc = opciones_configurarcion.animate()
                .translationX(-300)
                .alpha(0.0f)
                .setDuration(1000);
        animatorc.setListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(@NonNull Animator animation) {
            }

            @Override
            public void onAnimationEnd(@NonNull Animator animation) {
                texto_bienvenida.setVisibility(View.GONE);
                opciones_configurarcion.setVisibility(View.GONE);
                LanzarEscanerQR();
            }

            @Override
            public void onAnimationCancel(@NonNull Animator animation) {
            }

            @Override
            public void onAnimationRepeat(@NonNull Animator animation) {
            }
        });
        animatort.start();
        animatorb.start();
        animatorc.start();
    }

    private void LanzarEscanerQR() {
        IntentIntegrator intentIntegrator = new IntentIntegrator(DeviceSettings.this);
        intentIntegrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
        intentIntegrator.setPrompt("Escanea el codigo de tu Healtywather");
        intentIntegrator.setBeepEnabled(false);
        intentIntegrator.initiateScan();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                desapareceranim();
            } else {
                Toast.makeText(this, "Bluetooth no fue activado", Toast.LENGTH_SHORT).show();
            }
        }
        // Procesar el resultado del escaner
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            btn_back_config_disp.setVisibility(View.VISIBLE);
            btn_back_config_disp.setAlpha(1.0f);
            if (result.getContents() != null) {
                String qrContent = result.getContents();
                Log.d("Conexion", "QR Content: " + qrContent);
                deviceAddress = obtenerDireccionMacDesdeQR(qrContent);
                // Conectar al dispositivo Bluetooth conocido
                if (deviceAddress != null) {
                    animacion_conectando();
                    en_busqueda = true;
                    new BluetoothConnectTask(deviceAddress).execute();
                } else {
                    Toast.makeText(this, "Error al obtener la dirección MAC del QR", Toast.LENGTH_SHORT).show();
                }
            } else {
                texto_cancelarQr.setVisibility(View.VISIBLE);
                texto_cancelarQr.setAlpha(1.0f);

                card_reintentarscan.setVisibility(View.VISIBLE);
                card_reintentarscan.setAlpha(1.0f);

                btn_back_config_disp.setTranslationX(0);
            }
        }
    }

    private String obtenerDireccionMacDesdeQR(String qrContent) {
        if (qrContent != null && qrContent.startsWith("MAC:") && qrContent.length() >= 18) {
            return qrContent.substring(4);
        } else {
            return null;
        }
    }

    private void animacion_conectando() {
        conectando_a_dispositivo.setVisibility(View.VISIBLE);
        conectando_a_dispositivo.setAlpha(0.0f);
        ViewPropertyAnimator animator = conectando_a_dispositivo.animate()
                .alpha(1.0f)
                .setDuration(1500);
        animator.start();
    }

    @SuppressLint("StaticFieldLeak")
    private class BluetoothConnectTask extends AsyncTask<Void, Void, Boolean> {
        private final String deviceAddress;
        private final Handler mHandler = new Handler();
        BluetoothDevice device;

        public BluetoothConnectTask(String deviceAddress) {
            this.deviceAddress = deviceAddress;
        }

        @SuppressLint("SetTextI18n")
        public Boolean ConectarBT() {
            try {
                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
                bluetoothSocket.connect();
                return true;
            } catch (IOException e) {
                Log.e("BluetoothConnectTask", "Error de conexión", e);
                e.printStackTrace();
                runOnUiThread(() -> {
                    btn_back_config_disp.setAlpha(0.0f);
                    texto_escaneo.setText("No se pudo Conectar");
                    loadinganim.setVisibility(View.GONE);
                    erroranim.setVisibility(View.VISIBLE);
                    card_reintentar.setAlpha(0.0f);
                    card_reintentar.setVisibility(View.VISIBLE);
                    card_reintentar.setTranslationY(100);
                    ViewPropertyAnimator anim1 = card_reintentar.animate()
                            .setDuration(500)
                            .alpha(1.0f)
                            .translationY(0);
                    ViewPropertyAnimator anim = btn_back_config_disp.animate()
                            .setDuration(500)
                            .alpha(1.0f)
                            .translationX(0);
                });
                return false;
            }
        }

        protected Boolean doInBackground(Void... params) {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            device = bluetoothAdapter.getRemoteDevice(deviceAddress);
            try {
                UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
                if (ActivityCompat.checkSelfPermission(DeviceSettings.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        ActivityCompat.requestPermissions(DeviceSettings.this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, MY_PERMISSIONS_REQUEST_BLUETOOTH_CONNECT);
                    }
                    return false;
                }
                bluetoothSocket = device.createRfcommSocketToServiceRecord(uuid);
                if (ConectarBT()) return true;
            } catch (IOException e) {
                Log.e("BluetoothConnectTask", "Error de conexión", e);
                e.printStackTrace();
                return false;
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                loadinganim.setVisibility(View.GONE);
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    sucessanim.setVisibility(View.VISIBLE);
                    try {
                        OutputStream outputStream = bluetoothSocket.getOutputStream();
                        if (!ConfiguracionBT) {
                            outputStream.write("iniciawf".getBytes());
                        } else {
                            outputStream.write("iniciabt".getBytes());
                        }
                    } catch (IOException e) {
                        Log.e("BluetoothConnectTask", "Error al enviar datos al ESP32", e);
                    }
                    try {
                        bluetoothSocket.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        Intent intent;
                        if (!ConfiguracionBT) {
                            intent = new Intent(DeviceSettings.this, DeviceSettings2.class);
                        } else {
                            intent = new Intent(DeviceSettings.this, MenuuActivity.class);
                        }
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.putExtra("MAC", device.getAddress());
                        startActivity(intent);
                        sucessanim.setVisibility(View.GONE);
                        DeviceSettings.this.finish();
                    }, 1360);
                }, 500);
                Log.i("BluetoothConnectTask", "Conexión exitosa");

            } else {
                Log.e("BluetoothConnectTask", "No se pudo conectar al dispositivo");
            }
        }
    }
}