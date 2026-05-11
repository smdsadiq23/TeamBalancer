package com.example.teambalancer;

import android.content.Context;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class ApiModule {

    private static volatile TeamBalancerApi api;

    private ApiModule() {}

    public static void init(Context context) {
        if (api != null) {
            return;
        }
        synchronized (ApiModule.class) {
            if (api != null) {
                return;
            }
            Context app = context.getApplicationContext();
            OkHttpClient.Builder http = new OkHttpClient.Builder()
                    .connectTimeout(20, TimeUnit.SECONDS)
                    .readTimeout(45, TimeUnit.SECONDS)
                    .writeTimeout(45, TimeUnit.SECONDS)
                    .addInterceptor(new AuthInterceptor(app));
            if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor log = new HttpLoggingInterceptor();
                log.setLevel(HttpLoggingInterceptor.Level.BODY);
                http.addInterceptor(log);
            }
            Gson gson = new GsonBuilder().serializeNulls().create();
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BuildConfig.API_BASE_URL)
                    .client(http.build())
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();
            api = retrofit.create(TeamBalancerApi.class);
        }
    }

    public static TeamBalancerApi api() {
        TeamBalancerApi a = api;
        if (a == null) {
            throw new IllegalStateException("ApiModule.init was not called");
        }
        return a;
    }
}
