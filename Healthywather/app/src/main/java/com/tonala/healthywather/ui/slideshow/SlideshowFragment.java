package com.tonala.healthywather.ui.slideshow;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.tonala.healthywather.R;
import com.tonala.healthywather.databinding.FragmentSlideshowBinding;

import org.w3c.dom.Text;

public class SlideshowFragment extends Fragment {

private FragmentSlideshowBinding binding;
private MediaPlayer mediaPlayer;
private SharedPreferences sharedPreferences;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        SlideshowViewModel slideshowViewModel = new ViewModelProvider(this).get(SlideshowViewModel.class);
        binding = FragmentSlideshowBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        TextView VerMas = root.findViewById(R.id.textview_link);
        SpannableString content = new SpannableString("Ver más");
        content.setSpan(new UnderlineSpan(), 0, content.length(), 0);
        VerMas.setText(content);
        VerMas.setOnClickListener(v -> {
            String url = "https://healthywater.proyectos8c.com/healthywater_page/terminos.html";
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            startActivity(intent);
        });
        sharedPreferences = getContext().getSharedPreferences("user_data", Context.MODE_PRIVATE);

        return root;
    }

@Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
    public void onResume(){
        super.onResume();
        if (sharedPreferences.getBoolean("Audio",false) == true){
            mediaPlayer = MediaPlayer.create(getContext(), R.raw.acercadehealthywater);
            mediaPlayer.start();
        }
    }
}