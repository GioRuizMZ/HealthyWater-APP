package com.tonala.healthywather.ui.servicios;

import android.animation.Animator;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.airbnb.lottie.LottieAnimationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.tonala.healthywather.Activities.MenuuActivity;
import com.tonala.healthywather.R;
import com.tonala.healthywather.Activities.ServicesActivity;
import com.tonala.healthywather.databinding.FragmentServiciosBinding;

import java.util.ArrayList;
import java.util.List;

public class ServiciosFragment extends Fragment {
    private ImageButton AgregarCita, AgregarCitaSuperior;
    private FragmentServiciosBinding binding;
    private ListView Citas;
    private FirebaseAuth mAuth = MenuuActivity.mAuth;
    private ArrayList<String> listaCitas;
    private CitasAdapter adapter;
    private ValueEventListener CitasListener;
    private String uid = mAuth.getCurrentUser().getUid();
    private DatabaseReference citasRef = FirebaseDatabase.getInstance().getReference().child("Citas").child(uid);
    private View Mensaje_proceso;
    private Dialog dialogo;
    private DataSnapshot ObtenerCitaSnapshot, citaSnapshot;
    private ConstraintLayout layout_confirmacion, layout_proceso, SinCitas, ConCitas;
    private Button btn_Aceptar, btn_ModificarCita, btn_Eliminar;
    private CardView card_btnAceptar, card_btnModificar, card_btnEliminarCita;
    private LottieAnimationView anim_MensajeLoading, anim_MensajeOk, anim_MensajeError, anim_ListaLoading;
    private String ID_CITA;
    private MediaPlayer mediaPlayer;
    private int CitasCreadas = 0;
    private SharedPreferences sharedPreferences;
    private TextView NombreCita, DireccionCita, ReferenciasCita, TipoLugarCita, HoraCita, FechaCita, Peticion, Titulo_proceso, desc_proceso, Telefono, Titulo_confirmacion;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentServiciosBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        sharedPreferences = requireContext().getSharedPreferences("user_data", Context.MODE_PRIVATE);
        CitasCreadas = sharedPreferences.getInt("citas", 0);
        SinCitas = root.findViewById(R.id.Vista_sinCitas);
        ConCitas = root.findViewById(R.id.Vista_conCitas);

        if (CitasCreadas <= 0) {
            SinCitas.setVisibility(View.VISIBLE);
            ConCitas.setVisibility(View.GONE);
        }
        AgregarCita = root.findViewById(R.id.btn_agregarCita);
        AgregarCitaSuperior = root.findViewById(R.id.btn_AgregarCitas);
        anim_ListaLoading = root.findViewById(R.id.anim_LoadingCita2);
        listaCitas = new ArrayList<>();
        Citas = root.findViewById(R.id.ListaCitas);
        adapter = new CitasAdapter(getActivity(), listaCitas);
        Citas.setAdapter(adapter);
        AgregarCita.setOnClickListener(v -> startActivity(new Intent(getActivity(), ServicesActivity.class)));
        AgregarCitaSuperior.setOnClickListener(v -> startActivity(new Intent(getActivity(), ServicesActivity.class)));
        return root;
    }

    @Override
    public void onStart() {
        super.onStart();
        setupUI();
        ObtenerCitasFirebase();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        if (CitasListener != null) {
            citasRef.removeEventListener(CitasListener);
        }
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (sharedPreferences.getBoolean("Audio",false) == true){
            mediaPlayer = MediaPlayer.create(getContext(), R.raw.servicios);
            mediaPlayer.start();
        }
        VerificarCitasCreadas(false);
    }

    private void setupUI() {
        Citas.setOnItemClickListener((parent, view, position, id) -> {
            if (ObtenerCitaSnapshot != null && ObtenerCitaSnapshot.getChildrenCount() > 0) {
                citaSnapshot = ObtenerCitaSnapshot.getChildren().iterator().next();
                dialogo.show();
                Titulo_confirmacion.setText("Informacion de la Cita");
                layout_confirmacion.setVisibility(View.VISIBLE);
                layout_proceso.setVisibility(View.GONE);
                ID_CITA = citaSnapshot.getKey();
                NombreCita.setText("Nombre :" + citaSnapshot.child("nombre").getValue(String.class));
                DireccionCita.setText("Direccion :" + citaSnapshot.child("direccion").getValue(String.class));
                FechaCita.setText("Fecha :" + citaSnapshot.child("fecha").getValue(String.class));
                HoraCita.setText("Hora :" + citaSnapshot.child("hora").getValue(String.class));
                ReferenciasCita.setText("Referencias :" + citaSnapshot.child("referencias").getValue(String.class));
                Telefono.setText("Telefono :" + citaSnapshot.child("telefono").getValue(String.class));
                TipoLugarCita.setText("Tipo de lugar :" + citaSnapshot.child("tipo_lugar").getValue(String.class));
                Peticion.setText("Peticion :" + citaSnapshot.child("peticion").getValue(String.class));
            }
        });
        ModificarCita();
    }
    private void ModificarCita() {
        Mensaje_proceso = getLayoutInflater().inflate(R.layout.mensajeconfirmarcita, null);
        layout_confirmacion = Mensaje_proceso.findViewById(R.id.layout_msgConfirmar);
        layout_proceso = Mensaje_proceso.findViewById(R.id.layout_Proceso);
        btn_ModificarCita = Mensaje_proceso.findViewById(R.id.btn_Modificar);
        btn_Aceptar = Mensaje_proceso.findViewById(R.id.btn_aceptar);
        btn_Eliminar = Mensaje_proceso.findViewById(R.id.btn_CCrearcita);
        btn_Eliminar.setText("Eliminar");
        card_btnAceptar = Mensaje_proceso.findViewById(R.id.card_btn_aceptar);
        card_btnModificar = Mensaje_proceso.findViewById(R.id.card_btn_Modificar);
        card_btnEliminarCita = Mensaje_proceso.findViewById(R.id.card_btn_CCrearcita);
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
        Titulo_confirmacion = Mensaje_proceso.findViewById(R.id.txt_Titulo);
        desc_proceso = Mensaje_proceso.findViewById(R.id.txt_DescProceso);
        btn_Aceptar.setOnClickListener(v -> dialogo.cancel());
        btn_ModificarCita.setOnClickListener(v -> {
            dialogo.cancel();
            startActivity(new Intent(getActivity(), ServicesActivity.class).putExtra("Modificar", ID_CITA));
        });
        btn_Eliminar.setOnClickListener(v -> {
            dialogo.setCancelable(false);
            ViewPropertyAnimator animc = layout_confirmacion.animate().setDuration(500).alpha(0.0f);
            animc.setListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(@NonNull Animator animation) {
                }

                @Override
                public void onAnimationEnd(@NonNull Animator animation) {
                    layout_confirmacion.setVisibility(View.GONE);
                    layout_proceso.setVisibility(View.VISIBLE);
                    if (isOnline()) {
                        anim_MensajeError.setVisibility(View.GONE);
                        anim_MensajeLoading.setVisibility(View.VISIBLE);
                        Titulo_proceso.setText("Eliminando la Cita");
                        desc_proceso.setText("Espere mientras Eliminamos su cita ...");
                        Eliminar_cita();
                    } else {
                        anim_MensajeError.setVisibility(View.VISIBLE);
                        anim_MensajeLoading.setVisibility(View.GONE);
                        Titulo_proceso.setText("Sin conexion a internet");
                        desc_proceso.setText("Revise su conexion a internet e intente de nuevo");
                        MostrarBtnAceptar();
                    }
                    layout_proceso.setAlpha(0.0f);
                    ViewPropertyAnimator animp = layout_proceso.animate().setDuration(500).alpha(1.0f);
                }

                @Override
                public void onAnimationCancel(@NonNull Animator animation) {
                }

                @Override
                public void onAnimationRepeat(@NonNull Animator animation) {
                }
            });
        });

        dialogo = new Dialog(requireContext(), R.style.DialogTheme);
        dialogo.setContentView(Mensaje_proceso);
        dialogo.getWindow().setLayout(1000, 1400);
        ConstraintLayout dialogContainer = dialogo.findViewById(R.id.layout_main);
        dialogContainer.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.fondo_mensaje));
    }

    private void Eliminar_cita() {
        if (ObtenerCitaSnapshot != null && ObtenerCitaSnapshot.getChildrenCount() > 0) {
            citasRef.child(ID_CITA).removeValue().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    anim_MensajeLoading.setVisibility(View.GONE);
                    anim_MensajeOk.setVisibility(View.VISIBLE);
                    Titulo_proceso.setText("Cita eliminada con éxito");
                    desc_proceso.setText("La cita se eliminó y se canceló correctamente, para crear una nueva cita presione + en la parte superior derecha");
                    MostrarBtnAceptar();
                    VerificarCitasCreadas(true);
                } else {
                    anim_MensajeLoading.setVisibility(View.GONE);
                    anim_MensajeError.setVisibility(View.VISIBLE);
                    Titulo_proceso.setText("Error al eliminar la cita");
                    desc_proceso.setText("Verifica tu conexión e intenta de nuevo");
                    MostrarBtnAceptar();
                }
            });
        }
    }

    private void VerificarCitasCreadas(Boolean Eliminar) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        CitasCreadas = sharedPreferences.getInt("citas", 0);
        if (CitasCreadas > 0) {
            SinCitas.setVisibility(View.GONE);
            ConCitas.setVisibility(View.VISIBLE);
            if (Eliminar) {
                editor.putInt("citas", CitasCreadas - 1);
                editor.apply();
                CitasCreadas--;
                if (CitasCreadas > 0) {
                    ObtenerCitasFirebase();
                } else {
                    SinCitas.setVisibility(View.VISIBLE);
                    ConCitas.setVisibility(View.GONE);
                }
            } else {
                ObtenerCitasFirebase();
            }
        } else {
            SinCitas.setVisibility(View.VISIBLE);
            ConCitas.setVisibility(View.GONE);
        }
    }

    private void ObtenerCitasFirebase() {
        List<String> ActualizacionCitas = new ArrayList<>();
        CitasListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot citaSnapshot : dataSnapshot.getChildren()) {
                    String Fecha = citaSnapshot.child("fecha").getValue(String.class);
                    String Hora = citaSnapshot.child("hora").getValue(String.class);
                    String Estado = citaSnapshot.child("Estado").getValue(String.class);
                    String cita = Fecha + "," + Hora + "," + Estado;
                    ObtenerCitaSnapshot = dataSnapshot;
                    anim_ListaLoading.setVisibility(View.GONE);
                    ActualizacionCitas.add(cita);
                }
                listaCitas.clear();
                listaCitas.addAll(ActualizacionCitas);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("FirebaseError", "Error al leer los datos: " + databaseError.getMessage());
            }
        };
        citasRef.addValueEventListener(CitasListener);
    }

    private void MostrarBtnAceptar() {
        card_btnAceptar.setAlpha(0.0f);
        btn_Aceptar.setEnabled(false);
        card_btnAceptar.setVisibility(View.VISIBLE);
        card_btnAceptar.setTranslationY(30);
        ViewPropertyAnimator anim = card_btnAceptar.animate()
                .translationY(0)
                .setDuration(500)
                .alpha(1.0f)
                .withEndAction(() -> btn_Aceptar.setEnabled(true));
    }

    private boolean isOnline() {
        ConnectivityManager connectivityManager = (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    public class CitasAdapter extends ArrayAdapter<String> {
        private Context mContext;
        private List<String> mCitas;

        public CitasAdapter(Context context, List<String> citas) {
            super(context, 0, citas);
            mContext = context;
            mCitas = citas;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            View listItem = convertView;
            if (listItem == null) {
                listItem = LayoutInflater.from(mContext).inflate(R.layout.citacreada, parent, false);
            }

            String cita = mCitas.get(position);
            TextView horaCita = listItem.findViewById(R.id.txt_HoraCita);
            TextView fechaCita = listItem.findViewById(R.id.txt_FechaCita);
            TextView estadoCita = listItem.findViewById(R.id.txt_EstadoCita);

            String[] parts = cita.split(",");
            if (parts.length == 3) {
                horaCita.setText(parts[0]);
                fechaCita.setText(parts[1]);
                estadoCita.setText(parts[2]);
            }

            return listItem;
        }
    }
}