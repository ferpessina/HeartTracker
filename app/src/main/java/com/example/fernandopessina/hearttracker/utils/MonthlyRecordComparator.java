package com.example.fernandopessina.hearttracker.utils;

import com.example.fernandopessina.hearttracker.model.MonthlyRecord;

import java.util.Comparator;

/**
 * Created by fernando.pessina on 10/11/2016.
 */

public class MonthlyRecordComparator implements Comparator<MonthlyRecord> {
    @Override
    public int compare(MonthlyRecord o1, MonthlyRecord o2) {
        String []aux1 = o1.getName().split(" ");
        String []aux2 = o2.getName().split(" ");
        int year1 = Integer.parseInt(aux1[1]);
        int year2 = Integer.parseInt(aux2[1]);
        if(year1!=year2)
            return year2 - year1;
        return ConversionUtil.monthToInt(aux2[0])-ConversionUtil.monthToInt(aux1[0]);
    }
}
