package com.example.remediaplayer.peertube;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class PeerTubeResponse {

    @SerializedName("total")
    private int total;

    @SerializedName("data")
    private List<PeerTubeVideo> data;

    public int getTotal() {
        return total;
    }

    public List<PeerTubeVideo> getData() {
        return data;
    }
}
