package com.example.p10;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import io.appwrite.Client;
import io.appwrite.coroutines.CoroutineCallback;
import io.appwrite.exceptions.AppwriteException;
import io.appwrite.models.DocumentList;
import io.appwrite.services.Databases;

public class DeletePost {

    public interface DeleteCallback {
        void onComplete(boolean success);
    }

    public interface RestoreCallback {
        void onComplete(boolean success);
    }

    public static void deletePost(Client client, String postId, Context context, DeleteCallback callback) {
        String databaseId = context.getString(R.string.APPWRITE_DATABASE_ID);
        String collectionId = context.getString(R.string.APPWRITE_POSTS_COLLECTION_ID);
        Databases databases = new Databases(client);
        Handler mainHandler = new Handler(Looper.getMainLooper());
        databases.deleteDocument(
                databaseId,
                collectionId,
                postId,
                new CoroutineCallback<>((result, error) -> {
                    if (error != null) {
                        error.printStackTrace();
                        mainHandler.post(() -> callback.onComplete(false));
                        return;
                    }
                    mainHandler.post(() -> callback.onComplete(true));
                })
        );
    }

    public static void restorePost(Client client, Map<String, Object> postData, Context context, RestoreCallback callback) {
        // Crear una copia y eliminar campos de sistema
        Map<String, Object> dataToRestore = new HashMap<>(postData);
        dataToRestore.keySet().removeIf(key -> key.startsWith("$"));

        String databaseId = context.getString(R.string.APPWRITE_DATABASE_ID);
        String collectionId = context.getString(R.string.APPWRITE_POSTS_COLLECTION_ID);
        Databases databases = new Databases(client);
        Handler mainHandler = new Handler(Looper.getMainLooper());
        try {
            databases.createDocument(
                    databaseId,
                    collectionId,
                    "unique()",
                    dataToRestore,
                    new ArrayList<>(), // Permisos opcionales
                    new CoroutineCallback<>((result, error) -> {
                        if (error != null) {
                            error.printStackTrace();
                            mainHandler.post(() -> callback.onComplete(false));
                            return;
                        }
                        mainHandler.post(() -> callback.onComplete(true));
                    })
            );
        } catch (AppwriteException e) {
            e.printStackTrace();
            mainHandler.post(() -> callback.onComplete(false));
        }
    }

}
