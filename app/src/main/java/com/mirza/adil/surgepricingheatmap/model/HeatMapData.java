package com.mirza.adil.surgepricingheatmap.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;


public class HeatMapData {

    @SerializedName("size")
    private Double mSize;
    @SerializedName("surges")
    private List<Surge> mSurges;

    public Double getSize() {
        return mSize;
    }

    public List<Surge> getSurges() {
        return mSurges;
    }

    public static class Builder {

        private Double mSize;
        private List<Surge> mSurges;

        public HeatMapData.Builder withSize(Double size) {
            mSize = size;
            return this;
        }

        public HeatMapData.Builder withSurges(List<Surge> surges) {
            mSurges = surges;
            return this;
        }

        public HeatMapData build() {
            HeatMapData heatMapData = new HeatMapData();
            heatMapData.mSize = mSize;
            heatMapData.mSurges = mSurges;
            return heatMapData;
        }

    }

}
