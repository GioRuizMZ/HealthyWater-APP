package com.tonala.healthywather.ui.gallery;

import android.content.SharedPreferences;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GalleryViewModel extends ViewModel {
    private final MutableLiveData<String> nombre = new MutableLiveData<>();
    private final MutableLiveData<String> correo = new MutableLiveData<>();
    private final MutableLiveData<String> telefono = new MutableLiveData<>();
    private final MutableLiveData<String> imagenPerfil = new MutableLiveData<>();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public GalleryViewModel() {
    }

    public LiveData<String> getNombre() {
        return nombre;
    }

    public LiveData<String> getCorreo() {
        return correo;
    }

    public LiveData<String> getTelefono() {
        return telefono;
    }
    public LiveData<String> getImagenPerfil() {
        return imagenPerfil;
    }

    public void cargarDatosUsuario(SharedPreferences sharedPreferences) {
        executorService.execute(() -> {
            String nombreValue = sharedPreferences.getString("nombre", "");
            String correoValue = sharedPreferences.getString("correo", "");
            String telefonoValue = sharedPreferences.getString("telefono", "");
            String imagenPerfilValue = sharedPreferences.getString("imagen", "");

            nombre.postValue(nombreValue);
            correo.postValue(correoValue);
            telefono.postValue(telefonoValue);
            imagenPerfil.postValue(imagenPerfilValue);
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executorService.shutdown();
    }
}
