package com.example.fernandopessina.hearttracker.utils;

/**
 * Created by fernando.pessina on 14/11/2016.
 */

public class LowPassFilter {


    private static double []filter_taps = new double[]{
            0.015245556065628673,
            0.0112094547817341,
            -0.02779201932253918,
            -0.07381608079855045,
            -0.046379199270029384,
            0.0990354822417061,
            0.2893852980127629,
            0.37809179708292356,
            0.2893852980127629,
            0.0990354822417061,
            -0.046379199270029384,
            -0.07381608079855045,
            -0.02779201932253918,
            0.0112094547817341,
            0.015245556065628673
    };
    private static final int TAP_NUM = filter_taps.length;
    private double []history = new double[TAP_NUM];
    private int lastIndex = 0;

    public void put(double input) {
        history[lastIndex++] = input;
        lastIndex%=TAP_NUM;
    }

    public int get() {
        double acc = 0;
        int index = lastIndex;
        for(int i = 0; i < TAP_NUM; ++i) {
            index = index != 0 ? index-1 : TAP_NUM-1;
            acc += history[index] * filter_taps[i];
        };
        return (int) acc;
    }

}
