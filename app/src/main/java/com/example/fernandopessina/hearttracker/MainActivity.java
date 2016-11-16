package com.example.fernandopessina.hearttracker;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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

import java.security.acl.Group;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    public static final String STORAGE_NAME = "HRHist";
    private Toolbar toolbar;
    private List<MonthlyRecord> months;
    private int filterState = 0;
    private static final int FILTER_ALL = 0;
    private static final int FILTER_REST = 1;
    private static final int FILTER_WARM = 2;
    private static final int FILTER_75 = 3;
    private static final int FILTER_100 = 4;
    private static final int FILTER_AFT_EX = 5;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        loadData();
        loadViews();

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Context context = getApplicationContext();
                Intent intent = new Intent(context, MeasureActivity.class);
                startActivityForResult(intent, 2);//random request code
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        // check if the request code is same as what is passed  here it is 2

        if(resultCode == RESULT_OK) {
            String message = data.getStringExtra("MESSAGE");
            CoordinatorLayout coordinator = (CoordinatorLayout) findViewById(R.id.coordinatorMain);
            Snackbar snackbar = Snackbar.make(coordinator, message, Snackbar.LENGTH_LONG);
            snackbar.show();
        }else{
            String message = getString(R.string.measureCanceled);
            CoordinatorLayout coordinator = (CoordinatorLayout) findViewById(R.id.coordinatorMain);
            Snackbar snackbar = Snackbar.make(coordinator, message, Snackbar.LENGTH_LONG);
            snackbar.show();
        }

    }

    private void loadData(){
        // Restore preferences
        SharedPreferences settings = getSharedPreferences(STORAGE_NAME, 0);
        Set<String> monthsString = settings.getStringSet("months", null);
        if(monthsString != null) {
            months = ConversionUtil.toMonthlyRecordList(monthsString);
            for (MonthlyRecord m : months) {
                List<BpmRecord> entries = ConversionUtil.toRecordsList(settings.getStringSet(m.getName(), new HashSet<String>()));
                m.setEntries(entries);
            }
        }else{
            months = new ArrayList<>();
        }
    }

    @Override
    protected void onResume(){
        super.onResume();
        loadData();
        loadViews();
    }

    private void loadViews(){
        RecyclerView mRecyclerView = (RecyclerView) findViewById(R.id.history);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        List<ExpandableListAdapter.Item> data = new ArrayList<>();

        Collections.sort(months, new MonthlyRecordComparator());
        for(MonthlyRecord m : months){
            ExpandableListAdapter.Item month = new ExpandableListAdapter.Item(ExpandableListAdapter.HEADER, m.getName(), m.getAverage());
            List<BpmRecord> entries = m.getEntries();
            List<BpmRecord> filteredEntries = new ArrayList<>();
            for(BpmRecord r : entries){
                int type = 0;
                if(r.getType().equals(getString(R.string.resting)))
                    type = FILTER_REST;
                if(r.getType().equals(getString(R.string.warming_up)))
                    type = FILTER_WARM;
                if(r.getType().equals(getString(R.string.exercise_75)))
                    type = FILTER_75;
                if(r.getType().equals(getString(R.string.exercise_100)))
                    type = FILTER_100;
                if(r.getType().equals(getString(R.string.after_exercise)))
                    type = FILTER_AFT_EX;
                if(type==filterState || filterState == 0)
                    filteredEntries.add(r);
            }
            if(filteredEntries.size()>0){
                data.add(month);
                for(BpmRecord r : filteredEntries){
                    data.add(new ExpandableListAdapter.Item(ExpandableListAdapter.CHILD, r.getBpm(), r.getType(), r.getDate()));
                }
            }
        }
        mRecyclerView.setAdapter(new ExpandableListAdapter(data));

        List<Integer> values = new ArrayList<>();
        Collections.sort(months, new MonthlyRecordComparator());
        int average = 0;
        int i = 0;
        for(MonthlyRecord m : months){
            m.calculateAverage();
            List<BpmRecord> entries = m.getEntries();
            for(BpmRecord r : entries){
                int type = 0;
                if(r.getType().equals(getString(R.string.resting)))
                    type = FILTER_REST;
                if(r.getType().equals(getString(R.string.warming_up)))
                    type = FILTER_WARM;
                if(r.getType().equals(getString(R.string.exercise_75)))
                    type = FILTER_75;
                if(r.getType().equals(getString(R.string.exercise_100)))
                    type = FILTER_100;
                if(r.getType().equals(getString(R.string.after_exercise)))
                    type = FILTER_AFT_EX;
                if(type==filterState || filterState == 0){
                    values.add(r.getBpm());
                    average+=r.getBpm();
                    i++;
                }
            }
        }
        if(i>0)
            average/=i;
        Integer []bpms = values.toArray(new Integer[0]);
        GraphView graph = (GraphView) findViewById(R.id.graph);
        graph.removeAllSeries();
        DataPoint[] points = new DataPoint[bpms.length];
        DataPoint[] points2 = new DataPoint[bpms.length];
        int max = bpms[0],min = bpms[0];
        for(int ii=0;ii<bpms.length;ii++){
            points[ii] = new DataPoint(ii,bpms[bpms.length-1-ii]);
            if(bpms[ii]>max){
                max = bpms[ii];
            }
            if(bpms[ii]<min){
                min = bpms[ii];
            }
            points2[ii] = new DataPoint(ii,average);
        }
        BarGraphSeries<DataPoint> series = new BarGraphSeries<>(points);
        LineGraphSeries<DataPoint> series2 = new LineGraphSeries<>(points2);
        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setMinX(bpms.length-15);
        graph.getViewport().setMaxX(bpms.length-1);
        graph.getViewport().setMaxY(max+5);
        graph.getViewport().setMinY(min-5);
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
    }

    @Override
    protected void onStop(){
        super.onStop();
    }

    // Menu icons are inflated just as they were with actionbar
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        View v = findViewById(R.id.filterHist);
        switch (item.getItemId()) {
            case R.id.deleteHist:
                // User chose the "Settings" item, show the app settings UI...
                SharedPreferences settings = getSharedPreferences(STORAGE_NAME, 0);
                SharedPreferences.Editor editor = settings.edit();
                editor.clear();
                editor.apply();
                loadData();
                loadViews();
                return true;

            case R.id.filterHist:
                PopupMenu popup = new PopupMenu(this, v);
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        int id = item.getItemId();
                        switch (id){
                            case R.id.menu_all:
                                filterState = FILTER_ALL;
                                break;
                            case R.id.menu_resting:
                                filterState = FILTER_REST;
                                break;
                            case R.id.menu_warming_up:
                                filterState = FILTER_WARM;
                                break;
                            case R.id.menu_75:
                                filterState = FILTER_75;
                                break;
                            case R.id.menu_100:
                                filterState = FILTER_100;
                                break;
                            case R.id.menu_after_exercise:
                                filterState = FILTER_AFT_EX;
                                break;
                        }
                        loadData();
                        loadViews();
                        return true;
                    }
                });
                MenuInflater inflater = popup.getMenuInflater();
                inflater.inflate(R.menu.filter_menu, popup.getMenu());
                popup.show();
                return true;
            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }
}

