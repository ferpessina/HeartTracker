package com.example.fernandopessina.hearttracker;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.example.fernandopessina.hearttracker.graphs.ExpandableListAdapter;
import com.example.fernandopessina.hearttracker.model.BpmRecord;
import com.example.fernandopessina.hearttracker.model.MonthlyRecord;
import com.example.fernandopessina.hearttracker.utils.ConversionUtil;
import com.example.fernandopessina.hearttracker.utils.MonthlyRecordComparator;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.BarGraphSeries;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    public static final String STORAGE_NAME = "HRHist";
    private Toolbar toolbar;
    private List<MonthlyRecord> months;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Restore preferences
        SharedPreferences settings = getSharedPreferences(STORAGE_NAME, 0);
        Set<String> monthsString = settings.getStringSet("months", null);
        if(monthsString == null){
            months = fillHistWithGarbage();
        }else {
            months = ConversionUtil.toMonthlyRecordList(monthsString);
            for (MonthlyRecord m : months) {
                List<BpmRecord> entries = ConversionUtil.toRecordsList(settings.getStringSet(m.getName(), new HashSet<String>()));
                m.setEntries(entries);
            }
        }

        loadViews();

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Context context = getApplicationContext();
                Intent intent = new Intent(context, MeasureActivity.class);
                startActivity(intent);
//                if(months.size() == 0){
//                    months = fillHistWithGarbage();
//                }else{
//                    months = new ArrayList<>();
//                    SharedPreferences settings = getSharedPreferences(STORAGE_NAME, 0);
//                    SharedPreferences.Editor editor = settings.edit();
//                    editor.clear();
//                    editor.apply();
//                }
//                loadViews();
            }
        });
    }

    private void loadViews(){
        List<Integer> values = new ArrayList<>();
        Collections.sort(months, new MonthlyRecordComparator());
        int average = 0;
        int i = 0;
        for(MonthlyRecord m : months){
            m.calculateAverage();
            List<BpmRecord> entries = m.getEntries();
            for(BpmRecord r : entries){
                values.add(r.getBpm());
                average+=r.getBpm();
                i++;
            }
        }
        if(i>0)
            average/=i;
        Integer []bpms = values.toArray(new Integer[0]);
        GraphView graph = (GraphView) findViewById(R.id.graph);
        graph.removeAllSeries();
        DataPoint[] points = new DataPoint[bpms.length];
        DataPoint[] points2 = new DataPoint[bpms.length];
        for(int ii=0;ii<bpms.length;ii++){
            points[ii] = new DataPoint(ii,bpms[bpms.length-1-ii]);
            points2[ii] = new DataPoint(ii,average);
        }
        BarGraphSeries<DataPoint> series = new BarGraphSeries<>(points);
        LineGraphSeries<DataPoint> series2 = new LineGraphSeries<>(points2);
        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(bpms.length-15);
        graph.getViewport().setMaxX(bpms.length-1);
        // enable scaling and scrolling
        graph.getViewport().setScrollable(true); // enables horizontal scrolling
        graph.addSeries(series);
        graph.addSeries(series2);
        final Context context = getApplicationContext();
        graph.setBackgroundColor(ContextCompat.getColor(context, R.color.darkBackground));
        series.setColor(ContextCompat.getColor(context, R.color.colorPrimary));
        series.setSpacing(75);
        series2.setColor(ContextCompat.getColor(context, R.color.white));
        series2.setTitle("avg");
        graph.getGridLabelRenderer().setVerticalLabelsVisible(false);
        graph.getGridLabelRenderer().setHorizontalLabelsVisible(false);
        graph.getGridLabelRenderer().setGridColor(ContextCompat.getColor(context, R.color.darkBackground));
        // draw values on top
        series.setDrawValuesOnTop(true);
        series.setValuesOnTopColor(ContextCompat.getColor(context, R.color.white));

        RecyclerView mRecyclerView = (RecyclerView) findViewById(R.id.history);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        List<ExpandableListAdapter.Item> data = new ArrayList<>();

        Collections.sort(months, new MonthlyRecordComparator());

        for(MonthlyRecord m : months){
            ExpandableListAdapter.Item month = new ExpandableListAdapter.Item(ExpandableListAdapter.HEADER, m.getName(), m.getAverage());
            month.invisibleChildren = new ArrayList<>();
            List<BpmRecord> entries = m.getEntries();
            for(BpmRecord r : entries){
                month.invisibleChildren.add(new ExpandableListAdapter.Item(ExpandableListAdapter.CHILD, r.getBpm(), r.getType(), r.getDate()));
            }
            data.add(month);
        }

        mRecyclerView.setAdapter(new ExpandableListAdapter(data));
    }

    @Override
    protected void onStop(){
        super.onStop();

        SharedPreferences settings = getSharedPreferences(STORAGE_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();

        editor.putStringSet("months",ConversionUtil.toMonthsStringSet(months));
        for(MonthlyRecord m : months){
            List<BpmRecord> entries = m.getEntries();
            editor.putStringSet(m.getName(),ConversionUtil.toRecordsStringSet(entries));
        }

        editor.apply();
    }

    private List<MonthlyRecord> fillHistWithGarbage(){

        List<MonthlyRecord> monthsSet = new ArrayList<>();
        MonthlyRecord oct = new MonthlyRecord("October 2016");
        List<BpmRecord> entries = new ArrayList<>();
        entries.add(new BpmRecord(78, "Rest", "25/10/16"));
        entries.add(new BpmRecord(78, "Rest", "25/10/16"));
        entries.add(new BpmRecord(78, "Rest", "22/10/16"));
        entries.add(new BpmRecord(86, "Rest", "20/10/16"));
        entries.add(new BpmRecord(78, "Rest", "15/10/16"));
        entries.add(new BpmRecord(78, "Rest", "12/10/16"));
        oct.setEntries(entries);
        monthsSet.add(oct);
        MonthlyRecord nov = new MonthlyRecord("November 2016");
        entries = new ArrayList<>();
        entries.add(new BpmRecord(75, "Rest", "30/11/16"));
        entries.add(new BpmRecord(80, "Rest", "28/11/16"));
        entries.add(new BpmRecord(84, "Rest", "27/11/16"));
        entries.add(new BpmRecord(75, "Rest", "26/11/16"));
        entries.add(new BpmRecord(72, "Rest", "25/11/16"));
        entries.add(new BpmRecord(82, "Rest", "22/11/16"));
        entries.add(new BpmRecord(75, "Rest", "19/11/16"));
        entries.add(new BpmRecord(69, "Rest", "18/11/16"));
        entries.add(new BpmRecord(77, "Rest", "17/11/16"));
        entries.add(new BpmRecord(75, "Rest", "16/11/16"));
        entries.add(new BpmRecord(72, "Rest", "14/11/16"));
        entries.add(new BpmRecord(82, "Rest", "12/11/16"));
        entries.add(new BpmRecord(75, "Rest", "11/11/16"));
        entries.add(new BpmRecord(80, "Rest", "9/11/16"));
        entries.add(new BpmRecord(77, "Rest", "8/11/16"));
        entries.add(new BpmRecord(75, "Rest", "7/11/16"));
        entries.add(new BpmRecord(72, "Rest", "5/11/16"));
        entries.add(new BpmRecord(82, "Rest", "2/11/16"));
        nov.setEntries(entries);
        monthsSet.add(nov);
        MonthlyRecord dec = new MonthlyRecord("December 2016");
        entries = new ArrayList<>();
        entries.add(new BpmRecord(75, "Rest", "12/12/16"));
        entries.add(new BpmRecord(80, "Rest", "11/12/16"));
        entries.add(new BpmRecord(77, "Rest", "9/12/16"));
        entries.add(new BpmRecord(75, "Rest", "7/12/16"));
        entries.add(new BpmRecord(72, "Rest", "5/12/16"));
        entries.add(new BpmRecord(82, "Rest", "2/12/16"));
        dec.setEntries(entries);
        monthsSet.add(dec);

        return monthsSet;
    }
}

