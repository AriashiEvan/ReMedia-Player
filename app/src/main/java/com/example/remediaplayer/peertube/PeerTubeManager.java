package com.example.remediaplayer.peertube;

import android.net.Uri;
import android.util.Log;

import com.example.remediaplayer.VideoItem;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class PeerTubeManager {

    private static final String TAG = "PEERTUBE";
    private static final String FALLBACK_SEARCH = "https://peertube2.cpy.re/api/v1/";

    private final PeerTubeApi searchApi;

    public interface PeerTubeCallback {
        void onResult(List<VideoItem> results, int total);
    }

    public interface ResolveCallback {
        void onResolved(String streamUrl);
        void onError(Throwable t);
    }

    public PeerTubeManager(android.content.Context ctx) {

        HttpLoggingInterceptor logger =
                new HttpLoggingInterceptor(msg -> Log.d("PEERTUBE_JSON", msg));

        logger.setLevel(HttpLoggingInterceptor.Level.BODY);


        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(logger)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(FALLBACK_SEARCH)
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();

        searchApi = retrofit.create(PeerTubeApi.class);
    }

    public void search(String query, int start, int count, PeerTubeCallback cb) {

        Call<PeerTubeResponse> call = searchApi.searchVideos(
                (query == null || query.trim().isEmpty()) ? null : query,
                start,
                count,
                "-publishedAt"
        );

        call.enqueue(new Callback<PeerTubeResponse>() {
            @Override
            public void onResponse(Call<PeerTubeResponse> call,
                                   Response<PeerTubeResponse> r) {

                if (!r.isSuccessful() || r.body() == null) {
                    cb.onResult(new ArrayList<>(), 0);
                    return;
                }

                List<PeerTubeVideo> data = r.body().getData();
                int total = r.body().getTotal();
                List<VideoItem> results = new ArrayList<>();

                for (PeerTubeVideo v : data) {
                    Log.d("PT_JSON", "JSON Item: " + new Gson().toJson(v));

                    String instance = "";
                    String videoUrl = v.getVideoUrl();

                    if (videoUrl != null && videoUrl.contains("/videos/watch/")) {
                        instance = videoUrl.split("/videos/watch/")[0];
                    }


                    String thumb = null;
                    String uuid = v.getUuid();

                    if (uuid != null && !uuid.isEmpty()) {
                        thumb = instance + "/static/previews/" + uuid + ".jpg";
                    }

                    Log.d("PT_DEBUG", "VIDEO: " + v.getName() +
                            "\nvideoUrl: " + v.getVideoUrl() +
                            "\nthumb: " + thumb);

                    results.add(
                            VideoItem.fromOnline(
                                    v.getName(),
                                    v.getVideoUrl(),
                                    v.getDuration() * 1000L,
                                    thumb
                            )
                    );
                }

                cb.onResult(results, total);
            }

            @Override
            public void onFailure(Call<PeerTubeResponse> call, Throwable t) {
                Log.e("PEERTUBE_SEARCH", "Search failed: " + t.getMessage(), t);
                cb.onResult(new ArrayList<>(), 0);
            }
        });
    }

    public void resolveStreamUrl(String watchUrl, ResolveCallback cb) {

        Log.d(TAG, "REQUEST: " + watchUrl);

        try {
            if (!watchUrl.contains("/videos/watch/")) {
                cb.onError(new IllegalArgumentException("Not a PeerTube watch URL"));
                return;
            }

            Uri uri = Uri.parse(watchUrl);
            String instanceBase = uri.getScheme() + "://" + uri.getHost();

            List<String> seg = uri.getPathSegments();
            String id = null;

            for (int i = 0; i < seg.size(); i++) {
                if (seg.get(i).equals("watch") && i + 1 < seg.size()) {
                    id = seg.get(i + 1);
                    break;
                }
            }

            if (id == null) {
                cb.onError(new RuntimeException("Cannot extract ID"));
                return;
            }

            Retrofit dynRetrofit = new Retrofit.Builder()
                    .baseUrl(instanceBase + "/api/v1/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            PeerTubeApi api = dynRetrofit.create(PeerTubeApi.class);

            api.getVideoDetails(id).enqueue(new Callback<PeerTubeVideoDetails>() {
                @Override
                public void onResponse(Call<PeerTubeVideoDetails> call,
                                       Response<PeerTubeVideoDetails> res) {

                    if (!res.isSuccessful() || res.body() == null) {
                        cb.onError(new RuntimeException("Details missing / invalid"));
                        return;
                    }

                    PeerTubeVideoDetails details = res.body();

                    List<PeerTubeVideoDetails.PeerTubeFile> files = details.getFiles();

                    if (files != null && !files.isEmpty()) {

                        PeerTubeVideoDetails.PeerTubeFile best = null;
                        long bestScore = Long.MIN_VALUE;

                        for (PeerTubeVideoDetails.PeerTubeFile f : files) {

                            String u = f.getAnyUrl();
                            if (u == null) continue;

                            long score = 0;
                            String low = u.toLowerCase(Locale.ROOT);

                            if (low.endsWith(".mp4")) score += 50;
                            if (low.endsWith(".webm")) score += 25;

                            long br = f.getBitrate();
                            if (br == 0) br = f.getSize();
                            if (br > 200_000 && br < 3_000_000) score += 20;

                            if (score > bestScore) {
                                bestScore = score;
                                best = f;
                            }
                        }

                        if (best != null) {
                            String abs = makeAbsolute(instanceBase, best.getAnyUrl());
                            cb.onResolved(abs);
                            return;
                        }
                    }


                    List<PeerTubeStreaming> streams = details.getStreamingPlaylists();

                    if (streams != null && !streams.isEmpty()) {
                        for (PeerTubeStreaming s : streams) {
                            String hls = s.getPlaylistUrl();
                            if (hls == null) hls = s.getUrl();

                            if (hls != null && hls.contains(".m3u8")) {
                                cb.onResolved(makeAbsolute(instanceBase, hls));
                                return;
                            }
                        }

                        PeerTubeStreaming s0 = streams.get(0);
                        String first = (s0.getPlaylistUrl() != null) ?
                                s0.getPlaylistUrl() : s0.getUrl();

                        if (first != null) {
                            cb.onResolved(makeAbsolute(instanceBase, first));
                            return;
                        }
                    }

                    cb.onResolved(watchUrl + "/download");
                }

                @Override
                public void onFailure(Call<PeerTubeVideoDetails> call, Throwable t) {
                    cb.onError(t);
                }
            });

        } catch (Exception e) {
            cb.onError(e);
        }
    }

    private String makeAbsolute(String instance, String url) {

        if (url == null) return null;

        if (url.startsWith("http://") || url.startsWith("https://"))
            return url;

        if (url.startsWith("/"))
            return instance + url;

        return instance + "/" + url;
    }

}
