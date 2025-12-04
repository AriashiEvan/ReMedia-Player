package com.example.remediaplayer.peertube;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface PeerTubeApi {

    @GET("videos")
    Call<PeerTubeResponse> searchVideos(
            @Query("search") String search,
            @Query("start") int start,
            @Query("count") int count,
            @Query("sort") String sort
    );
    @GET("videos/{id}")
    Call<PeerTubeVideoDetails> getVideoDetails(
            @Path("id") String id
    );
}
