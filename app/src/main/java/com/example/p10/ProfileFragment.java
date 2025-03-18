package com.example.p10;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.example.p10.R;

import io.appwrite.Client;
import io.appwrite.coroutines.CoroutineCallback;
import io.appwrite.exceptions.AppwriteException;
import io.appwrite.services.Account;

public class ProfileFragment extends Fragment {

    private static final String PREFS_NAME = "profile_prefs";
    private static final String KEY_PHOTO_URI = "photo_uri";

    NavController navController;
    ImageView photoImageView;
    TextView displayNameTextView, emailTextView;
    private ActivityResultLauncher<String> galleryLauncher;

    public ProfileFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);

        photoImageView = view.findViewById(R.id.photoImageView);
        displayNameTextView = view.findViewById(R.id.displayNameTextView);
        emailTextView = view.findViewById(R.id.emailTextView);

        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String savedPhotoUriString = prefs.getString(KEY_PHOTO_URI, null);
        if (savedPhotoUriString != null) {
            Uri savedPhotoUri = Uri.parse(savedPhotoUriString);
            Glide.with(requireContext()).load(savedPhotoUri).into(photoImageView);
        } else {
            Glide.with(requireContext()).load(R.drawable.user).into(photoImageView);
        }

        Client client = new Client(requireContext())
                .setProject(getString(R.string.APPWRITE_PROJECT_ID));
        Account account = new Account(client);
        Handler mainHandler = new Handler(Looper.getMainLooper());

        try {
            account.get(new CoroutineCallback<>((result, error) -> {
                if (error != null) {
                    error.printStackTrace();
                    return;
                }
                mainHandler.post(() -> {
                    displayNameTextView.setText(result.getName());
                    emailTextView.setText(result.getEmail());
                });
            }));
        } catch (AppwriteException e) {
            throw new RuntimeException(e);
        }

        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                (Uri uri) -> {
                    if (uri != null) {
                        Glide.with(requireContext())
                                .load(uri)
                                .circleCrop()
                                .into(photoImageView);
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putString(KEY_PHOTO_URI, uri.toString());
                        editor.apply();
                    }
                }
        );

        // Al hacer click en la foto, se abre la galería
        photoImageView.setOnClickListener(v -> galleryLauncher.launch("image/*"));
    }
}
