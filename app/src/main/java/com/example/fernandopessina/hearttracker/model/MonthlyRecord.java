package com.example.fernandopessina.hearttracker.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by fernando.pessina on 10/11/2016.
 *
 * Monthly record model
 */

public class MonthlyRecord {
    private String name;
    private int average;
    private List<BpmRecord> entries = new ArrayList<>();

    public void calculateAverage(){
        average = 0;
        for(BpmRecord r : entries){
            average += r.getBpm();
        }
        average /= entries.size();
    }

    public MonthlyRecord(String name){
        this.name = name;
    }

    public List<BpmRecord> getEntries() {
        return entries;
    }

    public void setEntries(List<BpmRecord> entries) {
        this.entries = entries;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAverage() {
        return average;
    }

    public void setAverage(int average) {
        this.average = average;
    }

}
