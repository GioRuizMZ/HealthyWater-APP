package com.tonala.healthywather.Activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.animation.Animator;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.tonala.healthywather.R;

import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class ServicesActivity extends AppCompatActivity implements TextWatcher {
    private static final int REQUEST_CODE_LOCATION_PERMISSION = 1;
    private String ModificarCita, uid;
    private  DatabaseReference ObtenerRef;
    private ValueEventListener ObtenerCitaUsuario;
    private TextInputLayout caja_Fechacita, caja_dirreccion, caja_Horacita, caja_Nombre, caja_referencias, caja_Tipolugar, caja_peticion;
    private TextInputEditText edt_Fechacita, edt_direccion, edt_Horacita, edt_Nombre, edt_referencias, edt_Tipolugar, edt_peticion;
    private DatePicker calendario;
    private ConstraintLayout texto_cita,cita;
    private ImageButton btn_back ;
    private FloatingActionButton btn_calendario, btn_ubicacion, btn_Horacita;
    private Button btn_Crearcita, btn_ModificarCita, btn_ConfirmarCC, btn_Aceptar;
    final Calendar c = Calendar.getInstance();
    int dia, mes, año, hora, minuto, horasec, minutosec, diasec;
    private String horaSeleccionada, fechaSeleccionada;
    private Boolean RevisarErrores = false, CitaCreada = false;
    private View Mensaje_proceso;
    private CardView card_btnAceptar,card_btnModificar,card_btnCrearCita;
    private LottieAnimationView anim_MensajeLoading, anim_MensajeOk, anim_MensajeError;
    private TextView NombreCita, DireccionCita, ReferenciasCita, TipoLugarCita, FechaCita, HoraCita, Telefono, Peticion, Titulo_proceso, desc_proceso,creaCita;
    private Dialog dialogo;
    private SharedPreferences sharedPreferences;
    private FirebaseAuth mAuth ;
    private String Nombre, Direccion, Referencias, Tipolugar, Fecha, Horac,Telefonoc,Peticionc;
    private ConstraintLayout layout_confirmacion, layout_proceso;
    private void ConfirmarDatos(){

        Mensaje_proceso = getLayoutInflater().inflate(R.layout.mensajeconfirmarcita, null);
        layout_confirmacion = Mensaje_proceso.findViewById(R.id.layout_msgConfirmar);
        layout_proceso = Mensaje_proceso.findViewById(R.id.layout_Proceso);

        btn_ModificarCita = Mensaje_proceso.findViewById(R.id.btn_Modificar);
        btn_Aceptar = Mensaje_proceso.findViewById(R.id.btn_aceptar);
        btn_ConfirmarCC = Mensaje_proceso.findViewById(R.id.btn_CCrearcita);
        if(ModificarCita != null) {
            btn_ConfirmarCC.setText("Actualizar");
        }

        card_btnAceptar = Mensaje_proceso.findViewById(R.id.card_btn_aceptar);
        card_btnModificar = Mensaje_proceso.findViewById(R.id.card_btn_Modificar);
        card_btnCrearCita = Mensaje_proceso.findViewById(R.id.card_btn_CCrearcita);

        anim_MensajeLoading = Mensaje_proceso.findViewById(R.id.anim_LoadingCita);
        anim_MensajeOk = Mensaje_proceso.findViewById(R.id.anim_okCita);
        anim_MensajeError = Mensaje_proceso.findViewById(R.id.anim_ErrorCita);

        NombreCita = Mensaje_proceso.findViewById(R.id.txt_NombreCita);
        DireccionCita = Mensaje_proceso.findViewById(R.id.txt_DireccionCita);
        ReferenciasCita = Mensaje_proceso.findViewById(R.id.txt_ReferenciasDireccion);
        TipoLugarCita = Mensaje_proceso.findViewById(R.id.txt_TipoLugar);
        FechaCita = Mensaje_proceso.findViewById(R.id.txt_FechaCita);
        HoraCita = Mensaje_proceso.findViewById(R.id.txt_HoraCita);
        Telefono = Mensaje_proceso.findViewById(R.id.txt_Telefono);
        Peticion = Mensaje_proceso.findViewById(R.id.txt_Peticion);
        Titulo_proceso = Mensaje_proceso.findViewById(R.id.txt_Titulo2);
        desc_proceso = Mensaje_proceso.findViewById(R.id.txt_DescProceso);

        dialogo = new Dialog(this,R.style.DialogTheme);
        dialogo.setContentView(Mensaje_proceso);
        dialogo.getWindow().setLayout(1000, 1400);
        dialogo.setCancelable(false);
        ConstraintLayout dialogContainer = dialogo.findViewById(R.id.layout_main);
        dialogContainer.setBackground(ContextCompat.getDrawable(this, R.drawable.fondo_mensaje));

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_servicios);
        creaCita = findViewById(R.id.txt_creacita);
        //FIREBASE
        mAuth = FirebaseAuth.getInstance();
        uid = mAuth.getCurrentUser().getUid();
        if (getIntent().getExtras() != null) {
            ModificarCita = getIntent().getStringExtra("Modificar");
            creaCita.setText("Modificar Cita");
            ObtenerCitaFirebase();
        }
        sharedPreferences = getSharedPreferences("user_data", Context.MODE_PRIVATE);
        //Calendario
        año = c.get(Calendar.YEAR);
        mes = c.get(Calendar.MONTH);
        dia = c.get(Calendar.DAY_OF_MONTH);
        //Edit text's
        edt_Fechacita = findViewById(R.id.Fechacita);
        edt_Fechacita.setEnabled(false);
        edt_direccion = findViewById(R.id.direccion_cita);
        edt_Horacita = findViewById(R.id.Horacita);
        edt_Horacita.setEnabled(false);
        edt_Nombre = findViewById(R.id.nombre_cita);
        edt_referencias = findViewById(R.id.referencias_cita);
        edt_Tipolugar = findViewById(R.id.Tipolugar);
        edt_peticion = findViewById(R.id.Peticionservicio);

        caja_Fechacita = findViewById(R.id.Fechacita_layout);
        caja_dirreccion = findViewById(R.id.direccion_cita_layout);
        caja_Horacita = findViewById(R.id.Horacita_layout);
        caja_Nombre = findViewById(R.id.nombre_cita_layout);
        caja_referencias = findViewById(R.id.referencias_cita_layout);
        caja_Tipolugar = findViewById(R.id.Tipolugar_layout);
        caja_peticion = findViewById(R.id.Peticionservicio_layout);

        texto_cita = findViewById(R.id.layout_texto_cita);
        cita = findViewById(R.id.layout_cita);
        btn_back = findViewById(R.id.btn_back_cita);
        btn_calendario = findViewById(R.id.btn_calendario);
        btn_ubicacion = findViewById(R.id.btn_ubicacion);
        btn_Horacita = findViewById(R.id.btn_hora);
        btn_Crearcita = findViewById(R.id.btn_Crearcita);
        if(ModificarCita != null){
            btn_Crearcita.setText("Actualizar");
        }
        calendario = findViewById(R.id.Fecha_cita);
        texto_cita.setVisibility(View.VISIBLE);
        texto_cita.setAlpha(0.0f);
        ConfirmarDatos();
        ViewPropertyAnimator anim = texto_cita.animate()
                .setDuration(500)
                .alpha(1.0f);
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                ViewPropertyAnimator anim = texto_cita.animate()
                        .setDuration(500)
                        .translationX(-300)
                        .alpha(0.0f);
                anim.setListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(@NonNull Animator animation) {

                    }

                    @Override
                    public void onAnimationEnd(@NonNull Animator animation) {
                        cita.setVisibility(View.VISIBLE);
                        cita.setAlpha(0.0f);
                        cita.setTranslationX(300);
                        ViewPropertyAnimator anim2 = cita.animate()
                                .setDuration(500)
                                .translationX(0)
                                .alpha(1.0f);
                    }

                    @Override
                    public void onAnimationCancel(@NonNull Animator animation) {

                    }

                    @Override
                    public void onAnimationRepeat(@NonNull Animator animation) {

                    }
                });

            }
        },1000);
        //Listeners text
        edt_Nombre.addTextChangedListener(this);
        edt_direccion.addTextChangedListener(this);
        edt_referencias.addTextChangedListener(this);
        edt_Tipolugar.addTextChangedListener(this);
        edt_Fechacita.addTextChangedListener(this);
        edt_Horacita.addTextChangedListener(this);
        edt_peticion.addTextChangedListener(this);
        //Listeners botones
        btn_back.setOnClickListener(v -> ServicesActivity.this.finish());
        btn_calendario.setOnClickListener(v -> mostrarDatePicker());
        btn_ubicacion.setOnClickListener(v -> obtenerUbicacionYDireccion());
        btn_Horacita.setOnClickListener(v -> mostrarTimePickerDialog());
        btn_Crearcita.setOnClickListener(v -> VerificarCita());
        btn_ModificarCita.setOnClickListener(v -> dialogo.cancel());
        btn_ConfirmarCC.setOnClickListener(v -> {
            ViewPropertyAnimator animc = layout_confirmacion.animate()
                    .setDuration(500)
                    .alpha(0.0f);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    layout_confirmacion.setVisibility(View.GONE);
                    layout_proceso.setVisibility(View.VISIBLE);
                    if (isOnline()){
                        anim_MensajeError.setVisibility(View.GONE);
                        anim_MensajeLoading.setVisibility(View.VISIBLE);
                        Titulo_proceso.setText("Creando la Cita");
                        desc_proceso.setText("Espere mientras creamos su cita ...");
                    }
                    else{
                        anim_MensajeError.setVisibility(View.VISIBLE);
                        anim_MensajeLoading.setVisibility(View.GONE);
                        Titulo_proceso.setText("Sin conexion a internet");
                        desc_proceso.setText("Revise su conexion a internet e intente de nuevo");
                        MostrarBtnAceptar();
                    }
                    layout_proceso.setAlpha(0.0f);
                    ViewPropertyAnimator animp = layout_proceso.animate()
                            .setDuration(500)
                            .alpha(1.0f);
                    CrearCitaFirebase(Nombre, Direccion, Referencias, Tipolugar, Fecha, Horac, Peticionc, Telefonoc);
                }
            }, 500);
        });
        btn_Aceptar.setOnClickListener(v -> {
            if (CitaCreada) {
                dialogo.cancel();
                this.finish();
            }
            else{
                layout_proceso.setVisibility(View.GONE);
                layout_confirmacion.setVisibility(View.VISIBLE);
                layout_confirmacion.setAlpha(1.0f);
                dialogo.cancel();
            }
        });
    }
    private void obtenerUbicacionYDireccion() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (location != null) {
                double latitud = location.getLatitude();
                double longitud = location.getLongitude();
                obtenerDireccion(latitud, longitud);
            } else {
                edt_direccion.setText("Ubicación no disponible");
            }
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE_LOCATION_PERMISSION);
        }
    }
    private void obtenerDireccion(double latitud, double longitud) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latitud, longitud, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                String direccion = address.getAddressLine(0);
                edt_direccion.setText(direccion);
            } else {
                edt_direccion.setText("Dirección no disponible");
            }
        } catch (IOException e) {
            e.printStackTrace();
            edt_direccion.setText("Error al obtener la dirección");
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                obtenerUbicacionYDireccion();
            } else {
                edt_direccion.setText("Permiso de ubicación denegado");
            }
        }
    }
    private void mostrarDatePicker() {
        DatePickerDialog.OnDateSetListener dateSetListener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                fechaSeleccionada = dayOfMonth + "/" + (monthOfYear + 1) + "/" + year;
                edt_Fechacita.setText(fechaSeleccionada);
                diasec = dayOfMonth;
            }
        };
        DatePickerDialog datePickerDialog = new DatePickerDialog(this,R.style.Calendario, dateSetListener, año, mes, dia);
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePickerDialog.show();
    }
    private void mostrarTimePickerDialog() {

        hora = c.get(Calendar.HOUR_OF_DAY);
        minuto = c.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(this,R.style.Calendario, new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hour, int minute ) {

                horaSeleccionada = String.format(Locale.getDefault(), "%02d:%02d", hour, minute);
                edt_Horacita.setText(horaSeleccionada);
                horasec = hour;
                minutosec = minute;

                if (edt_Horacita.getText().toString().isEmpty()) {
                    caja_Horacita.setError("Elige la hora de la cita");
                } else if (horasec < 8  || horasec >= 20 && minutosec > 0) {
                    caja_Horacita.setError("Elige una hora entre las 8:00 y 20:00 hrs");
                } else if (horasec <= hora && dia == diasec) {
                    caja_Horacita.setError("La hora de cita no es valida");
                } else if (horasec <= hora + 2 && dia == diasec && minutosec < minuto || horasec == hora+1 && dia == diasec ) {
                    caja_Horacita.setError("La cita debe agendarse min 2hrs antes");
                } else {
                    caja_Horacita.setErrorEnabled(false);
                }
            }
        }, hora, minuto, false);
        timePickerDialog.show();
    }
    private void VerificarCita(){

        RevisarErrores = false;
        Nombre = edt_Nombre.getText().toString();
        Direccion = edt_direccion.getText().toString();
        Referencias = edt_referencias.getText().toString();
        Tipolugar = edt_Tipolugar.getText().toString();
        Fecha = edt_Fechacita.getText().toString();
        Horac = edt_Horacita.getText().toString();
        Peticionc = edt_peticion.getText().toString();

        if (Nombre.isEmpty()){
            caja_Nombre.setError("Ingresa un nombre");
            RevisarErrores = true;
        } else if (Nombre.length() > 0 && Nombre.length() <4){
            caja_Nombre.setError("El nombre debe de tener minimo 4 caracteres");
            RevisarErrores = true;
        }
        else{
            caja_Nombre.setErrorEnabled(false);
        }

        if(Direccion.isEmpty() || Direccion.equals("Error al obtener la dirección") || Direccion.equals("Direccion no disponible")){
            caja_dirreccion.setError("Ingresa una direccion ");
            RevisarErrores = true;
        }
        else{
            caja_dirreccion.setErrorEnabled(false);
        }

        if(Tipolugar.isEmpty()){
            caja_Tipolugar.setError("Ingresa el tipo de lugar para el servicio");
            RevisarErrores = true;
        }
        else{
            caja_Tipolugar.setErrorEnabled(false);
        }

        if(Fecha.isEmpty()){
            caja_Fechacita.setError("Elige la fecha de la cita");
            RevisarErrores = true;
        }
        else{
            caja_Fechacita.setErrorEnabled(false);
        }

        if(Horac.isEmpty()){
            caja_Horacita.setError("Elige la hora de la cita");
            RevisarErrores = true;
        }
        else if (horasec < 8  || horasec >= 20 && minutosec > 0) {
            caja_Horacita.setError("Elige una hora entre las 8:00 y 20:00 hrs");
            RevisarErrores = true;
        }
        else if (horasec <= hora  && dia == diasec){
            caja_Horacita.setError("La hora de cita no es valida");
            RevisarErrores = true;
        }
        else if (horasec <= hora + 2 && dia == diasec && minutosec < minuto || horasec == hora+1 && dia == diasec){
            RevisarErrores = true;
            caja_Horacita.setError("La cita debe agendarse min 2hrs antes");
        }
        else{
            caja_Horacita.setErrorEnabled(false);
        }

        if(Peticionc.isEmpty()) {
            caja_peticion.setError("Ingresa el tipo de servicio que requieres");
            RevisarErrores = true;
        }
        else{
            caja_peticion.setErrorEnabled(false);
        }
        if(!RevisarErrores){

            Telefonoc = sharedPreferences.getString("telefono", "");
            dialogo.show();
            NombreCita.setText("Nombre: "+Nombre);
            DireccionCita.setText("Direccion :"+Direccion);
            ReferenciasCita.setText("Referencias :"+Referencias);
            TipoLugarCita.setText("Tipo de lugar :"+Tipolugar);
            FechaCita.setText("Fecha cita :"+Fecha);
            HoraCita.setText("Hora cita :"+Horac);
            Peticion.setText("Peticion :"+Peticionc);
            Telefono.setText("Telefono :"+Telefonoc);

        }
    }
    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }
    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

        String Nombre = edt_Nombre.getText().toString();
        String Direccion = edt_direccion.getText().toString();
        String Referencias = edt_referencias.getText().toString();
        String Tipolugar = edt_Tipolugar.getText().toString();
        String Fecha = edt_Fechacita.getText().toString();
        String Horac = edt_Horacita.getText().toString();
        String Peticionc = edt_peticion.getText().toString();

        if(RevisarErrores) {
            if (Nombre.isEmpty()) {
                caja_Nombre.setError("Ingresa un nombre");
            } else if (Nombre.length() > 0 && Nombre.length() < 4) {
                caja_Nombre.setError("El nombre debe de tener minimo 4 caracteres");
            } else {
                caja_Nombre.setErrorEnabled(false);
            }

            if (Direccion.isEmpty() || Direccion.equals("Error al obtener la dirección") || Direccion.equals("Direccion no disponible")) {
                caja_dirreccion.setError("Ingresa una direccion ");
            } else {
                caja_dirreccion.setErrorEnabled(false);
            }

            if (Tipolugar.isEmpty()) {
                caja_Tipolugar.setError("Ingresa el tipo de lugar para el servicio");
            } else {
                caja_Tipolugar.setErrorEnabled(false);
            }

            if (Fecha.isEmpty()) {
                caja_Fechacita.setError("Elige la fecha de la cita");
            } else {
                caja_Fechacita.setErrorEnabled(false);
            }

            if (Horac.isEmpty()) {
                caja_Horacita.setError("Elige la hora de la cita");
            } else if (horasec < 8  || horasec >= 20 && minutosec > 0) {
                caja_Horacita.setError("Elige una hora entre las 8:00 y 20:00 hrs");
            } else if (horasec <= hora && dia == diasec) {
                caja_Horacita.setError("La hora de cita no es valida");
            } else if (horasec <= hora + 2 && dia == diasec && minutosec < minuto || horasec == hora+1 && dia == diasec) {
                caja_Horacita.setError("La cita debe agendarse min 2hrs antes");
            } else {
                caja_Horacita.setErrorEnabled(false);
            }

            if (Peticionc.isEmpty()) {
                caja_peticion.setError("Ingresa el tipo de servicio que requieres");
            } else {
                caja_peticion.setErrorEnabled(false);
            }
        }
    }
    @Override
    public void afterTextChanged(Editable s) {

    }
    private void ObtenerCitaFirebase(){
        ObtenerRef = FirebaseDatabase.getInstance().getReference().child("Citas").child(uid).child(ModificarCita);
        ObtenerCitaUsuario = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    edt_Nombre.setText(dataSnapshot.child("nombre").getValue(String.class));
                    edt_direccion.setText(dataSnapshot.child("direccion").getValue(String.class));

                    String Fecha = dataSnapshot.child("fecha").getValue(String.class);
                    edt_Fechacita.setText(Fecha);
                    String[] partesFecha = Fecha.split("/");
                    diasec = Integer.parseInt(partesFecha[0]);

                    String Hora = dataSnapshot.child("hora").getValue(String.class);
                    edt_Horacita.setText(Hora);
                    String[] partesHora = Hora.split(":");
                    horasec = Integer.parseInt(partesHora[0]);
                    minutosec = Integer.parseInt(partesHora[1]);

                    edt_referencias.setText(dataSnapshot.child("referencias").getValue(String.class));
                    edt_Tipolugar.setText(dataSnapshot.child("tipo_lugar").getValue(String.class));
                    edt_peticion.setText(dataSnapshot.child("peticion").getValue(String.class));
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        };
        ObtenerRef.addValueEventListener(ObtenerCitaUsuario);
    }
    private void CrearCitaFirebase(String nombre, String direccion, String referencias, String tipolugar, String fecha, String horac, String peticion, String telefono) {
        DatabaseReference citasRef = FirebaseDatabase.getInstance().getReference().child("Citas").child(uid);

        citasRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                boolean citaPendiente = false;
                String estado = null;
                for (DataSnapshot citaSnapshot : dataSnapshot.getChildren()) {
                    estado = citaSnapshot.child("Estado").getValue(String.class);
                    if ("Pendiente".equals(estado)) {
                        citaPendiente = true;
                        break;
                    }
                }

                if (citaPendiente && ModificarCita == null) {
                    anim_MensajeLoading.setVisibility(View.GONE);
                    anim_MensajeError.setVisibility(View.VISIBLE);
                    Titulo_proceso.setText("Ya tienes una cita pendiente");
                    desc_proceso.setText("No se puede crear una nueva cita porque ya tienes una cita pendiente.");
                    MostrarBtnAceptar();
                }
                else if (ModificarCita == null && "Aceptada".equals(estado)){
                    anim_MensajeLoading.setVisibility(View.GONE);
                    anim_MensajeError.setVisibility(View.VISIBLE);
                    Titulo_proceso.setText("No se puede modificar la cita");
                    desc_proceso.setText("El personal ya acepto su cita, si desea cancelar la cita elimine la cita en el apartado -> ServicesActivity -> Citas");
                    MostrarBtnAceptar();
                }
                else {
                    String FechaCracion = String.valueOf(System.currentTimeMillis());
                    HashMap<String, Object> citaMap = new HashMap<>();
                    citaMap.put("nombre", nombre);
                    citaMap.put("direccion", direccion);
                    citaMap.put("referencias", referencias);
                    citaMap.put("tipo_lugar", tipolugar);
                    citaMap.put("fecha", fecha);
                    citaMap.put("hora", horac);
                    citaMap.put("peticion", peticion);
                    citaMap.put("telefono", telefono);
                    citaMap.put("Estado", "Pendiente");

                    if (ModificarCita == null){
                        citasRef.child(FechaCracion).setValue(citaMap)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        SharedPreferences.Editor editor = sharedPreferences.edit();
                                        int CitasCreadas = sharedPreferences.getInt("citas", 0);
                                        editor.putInt("citas", CitasCreadas+1);
                                        editor.apply();
                                        anim_MensajeLoading.setVisibility(View.GONE);
                                        anim_MensajeOk.setVisibility(View.VISIBLE);
                                        Titulo_proceso.setText("Cita creada con éxito");
                                        desc_proceso.setText("Por favor espere a que el personal acepte su cita, Puede revisar el estado de la cita en el apartado ServicesActivity -> Citas.");
                                        MostrarBtnAceptar();
                                        CitaCreada = true;
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Toast.makeText(getApplicationContext(), "Error al crear la cita: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                    else{
                        ObtenerRef.updateChildren(citaMap).addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        anim_MensajeLoading.setVisibility(View.GONE);
                                        anim_MensajeOk.setVisibility(View.VISIBLE);
                                        Titulo_proceso.setText("Se actualizo la Cita");
                                        desc_proceso.setText("Por favor espere a que el personal acepte su cita, Puede revisar el estado de la cita en ServicesActivity -> Citas.");
                                        MostrarBtnAceptar();
                                        CitaCreada = true;
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        // Error al actualizar
                                    }
                                });
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(getApplicationContext(), "Error al verificar la cita: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

    }
    private boolean isOnline() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }
    private void MostrarBtnAceptar(){
        card_btnAceptar.setAlpha(0.0f);
        btn_Aceptar.setEnabled(false);
        card_btnAceptar.setVisibility(View.VISIBLE);
        card_btnAceptar.setTranslationY(30);
        ViewPropertyAnimator anim = card_btnAceptar.animate()
                .translationY(0)
                .setDuration(500)
                .alpha(1.0f);
        anim.setListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(@NonNull Animator animation) {

            }

            @Override
            public void onAnimationEnd(@NonNull Animator animation) {
                btn_Aceptar.setEnabled(true);
            }

            @Override
            public void onAnimationCancel(@NonNull Animator animation) {

            }

            @Override
            public void onAnimationRepeat(@NonNull Animator animation) {

            }
        });
    }
}