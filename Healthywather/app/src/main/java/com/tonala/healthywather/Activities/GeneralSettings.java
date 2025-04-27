package com.tonala.healthywather.Activities;

import android.animation.Animator;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.tonala.healthywather.R;
import com.tonala.healthywather.utils.DeleteDeviceService;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
public class GeneralSettings extends AppCompatActivity {
    private DatabaseReference dispositivoref;
    private MediaPlayer mediaPlayer;
    private ConstraintLayout opcionA, opcionB, opcionC;
    private TextView txt_notificacion_msg_eliminar, txt_mensaje_accion, txt_numeroVariables, txt_ValorVariable, txt_TituloVariable;
    private LottieAnimationView animacion_mensaje_ok;
    private ConstraintLayout sin_hw, con_hw;
    private TextInputLayout caja_edt_nombre_disp;
    private ValueEventListener variablesListener;
    private TextInputEditText edt_nombre_disp;
    private SharedPreferences sharedPreferences;
    private CardView cardbtnsi, cardbtnno, cardaceptar_eliminar;
    private Switch Notifiaciones_sw;
    private String ID_dispositivo;
    private RecyclerView variablesList;
    private VariableAdapter variableAdapter;
    private Boolean Audio = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configuracion_general);

        int numeroLayout = getIntent().getIntExtra("layout", 0);
        sharedPreferences = getSharedPreferences("user_data", Context.MODE_PRIVATE);
        if(sharedPreferences.getBoolean("Audio", false))
            Audio = true;
        caja_edt_nombre_disp = findViewById(R.id.edit_nombre_disp_layout);
        txt_numeroVariables = findViewById(R.id.txt_NumeroVariables);
        Notifiaciones_sw = findViewById(R.id.sw_NotificacionesVar);
        edt_nombre_disp = findViewById(R.id.edit_nombre_disp);
        ImageButton btn_back_A_layout = findViewById(R.id.btn_back_A_layout);
        ImageButton btn_back_B_layout = findViewById(R.id.btn_back_audio);
        ImageButton btn_back_C_layout = findViewById(R.id.btn_back_notificaciones);
        FloatingActionButton btn_compartir = findViewById(R.id.btn_compartir);
        Button borrar_disp = findViewById(R.id.btn_borrar_disp);
        Button guardar_Configuracion = findViewById(R.id.btn_guardar_config_disp);

        guardar_Configuracion.setOnClickListener(v -> {
            String nombreDisp = edt_nombre_disp.getText().toString();
            if(nombreDisp.length() <=3){
                caja_edt_nombre_disp.setError("El nombre del dispositivo debe tener minimo 4 caracteres");
            }
            else if(nombreDisp.length() > 20){
                caja_edt_nombre_disp.setError("El nombre del dispositivo es muy largo");
            }
            else {
                caja_edt_nombre_disp.setErrorEnabled(false);
                IniciarMSGGuardar();
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("Nombre_dispositivo", edt_nombre_disp.getText().toString());
                editor.apply();
            }
        });

        // Inicializar layouts
        opcionA = findViewById(R.id.A_mihw_layout);
        opcionB = findViewById(R.id.B_audio_layout);
        opcionC = findViewById(R.id.C_notificaciones_layout);
        sin_hw = findViewById(R.id.sin_hw_layout);
        con_hw = findViewById(R.id.con_hw_layout);

        // Inicializar RecyclerView
        variablesList = findViewById(R.id.lista_variables);
        variablesList.setLayoutManager(new LinearLayoutManager(this));
        variableAdapter = new VariableAdapter(this, new ArrayList<>());
        variablesList.setAdapter(variableAdapter);

        // Añadir ScrollListener para animación de desvanecimiento
        variablesList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                for (int i = 0; i < recyclerView.getChildCount(); i++) {
                    View child = recyclerView.getChildAt(i);
                    if (child != null) {
                        if (isViewVisible(recyclerView, child)) {
                            child.animate().alpha(1).setDuration(500).start();
                        } else {
                            child.animate().alpha(0).setDuration(500).start();
                        }
                    }
                }
            }

            private boolean isViewVisible(RecyclerView recyclerView, View view) {
                Rect scrollBounds = new Rect();
                recyclerView.getHitRect(scrollBounds);
                return view.getLocalVisibleRect(scrollBounds);
            }
        });
        switch (numeroLayout) {
            case 1:
                mostrarLayout1();
                break;
            case 2:
                mostrarLayout2();
                break;
            case 3:
                mostrarLayout3();
                break;
        }

        // Configurar los botones de retroceso
        btn_back_A_layout.setOnClickListener(v -> onBackPressed());
        btn_back_B_layout.setOnClickListener(v -> onBackPressed());
        btn_back_C_layout.setOnClickListener(v -> onBackPressed());
        borrar_disp.setOnClickListener(v -> IniciarMSGEliminar());
        btn_compartir.setOnClickListener(v -> IniciarMSGCompartir());

    }
    private String CompartirCodigo(String codigo) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < codigo.length(); i++) {
            char c = codigo.charAt(i);
            if (i < 2) {
                sb.append((char) (c + 5));
            } else if (i < 5) {
                sb.append((char) (c + 4));
            } else {
                sb.append((char) (c + 3));
            }
        }
        while (sb.length() < 8) {
            sb.append('X');
        }
        return sb.toString();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        removeVariablesListener();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
    private void removeVariablesListener() {
        if (dispositivoref != null && variablesListener != null) {
            dispositivoref.removeEventListener(variablesListener);
        }
    }
    private void IniciarMSGGuardar() {
        View mensaje_Guardar = getLayoutInflater().inflate(R.layout.mensaje_guardarcambios, null);
        Button guardar_ConfiguracionD = mensaje_Guardar.findViewById(R.id.btn_aceptarGuardar);
        Dialog dialogo_guardar = new Dialog(this, R.style.DialogTheme);
        dialogo_guardar.setContentView(mensaje_Guardar);
        dialogo_guardar.getWindow().setLayout(1000, 1000);
        dialogo_guardar.setCancelable(false);
        ConstraintLayout dialogContainer = dialogo_guardar.findViewById(R.id.layout_Guardar);
        dialogContainer.setBackground(ContextCompat.getDrawable(this, R.drawable.fondo_mensaje));
        guardar_ConfiguracionD.setOnClickListener(v -> {
            caja_edt_nombre_disp.setErrorEnabled(false);
            dialogo_guardar.cancel();
            this.finish();
        });
        dialogo_guardar.show();
    }
    private void IniciarMSGCompartir() {
        View mensaje_Compartir = getLayoutInflater().inflate(R.layout.mensaje_compartir, null);
        Dialog dialogo_compartir;
        Button Aceptar = mensaje_Compartir.findViewById(R.id.btn_AceptarCompartir);
        FloatingActionButton copiar = mensaje_Compartir.findViewById(R.id.btn_copiar);
        TextView Codigo = mensaje_Compartir.findViewById(R.id.txt_codigoCompartir);
        Codigo.setText(CompartirCodigo("ID"+ID_dispositivo));
        dialogo_compartir = new Dialog(this, R.style.DialogTheme);
        dialogo_compartir.setContentView(mensaje_Compartir);
        dialogo_compartir.getWindow().setLayout(1000, 1000);
        dialogo_compartir.setCancelable(false);
        Aceptar.setOnClickListener(v -> {
            dialogo_compartir.cancel();
        });
        copiar.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Código Ofuscado", Codigo.getText());
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Se copio el Codigo", Toast.LENGTH_SHORT).show();
        });
        ConstraintLayout dialogContainer = dialogo_compartir.findViewById(R.id.layout_compartir);
        dialogContainer.setBackground(ContextCompat.getDrawable(this, R.drawable.fondo_mensaje));
        dialogo_compartir.show();
    }

    private void IniciarMSGCalibracion() {
        View mensaje_calibracion = getLayoutInflater().inflate(R.layout.mensaje_calibracion, null);
        Button guardar_calibracion = mensaje_calibracion.findViewById(R.id.btn_GuardarCalibracion);
        txt_ValorVariable = mensaje_calibracion.findViewById(R.id.txt_ValorCalibracion);
        txt_TituloVariable = mensaje_calibracion.findViewById(R.id.txt_TituloVariableCab);
        ImageButton btn_BackCalibracion = mensaje_calibracion.findViewById(R.id.btn_backCalibracion);
        Dialog dialogo_calibracion = new Dialog(this, R.style.DialogTheme);
        guardar_calibracion.setOnClickListener(v -> dialogo_calibracion.cancel());
        dialogo_calibracion.setContentView(mensaje_calibracion);
        dialogo_calibracion.getWindow().setLayout(1000, 1320);
        dialogo_calibracion.setCancelable(false);
        ConstraintLayout dialogContainer = dialogo_calibracion.findViewById(R.id.layout_msgCalibracion);
        dialogContainer.setBackground(ContextCompat.getDrawable(this, R.drawable.fondo_mensaje));
        btn_BackCalibracion.setOnClickListener(v -> dialogo_calibracion.cancel());
        dialogo_calibracion.show();
    }

    private void IniciarMSGEliminar() {
        View mensaje_eliminar = getLayoutInflater().inflate(R.layout.mensaje_eliminar_disp, null);
        cardbtnsi = mensaje_eliminar.findViewById(R.id.card_eliminar);
        cardbtnno = mensaje_eliminar.findViewById(R.id.card_noregresar);
        cardaceptar_eliminar = mensaje_eliminar.findViewById(R.id.card_aceptar_eliminar_disp);
        animacion_mensaje_ok = mensaje_eliminar.findViewById(R.id.anim_eliminar_disp);
        animacion_mensaje_ok.setVisibility(View.GONE);
        txt_notificacion_msg_eliminar = mensaje_eliminar.findViewById(R.id.notifiacion_msg_eliminar);
        txt_mensaje_accion = mensaje_eliminar.findViewById(R.id.txt_mensaje_accion);
        Button si_eliminar = mensaje_eliminar.findViewById(R.id.btn_si_eliminar);
        Button no_regresar = mensaje_eliminar.findViewById(R.id.btn_no_regresar);
        Button aceptar_eliminar = mensaje_eliminar.findViewById(R.id.btn_aceptar_eliminar_disp);
        Dialog dialogo = new Dialog(this, R.style.DialogTheme);
        dialogo.setContentView(mensaje_eliminar);
        dialogo.getWindow().setLayout(950, 1000);
        dialogo.setCancelable(false);
        ConstraintLayout dialogContainer = dialogo.findViewById(R.id.layout_main);
        dialogContainer.setBackground(ContextCompat.getDrawable(this, R.drawable.fondo_mensaje));
        si_eliminar.setOnClickListener(v -> {

            animacion_mensaje_ok.setVisibility(View.VISIBLE);
            ViewPropertyAnimator anim1 = cardbtnno.animate()
                    .setDuration(700)
                    .alpha(0.0f)
                    .translationY(100);
            ViewPropertyAnimator anim2 = cardbtnsi.animate()
                    .setDuration(700)
                    .alpha(0.0f)
                    .translationY(100);
            anim2.setListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(@NonNull Animator animation) {
                }

                @Override
                public void onAnimationEnd(@NonNull Animator animation) {
                    cardaceptar_eliminar.setVisibility(View.VISIBLE);
                    cardaceptar_eliminar.setAlpha(0.0f);
                    cardaceptar_eliminar.setTranslationY(100);
                    si_eliminar.setEnabled(false);
                    ViewPropertyAnimator anim3 = cardaceptar_eliminar.animate()
                            .setDuration(700)
                            .alpha(1.0f)
                            .translationY(0)
                                    .withEndAction(() -> si_eliminar.setEnabled(true));
                    DeleteDeviceService.eliminarVariableEnSegundoPlano(getApplicationContext(), new DeleteDeviceService.DeleteCallback() {
                        @Override
                        public void onDeleteResult(boolean success) {
                            if (success) {
                                txt_mensaje_accion.setText("Se eliminó con éxito");
                                txt_notificacion_msg_eliminar.setText("Se eliminó tu Healthywater en este dispositivo");
                            } else {
                                txt_mensaje_accion.setText("Se eliminó localmente");
                                txt_notificacion_msg_eliminar.setText("Se eliminó tu Healthywater en este dispositivo, para eliminarlo completamente conéctate a internet");
                            }
                        }
                    });

                }

                @Override
                public void onAnimationCancel(@NonNull Animator animation) {
                }

                @Override
                public void onAnimationRepeat(@NonNull Animator animation) {
                }
            });
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("ID_dispositivo", "");
            editor.putString("Nombre_dispositivo", "");
            editor.putString("MAC", "");
            editor.apply();
        });
        no_regresar.setOnClickListener(v -> dialogo.cancel());
        aceptar_eliminar.setOnClickListener(v -> {
            Intent intent = new Intent(GeneralSettings.this, MenuuActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            GeneralSettings.this.finish();
        });
        dialogo.show();
    }
    private void mostrarLayout1() {
        opcionA.setTranslationX(-300);
        opcionA.setAlpha(0.0f);
        opcionA.setVisibility(View.VISIBLE);
        ViewPropertyAnimator animator1 = opcionA.animate()
                .translationX(0)
                .alpha(1.0f)
                .setDuration(500);
        if (Audio) {
            mediaPlayer = MediaPlayer.create(this, R.raw.mihealthywater);
            mediaPlayer.start();
        }
        ID_dispositivo = sharedPreferences.getString("ID_dispositivo", "");
        String nombre_dispositivo = sharedPreferences.getString("Nombre_dispositivo", "");
        String MAC = sharedPreferences.getString("MAC", "");
        if (!ID_dispositivo.isEmpty() && !nombre_dispositivo.isEmpty()) {
            sin_hw.setVisibility(View.GONE);
            con_hw.setVisibility(View.VISIBLE);
            edt_nombre_disp.setText(nombre_dispositivo);
            new LoadVariablesTask().execute(true);
        } else if (!MAC.isEmpty()) {
            sin_hw.setVisibility(View.GONE);
            con_hw.setVisibility(View.VISIBLE);
            edt_nombre_disp.setEnabled(false);
        }
    }

    private void mostrarLayout2() {
        opcionB.setTranslationX(-300);
        opcionB.setAlpha(0.0f);
        opcionB.setVisibility(View.VISIBLE);
        ViewPropertyAnimator animator1 = opcionB.animate()
                .translationX(0)
                .alpha(1.0f)
                .setDuration(500);
                if (Audio) {
                    mediaPlayer = MediaPlayer.create(this, R.raw.audio);
                    mediaPlayer.start();
                }
        Switch audio = findViewById(R.id.sw_textoavoz);
        boolean audioB = sharedPreferences.getBoolean("Audio", false);
        audio.setChecked(audioB);
        audio.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("Audio", isChecked);
            editor.apply();
        });
    }
    private void mostrarLayout3() {
        TextView ppm = findViewById(R.id.txt_ppm);
        TextView turbidez = findViewById(R.id.txt_turbidez);
        TextView ph = findViewById(R.id.txt_ph);
        TextView valph = findViewById(R.id.txt_Valph);
        ph.setAlpha(0.0f);
        valph.setAlpha(0.0f);

        Spinner spinnerOptions = findViewById(R.id.Opc_uso);
        String[] opciones = {"Agua potable", "Domestico", "Alberca"};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, opciones);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerOptions.setAdapter(adapter);
        int savedPosition = sharedPreferences.getInt("spinner_selection", 0);
        spinnerOptions.setSelection(savedPosition);

        spinnerOptions.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                editor.putInt("spinner_selection", position);
                editor.apply();
                if (position == 0) {
                    ViewPropertyAnimator anim1 = ph.animate()
                            .setDuration(300)
                            .alpha(0.0f);
                    ViewPropertyAnimator anim = valph.animate()
                            .setDuration(300)
                            .alpha(0.0f);
                    ppm.setText("500");
                    turbidez.setText("100 NTU");
                    updateModeInDatabase("P");
                } else if (position == 1) {
                    ViewPropertyAnimator anim1 = ph.animate()
                            .setDuration(300)
                            .alpha(0.0f);
                    ViewPropertyAnimator anim = valph.animate()
                            .setDuration(300)
                            .alpha(0.0f);
                    ppm.setText("750");
                    turbidez.setText("450 NTU");
                    updateModeInDatabase("D");
                }
                else{
                    ViewPropertyAnimator anim1 = ph.animate()
                                    .setDuration(300)
                                            .alpha(1.0f);
                    ViewPropertyAnimator anim = valph.animate()
                            .setDuration(300)
                            .alpha(1.0f);
                    ppm.setText("5");
                    turbidez.setText("0.5 NTU");
                    updateModeInDatabase("A");
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        opcionC.setTranslationX(-300);
        opcionC.setAlpha(0.0f);
        opcionC.setVisibility(View.VISIBLE);
        ViewPropertyAnimator animator1 = opcionC.animate()
                .translationX(0)
                .alpha(1.0f)
                .setDuration(500);

        if (Audio) {
            mediaPlayer = MediaPlayer.create(this, R.raw.notificaciones);
            mediaPlayer.start();
        }

        boolean Notificaciones_variables = sharedPreferences.getBoolean("alertas_notificacion", true);
        Notifiaciones_sw.setChecked(Notificaciones_variables);

        Notifiaciones_sw.setOnCheckedChangeListener((buttonView, isChecked) -> {
            editor.putBoolean("alertas_notificacion", isChecked);
            editor.apply();
            updateNotificationPreferenceInDatabase(isChecked);
        });
    }
    private void updateModeInDatabase(String mode) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            DatabaseReference userRef = FirebaseDatabase.getInstance()
                    .getReference("usuarios")
                    .child(user.getUid())
                    .child("notificaciones")
                    .child("Modo");

            userRef.setValue(mode).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Log.d("Firebase", "Mode updated to: " + mode);
                } else {
                    Log.d("Firebase", "Failed to update mode", task.getException());
                }
            });
        }
    }
    private void updateNotificationPreferenceInDatabase(boolean isChecked) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            DatabaseReference userRef = FirebaseDatabase.getInstance()
                    .getReference("usuarios")
                    .child(user.getUid());
            userRef.child("notificaciones")
                    .child("alertas")
                    .setValue(isChecked)
                    .addOnCompleteListener(task -> {
                        String msg = isChecked ? "Notification preference updated: ON" : "Notification preference updated: OFF";
                        Log.d("FCM", msg);
                    });
        }
    }

    public void onBackPressed() {
        super.onBackPressed();
    }

    public static class VariableItem {
        private final String name;
        private final double value;

        public VariableItem(String name, double value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public double getValue() {
            return value;
        }
    }
    public class VariableAdapter extends RecyclerView.Adapter<VariableAdapter.VariableViewHolder> {
        private final List<VariableItem> variables;
        private final Context context;

        public VariableAdapter(Context context, List<VariableItem> variables) {
            this.context = context;
            this.variables = variables;
        }

        @NonNull
        @Override
        public VariableViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_variable, parent, false);
            return new VariableViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull VariableViewHolder holder, int position) {
            VariableItem variableItem = variables.get(position);
            holder.bind(variableItem);
            holder.itemView.setAlpha(0);
            holder.itemView.animate().alpha(1).setDuration(500).start();
        }

        @Override
        public int getItemCount() {
            return variables.size();
        }

        public class VariableViewHolder extends RecyclerView.ViewHolder {
            TextView variableNameTextView;
            FloatingActionButton calibrar;

            public VariableViewHolder(@NonNull View itemView) {
                super(itemView);
                variableNameTextView = itemView.findViewById(R.id.titulo);
                calibrar = itemView.findViewById(R.id.btn_calibrar);
            }

            public void bind(VariableItem variableItem) {
                switch (variableItem.getName()) {
                    case "ppm_e":
                        variableNameTextView.setText("Ppm entrada");
                        break;
                    case "ppm_s":
                        variableNameTextView.setText("Ppm salida");
                        break;
                    case "turbidez_e":
                        variableNameTextView.setText("Turbidez entrada");
                        break;
                    case "turbidez_s":
                        variableNameTextView.setText("Turbidez salida");
                        break;
                    case "temp_e":
                        variableNameTextView.setText("Temperatura entrada");
                        break;
                    case "temp_s":
                        variableNameTextView.setText("Temperatura salida");
                        break;
                    case "ph_e":
                        variableNameTextView.setText("PH entrada");
                        break;
                    case "ph_s":
                        variableNameTextView.setText("PH salida");
                        break;
                }
                if ("ph_e".equals(variableItem.getName()) || "ph_s".equals(variableItem.getName())) {
                    calibrar.setVisibility(View.VISIBLE);
                    calibrar.setOnClickListener(v -> {
                            IniciarMSGCalibracion();
                            txt_TituloVariable.setText(variableNameTextView.getText());
                            DatabaseReference variableRef = FirebaseDatabase.getInstance()
                                    .getReference("Dispositivos")
                                    .child("ID" + ID_dispositivo)
                                    .child(variableItem.getName());
                            variableRef.addValueEventListener(new ValueEventListener() {
                                DecimalFormat df = new DecimalFormat("#.00");
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    Double value = snapshot.getValue(Double.class);
                                    if (value != null) {
                                        DecimalFormat df = new DecimalFormat("#.00");
                                        value = Double.valueOf(df.format(value));
                                        txt_ValorVariable.setText(String.valueOf(value));
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    Log.e("Firebase", "Error al leer el valor de la variable", error.toException());
                                }
                            });
                    });
                } else {
                    calibrar.setVisibility(View.GONE);
                }
            }
        }
    }
    private class LoadVariablesTask extends AsyncTask<Boolean, Void, List<VariableItem>> {
        @Override
        protected List<VariableItem> doInBackground(Boolean... params) {
            Boolean modo = params[0];
            List<VariableItem> variableItems = new ArrayList<>();
            if (modo) {
                dispositivoref = FirebaseDatabase.getInstance()
                        .getReference("Dispositivos")
                        .child("ID" + ID_dispositivo);

                variablesListener = new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        variableItems.clear(); // Limpiar la lista antes de llenarla nuevamente
                        for (DataSnapshot variableSnapshot : snapshot.getChildren()) {
                            String variableName = variableSnapshot.getKey();
                            Double variableValue = null;

                            try {
                                variableValue = variableSnapshot.getValue(Double.class);
                            } catch (Exception e) {
                                try {
                                    variableValue = Double.valueOf(variableSnapshot.getValue(Integer.class));
                                } catch (Exception ex) {
                                    Log.e("ConfigurarVariables", "Error al convertir el valor", ex);
                                }
                            }
                            if (variableValue != null) {
                                boolean shouldAdd = false;
                                switch (variableName) {
                                    case "ppm_e":
                                    case "ppm_s":
                                    case "turbidez_e":
                                    case "turbidez_s":
                                        shouldAdd = variableValue > 0;
                                        break;
                                    case "temp_e":
                                    case "temp_s":
                                        shouldAdd = variableValue > -127.00;
                                        break;
                                    case "ph_e":
                                    case "ph_s":
                                        shouldAdd = variableValue < 21;
                                        break;
                                }
                                if (shouldAdd) {
                                    variableItems.add(new VariableItem(variableName, variableValue));
                                }
                            }
                        }
                        txt_numeroVariables.setText(String.valueOf(variableItems.size()));
                        onPostExecute(variableItems);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("ConfigurarVariables", "Error al leer la base de datos", error.toException());
                    }
                };
                dispositivoref.addValueEventListener(variablesListener);
            }
            return variableItems;
        }
        @Override
        protected void onProgressUpdate(Void... values) {
        }
        @Override
        protected void onPostExecute(List<VariableItem> variableItems) {
            variableAdapter = new VariableAdapter(GeneralSettings.this, variableItems);
            variablesList.setAdapter(variableAdapter);
        }
    }
}

