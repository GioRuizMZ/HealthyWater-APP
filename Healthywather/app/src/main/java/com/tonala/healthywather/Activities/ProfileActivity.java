package com.tonala.healthywather.Activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.tonala.healthywather.R;
import com.yalantis.ucrop.UCrop;
import com.yalantis.ucrop.model.AspectRatio;
import com.airbnb.lottie.LottieAnimationView;
import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.SignInMethodQueryResult;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.File;
import java.io.FilenameFilter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class ProfileActivity extends AppCompatActivity implements TextWatcher   {
    private TextInputEditText edtnombre, edtccorreo, edtccontrasena, edtrepcontrasena, edttelefono;
    private TextInputLayout caja_cnom, cajaccorreo, caja_crearcontra, caja_repcontra, caja_creartelefono;
    private TextView notiificacion_perfil;
    CardView cardboton;
    boolean revisar_erroresact = false;
    SharedPreferences sharedPreferences;
    FirebaseAuth mAuth;
    FirebaseStorage storage = FirebaseStorage.getInstance();
    Uri cambio_De_imagen = null ;
    private LottieAnimationView animacion_mensaje,animacion_mensaje_ok,animacion_mensaje_error;
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int REQUEST_SELECT_PICTURE = 0x01;
    private static final int REQUEST_CROP_PICTURE = 0x02;
    private String imagenAnteriorURL = null;
    private MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil);
        ConstraintLayout todo = findViewById(R.id.layout_editar_perfil);
        todo.setVisibility(View.GONE);
        sharedPreferences = this.getSharedPreferences("user_data", Context.MODE_PRIVATE);
        if (sharedPreferences.getBoolean("Audio",false) == true){
            mediaPlayer = MediaPlayer.create(this, R.raw.editarperfil);
            mediaPlayer.start();
        }
        //Botones
        Button actualizar = findViewById(R.id.btn_actualizarperfil);
        ImageButton back_perfil = findViewById(R.id.btn_back_perfil);
        // EDIT TEXTS
        edtnombre = findViewById(R.id.actualizar_nombre);
        edtccorreo = findViewById(R.id.actualizar_correo);
        edtccontrasena = findViewById(R.id.actualizar_contraseña);
        edtrepcontrasena = findViewById(R.id.act_repetir_contraseña);
        edttelefono = findViewById(R.id.actualizar_telefono);
        ImageButton editar_foto = findViewById(R.id.btn_editar_foto);
        // CAJAS LAYOUT
        caja_cnom = findViewById(R.id.act_nombre_layout);
        cajaccorreo = findViewById(R.id.act_correo_layout);
        caja_crearcontra = findViewById(R.id.act_contraseña_layout);
        caja_repcontra = findViewById(R.id.act_repetir_contraseña_layout);
        caja_creartelefono = findViewById(R.id.act_telefono_layout);
        todo.setTranslationX(-300);
        todo.setAlpha(0.0f);
        todo.setVisibility(View.VISIBLE);
        ViewPropertyAnimator animator1 = todo.animate()
                .translationX(0)
                .alpha(1.0f)
                .setDuration(500);
        //Listeners text
        edtnombre.addTextChangedListener(this);
        edtccorreo.addTextChangedListener(this);
        edtccontrasena.addTextChangedListener(this);
        edtrepcontrasena.addTextChangedListener(this);
        edttelefono.addTextChangedListener(this);
        //FIREBASE
        mAuth = FirebaseAuth.getInstance();
        //cache
        sharedPreferences = getSharedPreferences("user_data", Context.MODE_PRIVATE);
        //Obtener datos del cache
        String nombre = sharedPreferences.getString("nombre", "");
        String correo = sharedPreferences.getString("correo", "");
        String contraseña = sharedPreferences.getString("contraseña", "");
        String telefono = sharedPreferences.getString("telefono", "");
        String imagenPerfil = sharedPreferences.getString("imagen", "");
        edtnombre.setText(nombre);
        edtccorreo.setText(correo);
        edtccontrasena.setText(contraseña);
        edtrepcontrasena.setText(contraseña);
        if (!imagenPerfil.isEmpty()) {
            Glide.with(this)
                    .load(Uri.parse(imagenPerfil))
                    .into((ShapeableImageView) findViewById(R.id.foto_perfil_edit));
        }
        if (contraseña.isEmpty()){
            caja_repcontra.setHint("Confirma la contraseña");
            caja_crearcontra.setHint("Agrega una contraseña");
        }
        if (telefono.isEmpty()) {
            caja_creartelefono.setHint("Agrega un telefono");
        }
        edttelefono.setText(telefono);
        //Iniciar con texto las cajas

        actualizar.setOnClickListener(v -> {
            revisar_erroresact = true;
            try {
                Validarcreacion();
            } catch (Exception e) {
                Log.e("PerfilActivity", "Error en Validarcreacion: " + e.getMessage());
                e.printStackTrace();
            }
        });
        back_perfil.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        editar_foto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                abrirGaleria();
            }
        });
    }
    private void IniciarMSG() {
        View mensaje_proceso = getLayoutInflater().inflate(R.layout.mensajeactuser, null);
        cardboton = mensaje_proceso.findViewById(R.id.card_btn);
        animacion_mensaje = mensaje_proceso.findViewById(R.id.anim_loading);
        animacion_mensaje_ok = mensaje_proceso.findViewById(R.id.anim_actualizacionl_ok);
        animacion_mensaje_error = mensaje_proceso.findViewById(R.id.anim_actualizacion_error);
        notiificacion_perfil = mensaje_proceso.findViewById(R.id.txt_version);
        animacion_mensaje_ok.setVisibility(View.GONE);
        animacion_mensaje_error.setVisibility(View.GONE);
        Button aceptar = mensaje_proceso.findViewById(R.id.btn_aceptar_actualizacion);
        cardboton.setVisibility(View.GONE);
        Dialog dialogo = new Dialog(this,R.style.DialogTheme);
        dialogo.setContentView(mensaje_proceso);
        dialogo.getWindow().setLayout(830, 1190);
        dialogo.setCancelable(false);
        ConstraintLayout dialogContainer = dialogo.findViewById(R.id.layout_main);
        dialogContainer.setBackground(ContextCompat.getDrawable(this, R.drawable.fondo_mensaje));
        aceptar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialogo.cancel();
                ProfileActivity.this.finish();
            }
        });
        dialogo.show();
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (revisar_erroresact) {
            String nombre = edtnombre.getText().toString();
            String correo = edtccorreo.getText().toString();
            String contraseña = edtccontrasena.getText().toString();
            String confirmarContraseña = edtrepcontrasena.getText().toString();
            String telefono = edttelefono.getText().toString();
            // NOMBRE
            if (nombre.length() <= 3) {
                setError(caja_cnom, "El nombre debe tener más de 3 caracteres");
            } else if (nombre.length() >16) {
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
    private boolean isOnline() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }
    private void setError(TextInputLayout layout, String error) {
        layout.setError(error);
    }
    private void clearError(TextInputLayout layout) {
        layout.setErrorEnabled(false);
    }
    private boolean isValidEmail(String email) {
        String emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";
        return !email.isEmpty() && email.matches(emailPattern);
    }
    @Override
    public void afterTextChanged(Editable s) {

    }
    @Override
    protected void onDestroy(){
        super.onDestroy();

        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
    private void aparecer_btn_aceptar(){
        cardboton.setVisibility(View.VISIBLE);
        cardboton.setAlpha(0.0f);
        cardboton.setTranslationY(30);
        ViewPropertyAnimator anim = cardboton.animate()
                .translationY(0)
                .setDuration(500)
                .alpha(1.0f);
    }
    private boolean Validarcreacion() {
        boolean hayErrores = false;
        // NOMBRE
        String nombre = edtnombre.getText().toString();
        if (nombre.length() <= 3) {
            setError(caja_cnom, "El nombre debe tener más de 3 caracteres");
            hayErrores = true;
        } else if (nombre.length() > 16) {
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
        } else if (contraseña.length() < 8) {
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
        if (hayErrores) {
            toast_revisar_campos();
        } else {
            IniciarMSG();
            if (!isOnline()) {
                animacion_mensaje.setVisibility(View.GONE);
                animacion_mensaje_error.setVisibility(View.VISIBLE);
                notiificacion_perfil.setText("Sin conexión a internet");
                aparecer_btn_aceptar();
            } else {
                try {
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
                                                    Toast.makeText(ProfileActivity.this, "Este correo está registrado con otro método de inicio", Toast.LENGTH_LONG).show();
                                                }
                                            });
                                        } else {
                                            actualizarInformacionUsuarioFirebase(mAuth.getCurrentUser().getUid().toString(), nombre, correo, contraseña, telefono);
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
                } catch (Exception e) {
                    Log.e("FirebaseError", "Error al verificar métodos de inicio de sesión: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }

        return !hayErrores;
    }
    private void actualizarInformacionUsuarioFirebase(String uid, String nombre, String correo, String contrasena, String telefono) {
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("nombre", nombre);
        userInfo.put("correo", correo);
        userInfo.put("contrasena", contrasena);
        userInfo.put("telefono", telefono);

        DatabaseReference usuarioRef = FirebaseDatabase.getInstance().getReference("usuarios").child(uid);
        usuarioRef.child("imagen").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    imagenAnteriorURL = dataSnapshot.getValue(String.class);
                }
                continuarConActualizacion(userInfo, usuarioRef, uid, nombre, correo, contrasena, telefono);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    private void continuarConActualizacion(Map<String, Object> userInfo, DatabaseReference usuarioRef,
                                           String uid, String nombre, String correo, String contrasena, String telefono) {
        if (!TextUtils.isEmpty(imagenAnteriorURL)) {
            StorageReference storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(imagenAnteriorURL);
            storageRef.getMetadata().addOnSuccessListener(metadata -> {
                // El objeto existe, proceder con la eliminación
                String metadataUid = metadata.getCustomMetadata("uid");
                if (metadataUid != null && metadataUid.equals(uid)) {
                    storageRef.delete().addOnSuccessListener(aVoid -> {
                        Log.d("FirebaseStorage", "Imagen anterior eliminada");
                        subirNuevaImagen(userInfo, usuarioRef, uid, nombre, correo, contrasena, telefono);
                    }).addOnFailureListener(exception -> {
                        Log.e("FirebaseStorage", "Error al eliminar imagen anterior: " + exception.getMessage());
                    });
                } else {
                    // La imagen almacenada no corresponde al UID del usuario
                    // Agrega tu lógica para manejar este caso, por ejemplo, mostrar un mensaje de advertencia
                    Log.w("FirebaseStorage", "La imagen almacenada no corresponde al UID del usuario");
                    subirNuevaImagen(userInfo, usuarioRef, uid, nombre, correo, contrasena, telefono);
                }
            }).addOnFailureListener(exception -> {
                // El objeto no existe o hay un error al obtener sus metadatos
                Log.e("FirebaseStorage", "Error al obtener metadatos de la imagen anterior: " + exception.getMessage());
                subirNuevaImagen(userInfo, usuarioRef, uid, nombre, correo, contrasena, telefono);
            });
        } else {
            subirNuevaImagen(userInfo, usuarioRef, uid, nombre, correo, contrasena, telefono);
        }
    }

    private void subirNuevaImagen(Map<String, Object> userInfo, DatabaseReference usuarioRef,
                                  String uid, String nombre, String correo, String contrasena, String telefono) {
        if (cambio_De_imagen != null) {
            String fileName = "perfil_" + uid + ".jpg";
            StorageReference imageRef = FirebaseStorage.getInstance().getReference().child("perfiles/" + fileName);
            imageRef.putFile(cambio_De_imagen)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            imageRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    userInfo.put("imagen", uri.toString());
                                    actualizarInformacionUsuario(userInfo, usuarioRef, uid, nombre, correo, contrasena, telefono);
                                }
                            });
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception exception) {
                            Log.e("FirebaseStorage", "Error al cargar nueva imagen: " + exception.getMessage());
                        }
                    });
        } else {
            actualizarInformacionUsuario(userInfo, usuarioRef, uid, nombre, correo, contrasena, telefono);
        }
    }
    private void actualizarInformacionUsuario(Map<String, Object> userInfo, DatabaseReference usuarioRef,
                                              String uid, String nombre, String correo, String contrasena, String telefono) {
        usuarioRef.updateChildren(userInfo)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        ActualizarInformacionUsuarioCache(nombre, correo, contrasena, telefono);
                        animacion_mensaje.setVisibility(View.GONE);
                        animacion_mensaje_ok.setVisibility(View.VISIBLE);
                        notiificacion_perfil.setText("Se actualizo la informacion");
                        aparecer_btn_aceptar();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e("FirebaseError", "Error al actualizar información del usuario: " + e.getMessage());
                        e.printStackTrace();
                    }
                });
    }
    private void ActualizarInformacionUsuarioCache(String nombre, String correo, String contraseña, String telefono) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("nombre", nombre);
        editor.putString("correo", correo);
        editor.putString("contraseña", contraseña);
        editor.putString("telefono", telefono);
        if(cambio_De_imagen!=null)
            editor.putString("imagen",cambio_De_imagen.toString());
        try{
            editor.apply();
        }
        catch (Exception e){
            Log.e("SharedPreferences", "Error al aplicar cambios: " + e.getMessage());
            e.printStackTrace();
        }
    }
    private void abrirGaleria() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_SELECT_PICTURE);
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_SELECT_PICTURE) {
                Uri selectedImageUri = data.getData();
                if (selectedImageUri != null) {
                    lanzarUCrop(selectedImageUri);
                }
            } else if (requestCode == REQUEST_CROP_PICTURE) {
                Uri croppedImageUri = UCrop.getOutput(data);
                if (croppedImageUri != null) {
                    cargarImagenEnShapeableImageView(croppedImageUri);
                    cambio_De_imagen = croppedImageUri;
                }
            }
        }
    }
    private void lanzarUCrop(Uri sourceUri) {
        UCrop.Options options = new UCrop.Options();
        options.setAspectRatioOptions(0,
                new AspectRatio("1:1", 1, 1));
        options.setCompressionFormat(Bitmap.CompressFormat.JPEG);
        options.setCompressionQuality(80);
        Random random = new Random();
        int randomDigit = random.nextInt(100);
        String fileName = "imgperfil"+randomDigit+"jpg";
        File dataDir = getDataDir();
        File[] previousFiles = dataDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.startsWith("imgperfil");
            }
        });
        if (previousFiles != null) {
            for (File file : previousFiles) {
                file.delete();
            }
        }
        File newFile = new File(dataDir, fileName);
        cambio_De_imagen = null;
        UCrop uCrop = UCrop.of(sourceUri, Uri.fromFile(newFile))
                .withOptions(options);
        uCrop.start(this, REQUEST_CROP_PICTURE);
    }

    private void cargarImagenEnShapeableImageView(Uri imageUri) {
        Glide.with(this)
                .load(imageUri)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .into((ShapeableImageView) findViewById(R.id.foto_perfil_edit));
    }
    public void toast_revisar_campos(){
        LayoutInflater lyt = getLayoutInflater();
        View v = lyt.inflate(R.layout.mensaje_revisa_campos,(ViewGroup)findViewById(R.id.mensaeje_toast) );
        Toast toast = new Toast(getApplicationContext());
        toast.setGravity(Gravity.CENTER_VERTICAL|Gravity.BOTTOM,0,220);
        toast.setDuration(Toast.LENGTH_LONG);
        toast.setView(v);
        toast.show();
    }
}