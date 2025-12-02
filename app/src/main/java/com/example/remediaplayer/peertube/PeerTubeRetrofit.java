package com.example.remediaplayer.peertube;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class PeerTubeRetrofit {

    private static final String BASE_URL = "https://peertube2.cpy.re/api/v1/";

    private static Retrofit retrofit;

    public static Retrofit get() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }
}
