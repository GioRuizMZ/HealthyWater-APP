package com.tonala.healthywather.Activities;

import static android.view.View.VISIBLE;

import android.animation.Animator;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.tonala.healthywather.R;
import com.tonala.healthywather.utils.UploadDeviceService;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class DeviceSettings2 extends AppCompatActivity {
    private ToggleButton ppm_e, turb_e, temp_e, ph_e, ppm_s, turb_s, temp_s, ph_s;
    ListView wifiLista;
    List<ScanResult> results;
    WiFiAdapter wifiAdapter;
    CardView Vista_Red;
    TextView NombreRed;
    ImageView imgSeguridad;
    ConstraintLayout fondo, finalizar_configuracion_layout, final_layout, confirmacion_salir;
    ImageButton btn_atras_wifi;
    Button btn_conectar_red, btn_finalizar, btn_salir, btn_nosalir, btn_siguiente;
    String SSID, CONTRASENA, nombre_disp;
    TextInputLayout caja_contra_red, caja_nombre_disp;
    TextInputEditText edit_contra_red, edit_nombre_disp;
    private InputStream inputStream;
    private boolean connected = false;
    Boolean TIENE_CONTRA;
    LottieAnimationView loading_wifi, ok_wifi, error_wifi, loading_final, transicion, btn_actualizar_red;
    BluetoothSocket bluetoothSocket;
    OutputStream outputStream;
    char back = 'a';
    private final HashMap<ToggleButton, Boolean> toggleButtonStates = new HashMap<>();

    private boolean conectarBluetooth(BluetoothDevice device) {
        try {
            UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
            bluetoothSocket = device.createRfcommSocketToServiceRecord(uuid);
            bluetoothSocket.connect();
            return true;
        } catch (IOException e) {
            Log.e("BluetoothConnectTask", "Error de conexión", e);
            e.printStackTrace();
            return false;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_siguiente_configuracion_dispositivo);

        //botones toggle
        ppm_e = findViewById(R.id.tgppm_e);
        ppm_s = findViewById(R.id.tgppm_s);
        turb_e = findViewById(R.id.tgturb_e);
        turb_s = findViewById(R.id.tgturb_s);
        temp_e = findViewById(R.id.tgtemp_e);
        temp_s = findViewById(R.id.tgtemp_s);
        ph_e = findViewById(R.id.tgph_e);
        ph_s = findViewById(R.id.tgph_s);
        botones_variables();

        Vista_Red = findViewById(R.id.configura_Red);
        NombreRed = findViewById(R.id.txt_red_seleccionada);
        confirmacion_salir = findViewById(R.id.confirmacion_Salir);
        btn_salir = findViewById(R.id.btn_si_salir);
        btn_nosalir = findViewById(R.id.btn_no_quedarme);
        imgSeguridad = findViewById(R.id.img_seguridad_wifi);
        fondo = findViewById(R.id.config_wifi);
        loading_wifi = findViewById(R.id.loading_anim_wifi);
        ok_wifi = findViewById(R.id.ok_wifi_anim);
        error_wifi = findViewById(R.id.error_wifi_anim);
        finalizar_configuracion_layout = findViewById(R.id.finalizar_config);
        transicion = findViewById(R.id.transicion);
        final_layout = findViewById(R.id.final_layout);
        btn_atras_wifi = findViewById(R.id.btn_back_config_wifi);
        btn_finalizar = findViewById(R.id.btn_finalizar_config);
        btn_actualizar_red = findViewById(R.id.btn_recargar);
        btn_conectar_red = findViewById(R.id.btn_conectar_Red);
        btn_siguiente = findViewById(R.id.btn_siguiente_config);
        caja_contra_red = findViewById(R.id.contrasena_wifi_layout);
        caja_nombre_disp = findViewById(R.id.crear_nombre_disp_layou);
        edit_nombre_disp = findViewById(R.id.crear_nombre_disp);
        loading_final = findViewById(R.id.loading_anim_nombredisp);
        edit_contra_red = findViewById(R.id.txt_contrasena_wifi);

        //Intentar conexion
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            String bluetoothDeviceAddress = bundle.getString("MAC");
            if (bluetoothDeviceAddress != null) {
                BluetoothDevice device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(bluetoothDeviceAddress);
                if (conectarBluetooth(device)) {
                    if (bluetoothSocket != null && bluetoothSocket.isConnected()) {
                        try {
                            outputStream = bluetoothSocket.getOutputStream();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    } else {
                        Toast.makeText(DeviceSettings2.this, "Socket no está conectado", Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(DeviceSettings2.this, "No se pudo conectar", Toast.LENGTH_LONG).show();
                }
            } else {
                Log.e("Siguiente_Configuracion", "La dirección MAC es nula");
                Toast.makeText(DeviceSettings2.this, "La dirección MAC es nula", Toast.LENGTH_LONG).show();
            }
        } else {
            Log.e("Siguiente_Configuracion", "Bundle es nulo");
            Toast.makeText(DeviceSettings2.this, "Bundle es nulo", Toast.LENGTH_LONG).show();
        }

        // Inicializar lista
        wifiLista = findViewById(R.id.wifi_list);
        results = new ArrayList<>();
        wifiAdapter = new WiFiAdapter(this, results);
        wifiLista.setAdapter(wifiAdapter);

        // Agregar OnItemClickListener al ListView
        wifiLista.setOnItemClickListener((parent, view, position, id) -> {
            ScanResult selectedNetwork = wifiAdapter.getItem(position);
            if (selectedNetwork != null) {
                Mostrar_ventana_Wifi(selectedNetwork.SSID);
                mostrarcard();
            }
        });

        btn_atras_wifi.setOnClickListener(v -> desaparecercard());
        btn_actualizar_red.setOnClickListener(v -> {
            btn_actualizar_red.playAnimation();
            escanearRedesWiFi();
            new Handler(Looper.getMainLooper()).postDelayed(() -> btn_actualizar_red.pauseAnimation(), 1000);
        });
        escanearRedesWiFi();
        btn_conectar_red.setOnClickListener(v -> {
            String txtcontrasena = Objects.requireNonNull(edit_contra_red.getText()).toString();
            //Red con contraseña
            if (TIENE_CONTRA) {
                if (txtcontrasena.isEmpty()) {
                    caja_contra_red.setError("Ingresa la contraseña");
                } else {
                    ocultarTeclado();
                    SSID = NombreRed.getText().toString();
                    CONTRASENA = edit_contra_red.getText().toString();
                    caja_contra_red.setErrorEnabled(false);
                    try {
                        outputStream.write((SSID + " " + CONTRASENA).trim().getBytes());
                        loading_wifi.setVisibility(View.VISIBLE);
                        error_wifi.setVisibility(View.GONE);
                        new Handler(Looper.getMainLooper()).postDelayed(() -> new BluetoothReceiveTask().execute(), 1000);
                    } catch (IOException e) {
                        Log.e("BluetoothConnectTask", "Error al enviar datos al ESP32", e);
                        e.printStackTrace();
                        Toast.makeText(DeviceSettings2.this, "Error al enviar datos al ESP32", Toast.LENGTH_LONG).show();
                    }
                }
            }
            //Red abierta
            else {
                SSID = NombreRed.getText().toString();
                try {
                    outputStream.write((SSID).getBytes());
                    loading_wifi.setVisibility(View.VISIBLE);
                    new Handler(Looper.getMainLooper()).postDelayed(() -> new BluetoothReceiveTask().execute(), 1000);
                } catch (IOException e) {
                    Log.e("BluetoothConnectTask", "Error al enviar datos al ESP32", e);
                }
            }
        });
        btn_finalizar.setOnClickListener(v -> {
            nombre_disp = Objects.requireNonNull(edit_nombre_disp.getText()).toString().trim();
            if (nombre_disp.isEmpty()) {
                caja_nombre_disp.setError("Ingresa un nombre para identificar el dispositivo");
                if (nombre_disp.length() <= 4) {
                    caja_nombre_disp.setError("Nombre muy corto");
                }
            } else {
                ocultarTeclado();
                try {
                    outputStream.write((nombre_disp).getBytes());
                    loading_final.setVisibility(View.VISIBLE);
                    new Handler(Looper.getMainLooper()).postDelayed(() -> new BluetoothReceiveTask().execute(), 1000);
                } catch (IOException e) {
                    Log.e("BluetoothConnectTask", "Error al enviar datos al ESP32", e);
                }
            }
        });
        btn_salir.setOnClickListener(v -> {
            try {
                outputStream.write("cancelar".getBytes());
                bluetoothSocket.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            desaparecerConfirmacion();
            startActivity(new Intent(DeviceSettings2.this, MenuuActivity.class));
            DeviceSettings2.this.finish();
        });
        btn_nosalir.setOnClickListener(v -> desaparecerConfirmacion());
        btn_siguiente.setOnClickListener(v -> {
            SharedPreferences sharedPreferences = getSharedPreferences("variables", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            for (final ToggleButton button : toggleButtonStates.keySet()) {
                editor.putInt(button.getTag().toString(), button.isChecked() ? 1 : 0);
            }
            editor.apply();
        });
    }

    private void botones_variables() {
        // Agregar los ToggleButtons al HashMap
        toggleButtonStates.put(ppm_e, false);
        toggleButtonStates.put(ppm_s, false);
        toggleButtonStates.put(turb_e, false);
        toggleButtonStates.put(turb_s, false);
        toggleButtonStates.put(temp_e, false);
        toggleButtonStates.put(temp_s, false);
        toggleButtonStates.put(ph_e, false);
        toggleButtonStates.put(ph_s, false);

        for (final ToggleButton button : toggleButtonStates.keySet()) {
            button.setOnClickListener(v -> {
                boolean isChecked = !toggleButtonStates.get(button);
                toggleButtonStates.put(button, isChecked);
                updateButtonState(button, isChecked);
            });
        }
    }

    private void updateButtonState(ToggleButton button, boolean isChecked) {
        if (isChecked) {
            button.setBackgroundResource(getOnResource(button));
        } else {
            button.setBackgroundResource(getOffResource(button));
        }
    }

    private int getOnResource(ToggleButton button) {
        if (button == ppm_e || button == ppm_s) {
            return R.drawable.conductividad_on;
        } else if (button == turb_e || button == turb_s) {
            return R.drawable.turbidez_on;
        } else if (button == temp_e || button == temp_s) {
            return R.drawable.temperatura_on;
        } else if (button == ph_e || button == ph_s) {
            return R.drawable.ph_on;
        }
        return 0;
    }

    private int getOffResource(ToggleButton button) {
        if (button == ppm_e || button == ppm_s) {
            return R.drawable.conductividad_off;
        } else if (button == turb_e || button == turb_s) {
            return R.drawable.turbidez_off;
        } else if (button == temp_e || button == temp_s) {
            return R.drawable.temperatura_off;
        } else if (button == ph_e || button == ph_s) {
            return R.drawable.ph_off;
        }
        return 0;
    }

    public void mostrarcard() {
        Clear_error_red();
        Vista_Red.animate().cancel();
        Vista_Red.setVisibility(View.VISIBLE);
        Vista_Red.setTranslationY(0);
        Vista_Red.setAlpha(0.0f);
        ViewPropertyAnimator animatorRed = Vista_Red.animate()
                .translationY(100)
                .alpha(1.0f)
                .setDuration(500);
        fondo.setAlpha(1.0f);
        ViewPropertyAnimator animatorfondo = fondo.animate()
                .alpha(0.5f)
                .setDuration(500);
        animatorRed.start();
        animatorfondo.start();
        wifiLista.setEnabled(false);
        back = 'b';
    }

    public void desaparecercard() {
        ocultarTeclado();
        Vista_Red.animate().cancel();
        Vista_Red.setTranslationY(100);
        Vista_Red.setAlpha(1.0f);
        ViewPropertyAnimator animatorRed = Vista_Red.animate()
                .translationY(0)
                .alpha(0.0f)
                .setDuration(500);
        fondo.setAlpha(0.5f);
        ViewPropertyAnimator animatorfondo = fondo.animate()
                .alpha(1.0f)
                .setDuration(500);
        animatorRed.start();
        animatorfondo.start();
        wifiLista.setEnabled(true);
        back = 'a';
        new Handler(Looper.getMainLooper()).postDelayed(() -> Vista_Red.setVisibility(View.GONE), 500);
    }
    private void Clear_error_red() {
        edit_contra_red.setText("");
        caja_contra_red.setErrorEnabled(false);
        error_wifi.setVisibility(View.GONE);
        loading_wifi.setVisibility(View.GONE);
    }
    private void escanearRedesWiFi() {
        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        wifiManager.startScan();
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        List<ScanResult> scanResults = wifiManager.getScanResults();
        List<ScanResult> filteredResults = new ArrayList<>();
        for (ScanResult result : scanResults) {
            if (!result.SSID.trim().isEmpty()) {
                filteredResults.add(result);
            }
        }
        filteredResults.sort((result1, result2) -> Integer.compare(result2.level, result1.level));

        results.clear();
        results.addAll(filteredResults);
        wifiAdapter.notifyDataSetChanged();
    }
    private void Mostrar_ventana_Wifi(String selectedNetwork) {
        NombreRed.setText(selectedNetwork);
        for (ScanResult result : results) {
            if (result.SSID.equals(selectedNetwork)) {
                if (result.capabilities.contains("WPA") || result.capabilities.contains("WEP")) {
                    imgSeguridad.setImageResource(R.drawable.wifi_con_contra);
                    caja_contra_red.setVisibility(View.VISIBLE);
                    TIENE_CONTRA = true;
                } else {
                    imgSeguridad.setImageResource(R.drawable.wifi_sin_contra);
                    caja_contra_red.setVisibility(View.GONE);
                    TIENE_CONTRA = false;
                }
                break;
            }
        }
    }
    public static class WiFiAdapter extends ArrayAdapter<ScanResult> {
        private final List<ScanResult> wifiList;
        private final Context context;
        public WiFiAdapter(Context context, List<ScanResult> wifiList) {
            super(context, 0, wifiList);
            this.context = context;
            this.wifiList = wifiList;
        }
        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.item_red, parent, false);
            }

            ScanResult wifiNetwork = wifiList.get(position);

            TextView txtRed = convertView.findViewById(R.id.txt_red);
            ImageView imgSeguridad = convertView.findViewById(R.id.wifi_seguridad);
            ImageView imgIntensidad = convertView.findViewById(R.id.wifi_intensidad);

            txtRed.setText(wifiNetwork.SSID);

            if (wifiNetwork.capabilities.contains("WPA") || wifiNetwork.capabilities.contains("WEP")) {
                imgSeguridad.setImageResource(R.drawable.wifi_con_contra);
            } else {
                imgSeguridad.setImageResource(R.drawable.wifi_sin_contra);
            }
            int level = WifiManager.calculateSignalLevel(wifiNetwork.level, 4);
            switch (level) {
                case 0:
                    imgIntensidad.setImageResource(R.drawable.wifi_bajo);
                    break;
                case 1:
                    imgIntensidad.setImageResource(R.drawable.wifi_medio);
                    break;
                case 2:
                    imgIntensidad.setImageResource(R.drawable.wifi_medioalto);
                    break;
                case 3:
                    imgIntensidad.setImageResource(R.drawable.wifi_alto);
                    break;
            }
            return convertView;
        }
    }
    @SuppressLint("StaticFieldLeak")
    private class BluetoothReceiveTask extends AsyncTask<Void, Void, Boolean> {
        String receivedMessage;
        String IDdispositivo;
        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                inputStream = bluetoothSocket.getInputStream();
                connected = true;
                byte[] buffer = new byte[1024];
                int bytesRead;
                while (connected) {
                    bytesRead = inputStream.read(buffer);
                    receivedMessage = new String(buffer, 0, bytesRead).trim();
                    Log.d("BluetoothReceiveTask", "Recibi: " + receivedMessage);
                    if (receivedMessage.equals("Exito")) {
                        connected=false;
                        return true;
                    }
                    else if (receivedMessage.length() == 6 && receivedMessage.startsWith("0")) {
                        IDdispositivo = receivedMessage;
                        Log.d("BluetoothReceiveTask", "Recibi: " + receivedMessage);
                        SharedPreferences sharedPreferences = getSharedPreferences("user_data", Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString("ID_dispositivo",receivedMessage);
                        editor.putString("Nombre_dispositivo",nombre_disp);
                        editor.apply();
                        connected=false;
                        disconnectBluetooth();
                        runOnUiThread(() -> {
                            finalizar_configuracion_layout.setTranslationY(0);
                            finalizar_configuracion_layout.setAlpha(1.0f);
                            ViewPropertyAnimator animator1 = finalizar_configuracion_layout.animate()
                                    .alpha(0.0f)
                                    .translationY(-500)
                                    .setDuration(500);
                            animator1.start();
                            animator1.setListener(new Animator.AnimatorListener() {
                                @Override
                                public void onAnimationStart(@NonNull Animator animation) {

                                }

                                @Override
                                public void onAnimationEnd(@NonNull Animator animation) {
                                    finalizar_configuracion_layout.setVisibility(View.GONE);
                                    mostrar_mensaje_exito();
                                }

                                @Override
                                public void onAnimationCancel(@NonNull Animator animation) {

                                }

                                @Override
                                public void onAnimationRepeat(@NonNull Animator animation) {

                                }
                            });
                        });
                    }else if(receivedMessage.equals("ErrorWifi")) {
                        runOnUiThread(() -> {
                            loading_wifi.setVisibility(View.GONE);
                            error_wifi.playAnimation();
                            error_wifi.setVisibility(VISIBLE);
                            caja_contra_red.setError("Contraseña incorrecta");
                            aparecerTeclado();
                            new Handler(Looper.getMainLooper()).postDelayed(() -> error_wifi.pauseAnimation(), 1700);
                        });
                    }
                }
            } catch (IOException e) {
                Log.e("BluetoothReceiveTask", "Error al leer datos", e);
            }
            return false;
        }
        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                UploadDeviceService.subirVariableEnSegundoPlano("ID" + IDdispositivo, new UploadDeviceService.UploadCallback() {
                    @Override
                    public void onUploadResult(boolean success) {
                        if (success) {
                            runOnUiThread(() -> {
                                loading_wifi.setVisibility(View.GONE);
                                ok_wifi.setVisibility(View.VISIBLE);
                                connected = false;
                                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                    ok_wifi.setVisibility(View.GONE);
                                    finalizemos_config();
                                }, 1380);
                            });
                        }
                    }
                });
            }
        }
        private void finalizemos_config(){
            desaparecercard();
            ocultarTeclado();
            fondo.setTranslationX(0);
            fondo.setAlpha(1.0f);
            ViewPropertyAnimator animator = fondo.animate()
                    .alpha(0.0f)
                    .translationX(-300)
                    .setDuration(500);
            animator.start();
            animator.setListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(@NonNull Animator animation) {

                }

                @Override
                public void onAnimationEnd(@NonNull Animator animation) {
                        fondo.setVisibility(View.GONE);
                }

                @Override
                public void onAnimationCancel(@NonNull Animator animation) {

                }

                @Override
                public void onAnimationRepeat(@NonNull Animator animation) {

                }
            });
            finalizar_configuracion_layout.setTranslationX(300);
            finalizar_configuracion_layout.setAlpha(0.0f);
            finalizar_configuracion_layout.setVisibility(VISIBLE);
            ViewPropertyAnimator animator2 = finalizar_configuracion_layout.animate()
                    .alpha(1.0f)
                    .translationX(0)
                    .setDuration(800);
            animator2.start();
        }
        private void disconnectBluetooth() {
            try {
                connected = false;
                if (inputStream != null) {
                    inputStream.close();
                }
                if (bluetoothSocket != null) {
                    bluetoothSocket.close();
                }
            } catch (IOException e) {
                Log.e("BluetoothReceiveTask", "Error al desconectar Bluetooth", e);
            }
        }
        private void mostrar_mensaje_exito(){
            transicion.setVisibility(VISIBLE);
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                transicion.setVisibility(View.GONE);
                final_layout.setAlpha(0.0f);
                final_layout.setVisibility(VISIBLE);
                final_layout.setTranslationY(-300);
                ViewPropertyAnimator animator1 = final_layout.animate()
                        .alpha(1.0f)
                        .translationX(0)
                        .setDuration(500);
                animator1.start();
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    ViewPropertyAnimator animator2 = final_layout.animate()
                            .alpha(0.0f)
                            .setDuration(500);
                    animator2.start();
                    startActivity(new Intent(DeviceSettings2.this, MenuuActivity.class));
                    DeviceSettings2.this.finish();
                },3000);

            },1500);
        }
    }
    private void ocultarTeclado() {
        View currentFocus = getCurrentFocus();
        if (currentFocus != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
            }
        }
    }
    private void aparecerTeclado() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
        }
    }

    private void aparecerConfirmacion(){
        confirmacion_salir.setVisibility(VISIBLE);
        confirmacion_salir.setAlpha(0.0f);
        confirmacion_salir.setTranslationX(300);
        ViewPropertyAnimator animator = confirmacion_salir.animate()
                .translationX(0)
                .alpha(1.0f)
                .setDuration(500);
        animator.start();
        back='c';
    }
    private void desaparecerConfirmacion(){
        confirmacion_salir.setAlpha(1.0f);
        confirmacion_salir.setTranslationX(0);
        ViewPropertyAnimator animator = confirmacion_salir.animate()
                .translationX(300)
                .alpha(0.0f)
                .setDuration(500);
        animator.start();
        back='a';
        new Handler(Looper.getMainLooper()).postDelayed(() -> confirmacion_salir.setVisibility(View.GONE),500);
    }
    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
        if(back == 'a') {
            aparecerConfirmacion();
        } else if (back == 'b') {
            desaparecercard();
        } else if (back == 'c') {
            desaparecerConfirmacion();
        }
    }
}
