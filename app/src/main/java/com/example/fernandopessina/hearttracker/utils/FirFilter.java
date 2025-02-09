package com.example.fernandopessina.hearttracker.utils;

/**
 * Created by fernando.pessina on 14/11/2016.
 */

public class FirFilter {


    private static double []filter_taps = new double[]{
            0.03253142195393607,
            0.03734597214082696,
            0.030438667287811035,
            0.00419360365370184,
            -0.025625853299653133,
            -0.038218629342608605,
            -0.02630198086741446,
            -0.004589084072477686,
            0.0021257711560950174,
            -0.017586245109089453,
            -0.04857136937482457,
            -0.061369098796338235,
            -0.041399159649016415,
            -0.007472297714487835,
            0.0014038041049778877,
            -0.036457334455796846,
            -0.09664689820131143,
            -0.11917521224717052,
            -0.05572543459116071,
            0.08395474326743022,
            0.22777103148056047,
            0.2886285859189255,
            0.22777103148056047,
            0.08395474326743022,
            -0.05572543459116071,
            -0.11917521224717052,
            -0.09664689820131143,
            -0.036457334455796846,
            0.0014038041049778877,
            -0.007472297714487835,
            -0.041399159649016415,
            -0.061369098796338235,
            -0.04857136937482457,
            -0.017586245109089453,
            0.0021257711560950174,
            -0.004589084072477686,
            -0.02630198086741446,
            -0.038218629342608605,
            -0.025625853299653133,
            0.00419360365370184,
            0.030438667287811035,
            0.03734597214082696,
            0.03253142195393607
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
