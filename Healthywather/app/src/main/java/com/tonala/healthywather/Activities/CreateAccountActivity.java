package com.tonala.healthywather.Activities;


import com.airbnb.lottie.LottieAnimationView;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;

import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.SignInMethodQueryResult;
import com.tonala.healthywather.R;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CreateAccountActivity extends AppCompatActivity implements TextWatcher {
    TextInputEditText edtnombre, edtccorreo, edtccontrasena, edtrepcontrasena, edttelefono;
    TextInputLayout caja_cnom, cajaccorreo, caja_crearcontra, caja_repcontra, caja_creartelefono;
    CheckBox terminos;
    Button crearcuentabtn,aceptar_confirmacion;
    ImageButton regresar;
    LottieAnimationView check;
    boolean revisar_errores = false;
    private FirebaseAuth mAuth;
    ConstraintLayout confirmacion_correo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crear_cuenta);
        // EDIT TEXTS
        edtnombre = findViewById(R.id.crear_nombre);
        edtccorreo = findViewById(R.id.crear_correo);
        edtccontrasena = findViewById(R.id.crear_contraseña);
        edtrepcontrasena = findViewById(R.id.repetir_contraseña);
        edttelefono = findViewById(R.id.crear_telefono);
        // CAJAS LAYOUT
        caja_cnom = findViewById(R.id.crear_nombre_layout);
        cajaccorreo = findViewById(R.id.crear_correo_layout);
        caja_crearcontra = findViewById(R.id.crear_contraseña_layout);
        caja_repcontra = findViewById(R.id.repetir_contraseña_layout);
        caja_creartelefono = findViewById(R.id.crear_telefono_layout);
        terminos = findViewById(R.id.terminos_y_condiciones);
        crearcuentabtn = findViewById(R.id.crearlacuenta);
        regresar = findViewById(R.id.btn_back);
        //Listeners text changed
        edtnombre.addTextChangedListener(this);
        edtccorreo.addTextChangedListener(this);
        edtccontrasena.addTextChangedListener(this);
        edtrepcontrasena.addTextChangedListener(this);
        edttelefono.addTextChangedListener(this);
        confirmacion_correo = findViewById(R.id.confirmacion_correo);
        aceptar_confirmacion = findViewById(R.id.btn_aceptar_confirmacion);
        check = findViewById(R.id.checkanim_correo);
        //FIREBASE
        mAuth = FirebaseAuth.getInstance();
        crearcuentabtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ocultarTeclado();
                revisar_errores = true;
                try {
                    Validarcreacion();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        aceptar_confirmacion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CreateAccountActivity.this.finish();
            }
        });
        regresar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //startActivity(new Intent(CreateAccountActivity.this, MainActivity.class));
                onBackPressed();
                overridePendingTransition(R.anim.aparecer,R.anim.deslizamiento_abajo);
            }
        });
    }
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.aparecer, R.anim.deslizamiento_abajo);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (revisar_errores) {
            String nombre = edtnombre.getText().toString();
            String correo = edtccorreo.getText().toString();
            String contraseña = edtccontrasena.getText().toString();
            String confirmarContraseña = edtrepcontrasena.getText().toString();
            String telefono = edttelefono.getText().toString();
            // NOMBRE
            if (nombre.length() <= 3) {
                setError(caja_cnom, "El nombre debe tener más de 3 caracteres");
            } else if (nombre.length() > 16) {
                setError(caja_cnom, "El nombre es muy largo");
            } else if (nombre.isEmpty()) {
                setError(caja_cnom, "El nombre está vacío");
            } else {
                clearError(caja_cnom);
            }
            // CORREO
            if (correo.isEmpty()) {
                setError(cajaccorreo, "El correo está vacío");
            } else if (!isValidEmail(correo)) {
                setError(cajaccorreo, "Correo electrónico inválido");
            } else {
                clearError(cajaccorreo);
            }
            // CONTRASEÑA-REPETIR CONTRASEÑA
            if (contraseña.isEmpty()) {
                setError(caja_crearcontra, "Contraseña vacía");
            } else if (contraseña.length()<8) {
                setError(caja_crearcontra, "La contraseña debe tener al menos 8 caracteres");
            }else {
                clearError(caja_crearcontra);
            }

            if (confirmarContraseña.isEmpty()) {
                setError(caja_repcontra, "Confirma la contraseña");
            } else if (!confirmarContraseña.equals(contraseña)) {
                setError(caja_repcontra, "Las contraseñas no coinciden");
            } else {
                clearError(caja_repcontra);
            }
            // TELEFONO
            if (telefono.isEmpty()) {
                setError(caja_creartelefono, "El teléfono está vacío");
            } else if (!telefono.startsWith("33") || telefono.length() != 10) {
                setError(caja_creartelefono, "El teléfono debe empezar con 33 y debe tener 10 dígitos");
            } else {
                clearError(caja_creartelefono);
            }
        }
    }
    private boolean isValidEmail(String email) {
        String emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";
        return !email.isEmpty() && email.matches(emailPattern);
    }
    private void setError(TextInputLayout layout, String error) {
        layout.setError(error);
        revisar_errores = true;
    }
    private void clearError(TextInputLayout layout) {
        layout.setError(null);
        layout.setErrorEnabled(false);
    }
    @Override
    public void afterTextChanged(Editable s) {
    }
    private boolean Validarcreacion() {
        boolean hayErrores = false;
        // NOMBRE
        String nombre = edtnombre.getText().toString();
        if (nombre.length() <= 3) {
            setError(caja_cnom, "El nombre debe tener más de 3 caracteres");
            hayErrores = true;
        }else if (nombre.length() >16) {
            setError(caja_cnom, "El nombre es muy largo");
            hayErrores = true;
        } else if (nombre.isEmpty()) {
            setError(caja_cnom, "El nombre está vacío");
            hayErrores = true;
        } else {
            clearError(caja_cnom);
        }
        // CORREO
        String correo = edtccorreo.getText().toString();
        if (correo.isEmpty()) {
            setError(cajaccorreo, "El correo está vacío");
            hayErrores = true;
        } else if (!isValidEmail(correo)) {
            setError(cajaccorreo, "Correo electrónico inválido");
            hayErrores = true;
        } else {
            clearError(cajaccorreo);
        }

        // CONTRASEÑA-REPETIR CONTRASEÑA
        String contraseña = edtccontrasena.getText().toString();
        String confirmarContraseña = edtrepcontrasena.getText().toString();
        if (contraseña.isEmpty()) {
            setError(caja_crearcontra, "Contraseña vacía");
            hayErrores = true;
        } else if (contraseña.length()<8) {
            setError(caja_crearcontra, "La contraseña debe tener al menos 8 caracteres");
            hayErrores = true;
        } else {
            clearError(caja_crearcontra);
        }
        if (confirmarContraseña.isEmpty()) {
            setError(caja_repcontra, "Confirma la contraseña");
            hayErrores = true;
        } else if (!confirmarContraseña.equals(contraseña)) {
            setError(caja_repcontra, "Las contraseñas no coinciden");
            hayErrores = true;
        } else {
            clearError(caja_repcontra);
        }
        // TELEFONO
        String telefono = edttelefono.getText().toString();
        if (telefono.isEmpty()) {
            setError(caja_creartelefono, "El teléfono está vacío");
            hayErrores = true;
        } else if (!telefono.startsWith("33") || telefono.length() != 10) {
            setError(caja_creartelefono, "El teléfono debe empezar con 33 y debe tener 10 dígitos");
            hayErrores = true;
        } else {
            clearError(caja_creartelefono);
        }
        // Muestra el mensaje de Toast solo si hay errores
        if (hayErrores) {
            toast_revisar_campos();
        } else if (!terminos.isChecked()) {
            toast_error_terminos();
        } else {
            mAuth.fetchSignInMethodsForEmail(correo)
                    .addOnCompleteListener(new OnCompleteListener<SignInMethodQueryResult>() {
                        @Override
                        public void onComplete(@NonNull Task<SignInMethodQueryResult> task) {
                            if (task.isSuccessful()) {
                                SignInMethodQueryResult result = task.getResult();
                                List<String> signInMethods = result.getSignInMethods();
                                if (signInMethods != null && !signInMethods.isEmpty()) {
                                    Log.d("Toast", "Size: " + signInMethods.size());
                                    Log.d("TOAST", "estoy en toast");
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Log.d("TOAST", "envie en toast");
                                            Toast.makeText(CreateAccountActivity.this, "Este correo está registrado con otro método de inicio", Toast.LENGTH_LONG).show();
                                        }
                                    });
                                } else {
                                    mAuth.createUserWithEmailAndPassword(correo, contraseña)
                                            .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                                                @Override
                                                public void onComplete(@NonNull Task<AuthResult> task) {
                                                    if (task.isSuccessful()) {
                                                        // Usuario creado exitosamente
                                                        String uid = mAuth.getCurrentUser().getUid();
                                                        guardarInformacionUsuario(uid, nombre, correo, contraseña, telefono);
                                                        mAuth.getCurrentUser().sendEmailVerification();
                                                        confirmacion_correo.setVisibility(View.VISIBLE);
                                                        confirmacion_correo.setTranslationY(200);
                                                        confirmacion_correo.setAlpha(0.0f);
                                                        ViewPropertyAnimator anim1 = confirmacion_correo.animate()
                                                                .alpha(1.0f)
                                                                .translationY(0)
                                                                .setDuration(500);
                                                        anim1.start();
                                                        check.setVisibility(View.VISIBLE);
                                                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                check.pauseAnimation();
                                                            }
                                                        },1360);
                                                    } else {
                                                        // Manejar errores al crear el usuario
                                                        Exception exception = task.getException();
                                                        if (exception != null) {
                                                            if (exception instanceof FirebaseAuthUserCollisionException) {
                                                                // El correo ya está en uso por otra cuenta
                                                                runOnUiThread(new Runnable() {
                                                                    @Override
                                                                    public void run() {
                                                                        toast_error_correo();
                                                                    }
                                                                });
                                                            } else {
                                                                // Otras excepciones
                                                                Log.e("FirebaseError", exception.getMessage());
                                                                exception.printStackTrace();
                                                                Toast.makeText(CreateAccountActivity.this, "Error de registro", Toast.LENGTH_LONG).show();
                                                            }
                                                        }
                                                    }
                                                }
                                            });
                                }
                            } else {
                                // Maneja cualquier error al verificar los métodos de inicio de sesión
                                Exception exception = task.getException();
                                if (exception != null) {
                                    Log.e("FirebaseError", exception.getMessage());
                                    exception.printStackTrace();
                                }
                            }
                        }
                    });
        }
        return !hayErrores;
    }
    private void guardarInformacionUsuario(String uid, String nombre, String correo, String contrasena, String telefono) {
        // Crear un mapa con la información del usuario
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("nombre", nombre);
        userInfo.put("correo", correo);
        userInfo.put("contrasena", contrasena);
        userInfo.put("telefono", telefono);
        // Obtener la referencia al nodo del usuario en la base de datos
        DatabaseReference usuarioRef = FirebaseDatabase.getInstance().getReference("usuarios").child(uid);

        // Guardar la información del usuario en la base de datos
        usuarioRef.setValue(userInfo)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        // Éxito al guardar la información del usuario en la base de datos
                        Toast.makeText(CreateAccountActivity.this, "REGISTRO COMPLETADO", Toast.LENGTH_LONG).show();
                        Log.d("Firebase", "Información del usuario guardada exitosamente");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Manejar errores al guardar la información del usuario en la base de datos
                        Toast.makeText(CreateAccountActivity.this, "ERROR DE REGISTRO", Toast.LENGTH_LONG).show();
                        Log.e("FirebaseError", "Error al guardar la información del usuario: " + e.getMessage());
                        e.printStackTrace();
                    }
                });
    }
    public void toast_revisar_campos(){
        LayoutInflater lyt = getLayoutInflater();
        View v = lyt.inflate(R.layout.mensaje_revisa_campos,(ViewGroup)findViewById(R.id.mensaeje_toast) );
        Toast toast = new Toast(getApplicationContext());
        toast.setGravity(Gravity.CENTER_VERTICAL|Gravity.BOTTOM,0,100);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(v);
        toast.show();
    }
    public void toast_error_correo(){
        LayoutInflater lyt = getLayoutInflater();
        View v = lyt.inflate(R.layout.mensaje_error_correo,(ViewGroup)findViewById(R.id.mensaeje_toast) );
        Toast toast = new Toast(getApplicationContext());
        toast.setGravity(Gravity.CENTER_VERTICAL|Gravity.BOTTOM,0,100);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(v);
        toast.show();
    }
    public void toast_error_terminos(){
        LayoutInflater lyt = getLayoutInflater();
        View v = lyt.inflate(R.layout.mensaje_aceptaterminos,(ViewGroup)findViewById(R.id.mensaeje_toast) );
        Toast toast = new Toast(getApplicationContext());
        toast.setGravity(Gravity.CENTER_VERTICAL|Gravity.BOTTOM,0,100);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(v);
        toast.show();
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
}