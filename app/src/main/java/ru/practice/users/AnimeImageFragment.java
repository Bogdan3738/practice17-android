package ru.practice.users;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

/** Фрагмент управляет только изображением и его анимацией. */
public class AnimeImageFragment extends Fragment {
    private ImageView image;

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                       Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_anime_image, container, false);
        Init(root);
        return root;
    }
    private void Init(View root) {
        image = root.findViewById(R.id.imageForLoaded);
        image.startAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.logo_rotate_animation));
    }
    @Override public void onStart() {
        super.onStart();
        if (image != null && image.getAnimation() == null) Init(getView());
    }
    @Override public void onStop() {
        if (image != null) image.clearAnimation();
        super.onStop();
    }
    @Override public void onDestroyView() {
        if (image != null) image.clearAnimation();
        image = null;
        super.onDestroyView();
    }
}
