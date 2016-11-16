package com.example.fernandopessina.hearttracker.model;

/**
 * Created by fernando.pessina on 10/11/2016.
 * Model for BPM records
 */

public class BpmRecord {
    private int bpm;
    private String type;
    private String date;

    public BpmRecord(int bpm, String type, String date) {
        this.bpm = bpm;
        this.type = type;
        this.date = date;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public int getBpm() {
        return bpm;
    }

    public void setBpm(int bpm) {
        this.bpm = bpm;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
