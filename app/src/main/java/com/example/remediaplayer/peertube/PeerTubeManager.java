package com.example.remediaplayer.peertube;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.example.remediaplayer.VideoItem;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class PeerTubeManager {

    private static final String BASE_URL = "https://peertube2.cpy.re/api/v1/";
    private final PeerTubeApi api;
    private final String baseHost;

    public interface PeerTubeCallback {
        void onResult(List<VideoItem> results);
    }

    public PeerTubeManager(Context ctx) {
        HttpLoggingInterceptor logger = new HttpLoggingInterceptor(message -> {

            Log.d("PEERTUBE_JSON", message);
        });
        logger.setLevel(HttpLoggingInterceptor.Level.BASIC);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(logger)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();

        api = retrofit.create(PeerTubeApi.class);


        String tmp = BASE_URL;
        if (tmp.endsWith("api/v1/")) {
            baseHost = tmp.replace("api/v1/", "");
        } else {
            baseHost = tmp;
        }
    }

    public void search(String query, PeerTubeCallback callback) {

        if (query != null && query.trim().isEmpty())
            query = null;

        Call<PeerTubeResponse> call = api.searchVideos(
                query,
                0,
                20,
                "-publishedAt"
        );

        call.enqueue(new Callback<PeerTubeResponse>() {
            @Override
            public void onResponse(Call<PeerTubeResponse> call, Response<PeerTubeResponse> response) {

                if (!response.isSuccessful() || response.body() == null) {
                    callback.onResult(new ArrayList<>());
                    return;
                }

                List<PeerTubeVideo> data = response.body().getData();
                if (data == null || data.isEmpty()) {
                    callback.onResult(new ArrayList<>());
                    return;
                }


                List<VideoItem> results = new ArrayList<>();


                AtomicInteger pending = new AtomicInteger(data.size());

                for (PeerTubeVideo v : data) {

                    String fullThumb;
                    if (v.getThumbnailPath() != null && !v.getThumbnailPath().trim().isEmpty()) {

                        String thumb = v.getThumbnailPath();
                        if (thumb.startsWith("/")) fullThumb = baseHost + thumb;
                        else fullThumb = baseHost + "/" + thumb;
                    } else {
                        fullThumb = null;
                    }

                    String uuid = parseUuidFromWatchUrl(v.getVideoUrl());

                    if (uuid == null) {

                        VideoItem item = VideoItem.fromOnline(
                                v.getName(),
                                v.getVideoUrl(),
                                v.getDuration() * 1000L,
                                fullThumb
                        );
                        results.add(item);
                        if (pending.decrementAndGet() == 0) {
                            callback.onResult(results);
                        }
                        continue;
                    }

                    Call<VideoDetailsResponse> detailsCall = api.getVideoDetails(uuid);
                    detailsCall.enqueue(new Callback<VideoDetailsResponse>() {
                        @Override
                        public void onResponse(Call<VideoDetailsResponse> call, Response<VideoDetailsResponse> response) {
                            try {
                                String fileUrl = null;

                                if (response.isSuccessful() && response.body() != null) {
                                    VideoDetailsResponse body = response.body();

                                    // streamingPlaylists -> first playlist -> files -> first file -> fileUrl
                                    if (body.streamingPlaylists != null && !body.streamingPlaylists.isEmpty()) {
                                        for (VideoDetailsResponse.StreamingPlaylist pl : body.streamingPlaylists) {
                                            if (pl.files != null && !pl.files.isEmpty()) {
                                                // prefer the first file that has a fileUrl
                                                for (VideoDetailsResponse.FileEntry fe : pl.files) {
                                                    if (fe != null && fe.fileUrl != null && !fe.fileUrl.trim().isEmpty()) {
                                                        fileUrl = fe.fileUrl;
                                                        break;
                                                    }
                                                }
                                            }
                                            if (fileUrl != null) break;
                                        }
                                    }

                                    if (fileUrl == null && body.files != null && !body.files.isEmpty()) {
                                        for (VideoDetailsResponse.FileEntry fe : body.files) {
                                            if (fe != null && fe.fileUrl != null && !fe.fileUrl.trim().isEmpty()) {
                                                fileUrl = fe.fileUrl;
                                                break;
                                            }
                                        }
                                    }
                                }

                                String playable = (fileUrl != null) ? fileUrl : v.getVideoUrl();

                                VideoItem item = VideoItem.fromOnline(
                                        v.getName(),
                                        playable,
                                        v.getDuration() * 1000L,
                                        fullThumb
                                );

                                results.add(item);
                            } catch (Exception ex) {
                                ex.printStackTrace();

                                VideoItem item = VideoItem.fromOnline(
                                        v.getName(),
                                        v.getVideoUrl(),
                                        v.getDuration() * 1000L,
                                        fullThumb
                                );
                                results.add(item);
                            } finally {
                                if (pending.decrementAndGet() == 0) {
                                    callback.onResult(results);
                                }
                            }
                        }

                        @Override
                        public void onFailure(Call<VideoDetailsResponse> call, Throwable t) {
                            t.printStackTrace();

                            VideoItem item = VideoItem.fromOnline(
                                    v.getName(),
                                    v.getVideoUrl(),
                                    v.getDuration() * 1000L,
                                    fullThumb
                            );
                            results.add(item);
                            if (pending.decrementAndGet() == 0) {
                                callback.onResult(results);
                            }
                        }
                    });
                }
            }

            @Override
            public void onFailure(Call<PeerTubeResponse> call, Throwable t) {
                Log.e("PeerTube", "Search failure: " + t.getMessage());
                callback.onResult(new ArrayList<>());
            }
        });
    }


    private String parseUuidFromWatchUrl(String url) {
        if (url == null) return null;
        try {
            Uri u = Uri.parse(url);
            List<String> segments = u.getPathSegments();
            if (segments == null || segments.isEmpty()) return null;

            for (int i = 0; i < segments.size(); i++) {
                String s = segments.get(i);
                if ("watch".equalsIgnoreCase(s) && i + 1 < segments.size()) {
                    return segments.get(i + 1);
                }
            }


            String last = segments.get(segments.size() - 1);
            if (last != null && last.length() > 10) return last;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static class VideoDetailsResponse {

        @com.google.gson.annotations.SerializedName("streamingPlaylists")
        public List<StreamingPlaylist> streamingPlaylists;

        @com.google.gson.annotations.SerializedName("files")
        public List<FileEntry> files;

        public static class StreamingPlaylist {
            @com.google.gson.annotations.SerializedName("files")
            public List<FileEntry> files;
        }

        public static class FileEntry {

            @com.google.gson.annotations.SerializedName("fileUrl")
            public String fileUrl;


            @com.google.gson.annotations.SerializedName("url")
            public String url;

            @com.google.gson.annotations.SerializedName("downloadUrl")
            public String downloadUrl;
        }
    }
}
