package com.tonala.healthywather.Activities;

import android.Manifest;
import android.animation.Animator;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.tonala.healthywather.R;
import com.tonala.healthywather.databinding.ActivityMenuuBinding;
import com.tonala.healthywather.ui.home.HomeFragment;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MenuuActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 1;
    private Dialog dialog;
    private CardView card_btnGuardar, card_btnSoloconectar, card_btnAceptar;
    private LottieAnimationView animok;
    private ActivityMenuuBinding binding;
    private BluetoothSocket mmSocket;
    private InputStream mmInputStream;
    private OutputStream mmOutputStream;
    private GoogleSignInClient mGoogleSignInClient;
    private AppBarConfiguration mAppBarConfiguration;
    private String nwMAC;
    private SharedPreferences sharedPreferences;
    public static FirebaseAuth mAuth = FirebaseAuth.getInstance();
    public static boolean BT_Conectado = false;
    private ExecutorService executorService;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private NetworkChangeReceiver networkChangeReceiver;
    private String[] permissions = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.POST_NOTIFICATIONS
    };
    private int currentPermissionIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMenuuBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.appBarMenuu.toolbar);
        sharedPreferences = getSharedPreferences("user_data", Context.MODE_PRIVATE);
        executorService = Executors.newFixedThreadPool(2);

        String MAC = sharedPreferences.getString("MAC", "");
        Bundle bundle = getIntent().getExtras();
        if (bundle != null && bundle.containsKey("MAC")) {
            nwMAC = bundle.getString("MAC");
            if (nwMAC != null && !nwMAC.isEmpty()) {
                IniciarMSG();
                connectBluetoothInBackground(nwMAC);
                handler.postDelayed(() -> dialog.show(), 1000);
            }
        } else if (!MAC.isEmpty()) {
            connectBluetoothInBackground(MAC);
        }
        setupNavigation();
        setupNavigationView();
    }
    @Override
    protected void onStart(){
        super.onStart();
        PedirPermisos();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdownNow();
        BT_Conectado = false;
        if (mmSocket != null) {
            try {
                mmSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    private void PedirPermisos() {
        if (currentPermissionIndex < permissions.length) {
            String currentPermission = permissions[currentPermissionIndex];
            if (ContextCompat.checkSelfPermission(this, currentPermission) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{currentPermission}, PERMISSION_REQUEST_CODE);
            } else {
                currentPermissionIndex++;
                PedirPermisos();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                currentPermissionIndex++;
                PedirPermisos();
            } else {
                if (grantResults.length > 0) {
                    String deniedPermission = this.permissions[currentPermissionIndex];
                    if (Manifest.permission.ACCESS_FINE_LOCATION.equals(deniedPermission)) {
                        Toast.makeText(this, "Se necesita el permiso para la ubicación", Toast.LENGTH_SHORT).show();
                    } else if (Manifest.permission.BLUETOOTH_CONNECT.equals(deniedPermission)) {
                        Toast.makeText(this, "Se necesita el permiso para el escaneo Bluetooth", Toast.LENGTH_SHORT).show();
                    } else if (Manifest.permission.POST_NOTIFICATIONS.equals(deniedPermission)) {
                        Toast.makeText(this, "Se necesita el permiso para las notificaciones", Toast.LENGTH_SHORT).show();
                    }
                }
                currentPermissionIndex++;
                PedirPermisos();
            }
        }
    }
    private void connectBluetoothInBackground(final String macAddress) {
        if (!executorService.isShutdown()) {
            executorService.execute(() -> connectBluetooth(macAddress));
        }
    }

    private void connectBluetooth(String macAddress) {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            return;
        }

        BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(macAddress);
        if (bluetoothDevice == null) {
            return;
        }

        try {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return;
            }

            mmSocket = bluetoothDevice.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
            mmSocket.connect();
            mmInputStream = mmSocket.getInputStream();
            mmOutputStream = mmSocket.getOutputStream();
            receiveDataInBackground();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void receiveDataInBackground() {
        if (!executorService.isShutdown()) {
            executorService.execute(() -> {
                byte[] buffer = new byte[1024];
                while (true) {
                    try {
                        if (mmInputStream != null) {
                            int bytes = mmInputStream.read(buffer);
                            if (bytes > 0) {
                                processData(Arrays.copyOf(buffer, bytes));
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        break;
                    }
                }
            });
        }
    }

    private void processData(final byte[] data) {
        String dataString = new String(data, StandardCharsets.UTF_8);
        String[] parts = dataString.split(",");
        if (parts.length >= 8) {
            try {
                int tdsValue1 = (int) Float.parseFloat(parts[0]);
                int tdsValue2 = (int) Float.parseFloat(parts[1]);
                int sturb1 = (int) Float.parseFloat(parts[2]);
                int sturb2 = (int) Float.parseFloat(parts[3]);
                double stemp1 = Double.parseDouble(parts[4]);
                double stemp2 = Double.parseDouble(parts[5]);
                double sph1 = Double.parseDouble(parts[6]);
                double sph2 = Double.parseDouble(parts[7]);
                sendDataToFragment(tdsValue1, tdsValue2, sturb1, sturb2, stemp1, stemp2, sph1, sph2);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendDataToFragment(final int tdsValue1, final int tdsValue2, final int sturb1, final int sturb2,
                                    final double stemp1, final double stemp2, final double sph1, final double sph2) {
        handler.post(() -> {
            FragmentManager fragmentManager = getSupportFragmentManager();
            Fragment currentFragment = Objects.requireNonNull(fragmentManager.findFragmentById(R.id.nav_host_fragment_content_menuu)).getChildFragmentManager().getFragments().get(0);
            if (currentFragment instanceof HomeFragment) {
                HomeFragment homeFragment = (HomeFragment) currentFragment;
                homeFragment.receiveData(tdsValue1, tdsValue2, sturb1, sturb2, stemp1, stemp2, sph1, sph2);
            }
        });
    }

    private void ActivarBT(Boolean bt) {
        handler.post(() -> {
            FragmentManager fragmentManager = getSupportFragmentManager();
            Fragment currentFragment = Objects.requireNonNull(fragmentManager.findFragmentById(R.id.nav_host_fragment_content_menuu)).getChildFragmentManager().getFragments().get(0);
            if (currentFragment instanceof HomeFragment) {
                HomeFragment homeFragment = (HomeFragment) currentFragment;
                homeFragment.ModoBT(bt);
            }
        });
    }

    private void IniciarMSG() {
        @SuppressLint("InflateParams") View mensaje_bt = getLayoutInflater().inflate(R.layout.mensajeconexionbt, null);
        card_btnGuardar = mensaje_bt.findViewById(R.id.card_btn_guardar);
        card_btnSoloconectar = mensaje_bt.findViewById(R.id.card_btn_soloconectar);
        card_btnAceptar = mensaje_bt.findViewById(R.id.card_btn_aceptar);
        Button btnGuardar = mensaje_bt.findViewById(R.id.btn_guardar);
        Button btnSoloconectar = mensaje_bt.findViewById(R.id.btn_soloconectar);
        Button btnAceptar = mensaje_bt.findViewById(R.id.btn_aceptar);
        animok = mensaje_bt.findViewById(R.id.anim_ok);
        dialog = new Dialog(this, R.style.DialogTheme);
        dialog.setContentView(mensaje_bt);
        Objects.requireNonNull(dialog.getWindow()).setLayout(1000, 1220);
        dialog.setCancelable(false);
        ConstraintLayout dialogContainer = dialog.findViewById(R.id.layout_main);
        dialogContainer.setBackground(ContextCompat.getDrawable(this, R.drawable.fondo_mensaje));

        btnGuardar.setOnClickListener(v -> {
            animok.setVisibility(View.VISIBLE);
            try {
                mmOutputStream.write("guarda".getBytes());
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("MAC", nwMAC);
                editor.apply();
                ActivarBT(true);
                card_btnGuardar.animate().setDuration(700).alpha(0.0f).translationY(100);
                card_btnSoloconectar.animate().setDuration(700).alpha(0.0f).translationY(100).setListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(@NonNull Animator animation) {}

                    @Override
                    public void onAnimationEnd(@NonNull Animator animation) {
                        card_btnGuardar.setVisibility(View.GONE);
                        card_btnSoloconectar.setVisibility(View.GONE);
                        card_btnAceptar.setVisibility(View.VISIBLE);
                        card_btnAceptar.setAlpha(0.0f);
                        card_btnAceptar.setTranslationY(100);
                        card_btnAceptar.animate().setDuration(700).alpha(1.0f).translationY(0);
                    }

                    @Override
                    public void onAnimationCancel(@NonNull Animator animation) {}

                    @Override
                    public void onAnimationRepeat(@NonNull Animator animation) {}
                });
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        btnSoloconectar.setOnClickListener(v -> {
            try {
                mmOutputStream.write("noguardes".getBytes());
                BT_Conectado = true;
                ActivarBT(BT_Conectado);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            dialog.cancel();
        });

        btnAceptar.setOnClickListener(v -> dialog.cancel());
    }

    private void configureFirebase() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    private void setupNavigation() {
        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_Inicio, R.id.nav_Configuracion, R.id.nav_Servicios, R.id.nav_Acercade, R.id.nav_Cerrars)
                .setOpenableLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_menuu);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
        });

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_Cerrars) {
                mostrarDialogoConfirmacionCerrarSesion();
                return true;
            } else {
                boolean handled = NavigationUI.onNavDestinationSelected(item, navController);
                if (handled) {
                    navController.addOnDestinationChangedListener(new NavController.OnDestinationChangedListener() {
                        @Override
                        public void onDestinationChanged(@NonNull NavController controller, @NonNull NavDestination destination, @Nullable Bundle arguments) {
                            drawer.postDelayed(() -> drawer.closeDrawer(GravityCompat.START), 300);
                            navController.removeOnDestinationChangedListener(this);
                        }
                    });
                }
                return handled;
            }
        });

    }

    private void setupNavigationView() {
        NavigationView navigationView = binding.navView;
        View headerView = navigationView.getHeaderView(0);
        String nombre = sharedPreferences.getString("nombre", "");
        TextView nombreUserTextView = headerView.findViewById(R.id.nombre_user);
        nombreUserTextView.setText(nombre);
    }

    private void mostrarDialogoConfirmacionCerrarSesion() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Cerrar Sesión");
        builder.setMessage("¿Estás seguro de que deseas cerrar sesión?");
        builder.setPositiveButton("Sí", (dialog, which) -> cerrarSesion());
        builder.setNegativeButton("Cancelar", (dialog, which) -> {});
        builder.show();
    }

    private void cerrarSesion() {
        mAuth.signOut();
        mGoogleSignInClient.signOut().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Intent mainActivity = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(mainActivity);
                finish();
                finishAffinity();
            } else {
                Toast.makeText(getApplicationContext(), "No se pudo cerrar sesión con Google", Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menuu, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_menuu);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration) || super.onSupportNavigateUp();
    }
    @Override
    protected void onResume() {
        super.onResume();
        configureFirebase();

        // Registrar el BroadcastReceiver
        if (networkChangeReceiver == null) {
            networkChangeReceiver = new NetworkChangeReceiver();
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkChangeReceiver, intentFilter);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (networkChangeReceiver != null) {
            unregisterReceiver(networkChangeReceiver);
        }
        if (mmInputStream != null) {
            try {
                mmInputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (mmOutputStream != null) {
            try {
                mmOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public class NetworkChangeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            new Handler().postDelayed(() -> {
                if (!isOnline()) {
                    inicializarDialogo();
                }
            }, 1000);
        }

        private boolean isOnline() {
            ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            return networkInfo != null && networkInfo.isConnected();
        }

        private void inicializarDialogo() {
            @SuppressLint("InflateParams") View mensaje_sinconexion = getLayoutInflater().inflate(R.layout.mensajesinconexion, null);
            Button btn_aceptar_sinconexion = mensaje_sinconexion.findViewById(R.id.btn_aceptar_actualizacion);
            Dialog dialogo = new Dialog(MenuuActivity.this, R.style.DialogTheme); // Usar 'MenuuActivity.this' para el contexto
            dialogo.setContentView(mensaje_sinconexion);
            CheckBox noMostrar = mensaje_sinconexion.findViewById(R.id.check_noMostrar);
            Objects.requireNonNull(dialogo.getWindow()).setLayout(900, 1250);
            dialogo.setCancelable(false);
            ConstraintLayout dialogContainer = dialogo.findViewById(R.id.mensajesinconexion);
            dialogContainer.setBackground(ContextCompat.getDrawable(MenuuActivity.this, R.drawable.fondo_mensaje)); // Usar 'MenuuActivity.this' para el contexto
            btn_aceptar_sinconexion.setOnClickListener(v -> {
                if (noMostrar.isChecked()){
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putBoolean("NoMostrar",true);
                    editor.apply();
                }
                dialogo.cancel();
            });
            if(!dialogo.isShowing() && !sharedPreferences.getBoolean("NoMostrar", false)) dialogo.show() ;
        }
    }
}
