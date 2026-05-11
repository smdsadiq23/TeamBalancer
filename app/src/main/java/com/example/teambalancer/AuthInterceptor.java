package com.example.teambalancer;

import android.content.Context;
import androidx.annotation.NonNull;
import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public final class AuthInterceptor implements Interceptor {

    private final Context appContext;

    public AuthInterceptor(Context appContext) {
        this.appContext = appContext.getApplicationContext();
    }

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request req = chain.request();
        String path = req.url().encodedPath();
        if (path.contains("/api/auth/login") || path.contains("/api/auth/register")) {
            return chain.proceed(req);
        }
        String token = SecureSessionStore.getToken(appContext);
        if (token != null && !token.isEmpty()) {
            req = req.newBuilder().header("Authorization", "Bearer " + token).build();
        }
        return chain.proceed(req);
    }
}
