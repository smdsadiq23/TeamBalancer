package com.example.teambalancer;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.Nullable;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;
import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * Stores JWT and user metadata in encrypted prefs (not accessible to other apps without root).
 */
public final class SecureSessionStore {

    private static final String PREFS = "TeamBalancerSecureAuth";
    private static final String K_TOKEN = "jwt";
    private static final String K_USER_ID = "user_id";
    private static final String K_USERNAME = "username";

    private SecureSessionStore() {}

    private static SharedPreferences prefs(Context context) throws GeneralSecurityException, IOException {
        Context app = context.getApplicationContext();
        MasterKey masterKey = new MasterKey.Builder(app).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build();
        return EncryptedSharedPreferences.create(
                app,
                PREFS,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
    }

    public static void save(Context context, String token, int userId, String username) {
        try {
            prefs(context)
                    .edit()
                    .putString(K_TOKEN, token)
                    .putInt(K_USER_ID, userId)
                    .putString(K_USERNAME, username)
                    .apply();
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void clear(Context context) {
        try {
            prefs(context).edit().clear().apply();
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean hasToken(Context context) {
        try {
            String t = prefs(context).getString(K_TOKEN, null);
            return t != null && !t.isEmpty();
        } catch (GeneralSecurityException | IOException e) {
            return false;
        }
    }

    @Nullable
    public static String getToken(Context context) {
        try {
            return prefs(context).getString(K_TOKEN, null);
        } catch (GeneralSecurityException | IOException e) {
            return null;
        }
    }
}
