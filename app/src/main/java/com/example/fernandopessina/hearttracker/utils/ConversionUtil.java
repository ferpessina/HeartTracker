package com.example.fernandopessina.hearttracker.utils;

import com.example.fernandopessina.hearttracker.model.BpmRecord;
import com.example.fernandopessina.hearttracker.model.MonthlyRecord;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Created by fernando.pessina on 10/11/2016.
 */

public final class ConversionUtil {
    private ConversionUtil(){}

    public static Set<String> toMonthsStringSet(List<MonthlyRecord> s){
        Set<String> ret = new HashSet<>();
        for(MonthlyRecord m : s){
            ret.add(m.getName());
        }
        return ret;
    }

    public static List<MonthlyRecord> toMonthlyRecordList(Set<String> set){
        List<MonthlyRecord> ret = new ArrayList<>();
        for(String s : set){
            ret.add(new MonthlyRecord(s));
        }
        return ret;
    }

    public static String getDateString(){
        String date = "";
        Calendar cal = new GregorianCalendar(Locale.getDefault());
        date+=(String.valueOf(cal.get(Calendar.DAY_OF_MONTH)));
        date+=("/");
        date+=(String.valueOf(cal.get(Calendar.MONTH)+1));
        date+=("/");
        date+=(String.valueOf(cal.get(Calendar.YEAR)));
        return date;
    }

    public static String getMonthString(){
        Calendar cal = new GregorianCalendar(Locale.getDefault());
        String month = "";
        switch(cal.get(Calendar.MONTH)){
            case Calendar.JANUARY:
                month = "JANUARY ";
                break;
            case Calendar.FEBRUARY:
                month = "FEBRUARY ";
                break;
            case Calendar.MARCH:
                month = "MARCH ";
                break;
            case Calendar.APRIL:
                month = "APRIL ";
                break;
            case Calendar.MAY:
                month = "MAY ";
                break;
            case Calendar.JUNE:
                month = "JUNE ";
                break;
            case Calendar.JULY:
                month = "JULY ";
                break;
            case Calendar.AUGUST:
                month = "AUGUST ";
                break;
            case Calendar.SEPTEMBER:
                month = "SEPTEMBER ";
                break;
            case Calendar.OCTOBER:
                month = "OCTOBER ";
                break;
            case Calendar.NOVEMBER:
                month = "NOVEMBER ";
                break;
            case Calendar.DECEMBER:
                month = "DECEMBER ";
                break;
        }
        month+=String.valueOf(cal.get(Calendar.YEAR));
        return month;
    }

    public static int monthToInt(String m){
        switch(m){
            case "January":
                return 1;
            case "February":
                return 2;
            case "March":
                return 3;
            case "April":
                return 4;
            case "May":
                return 5;
            case "June":
                return 6;
            case "July":
                return 7;
            case "August":
                return 8;
            case "September":
                return 9;
            case "October":
                return 10;
            case "November":
                return 11;
            case "December":
                return 12;
        }
        return 0;
    }

    public static List<BpmRecord> toRecordsList(Set<String> set){
        List<BpmRecord> ret = new ArrayList<>();
        for(String s : set){
            String[] parts = s.split(",");
            ret.add(new BpmRecord(Integer.parseInt(parts[0]),parts[1],parts[2]));
        }
        Collections.sort(ret, new Comparator<BpmRecord>() {
            @Override
            public int compare(BpmRecord o1, BpmRecord o2) {
                String [] aux1 = o1.getDate().split("/");
                String [] aux2 = o2.getDate().split("/");
                int a1 = Integer.parseInt(aux1[2]);
                int a2 = Integer.parseInt(aux2[2]);
                if(a1!=a2)
                    return a2-a1;
                a1 = Integer.parseInt(aux1[1]);
                a2 = Integer.parseInt(aux2[1]);
                if(a1!=a2)
                    return a2-a1;
                a1 = Integer.parseInt(aux1[0]);
                a2 = Integer.parseInt(aux2[0]);
                return a2-a1;
            }
        });
        return ret;
    }

    public static Set<String> toRecordsStringSet(List<BpmRecord> records){
        Set<String> ret = new HashSet<>();
        for(int i = 0; i<records.size();i++){
            BpmRecord r = records.get(i);
            ret.add(String.valueOf(r.getBpm())+","+r.getType()+","+r.getDate());
        }
        return ret;
    }
}
