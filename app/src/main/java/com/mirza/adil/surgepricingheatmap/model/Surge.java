package com.mirza.adil.surgepricingheatmap.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;


public class Surge {

    @SerializedName("geohashes")
    private List<Object> mGeohashes;
    @SerializedName("surge")
    private Double mSurge;

    public List<Object> getGeohashes() {
        return mGeohashes;
    }

    public Double getSurge() {
        return mSurge;
    }

    public static class Builder {

        private List<Object> mGeohashes;
        private Double mSurge;

        public Surge.Builder withGeohashes(List<Object> geohashes) {
            mGeohashes = geohashes;
            return this;
        }

        public Surge.Builder withSurge(Double surge) {
            mSurge = surge;
            return this;
        }

        public Surge build() {
          Surge surge = new Surge();
            surge.mGeohashes = mGeohashes;
            surge.mSurge = mSurge;
            return surge;
        }

    }

}
