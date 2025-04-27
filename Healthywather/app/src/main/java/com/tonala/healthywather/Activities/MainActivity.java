package com.tonala.healthywather.Activities;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.SignInMethodQueryResult;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.tonala.healthywather.R;
import com.tonala.healthywather.utils.MessagingService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements TextWatcher {
    private Boolean errores = false;
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    int RC_SIGN_IN = 1;
    private ImageView myImageView;
    private Dialog dialogo_restablecer;
    private TextInputLayout cajaCorreo, cajaContraseña, CajaCorreoR;
    private TextInputEditText editaCorreoLogin, editaContraseñaLogin, edt_RestablecerContra;
    private final String TAG = "GoogleSignIn";
    private ConstraintLayout Login, layout_EnviarCorreo, layout_CorreoEnviado;
    private TextView OlvidoContrasena;
    private ImageButton iniciaConGoogle;
    private Button crearCuenta, iniciarSesion;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        cambiarColorBarraNotificaciones(R.color.white);
        inicializarVistas();
        configurarFirebase();
        configurarAnimacionLogo();
        configurarBotonesAccion();
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
        handler = null;
        mAuth.removeAuthStateListener(null);
    }

    private void cambiarColorBarraNotificaciones(@ColorRes int colorResId) {
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(this, colorResId));
    }

    private void inicializarVistas() {
        Login = findViewById(R.id.login);
        Login.setVisibility(View.GONE);
        OlvidoContrasena = findViewById(R.id.txt_OlvidoContrasena);
        editaCorreoLogin = findViewById(R.id.txtinputcorreo);
        editaCorreoLogin.addTextChangedListener(this);
        editaContraseñaLogin = findViewById(R.id.txtinputcontraseña);
        editaContraseñaLogin.addTextChangedListener(this);
        cajaCorreo = findViewById(R.id.editcorreo);
        cajaContraseña = findViewById(R.id.editcontraseña);
        iniciaConGoogle = findViewById(R.id.iniciacongoogle);
        iniciarSesion = findViewById(R.id.iniciarsesion);
        crearCuenta = findViewById(R.id.crearcuenta);
        myImageView = findViewById(R.id.logo);
    }

    private void configurarFirebase() {
        FirebaseApp.initializeApp(this);
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        mAuth = FirebaseAuth.getInstance();
    }

    private void configurarAnimacionLogo() {
        myImageView.setAlpha(0f);
        myImageView.animate().alpha(1f).setDuration(300).withEndAction(this::comprobarUsuarioActual).start();
    }

    private void configurarBotonesAccion() {
        iniciaConGoogle.setOnClickListener(v -> signIn());
        crearCuenta.setOnClickListener(v -> {
            ocultarTeclado();
            Intent crearcuenta = new Intent(MainActivity.this, CreateAccountActivity.class);
            startActivity(crearcuenta);
            overridePendingTransition(R.anim.deslizamiento_arriba, R.anim.desvanecer);
        });
        iniciarSesion.setOnClickListener(v -> {
            ocultarTeclado();
            errores = validarLogin();
        });
        OlvidoContrasena.setOnClickListener(v -> iniciarMsgRestablecer());
    }

    private void iniciarMsgRestablecer() {
        @SuppressLint("InflateParams") View mensaje_restablcer = getLayoutInflater().inflate(R.layout.mensaje_olvidocontrasena, null);
        layout_EnviarCorreo = mensaje_restablcer.findViewById(R.id.layout_EnviarCorreo);
        layout_CorreoEnviado = mensaje_restablcer.findViewById(R.id.layout_CorreoEnviado);
        Button restablecerContra = mensaje_restablcer.findViewById(R.id.btn_RestablecerContra);
        Button aceptarRC = mensaje_restablcer.findViewById(R.id.btn_AceptarRC);
        edt_RestablecerContra = mensaje_restablcer.findViewById(R.id.txtinputcorreoR);
        CajaCorreoR = mensaje_restablcer.findViewById(R.id.editcorreo);
        dialogo_restablecer = new Dialog(this, R.style.DialogTheme);
        dialogo_restablecer.setContentView(mensaje_restablcer);
        Objects.requireNonNull(dialogo_restablecer.getWindow()).setLayout(1000, 1100);
        ConstraintLayout dialogContainer = dialogo_restablecer.findViewById(R.id.layout_msgRestablecer);
        dialogContainer.setBackground(ContextCompat.getDrawable(this, R.drawable.fondo_mensaje));
        restablecerContra.setOnClickListener(v -> {
            String email = Objects.requireNonNull(edt_RestablecerContra.getText()).toString().trim();
            if (isValidEmail(email)) {
                CajaCorreoR.setErrorEnabled(false);
                executorService.execute(() -> mAuth.fetchSignInMethodsForEmail(email).addOnCompleteListener(task -> handler.post(() -> {
                    if (task.isSuccessful()) {
                        List<String> signInMethods = task.getResult().getSignInMethods();
                        if (signInMethods != null) {
                            Log.d("EmailCheck", "Correo registrado: " + email);
                            mAuth.sendPasswordResetEmail(email).addOnCompleteListener(resetTask -> handler.post(() -> {
                                if (resetTask.isSuccessful()) {
                                    Log.d("PasswordReset", "Correo de restablecimiento enviado: " + email);
                                    layout_EnviarCorreo.animate().alpha(0.0f).setDuration(500).withEndAction(() -> {
                                        layout_EnviarCorreo.setVisibility(View.GONE);
                                        layout_CorreoEnviado.setAlpha(0.0f);
                                        layout_CorreoEnviado.setVisibility(View.VISIBLE);
                                        layout_CorreoEnviado.animate().alpha(1.0f).setDuration(500).start();
                                    }).start();
                                } else {
                                    Log.e("ResetPassword", "Error al enviar el correo de restablecimiento", resetTask.getException());
                                    Toast.makeText(MainActivity.this, "Error al enviar el correo de restablecimiento", Toast.LENGTH_LONG).show();
                                }
                            }));
                        } else {
                            Log.d("EmailCheck", "Correo no registrado: " + email);
                            CajaCorreoR.setError("Correo no registrado");
                            Toast.makeText(MainActivity.this, "Este correo no está registrado", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Log.e("FetchSignInMethods", "Error al verificar el correo", task.getException());
                        Toast.makeText(MainActivity.this, "Error al verificar el correo", Toast.LENGTH_LONG).show();
                    }
                })));
            } else {
                CajaCorreoR.setError("Correo inválido");
            }
        });
        aceptarRC.setOnClickListener(v -> dialogo_restablecer.cancel());
        dialogo_restablecer.show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            if (task.isSuccessful()) {
                try {
                    GoogleSignInAccount account = task.getResult(ApiException.class);
                    Log.d(TAG, "firebaseAuthWithGoogle:" + account.getId());
                    firebaseAuthWithGoogle(account.getIdToken());
                } catch (ApiException e) {
                    Log.w(TAG, "Google sign in failed with status code: " + e.getStatusCode(), e);
                }
            } else {
                Toast.makeText(this, "Se canceló el inicio de sesión", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void Tokens() {
        startService(new Intent(this, MessagingService.class));
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                Log.d(TAG, "signInWithCredential:success");
                String nombre = Objects.requireNonNull(mAuth.getCurrentUser()).getDisplayName();
                String correo = mAuth.getCurrentUser().getEmail();
                String uid = mAuth.getCurrentUser().getUid();
                if (guardarInformacionUsuarioFirebase(uid, nombre, correo)) {
                    toastCorrecto();
                    handler.postDelayed(this::iniciarActividadMenuu, 2600);
                }
            } else {
                Log.w(TAG, "signInWithCredential:failure", task.getException());
            }
        });
    }

    private void iniciarActividadMenuu() {
        Intent menuIntent = new Intent(MainActivity.this, MenuuActivity.class);
        startActivity(menuIntent);
        this.finish();
    }

    private void comprobarUsuarioActual() {
        executorService.execute(() -> {
            FirebaseUser user = mAuth.getCurrentUser();
            handler.post(() -> handler.postDelayed(() -> {
                if (user != null) {
                    silog(myImageView);
                } else {
                    nolog();
                }
            }, 1000));
        });
    }

    private void nolog() {
        int totalTranslationY = 1000;
        float finalScale = 0.5f;
        ValueAnimator animator = ValueAnimator.ofFloat(0, -totalTranslationY);
        animator.setInterpolator(new LinearInterpolator());
        animator.setDuration(500);
        animator.addUpdateListener(animation -> {
            float translationY = (float) animation.getAnimatedValue();
            myImageView.setTranslationY(translationY);
            float scale = 1 - (translationY / -totalTranslationY) * (1 - finalScale);
            myImageView.setScaleX(scale);
            myImageView.setScaleY(scale);
            if (translationY <= -540) {
                animator.cancel();
                Login.setAlpha(0f);
                Login.setVisibility(View.VISIBLE);
                Login.animate().alpha(1f).setDuration(500).start();
            }
        });
        animator.start();
    }

    private void silog(ImageView imageView) {
        imageView.animate().alpha(0f).setDuration(300).withEndAction(() -> {
            imageView.setVisibility(View.GONE);
            Tokens();
            iniciarActividadMenuu();
        }).start();
    }

    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

    @Override
    public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
        if(errores){
            if (start + count > 0) {
                if (charSequence.hashCode() == Objects.requireNonNull(editaCorreoLogin.getText()).hashCode()) {
                    validarCorreo(charSequence.toString());
                } else if (charSequence.hashCode() == Objects.requireNonNull(editaContraseñaLogin.getText()).hashCode()) {
                    cajaContraseña.setErrorEnabled(false);
                }
            }
        }
    }

    @Override
    public void afterTextChanged(Editable s) {}

    private boolean validarCorreo(String correo) {
        if (correo.isEmpty()) {
            cajaCorreo.setError("El correo está vacío");
            return true;
        } else if (!isValidEmail(correo)) {
            cajaCorreo.setError("Correo inválido");
            return true;
        } else {
            cajaCorreo.setErrorEnabled(false);
            executorService.execute(() -> mAuth.fetchSignInMethodsForEmail(correo).addOnCompleteListener(task -> handler.post(() -> {
                if (task.isSuccessful()) {
                    SignInMethodQueryResult result = task.getResult();
                    List<String> signInMethods = result.getSignInMethods();
                    if (signInMethods != null && !signInMethods.isEmpty()) {
                        Toast.makeText(MainActivity.this, "Este correo está registrado con otro método de inicio", Toast.LENGTH_LONG).show();
                    }
                } else {
                    Exception exception = task.getException();
                    if (exception != null) {
                        exception.printStackTrace();
                    }
                }
            })));
            return false;
        }
    }

    private boolean isValidEmail(CharSequence target) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches();
    }

    private boolean validarLogin() {
        String correo = editaCorreoLogin.getText().toString();
        String contraseña = editaContraseñaLogin.getText().toString();
        boolean errorCorreo = validarCorreo(correo);
        if (contraseña.isEmpty()) {
            cajaContraseña.setError("La contraseña está vacía");
            return true;
        } else {
            cajaContraseña.setErrorEnabled(false);
        }
        if (!errorCorreo) {
            mAuth.signInWithEmailAndPassword(correo, contraseña).addOnCompleteListener(this, task -> {
                if (task.isSuccessful() && mAuth.getCurrentUser().isEmailVerified()) {
                    DatabaseReference usuarioRef = FirebaseDatabase.getInstance().getReference("usuarios").child(mAuth.getCurrentUser().getUid());
                    usuarioRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists()) {
                                String nombre = dataSnapshot.child("nombre").getValue(String.class);
                                String telefono = dataSnapshot.child("telefono").getValue(String.class);
                                String fotoPerfilURL = dataSnapshot.child("imagen").getValue(String.class);
                                if (fotoPerfilURL == null) {
                                    fotoPerfilURL = "";
                                }
                                guardarDatosUsuarioLocalmente(nombre, correo, contraseña, telefono, fotoPerfilURL);
                            } else {
                                Toast.makeText(MainActivity.this, "No existe la tabla", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {}
                    });
                    toastCorrecto();
                    handler.postDelayed(this::iniciarActividadMenuu, 2600);
                } else {
                    manejarErroresLogin(task.getException());
                }
            });
        }
        return !errorCorreo;
    }

    private void manejarErroresLogin(Exception exception) {
        String errorMessage = exception.getMessage();
        Log.e("FirebaseError", errorMessage);
        if (exception instanceof FirebaseAuthInvalidUserException) {
            cajaCorreo.setError("Correo incorrecto");
        } else if (exception instanceof FirebaseAuthInvalidCredentialsException) {
            cajaContraseña.setError("Contraseña incorrecta");
        } else if (mAuth.getCurrentUser() != null && !mAuth.getCurrentUser().isEmailVerified()) {
            Toast.makeText(MainActivity.this, "No se verificó el correo", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(MainActivity.this, "No existe la cuenta", Toast.LENGTH_LONG).show();
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

    private static class ResultadoGuardado {
        boolean exito;

        ResultadoGuardado(boolean exito) {
            this.exito = exito;
        }
    }

    private boolean guardarInformacionUsuarioFirebase(String uid, String nombre, String correo) {
        String contraseña = "";
        String telefono = "";
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("nombre", nombre);
        userInfo.put("correo", correo);
        userInfo.put("contrasena", "");
        userInfo.put("telefono", "");

        DatabaseReference usuarioRef = FirebaseDatabase.getInstance().getReference("usuarios").child(uid);
        final ResultadoGuardado resultado = new ResultadoGuardado(true);

        usuarioRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    usuarioRef.setValue(userInfo).addOnSuccessListener(aVoid -> {
                        guardarDatosUsuarioLocalmente(nombre, correo, contraseña, telefono, null);
                        Log.d("Firebase", "Información del usuario guardada exitosamente");
                    }).addOnFailureListener(e -> {
                        resultado.exito = false;
                        Toast.makeText(MainActivity.this, "ERROR DE REGISTRO", Toast.LENGTH_LONG).show();
                        Log.e("FirebaseError", "Error al guardar la información del usuario: " + e.getMessage());
                        e.printStackTrace();
                    });
                } else {
                    usuarioRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists()) {
                                String nombre = dataSnapshot.child("nombre").getValue(String.class);
                                String contraseña = dataSnapshot.child("contrasena").getValue(String.class);
                                String telefono = dataSnapshot.child("telefono").getValue(String.class);
                                String fotoPerfilURL = dataSnapshot.child("imagen").getValue(String.class);
                                if (fotoPerfilURL == null) {
                                    fotoPerfilURL = "";
                                }
                                if (contraseña == null) {
                                    contraseña = "";
                                }
                                if (telefono == null) {
                                    telefono = "";
                                }
                                guardarDatosUsuarioLocalmente(nombre, correo, contraseña, telefono, fotoPerfilURL);
                            } else {
                                Toast.makeText(MainActivity.this, "No existe la tabla", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {}
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                resultado.exito = false;
                Log.e("FirebaseError", "Error en la lectura de datos: " + error.getMessage());
            }
        });
        return resultado.exito;
    }

    private void guardarDatosUsuarioLocalmente(String nombre, String correo, String contraseña, String telefono, String imagen) {
        SharedPreferences sharedPreferences = getSharedPreferences("user_data", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("nombre", nombre);
        editor.putString("correo", correo);
        editor.putString("contraseña", contraseña);
        editor.putString("telefono", telefono);
        editor.putString("imagen", imagen);
        editor.apply();
    }

    public void toastCorrecto() {
        LayoutInflater lyt = getLayoutInflater();
        View v = lyt.inflate(R.layout.mensaje_correcto, findViewById(R.id.mensaeje_inicioC));
        Toast toast = new Toast(getApplicationContext());
        toast.setGravity(Gravity.CENTER_VERTICAL | Gravity.BOTTOM, 0, 200);
        toast.setDuration(Toast.LENGTH_LONG);
        toast.setView(v);
        toast.show();
    }
}