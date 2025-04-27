package com.tonala.healthywather.ui.gallery;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.airbnb.lottie.LottieAnimationView;
import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.navigation.NavigationView;
import com.tonala.healthywather.Activities.HelpActivity;
import com.tonala.healthywather.Activities.GeneralSettings;
import com.tonala.healthywather.Activities.ProfileActivity;
import com.tonala.healthywather.R;
import com.tonala.healthywather.databinding.FragmentGalleryBinding;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
public class GalleryFragment extends Fragment {
    private FragmentGalleryBinding binding;
    private GalleryViewModel galleryViewModel;
    private Dialog dialogo;
    private MediaPlayer mediaPlayer;
    private CardView card_btn;
    private TextView txtnombre, txtcorreo, txttelefono, Nombreheader, txt_version, txt_msg;
    private ShapeableImageView imagen_perfil;
    private LottieAnimationView anim_actualizacion_ok;
    private LottieAnimationView anim_actualizacion_loading;
    private ExecutorService executorService;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentGalleryBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        executorService = Executors.newFixedThreadPool(2);
        galleryViewModel = new ViewModelProvider(this).get(GalleryViewModel.class);

        setupUI(root);


        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        observeViewModel();
    }
    @Override
    public void onStart() {
        super.onStart();
        iniciarMSG();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        executorService.shutdown();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        SharedPreferences sharedPreferences = binding.getRoot().getContext().getSharedPreferences("user_data", Context.MODE_PRIVATE);
        if (sharedPreferences.getBoolean("Audio",false) == true){
            mediaPlayer = MediaPlayer.create(getContext(), R.raw.configuracion);
            mediaPlayer.start();
        }
        galleryViewModel.cargarDatosUsuario(requireActivity().getSharedPreferences("user_data", Context.MODE_PRIVATE));
    }

    private void setupUI(View root) {
        NavigationView navigationView = requireActivity().findViewById(R.id.nav_view);
        View headerView = navigationView.getHeaderView(0);
        Nombreheader = headerView.findViewById(R.id.nombre_user);
        txtnombre = root.findViewById(R.id.nombre_config);
        txtcorreo = root.findViewById(R.id.correo_config);
        txttelefono = root.findViewById(R.id.telefono_config);
        imagen_perfil = root.findViewById(R.id.foto_perfil);
        Button editar_perfil = root.findViewById(R.id.btn_editarperfil);
        Button a_mihw = root.findViewById(R.id.configuracion_myhw);
        Button b_audio = root.findViewById(R.id.configuracion_audio);
        Button c_notificaciones = root.findViewById(R.id.configuracion_notificaciones);
        Button d_actualizaciones = root.findViewById(R.id.configuracion_actualizacion);
        Button e_ayuda = root.findViewById(R.id.configuracion_ayuda);

        editar_perfil.setOnClickListener(v -> startActivity(new Intent(getContext(), ProfileActivity.class)));
        a_mihw.setOnClickListener(v -> abrirActividadConLayout(1));
        b_audio.setOnClickListener(v -> abrirActividadConLayout(2));
        c_notificaciones.setOnClickListener(v -> abrirActividadConLayout(3));
        d_actualizaciones.setOnClickListener(v -> mostrarDialogoActualizacion());
        e_ayuda.setOnClickListener(v -> startActivity(new Intent(getContext(), HelpActivity.class)));
    }

    @SuppressLint("SetTextI18n")
    private void observeViewModel() {
        galleryViewModel.getNombre().observe(getViewLifecycleOwner(), nombre -> {
            txtnombre.setText(nombre);
            Nombreheader.setText(nombre);
        });
        galleryViewModel.getImagenPerfil().observe(getViewLifecycleOwner(), fotoPerfilURL -> {
            if (!TextUtils.isEmpty(fotoPerfilURL)) {
                Glide.with(requireContext())
                        .load(Uri.parse(fotoPerfilURL))
                        .into(imagen_perfil);
            }
        });
        galleryViewModel.getCorreo().observe(getViewLifecycleOwner(), correo -> txtcorreo.setText(correo));
        galleryViewModel.getTelefono().observe(getViewLifecycleOwner(), telefono -> {
            if (TextUtils.isEmpty(telefono)) {
                txttelefono.setText("Agrega un teléfono");
            } else {
                txttelefono.setText(telefono);
            }
        });
    }

    private void iniciarMSG() {
        @SuppressLint("InflateParams") View mensaje_actualizacion = getLayoutInflater().inflate(R.layout.mensajeactualizacion, null);
        card_btn = mensaje_actualizacion.findViewById(R.id.card_btn);
        anim_actualizacion_loading = mensaje_actualizacion.findViewById(R.id.anim_loading);
        anim_actualizacion_ok = mensaje_actualizacion.findViewById(R.id.anim_actualizacionl_ok);
        txt_version = mensaje_actualizacion.findViewById(R.id.txt_version);
        txt_msg = mensaje_actualizacion.findViewById(R.id.txt_msg);
        Button aceptar_actualizacion = mensaje_actualizacion.findViewById(R.id.btn_aceptar_actualizacion);
        dialogo = new Dialog(requireContext(), R.style.DialogTheme);
        dialogo.setContentView(mensaje_actualizacion);
        Objects.requireNonNull(dialogo.getWindow()).setLayout(830, 1190);
        dialogo.setCancelable(false);
        ConstraintLayout dialogContainer = dialogo.findViewById(R.id.layout_main);
        dialogContainer.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.fondo_mensaje));

        aceptar_actualizacion.setOnClickListener(v -> dialogo.cancel());
    }

    @SuppressLint("SetTextI18n")
    private void mostrarDialogoActualizacion() {
        dialogo.show();
        anim_actualizacion_loading.setVisibility(View.VISIBLE);
        anim_actualizacion_ok.setVisibility(View.GONE);
        card_btn.setVisibility(View.GONE);

        handler.postDelayed(() -> {
            anim_actualizacion_loading.setVisibility(View.GONE);
            anim_actualizacion_ok.setVisibility(View.VISIBLE);
            card_btn.setVisibility(View.VISIBLE);
            txt_msg.setText("¡Estás al día!");
            txt_version.setText("Última versión 1.0");
        }, 2000);
    }

    private void abrirActividadConLayout(int numeroLayout) {
        Intent intent = new Intent(getActivity(), GeneralSettings.class);
        intent.putExtra("layout", numeroLayout);
        startActivity(intent);
    }
}


