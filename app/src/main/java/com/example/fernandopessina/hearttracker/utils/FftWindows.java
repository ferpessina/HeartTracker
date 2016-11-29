package com.example.fernandopessina.hearttracker.utils;

/**
 * Created by fernando.pessina on 21/11/2016.
 */

public class FftWindows {
    public static double[] triangular(int size){
        double []window = new double[size];
        for(int i=0;i<size;i++){
            window[i] = 1- Math.abs((i-((size-1)/2))/(size/2));
        }
        return window;
    }

    public static double[] square(int size){
        double []window = new double[size];
        for(int i=0;i<size;i++){
            window[i] = 1;
        }
        return window;
    }
}
