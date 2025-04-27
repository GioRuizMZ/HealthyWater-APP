package com.tonala.healthywather.ui.home;

import static androidx.core.content.ContentProviderCompat.requireContext;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.tonala.healthywather.Activities.HelpActivity;
import com.tonala.healthywather.Activities.GeneralSettings;
import com.tonala.healthywather.Activities.DeviceSettings;
import com.tonala.healthywather.Activities.MenuuActivity;
import com.tonala.healthywather.R;
import com.tonala.healthywather.Activities.Recomendatios;
import com.tonala.healthywather.Activities.ServicesActivity;
import com.tonala.healthywather.databinding.FragmentHomeBinding;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
public class HomeFragment extends Fragment {
    private MediaPlayer mediaPlayer;
    private int Ppm_E, Ppm_S, Turbidez_E, Turbidez_S;
    private Map<Float, String> timeLabels = new HashMap<>();
    private double Temperatura_E, Temperatura_S, Ph_E, Ph_S;
    private String MAC, Nombre_dispositivo, ID_dispositivo;
    private LineChart chart;
    private CardView lastSelectedCardView, loading, variables, Grafica, loadingG;
    private DatabaseReference database;
    private ValueEventListener MonitoreoListener;
    private FragmentHomeBinding binding;
    private SharedPreferences sharedPreferences;
    private ConstraintLayout sin_dispositivo, si_dispositivo ;
    private TextView nombre_disp_text, txt_pb_conduct_e, txt_pb_conduct_s, txt_pb_temp_e, txt_pb_temp_s, txt_pb_turb_e, txt_pb_turb_s, txt_pb_ppm_e, txt_pb_ppm_s, txt_pb_ph_e, txt_pb_ph_s;
    private ProgressBar pb_conduct_e, pb_temp_e, pb_turb_e, pb_ppm_e, pb_ph_e, pb_conduct_s, pb_temp_s, pb_turb_s, pb_ppm_s, pb_ph_s;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable updateRunnable;
    private List<Entry> ppmEEntries, ppmSEntries, turbidezEEntries, turbidezSEntries, temperaturaEEntries, temperaturaSEntries, phEEntries, phSEntries;
    private long startTime;
    private ExecutorService executorService;


    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        sharedPreferences = root.getContext().getSharedPreferences("user_data", Context.MODE_PRIVATE);
        executorService = Executors.newFixedThreadPool(2);

        inicializarVistas(root);
        ID_dispositivo = sharedPreferences.getString("ID_dispositivo", "");
        Nombre_dispositivo = sharedPreferences.getString("Nombre_dispositivo", "");
        MAC = sharedPreferences.getString("MAC", "");

        ViewTreeObserver observer = root.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                root.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                cargarInterfaz();
            }
        });

        return root;
    }
    private void cargarInterfaz() {
        if (!ID_dispositivo.isEmpty() && !Nombre_dispositivo.isEmpty()) {
            mostrarDispositivo();
            database = FirebaseDatabase.getInstance().getReference("Dispositivos").child("ID" + ID_dispositivo);
            if (!executorService.isShutdown()) {
                executorService.execute(this::IniciarMonitoreo);
            }
            CargarDatosConDelay();
        } else if ((MAC != null && !MAC.isEmpty()) || MenuuActivity.BT_Conectado) {
            VistaBT();
            CargarDatosConDelay();
        } else {
            mostrarSinDispositivo();
        }
    }
    private void CargarDatosConDelay() {
        handler.postDelayed(() -> {
            Temporizador();
            setupButtons(binding.getRoot());
            loading.setVisibility(View.GONE);
            variables.setVisibility(View.VISIBLE);
            loadingG.setVisibility(View.GONE);
            Grafica.setVisibility(View.VISIBLE);
            initializeGraphData();
        }, 1000);
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        if (database != null && MonitoreoListener != null) {
            database.removeEventListener(MonitoreoListener);
        }
        if (handler != null) {
            handler.removeCallbacks(updateRunnable);
        }
        if (!executorService.isShutdown()) {
            executorService.shutdown();
        }
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
    @Override
    public void onResume() {
        super.onResume();
        Nombre_dispositivo = sharedPreferences.getString("Nombre_dispositivo", "");
        nombre_disp_text.setText(Nombre_dispositivo);
    }
    public void receiveData(final int tdsValue1, final int tdsValue2, final int sturb1, final int sturb2,
                            final double stemp1, final double stemp2, final double sph1, final double sph2) {
        requireActivy().runOnUiThread(() -> ActualizarMonitoreo(tdsValue1, sturb1, stemp1, sph1, tdsValue2, sturb2, stemp2, sph2));
    }

    private void inicializarVistas(View root) {
        loadingG = root.findViewById(R.id.card_loadingGrafica);
        Grafica = root.findViewById(R.id.card_Grafica);
        variables = root.findViewById(R.id.card_variables);
        loading = root.findViewById(R.id.card_loadingMenu);
        chart = root.findViewById(R.id.chart);
        chart.setNoDataText("Selecciona una variable a monitorear");
        chart.setNoDataTextColor(Color.BLACK);
        Typeface typeface = ResourcesCompat.getFont(requireContext(), R.font.poppins);
        chart.setNoDataTextTypeface(typeface);
        nombre_disp_text = root.findViewById(R.id.txt_nombre_disp_menu);
        ImageButton ayuda = root.findViewById(R.id.btn_Ayuda);
        ayuda.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), HelpActivity.class);
            intent.putExtra("valor", true);
            startActivity(intent);
        });

        Button btn_servicios = root.findViewById(R.id.btn_servicios);
        ImageButton btn_editar_variables = root.findViewById(R.id.btn_editar_variables);
        ImageButton btn_comprar = root.findViewById(R.id.btn_Comprar);
        ImageButton btn_massobrehw = root.findViewById(R.id.btn_masinfo);
        sin_dispositivo = root.findViewById(R.id.sin_dispositivo);
        si_dispositivo = root.findViewById(R.id.si_dispositivo);
        FloatingActionButton btn_ayuda = root.findViewById(R.id.fab_ayuda);

        pb_conduct_e = root.findViewById(R.id.pb_conduct_E);
        txt_pb_conduct_e = root.findViewById(R.id.txt_pb_cunduct_e);

        pb_ppm_e = root.findViewById(R.id.pb_ppm_E);
        txt_pb_ppm_e = root.findViewById(R.id.txt_pb_ppm_e);

        pb_turb_e = root.findViewById(R.id.pb_turb_E);
        txt_pb_turb_e = root.findViewById(R.id.txt_pb_turb_e);

        pb_temp_e = root.findViewById(R.id.pb_temp_E);
        txt_pb_temp_e = root.findViewById(R.id.txt_pb_temp_e);

        pb_ph_e = root.findViewById(R.id.pb_ph_E);
        txt_pb_ph_e = root.findViewById(R.id.txt_pb_ph_e);

        pb_conduct_s = root.findViewById(R.id.pb_conduct_S);
        txt_pb_conduct_s = root.findViewById(R.id.txt_pb_conduct_s);

        pb_ppm_s = root.findViewById(R.id.pb_ppm_s);
        txt_pb_ppm_s = root.findViewById(R.id.txt_pb_ppm_s);

        pb_turb_s = root.findViewById(R.id.pb_turb_s);
        txt_pb_turb_s = root.findViewById(R.id.txt_pb_turb_s);

        pb_temp_s = root.findViewById(R.id.pb_temp_s);
        txt_pb_temp_s = root.findViewById(R.id.txt_pb_temp_s);

        pb_ph_s = root.findViewById(R.id.pb_ph_S);
        txt_pb_ph_s = root.findViewById(R.id.txt_pb_ph_s);

        TextView saludoTextView = root.findViewById(R.id.tiempo_saludo);
        saludoTextView.setText(obtenerSaludo(Calendar.getInstance().get(Calendar.HOUR_OF_DAY)));
        ImageButton agregar_dispositivo = binding.getRoot().findViewById(R.id.btn_agregardisp);
        agregar_dispositivo.setOnClickListener(v -> startActivity(new Intent(getContext(), DeviceSettings.class)));

        btn_editar_variables.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), GeneralSettings.class);
            intent.putExtra("layout", 1);
            startActivity(intent);
        });

        btn_servicios.setOnClickListener(v -> mostrarBottomSheetServicios());
        btn_ayuda.setOnClickListener(v -> startActivity(new Intent(getContext(), HelpActivity.class)));

        btn_comprar.setOnClickListener(v -> {
            String url = "https://healthywater.proyectos8c.com/healthywater_page/Comprar.html";
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            startActivity(intent);
        });
        btn_massobrehw.setOnClickListener(v -> {
            String url = "https://healthywater.proyectos8c.com/healthywater_page/index.html";
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            startActivity(intent);
        });

    }
    private void mostrarDispositivo() {
        sin_dispositivo.setVisibility(View.GONE);
        si_dispositivo.setVisibility(View.VISIBLE);
        si_dispositivo.setTranslationY(200);
        si_dispositivo.setAlpha(0.0f);
        si_dispositivo.animate()
                .alpha(1.0f)
                .translationY(0)
                .setDuration(500)
                .start();
        nombre_disp_text.setText(Nombre_dispositivo);
    }
    private void mostrarSinDispositivo() {
        sin_dispositivo.setTranslationY(200);
        sin_dispositivo.setAlpha(0.0f);
        sin_dispositivo.setVisibility(View.VISIBLE);
        sin_dispositivo.animate()
                .alpha(1.0f)
                .translationY(0)
                .setDuration(500)
                .start();
    }

    public void ModoBT(boolean BT) {
        if (BT) VistaBT();
    }

    @SuppressLint("SetTextI18n")
    private void VistaBT() {
        sin_dispositivo.setVisibility(View.GONE);
        si_dispositivo.setVisibility(View.VISIBLE);
        si_dispositivo.setTranslationY(200);
        si_dispositivo.setAlpha(0.0f);
        si_dispositivo.animate()
                .alpha(1.0f)
                .translationY(0)
                .setDuration(500)
                .start();

        nombre_disp_text.setText("CONEXION LOCAL");
    }

    private String obtenerSaludo(int hora) {

        boolean audio;
        audio = sharedPreferences.getBoolean("Audio", false);
        int limiteManana = 11;
        int limiteTarde = 20;
        if (hora < limiteManana) {
            executorService.execute(() -> {
                if (audio) {
                    mediaPlayer = MediaPlayer.create(getContext(), R.raw.buenosdias);
                    mediaPlayer.start();
                }
            });
            return "¡BUENOS DÍAS!";
        } else if (hora < limiteTarde) {
            executorService.execute(() -> {
                if (audio) {
                    mediaPlayer = MediaPlayer.create(getContext(), R.raw.buenastardes);
                    mediaPlayer.start();
                }
            });
            return "¡BUENAS TARDES!";
        } else {
            executorService.execute(() -> {
                if (audio) {
                    mediaPlayer = MediaPlayer.create(getContext(), R.raw.buenasnoches);
                    mediaPlayer.start();
                }
            });
            return "¡BUENAS NOCHES!";
        }
    }

    private void mostrarBottomSheetServicios() {
        @SuppressLint("InflateParams") View bottomSheetView = getLayoutInflater().inflate(R.layout.servicios, null);
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        bottomSheetDialog.setContentView(bottomSheetView);
        bottomSheetDialog.show();

        ImageButton btn_min_servicios = bottomSheetView.findViewById(R.id.btn_min_servicios);
        Button btn_sanitizar = bottomSheetView.findViewById(R.id.btn_sanitizar);
        Button btn_recomendaciones = bottomSheetDialog.findViewById(R.id.btn_recomendaciones);

        btn_min_servicios.setOnClickListener(v -> bottomSheetDialog.cancel());

        btn_sanitizar.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), ServicesActivity.class));
            bottomSheetDialog.cancel();
        });

        Objects.requireNonNull(btn_recomendaciones).setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), Recomendatios.class));
            bottomSheetDialog.cancel();
        });
    }

    private void IniciarMonitoreo() {
        MonitoreoListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    int Ppm_E = dataSnapshot.child("ppm_e").getValue(Integer.class) != null ? dataSnapshot.child("ppm_e").getValue(Integer.class) : 0;
                    int Turbidez_E = dataSnapshot.child("turbidez_e").getValue(Integer.class) != null ? dataSnapshot.child("turbidez_e").getValue(Integer.class) : 0;
                    double Temperatura_E = dataSnapshot.child("temp_e").getValue(Double.class) != null ? dataSnapshot.child("temp_e").getValue(Double.class) : 0.0;
                    double Ph_E = dataSnapshot.child("ph_e").getValue(Double.class) != null ? dataSnapshot.child("ph_e").getValue(Double.class) : 0.0;

                    int Ppm_S = dataSnapshot.child("ppm_s").getValue(Integer.class) != null ? dataSnapshot.child("ppm_s").getValue(Integer.class) : 0;
                    int Turbidez_S = dataSnapshot.child("turbidez_s").getValue(Integer.class) != null ? dataSnapshot.child("turbidez_s").getValue(Integer.class) : 0;
                    double Temperatura_S = dataSnapshot.child("temp_s").getValue(Double.class) != null ? dataSnapshot.child("temp_s").getValue(Double.class) : 0.0;
                    double Ph_S = dataSnapshot.child("ph_s").getValue(Double.class) != null ? dataSnapshot.child("ph_s").getValue(Double.class) : 0.0;

                    handler.post(() -> ActualizarMonitoreo(Ppm_E, Turbidez_E, Temperatura_E, Ph_E, Ppm_S, Turbidez_S, Temperatura_S, Ph_S));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        };
        database.addValueEventListener(MonitoreoListener);
    }

    @SuppressLint("SetTextI18n")
    private void ActualizarMonitoreo(int Ppm_E, int Turbidez_E, Double Temperatura_E, Double Ph_E, int Ppm_S, int Turbidez_S, Double Temperatura_S, Double Ph_S) {
        DecimalFormat df = new DecimalFormat("#.#");
        df.setMaximumFractionDigits(1);
        df.setMinimumFractionDigits(1);
        txt_pb_conduct_e.setText("--");
        txt_pb_conduct_s.setText("--");

        this.Ppm_E = Ppm_E;
        this.Ppm_S = Ppm_S;
        this.Turbidez_E = Turbidez_E;
        this.Turbidez_S = Turbidez_S;
        this.Temperatura_E = Temperatura_E;
        this.Temperatura_S = Temperatura_S;
        this.Ph_E = Ph_E;
        this.Ph_S = Ph_S;
        int Conduct_E, Conduct_S;

        if (Ppm_E <= 0) {
            pb_conduct_e.setProgress(0);
            pb_ppm_e.setProgress(0);
            txt_pb_ppm_e.setText("--");
            txt_pb_conduct_e.setText("--");
        } else {
            Conduct_E = Ppm_E * 2;
            pb_ppm_e.setProgress(Ppm_E);
            pb_conduct_e.setProgress(Conduct_E);
            txt_pb_ppm_e.setText(String.valueOf(Ppm_E));
            txt_pb_conduct_e.setText(String.valueOf(Conduct_E));
        }
        if (Ppm_S <= 0) {
            pb_conduct_s.setProgress(0);
            pb_ppm_s.setProgress(0);
            txt_pb_ppm_s.setText("--");
            txt_pb_conduct_s.setText("--");
        } else {
            Conduct_S = Ppm_S * 2;
            pb_conduct_s.setProgress(Conduct_S);
            pb_ppm_s.setProgress(Ppm_S);
            txt_pb_ppm_s.setText(String.valueOf(Ppm_S));
            txt_pb_conduct_s.setText(String.valueOf(Conduct_S));
        }
        if (Turbidez_E <= 0) {
            pb_turb_e.setProgress(0);
            txt_pb_turb_e.setText("--");
        } else {
            pb_turb_e.setProgress(Turbidez_E);
            txt_pb_turb_e.setText(String.valueOf(Turbidez_E));
        }
        if (Turbidez_S <= 0) {
            pb_turb_s.setProgress(0);
            txt_pb_turb_s.setText("--");
        } else {
            pb_turb_s.setProgress(Turbidez_S);
            txt_pb_turb_s.setText(String.valueOf(Turbidez_S));
        }
        if (Temperatura_E <= -127.00) {
            pb_temp_e.setProgress(0);
            txt_pb_temp_e.setText("--");
        } else {
            pb_temp_e.setProgress(Temperatura_E.intValue());
            txt_pb_temp_e.setText(df.format(Temperatura_E) + "°C");
        }
        if (Temperatura_S <= -127.00) {
            pb_temp_s.setProgress(0);
            txt_pb_temp_s.setText("--");
        } else {
            pb_temp_s.setProgress(Temperatura_S.intValue());
            txt_pb_temp_s.setText(df.format(Temperatura_S) + "°C");
        }
        if (Ph_E >= 21) {
            pb_ph_e.setProgress(0);
            txt_pb_ph_e.setText("--");
        } else {
            pb_ph_e.setProgress(Ph_E.intValue());
            txt_pb_ph_e.setText(df.format(Ph_E));
        }
        if (Ph_S >= 21) {
            pb_ph_s.setProgress(0);
            txt_pb_ph_s.setText("--");
        } else {
            pb_ph_s.setProgress(Ph_S.intValue());
            txt_pb_ph_s.setText(df.format(Ph_S));
        }
        ComprobarValores();
    }
    private void ComprobarValores(){
        int savedPosition = sharedPreferences.getInt("spinner_selection", 0);
        if (savedPosition == 0){
            if (Ppm_E > 500) {
                txt_pb_ppm_e.setTextColor(ContextCompat.getColor(getContext(), R.color.errorColor));
                pb_ppm_e.setBackgroundResource(R.drawable.circular_error);
                pb_ppm_e.setProgressDrawable(ContextCompat.getDrawable(getContext(), R.drawable.cicular_progress_error));
            } else {
                txt_pb_ppm_e.setTextColor(ContextCompat.getColor(getContext(), R.color.colorclaro));
                pb_ppm_e.setBackgroundResource(R.drawable.circular_shape);
                pb_ppm_e.setProgressDrawable(ContextCompat.getDrawable(getContext(), R.drawable.cicular_progress_bar));
            }
            if (Ppm_S > 500){
                txt_pb_ppm_s.setTextColor(ContextCompat.getColor(getContext(), R.color.errorColor));
                pb_ppm_s.setBackgroundResource(R.drawable.circular_error);
                pb_ppm_s.setProgressDrawable(ContextCompat.getDrawable(getContext(), R.drawable.cicular_progress_error));
            }
            else{
                txt_pb_ppm_s.setTextColor(ContextCompat.getColor(getContext(), R.color.colorclaro));
                pb_ppm_s.setBackgroundResource(R.drawable.circular_shape);
                pb_ppm_s.setProgressDrawable(ContextCompat.getDrawable(getContext(), R.drawable.cicular_progress_bar));
            }
            if (Turbidez_E > 100){
                txt_pb_turb_e.setTextColor(ContextCompat.getColor(getContext(), R.color.errorColor));
                pb_turb_e.setBackgroundResource(R.drawable.circular_error);
                pb_turb_e.setProgressDrawable(ContextCompat.getDrawable(getContext(), R.drawable.cicular_progress_error));
            }
            else{
                txt_pb_turb_e.setTextColor(ContextCompat.getColor(getContext(), R.color.colorclaro));
                pb_turb_e.setBackgroundResource(R.drawable.circular_shape);
                pb_turb_e.setProgressDrawable(ContextCompat.getDrawable(getContext(), R.drawable.cicular_progress_bar));
            }
            if (Turbidez_S > 100){
                txt_pb_turb_s.setTextColor(ContextCompat.getColor(getContext(), R.color.errorColor));
                pb_turb_s.setBackgroundResource(R.drawable.circular_error);
                pb_turb_s.setProgressDrawable(ContextCompat.getDrawable(getContext(), R.drawable.cicular_progress_error));
            }
            else{
                txt_pb_turb_s.setTextColor(ContextCompat.getColor(getContext(), R.color.colorclaro));
                pb_turb_s.setBackgroundResource(R.drawable.circular_shape);
                pb_turb_s.setProgressDrawable(ContextCompat.getDrawable(getContext(), R.drawable.cicular_progress_bar));
            }
        }
        else if (savedPosition == 1){
            if (Ppm_E > 750) {
                txt_pb_ppm_e.setTextColor(ContextCompat.getColor(getContext(), R.color.errorColor));
                pb_ppm_e.setBackgroundResource(R.drawable.circular_error);
                pb_ppm_e.setProgressDrawable(ContextCompat.getDrawable(getContext(), R.drawable.cicular_progress_error));
            } else {
                txt_pb_ppm_e.setTextColor(ContextCompat.getColor(getContext(), R.color.colorclaro));
                pb_ppm_e.setBackgroundResource(R.drawable.circular_shape);
                pb_ppm_e.setProgressDrawable(ContextCompat.getDrawable(getContext(), R.drawable.cicular_progress_bar));
            }
            if (Ppm_S > 750){
                txt_pb_ppm_s.setTextColor(ContextCompat.getColor(getContext(), R.color.errorColor));
                pb_ppm_s.setBackgroundResource(R.drawable.circular_error);
                pb_ppm_s.setProgressDrawable(ContextCompat.getDrawable(getContext(), R.drawable.cicular_progress_error));
            }
            else{
                txt_pb_ppm_s.setTextColor(ContextCompat.getColor(getContext(), R.color.colorclaro));
                pb_ppm_s.setBackgroundResource(R.drawable.circular_shape);
                pb_ppm_s.setProgressDrawable(ContextCompat.getDrawable(getContext(), R.drawable.cicular_progress_bar));
            }
            if (Turbidez_E > 450){
                txt_pb_turb_e.setTextColor(ContextCompat.getColor(getContext(), R.color.errorColor));
                pb_turb_e.setBackgroundResource(R.drawable.circular_error);
                pb_turb_e.setProgressDrawable(ContextCompat.getDrawable(getContext(), R.drawable.cicular_progress_error));
            }
            else{
                txt_pb_turb_e.setTextColor(ContextCompat.getColor(getContext(), R.color.colorclaro));
                pb_turb_e.setBackgroundResource(R.drawable.circular_shape);
                pb_turb_e.setProgressDrawable(ContextCompat.getDrawable(getContext(), R.drawable.cicular_progress_bar));
            }
            if (Turbidez_S > 450){
                txt_pb_turb_s.setTextColor(ContextCompat.getColor(getContext(), R.color.errorColor));
                pb_turb_s.setBackgroundResource(R.drawable.circular_error);
                pb_turb_s.setProgressDrawable(ContextCompat.getDrawable(getContext(), R.drawable.cicular_progress_error));
            }
            else{
                txt_pb_turb_s.setTextColor(ContextCompat.getColor(getContext(), R.color.colorclaro));
                pb_turb_s.setBackgroundResource(R.drawable.circular_shape);
                pb_turb_s.setProgressDrawable(ContextCompat.getDrawable(getContext(), R.drawable.cicular_progress_bar));
            }
        }
        else {
            if (Ppm_E > 5) {
                txt_pb_ppm_e.setTextColor(ContextCompat.getColor(getContext(), R.color.errorColor));
                pb_ppm_e.setBackgroundResource(R.drawable.circular_error);
                pb_ppm_e.setProgressDrawable(ContextCompat.getDrawable(getContext(), R.drawable.cicular_progress_error));
            } else {
                txt_pb_ppm_e.setTextColor(ContextCompat.getColor(getContext(), R.color.colorclaro));
                pb_ppm_e.setBackgroundResource(R.drawable.circular_shape);
                pb_ppm_e.setProgressDrawable(ContextCompat.getDrawable(getContext(), R.drawable.cicular_progress_bar));
            }
            if (Ppm_S > 5){
                txt_pb_ppm_s.setTextColor(ContextCompat.getColor(getContext(), R.color.errorColor));
                pb_ppm_s.setBackgroundResource(R.drawable.circular_error);
                pb_ppm_s.setProgressDrawable(ContextCompat.getDrawable(getContext(), R.drawable.cicular_progress_error));
            }
            else{
                txt_pb_ppm_s.setTextColor(ContextCompat.getColor(getContext(), R.color.colorclaro));
                pb_ppm_s.setBackgroundResource(R.drawable.circular_shape);
                pb_ppm_s.setProgressDrawable(ContextCompat.getDrawable(getContext(), R.drawable.cicular_progress_bar));
            }
            if (Turbidez_E > 0.5){
                txt_pb_turb_e.setTextColor(ContextCompat.getColor(getContext(), R.color.errorColor));
                pb_turb_e.setBackgroundResource(R.drawable.circular_error);
                pb_turb_e.setProgressDrawable(ContextCompat.getDrawable(getContext(), R.drawable.cicular_progress_error));
            }
            else{
                txt_pb_turb_e.setTextColor(ContextCompat.getColor(getContext(), R.color.colorclaro));
                pb_turb_e.setBackgroundResource(R.drawable.circular_shape);
                pb_turb_e.setProgressDrawable(ContextCompat.getDrawable(getContext(), R.drawable.cicular_progress_bar));
            }
            if (Turbidez_S > 0.5){
                txt_pb_turb_s.setTextColor(ContextCompat.getColor(getContext(), R.color.errorColor));
                pb_turb_s.setBackgroundResource(R.drawable.circular_error);
                pb_turb_s.setProgressDrawable(ContextCompat.getDrawable(getContext(), R.drawable.cicular_progress_error));
            }
            else{
                txt_pb_turb_s.setTextColor(ContextCompat.getColor(getContext(), R.color.colorclaro));
                pb_turb_s.setBackgroundResource(R.drawable.circular_shape);
                pb_turb_s.setProgressDrawable(ContextCompat.getDrawable(getContext(), R.drawable.cicular_progress_bar));
            }
            if (Ph_E > 7.6 && Ph_E <21 || Ph_E < 7.0 ){
                txt_pb_ph_e.setTextColor(ContextCompat.getColor(getContext(), R.color.errorColor));
                pb_ph_e.setBackgroundResource(R.drawable.circular_error);
                pb_ph_e.setProgressDrawable(ContextCompat.getDrawable(getContext(), R.drawable.cicular_progress_error));
            }
            else{
                txt_pb_ph_e.setTextColor(ContextCompat.getColor(getContext(), R.color.colorclaro));
                pb_ph_e.setBackgroundResource(R.drawable.circular_shape);
                pb_ph_e.setProgressDrawable(ContextCompat.getDrawable(getContext(), R.drawable.cicular_progress_bar));
            }
            if (Ph_S > 7.6 && Ph_S <21 || Ph_S < 7.0 && Ph_S > 21){
                txt_pb_ph_s.setTextColor(ContextCompat.getColor(getContext(), R.color.errorColor));
                pb_ph_s.setBackgroundResource(R.drawable.circular_error);
                pb_ph_s.setProgressDrawable(ContextCompat.getDrawable(getContext(), R.drawable.cicular_progress_error));
            }
            else{
                txt_pb_ph_s.setTextColor(ContextCompat.getColor(getContext(), R.color.colorclaro));
                pb_ph_s.setBackgroundResource(R.drawable.circular_shape);
                pb_ph_s.setProgressDrawable(ContextCompat.getDrawable(getContext(), R.drawable.cicular_progress_bar));
            }
        }
    }

    private void agregarPuntoConHora(String hora) {
        String tipoDato = getSelectedVariable();
        List<Entry> entries;
        String descriptionLabel = "";
        float maxRange = 0f;
        float minutesSinceStart = Math.round((System.currentTimeMillis() - startTime) / 60000f * 10) / 10.0f;
        timeLabels.put(minutesSinceStart, hora);

        switch (tipoDato) {
            case "PPM E":
                entries = ppmEEntries;
                entries.add(new Entry(minutesSinceStart, Ppm_E));
                descriptionLabel = "PPM Entrada";
                maxRange = 1000f;
                break;
            case "PPM S":
                entries = ppmSEntries;
                entries.add(new Entry(minutesSinceStart, Ppm_S));
                descriptionLabel = "PPM Salida";
                maxRange = 1000f;
                break;
            case "Turbidez E":
                entries = turbidezEEntries;
                entries.add(new Entry(minutesSinceStart, Turbidez_E));
                descriptionLabel = "Turbidez Entrada";
                maxRange = 5000f;
                break;
            case "Turbidez S":
                entries = turbidezSEntries;
                entries.add(new Entry(minutesSinceStart, Turbidez_S));
                descriptionLabel = "Turbidez Salida";
                maxRange = 5000f;
                break;
            case "Temperatura E":
                entries = temperaturaEEntries;
                entries.add(new Entry(minutesSinceStart, (float) Temperatura_E));
                descriptionLabel = "Temperatura Entrada";
                maxRange = 100f;
                break;
            case "Temperatura S":
                entries = temperaturaSEntries;
                entries.add(new Entry(minutesSinceStart, (float) Temperatura_S));
                descriptionLabel = "Temperatura Salida";
                maxRange = 100f;
                break;
            case "PH E":
                entries = phEEntries;
                entries.add(new Entry(minutesSinceStart, (float) Ph_E));
                descriptionLabel = "PH Entrada";
                maxRange = 14f;
                break;
            case "PH S":
                entries = phSEntries;
                entries.add(new Entry(minutesSinceStart, (float) Ph_S));
                descriptionLabel = "PH Salida";
                maxRange = 14f;
                break;
            default:
                entries = new ArrayList<>();
                break;
        }

        LineDataSet dataSet = new LineDataSet(entries, descriptionLabel);
        dataSet.setColor(ContextCompat.getColor(requireContext(), R.color.colorclaro));
        dataSet.setValueTextColor(Color.RED);
        dataSet.setDrawValues(false);

        List<ILineDataSet> dataSets = new ArrayList<>();
        dataSets.add(dataSet);
        LineData lineData = new LineData(dataSets);
        chart.setData(lineData);

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setAxisMaximum(maxRange);
        leftAxis.setAxisMinimum(0f);

        XAxis xAxis = chart.getXAxis();
        xAxis.setGranularity(1f);
        xAxis.setGranularityEnabled(true);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                float roundedValue = Math.round(value * 10) / 10.0f;
                String label = timeLabels.get(roundedValue);
                return label != null ? label : "";
            }
        });

        chart.getDescription().setText(descriptionLabel);
        chart.invalidate();
    }

    private void Temporizador() {
        handler = new Handler(Looper.getMainLooper());
        startTime = System.currentTimeMillis();
        executorService.execute(() -> {
            updateRunnable = () -> {
                String horaActual = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
                agregarPuntoConHora(horaActual);
                handler.postDelayed(updateRunnable, 60000);
            };
            handler.postDelayed(updateRunnable, 60000);
        });
    }

    private void initializeGraphData() {
        ppmEEntries = new ArrayList<>();
        ppmSEntries = new ArrayList<>();
        turbidezEEntries = new ArrayList<>();
        turbidezSEntries = new ArrayList<>();
        temperaturaEEntries = new ArrayList<>();
        temperaturaSEntries = new ArrayList<>();
        phEEntries = new ArrayList<>();
        phSEntries = new ArrayList<>();
        timeLabels = new HashMap<>();
    }

    private void setupButtons(View rootView) {
        CardView cardPpmE = rootView.findViewById(R.id.card_ppm_e);
        CardView cardTurbidezE = rootView.findViewById(R.id.card_turbidez_e);
        CardView cardTemperaturaE = rootView.findViewById(R.id.card_temperatura_e);
        CardView cardPhE = rootView.findViewById(R.id.card_ph_e);
        CardView cardPpmS = rootView.findViewById(R.id.card_ppm_s);
        CardView cardTurbidezS = rootView.findViewById(R.id.card_turbidez_s);
        CardView cardTemperaturaS = rootView.findViewById(R.id.card_temperatura_s);
        CardView cardPhS = rootView.findViewById(R.id.card_ph_s);

        View.OnClickListener buttonClickListener = v -> {
            CardView selectedCardView = (CardView) v.getParent();
            if (selectedCardView == lastSelectedCardView) {
                return;
            }

            if (lastSelectedCardView != null) {
                lastSelectedCardView.setSelected(false);
                lastSelectedCardView.animate().translationY(0).setDuration(100).start();
            }

            selectedCardView.setSelected(true);
            selectedCardView.animate().translationY(-15).setDuration(100).start();
            lastSelectedCardView = selectedCardView;

            agregarPuntoConHora(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date()));
        };

        cardPpmE.findViewById(R.id.btn_ppm_e).setOnClickListener(buttonClickListener);
        cardTurbidezE.findViewById(R.id.btn_turbidez_e).setOnClickListener(buttonClickListener);
        cardTemperaturaE.findViewById(R.id.btn_temperatura_e).setOnClickListener(buttonClickListener);
        cardPhE.findViewById(R.id.btn_ph_e).setOnClickListener(buttonClickListener);
        cardPpmS.findViewById(R.id.btn_ppm_s).setOnClickListener(buttonClickListener);
        cardTurbidezS.findViewById(R.id.btn_turbidez_s).setOnClickListener(buttonClickListener);
        cardTemperaturaS.findViewById(R.id.btn_temperatura_s).setOnClickListener(buttonClickListener);
        cardPhS.findViewById(R.id.btn_ph_s).setOnClickListener(buttonClickListener);
    }

    private String getSelectedVariable() {
        if (lastSelectedCardView != null) {
            Button selectedButton = (Button) lastSelectedCardView.getChildAt(0);
            return selectedButton.getText().toString();
        }
        return "PPM E";
    }
}
