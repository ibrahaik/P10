package com.example.p10;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;  // Usamos EditText
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.p10.R;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.appwrite.Client;
import io.appwrite.Query;
import io.appwrite.coroutines.CoroutineCallback;
import io.appwrite.exceptions.AppwriteException;
import io.appwrite.models.Document;
import io.appwrite.models.DocumentList;
import io.appwrite.services.Account;
import io.appwrite.services.Databases;
import io.appwrite.services.Databases;
import io.appwrite.services.Storage;
import io.appwrite.services.Storage;
import io.appwrite.models.File;
import io.appwrite.models.InputFile;

public class HomeFragment extends Fragment {

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    AppViewModel appViewModel;

    private NavController navController;
    PostsAdapter adapter;
    private String mParam1;
    private String mParam2;

    private ImageView photoImageView;
    private TextView displayNameTextView, emailTextView;
    private Client client;
    private Account account;
    private String userId;

    public HomeFragment() {
    }

    public static HomeFragment newInstance(String param1, String param2) {
        HomeFragment fragment = new HomeFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        appViewModel = new ViewModelProvider(requireActivity()).get(AppViewModel.class);

        // Obtener referencias al encabezado del NavigationView
        NavigationView navigationView = view.getRootView().findViewById(R.id.nav_view);
        View header = navigationView.getHeaderView(0);
        photoImageView = header.findViewById(R.id.imageView);
        displayNameTextView = header.findViewById(R.id.displayNameTextView);
        emailTextView = header.findViewById(R.id.emailTextView);

        navController = Navigation.findNavController(view);

        // Inicializar Appwrite Client
        client = new Client(requireContext()).setProject(getString(R.string.APPWRITE_PROJECT_ID));
        account = new Account(client);
        Handler mainHandler = new Handler(Looper.getMainLooper());

        // Obtener información del usuario
        try {
            account.get(new CoroutineCallback<>((result, error) -> {
                if(error != null) {
                    error.printStackTrace();
                    return;
                }
                mainHandler.post(() -> {
                    userId = result.getId();
                    displayNameTextView.setText(result.getName());
                    emailTextView.setText(result.getEmail());
                    Glide.with(requireView()).load(R.drawable.user).into(photoImageView);
                    obtenerPosts();
                });
            }));
        } catch (AppwriteException e) {
            throw new RuntimeException(e);
        }

        view.findViewById(R.id.gotoNewPostFragmentButton).setOnClickListener(v -> {
            navController.navigate(R.id.newPostFragment);
        });

        RecyclerView postsRecyclerView = view.findViewById(R.id.postsRecyclerView);
        adapter = new PostsAdapter();
        postsRecyclerView.setAdapter(adapter);
    }

    class PostViewHolder extends RecyclerView.ViewHolder {
        ImageView authorPhotoImageView, likeImageView, mediaImageView, btnDelete, btnShare;
        TextView authorTextView, contentTextView, numLikesTextView;


        PostViewHolder(@NonNull View itemView) {
            super(itemView);
            authorPhotoImageView = itemView.findViewById(R.id.authorPhotoImageView);
            likeImageView = itemView.findViewById(R.id.likeImageView);
            mediaImageView = itemView.findViewById(R.id.mediaImage);
            authorTextView = itemView.findViewById(R.id.authorTextView);
            contentTextView = itemView.findViewById(R.id.contentTextView);
            numLikesTextView = itemView.findViewById(R.id.numLikesTextView);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            btnShare = itemView.findViewById(R.id.btnShare);

        }
    }

    // Adaptador de posts
    class PostsAdapter extends RecyclerView.Adapter<PostViewHolder> {
        DocumentList<Map<String, Object>> lista = null;

        @NonNull
        @Override
        public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.viewholder_post, parent, false);
            return new PostViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
            Map<String, Object> post = lista.getDocuments().get(position).getData();
            final String postId = post.get("$id").toString();  // Definido una sola vez

            // Configurar datos del post
            if (post.get("authorPhotoUrl") == null) {
                holder.authorPhotoImageView.setImageResource(R.drawable.user);
            } else {
                Glide.with(getContext())
                        .load(post.get("authorPhotoUrl").toString())
                        .circleCrop()
                        .into(holder.authorPhotoImageView);
            }
            holder.authorTextView.setText(post.get("author").toString());
            holder.contentTextView.setText(post.get("content").toString());

            // Gestión de likes
            List<String> likes = (List<String>) post.get("likes");
            if (likes.contains(userId))
                holder.likeImageView.setImageResource(R.drawable.like_on);
            else
                holder.likeImageView.setImageResource(R.drawable.like_off);
            holder.numLikesTextView.setText(String.valueOf(likes.size()));
            holder.likeImageView.setOnClickListener(view -> {
                Databases databases = new Databases(client);
                Handler mainHandler = new Handler(Looper.getMainLooper());
                List<String> nuevosLikes = likes;
                if(nuevosLikes.contains(userId))
                    nuevosLikes.remove(userId);
                else
                    nuevosLikes.add(userId);
                Map<String, Object> data = new HashMap<>();
                data.put("likes", nuevosLikes);
                try {
                    databases.updateDocument(
                            getString(R.string.APPWRITE_DATABASE_ID),
                            getString(R.string.APPWRITE_POSTS_COLLECTION_ID),
                            postId,
                            data,
                            new ArrayList<>(),
                            new CoroutineCallback<>((result, error) -> {
                                if(error != null){
                                    error.printStackTrace();
                                    return;
                                }
                                mainHandler.post(() -> obtenerPosts());
                            })
                    );
                } catch(AppwriteException e){
                    throw new RuntimeException(e);
                }
            });

            if (post.get("uid") != null && post.get("uid").toString().equals(userId)) {
                holder.btnDelete.setVisibility(View.VISIBLE);
                holder.btnDelete.setOnClickListener(view -> {
                    new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                            .setTitle("Eliminar Post")
                            .setMessage("¿Seguro que deseas eliminar este post?")
                            .setPositiveButton("Eliminar", (dialog, which) -> {
                                final Map<String, Object> deletedPost = new HashMap<>(post);
                                DeletePost.deletePost(client, postId, requireContext(), success -> {
                                    if (success) {
                                        obtenerPosts();
                                        Snackbar.make(requireView(), "Post eliminado", Snackbar.LENGTH_LONG)
                                                .setAction("Deshacer", v -> {
                                                    DeletePost.restorePost(client, deletedPost, requireContext(), restoreSuccess -> {
                                                        if (restoreSuccess) {
                                                            obtenerPosts();
                                                            Snackbar.make(requireView(), "Post restaurado", Snackbar.LENGTH_SHORT).show();
                                                        } else {
                                                            Snackbar.make(requireView(), "Error al restaurar el post", Snackbar.LENGTH_SHORT).show();
                                                        }
                                                    });
                                                }).show();
                                    } else {
                                        Snackbar.make(requireView(), "Error al eliminar el post", Snackbar.LENGTH_SHORT).show();
                                    }
                                });
                            })
                            .setNegativeButton("Cancelar", null)
                            .show();
                });
            } else {
                holder.btnDelete.setVisibility(View.GONE);
            }

            // Configuración de media
            if (post.get("mediaUrl") != null) {
                holder.mediaImageView.setVisibility(View.VISIBLE);
                if ("audio".equals(post.get("mediaType").toString())) {
                    Glide.with(requireView())
                            .load(R.drawable.audio)
                            .centerCrop()
                            .into(holder.mediaImageView);
                } else {
                    Glide.with(requireView())
                            .load(post.get("mediaUrl").toString())
                            .centerCrop()
                            .into(holder.mediaImageView);
                }
                holder.mediaImageView.setOnClickListener(view -> {
                    appViewModel.postSeleccionado.setValue(post);
                    navController.navigate(R.id.mediaFragment);
                });
            } else {
                holder.mediaImageView.setVisibility(View.GONE);
            }

            holder.btnShare.setOnClickListener(v -> {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                String contenido = post.get("content") != null ? post.get("content").toString() : "Sin contenido";
                String mensajeParaCompartir = "¡Echa un vistazo a esta publicación: " + "{ " +  contenido + " }" + " https://www.p10ibrahim.com";

                shareIntent.putExtra(Intent.EXTRA_TEXT, mensajeParaCompartir);
                Intent chooser = Intent.createChooser(shareIntent, "Compartir publicación");
                v.getContext().startActivity(chooser);
            });


        }

        @Override
        public int getItemCount() {
            return lista == null ? 0 : lista.getDocuments().size();
        }

        public void establecerLista(DocumentList<Map<String, Object>> lista) {
            this.lista = lista;
            notifyDataSetChanged();
        }
    }

    void obtenerPosts() {
        Databases databases = new Databases(client);
        Handler mainHandler = new Handler(Looper.getMainLooper());
        try {
            databases.listDocuments(
                    getString(R.string.APPWRITE_DATABASE_ID),
                    getString(R.string.APPWRITE_POSTS_COLLECTION_ID),
                    new ArrayList<>(),
                    new CoroutineCallback<>((result, error) -> {
                        if (error != null) {
                            Snackbar.make(requireView(), "Error al obtener los posts: " + error.toString(), Snackbar.LENGTH_LONG).show();
                            return;
                        }
                        mainHandler.post(() -> adapter.establecerLista(result));
                    })
            );
        } catch (AppwriteException e) {
            throw new RuntimeException(e);
        }
    }

}
