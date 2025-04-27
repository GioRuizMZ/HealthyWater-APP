package com.tonala.healthywather.Activities;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.tonala.healthywather.R;

import java.util.ArrayList;
import java.util.List;

public class HelpActivity extends AppCompatActivity {
    private Context context;
    private MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ayuda);
        ConstraintLayout raiz = findViewById(R.id.raiz);
        raiz.setTranslationX(-300);
        raiz.setAlpha(0.0f);
        ViewPropertyAnimator animator1 = raiz.animate()
                .translationX(0)
                .alpha(1.0f)
                .setDuration(500);
        SharedPreferences sharedPreferences = this.getSharedPreferences("user_data", Context.MODE_PRIVATE);
        if (sharedPreferences.getBoolean("Audio",false) == true){
            mediaPlayer = MediaPlayer.create(this, R.raw.enquepodemosayudar);
            mediaPlayer.start();
        }
        ImageButton btn_back = findViewById(R.id.btn_BackAyuda);
        btn_back.setOnClickListener(v-> onBackPressed());
        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new IndiceAdapter());
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null && !extras.isEmpty()) {

            recyclerView.post(() -> {

                int position = 5; // Asumiendo que "Variables del Agua" es el cuarto elemento
                IndiceAdapter.IndiceViewHolder viewHolder = (IndiceAdapter.IndiceViewHolder) recyclerView.findViewHolderForAdapterPosition(position);
                if (viewHolder != null) {
                    viewHolder.itemView.performClick(); // Simular clic en el título
                    viewHolder.recyclerViewSubtitulos.post(() -> {
                        int subPosition = 0; // Asumiendo que "- ¿Cuales son las variables?" es el primer subtítulo
                        SubtitulosAdapter.SubtitulosViewHolder subViewHolder = (SubtitulosAdapter.SubtitulosViewHolder) viewHolder.recyclerViewSubtitulos.findViewHolderForAdapterPosition(subPosition);
                        if (subViewHolder != null) {
                            subViewHolder.itemView.performClick(); // Simular clic en el subtítulo
                        }
                    });
                }
            });
        }
    }
    @Override
    protected void onDestroy(){
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
    public class IndiceAdapter extends RecyclerView.Adapter<IndiceAdapter.IndiceViewHolder> {
        private final String[] titulos = {"Inicio sesion / Crear cuenta", "Configuracion de la interfaz", "Configuracion del dispositivo", "ServicesActivity", "Variables del Agua"};
        @NonNull
        @Override
        public IndiceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_indice, parent, false);
            return new IndiceViewHolder(view);
        }
        @Override
        public void onBindViewHolder(@NonNull IndiceViewHolder holder, int position) {
            String titulo = titulos[position];
            holder.bind(titulo);
        }
        @Override
        public int getItemCount() {
            return titulos.length;
        }
        class IndiceViewHolder extends RecyclerView.ViewHolder {
            private final TextView tituloTextView;
            private final ImageButton desplegarImageButton;
            private final RecyclerView recyclerViewSubtitulos;
            private final SubtitulosAdapter subtitulosAdapter;
            IndiceViewHolder(@NonNull View itemView) {
                super(itemView);
                context = itemView.getContext();
                tituloTextView = itemView.findViewById(R.id.titulo);
                desplegarImageButton = itemView.findViewById(R.id.desplegar);
                recyclerViewSubtitulos = itemView.findViewById(R.id.recycler_view_subtitulos);
                recyclerViewSubtitulos.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
                subtitulosAdapter = new SubtitulosAdapter(new ArrayList<>());
                recyclerViewSubtitulos.setAdapter(subtitulosAdapter);

                desplegarImageButton.setOnClickListener(v -> {

                    List<String> subtitulosList = obtenerSubtitulosPorTitulo(tituloTextView.getText().toString());
                    subtitulosAdapter.actualizarSubtitulos(subtitulosList);

                    if (recyclerViewSubtitulos.getVisibility() == View.VISIBLE) {
                        desplegarImageButton.setImageResource(R.drawable.contraer_abajo);
                        Animation contraer = AnimationUtils.loadAnimation(context, R.anim.contraer);
                        recyclerViewSubtitulos.setAnimation(contraer);
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                recyclerViewSubtitulos.setVisibility(View.GONE);
                            }
                        }, 300);
                    } else {
                        desplegarImageButton.setImageResource(R.drawable.desplegar_arriba);
                        Animation translate_anim = AnimationUtils.loadAnimation(context, R.anim.desplegar);
                        recyclerViewSubtitulos.setAnimation(translate_anim);
                        recyclerViewSubtitulos.setVisibility(View.VISIBLE);
                    }
                });
            }
            void bind(final String titulo) {
                tituloTextView.setText(titulo);
            }
        }
        private List<String> obtenerSubtitulosPorTitulo(String titulo) {

            List<String> subtitulosList = new ArrayList<>();
            switch (titulo) {
                case "Inicio sesion / Crear cuenta":
                    subtitulosList.add("- No puedo crear una cuenta");
                    subtitulosList.add("- No puedo iniciar sesion 'La cuenta ya existe'");
                    subtitulosList.add("- No puedo iniciar sesion 'El email ha sido verificado'");
                    break;
                case "Configuracion de la interfaz":
                    subtitulosList.add("- Editar mi perfil");
                    subtitulosList.add("- Configuracion de notificaciones");
                    break;
                case "Configuracion del dispositivo":
                    subtitulosList.add("- Agregar / Eliminar dispositivos");
                    subtitulosList.add("- Modos de conexion");
                    subtitulosList.add("- Calibrar variables del dispositivo");
                    break;
                case "ServicesActivity":
                    subtitulosList.add("- Crear una cita");
                    subtitulosList.add("- Cancelar / Modificar una cita");
                    break;
                case "Variables del Agua":
                    subtitulosList.add("- ¿Cuales son las variables?");
                    subtitulosList.add("- ¿Como se la calidad del agua?");
                    break;
            }
            return subtitulosList;
        }
    }
    public class SubtitulosAdapter extends RecyclerView.Adapter<SubtitulosAdapter.SubtitulosViewHolder> {
        private final List<String> subtitulosList;
        public SubtitulosAdapter(List<String> subtitulosList) {
            this.subtitulosList = subtitulosList;
        }
        @NonNull
        @Override
        public SubtitulosViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_subtitulo, parent, false);
            return new SubtitulosViewHolder(view);
        }
        @Override
        public void onBindViewHolder(@NonNull SubtitulosViewHolder holder, int position) {
            String subtitulo = subtitulosList.get(position);
            holder.bind(subtitulo);
        }
        @Override
        public int getItemCount() {
            return subtitulosList.size();
        }
        @SuppressLint("NotifyDataSetChanged")
        public void actualizarSubtitulos(List<String> subtitulosList) {
            this.subtitulosList.clear();
            this.subtitulosList.addAll(subtitulosList);
            notifyDataSetChanged();
        }

        class SubtitulosViewHolder extends RecyclerView.ViewHolder {
            private final TextView subtituloTextView;
            private final TextView textoAdicional;
            private final LinearLayout textoAdicionalLayout;
            SubtitulosViewHolder(@NonNull View itemView) {
                super(itemView);
                subtituloTextView = itemView.findViewById(R.id.subtitulo);
                textoAdicionalLayout = itemView.findViewById(R.id.texto_adicional_layout);
                textoAdicional = itemView.findViewById(R.id.texto_adicional);
            }
            @SuppressLint("SetTextI18n")
            void bind(final String subtitulo) {
                subtituloTextView.setText(subtitulo);
                subtituloTextView.setOnClickListener(v -> {

                    if (textoAdicionalLayout.getVisibility() == View.GONE) {
                        switch (subtitulo) {
                            case "- No puedo crear una cuenta":
                                textoAdicional.setText("Para crear una cuenta primero necesitara un correo valido y un numero de telefono valido,\nintroduzca todos los campos y acepte los terminos y condiciones, despues verifique su correo e inicie sesion, es importante que al crear su cuenta se asegure de que ningun correo electronica este vinculado a otra cuenta");
                                break;
                            case "- No puedo iniciar sesion 'La cuenta ya existe'":
                                textoAdicional.setText("Si intenta crear una cuenta con un correo ya existente o loggeado con otro metodo de inicio de sesion, inicie sesion con el metodo de inicio que uso para esa cuenta o cambie la contraseña de la cuenta si no la recuerda");
                                break;
                            case "- No puedo iniciar sesion 'El email ha sido verificado'":
                                textoAdicional.setText("Si no ha verificado su email o no ve el correo de verificacion estos son los pasos que debe seguir: \n- Primero entre a su aplicacion de correo con el mismo email con el que se registro en la app \n- Dirigase a la carpeta de spam y busque el correo de healthywater,\n- Por ultimo presione el boton de verirficar y despues entre a la app e inicie sesion con ese email");
                                break;
                            case "- Editar mi perfil":
                                textoAdicional.setText("Para cambiar los datos de su perfil necesitara que todos los campos cumplan con los requisitos y una conexion a internet,\ndespues presione actualizar mi informacion y espere a que se actualize sus datos");
                                break;
                            case "- Configuracion de notificaciones":
                                textoAdicional.setText("Usted puede desactivar o activar las notificaciones en el apartado de -> Configuracion > Notificaciones");
                                break;
                            case "- Agregar / Eliminar dispositivos":
                                textoAdicional.setText("Para agregar un dispositivo debe de estar en la pantalla de inicio y pulsar el boton + que se encuentra en el medio de la pantalla,\nDespues seleccione el modo en el que desea conectarse al dipositivo y escanee el codigo qr que se enccuentra en el manual del dispositivo o tambien puedo encontrarlo en nuestra pagina web en el apartado de Mi healthywater\nEn caso de hacer una conexion wifi asigne un nombre al dispositivo y listo ,ya esta listo para su uso");
                                break;
                            case "- Modos de conexion":
                                textoAdicional.setText("Existen 3 moodos de conexion para su healthywater:\n\n- Modo Local \n- Modo remoto \n- Codigo de vinculacion \n\nEn el modo local usted se conectara por medio de bluetooth al dispositivo y sera una configuracion rapida ,En este modo no se podra monitorear a una distancia mayor a 4 metros y usted podra elegir si vincular y guardar el dispositivo para que cada que lo encienda y entre a la app se conecte automaticamente o solo vincular por esta vez (ideal si lo van a usar mas personas)" +
                                        "\nEl modo local es ideal en espacios donde no se encuentre la posibilidad de una conexion wifi o en casos donde se necesita estar cambiando de lugar el dispositivo constantemente.\nEn el modo remoto usted debe configurar una red y un nombre a su dispositivo para poder conectarse desde cualquier lugar a su healthywater ya que no hay un limite de distancia para que pueda monitorearlo,\ndespues de configurarlo la conexion sera automatica y no tendra que hacer ninguna conexion mas" +
                                        "El modo remoto es ideal en espacios fijos dodne haya una buena conexion a internet y que solo se usara el dispositivo en ese lugar\nEn la conexion por codigo de vinculacion el usuario qeu ya haya configurado un Healthywater puede compartir el codigo desde > Configuracion -> Mi Healthywater ,y otros usuarios puededen monitorearlo agregando el codigo en el boton de agregar un nuevo dispositivo -> Conexion por codigo.");
                                break;
                            case "- Calibrar variables del dispositivo":
                                textoAdicional.setText("Para calibrar las variables del dispositvo presione el icono de editar en la parte superior derecha en el tablero de monitoreo o dirigase a -> Configuracion > Mi healthywater, En ese apartado usted podra ver las variables en uso y calibrarlas tocando la rueda de configuracion al lado de ellas, despues al terminar de calibrar presione el boton de guardar configuracion para que los cambios se apliquen");
                                break;
                            case "- Crear una cita":
                                textoAdicional.setText("Para crear una cita dirigase a -> ServicesActivity > crear una cita , despues configure los datos de la cita y presione 'Crear cita' \nNota: si usted tiene una cita pendiente no podra crear otra hasta que la cita previa haya sido aceptada, cancelada o bien concluida");
                                break;
                            case "- Cancelar / Modificar una cita":
                                textoAdicional.setText("Para cancelar una cita dirigase a -> ServicesActivity > Citas y presiona la cita que quiere cancelar , despues presione el boton 'Eliminar' y espere a que se elimine la cita,con esto tambien cancelara la cita." +
                                        "\nPara modificar la cita seleccione la cita en el mismo apartado mencionado anteriormete y presione el boton 'Modificar' , modifique los campos que sea necesario y presione 'Actualizar' para que se apliquen los cambios" +
                                        "\nNota: no podra modificar la cita cuando ya haya sido aceptada , verifique bien sus datos antes de crear la cita");
                                break;
                            case "- ¿Cuales son las variables?":
                                textoAdicional.setText("Las variables que se miden en el agua incluyen:\n\n" +
                                        "- PH: Indica la acidez o alcalinidad del agua. Un pH de 7 es neutral, menos de 7 es ácido y más de 7 es alcalino.\n\n" +
                                        "- Temperatura: Influye en la solubilidad y reacciones químicas en el agua. El agua a diferentes temperaturas puede interferir en variables como el PH.\n\n" +
                                        "- Turbidez: Mide la claridad del agua. Alta turbidez puede indicar contaminación por partículas en suspensión.\n\n" +
                                        "- Conductividad: Indica la capacidad del agua para conducir electricidad, lo cual está relacionado con la presencia de sales disueltas.\n\n" +
                                        "- PPM: Partes por millón, mide la concentración de sustancias disueltas en el agua, como minerales y contaminantes.");
                                break;
                            case "- ¿Como se la calidad del agua?":
                                textoAdicional.setText("Los valores recomendados para el agua potable y el agua para uso doméstico son:\n\n" +
                                        "- PH: Para agua potable, un pH entre 6.5 y 8.5 es ideal. Para uso doméstico, un rango similar es adecuado teniendo 2 unidades mas o menos partiendo de 7 PH\n\n" +
                                        "- Temperatura: El agua potable debe estar preferiblemente por debajo de 25°C. Para uso doméstico, la temperatura puede variar más dependiendo de la aplicación.\n\n" +
                                        "- Turbidez: Para agua potable, la turbidez debe ser menor a 1 NTU (Unidad Nefelométrica de Turbidez). Para uso doméstico, valores más de 300 NTU altos pueden ser aceptables dependiendo del uso.\n\n" +
                                        "- Conductividad: El agua potable debe tener una conductividad menor a 500 µS/cm. Para uso doméstico, puede tolerarse hasta 2000 µS/cm dependiendo del uso.\n\n" +
                                        "- PPM: El agua potable debe tener menos de 500 ppm de sólidos disueltos totales (TDS). Para uso doméstico, puede ser hasta 750 ppm dependiendo de la aplicación.");
                                break;
                        }
                        Animation desplegartxt = AnimationUtils.loadAnimation(context, R.anim.desplegar);
                        textoAdicionalLayout.setAnimation(desplegartxt);
                        textoAdicionalLayout.setVisibility(View.VISIBLE);
                    } else {
                        Animation contraertxt = AnimationUtils.loadAnimation(context, R.anim.contraer);
                        contraertxt.setAnimationListener(new Animation.AnimationListener() {
                            @Override
                            public void onAnimationStart(Animation animation) {
                                // No hacer nada
                            }

                            @Override
                            public void onAnimationEnd(Animation animation) {
                                textoAdicionalLayout.setVisibility(View.GONE);
                            }

                            @Override
                            public void onAnimationRepeat(Animation animation) {
                                // No hacer nada
                            }
                        });
                        textoAdicionalLayout.startAnimation(contraertxt);
                    }
                });
            }
        }
    }
}