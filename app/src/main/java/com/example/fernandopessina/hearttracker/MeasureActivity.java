package com.example.fernandopessina.hearttracker;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.example.fernandopessina.hearttracker.model.BpmRecord;
import com.example.fernandopessina.hearttracker.utils.ConversionUtil;
import com.example.fernandopessina.hearttracker.utils.LowPassFilter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

public class MeasureActivity extends AppCompatActivity implements CvCameraViewListener2 {
    private static final String  TAG                 = "OCVSample::Activity";

    private JavaCameraView mOpenCvCameraView;

    public static final String STORAGE_NAME = "HRHist";

    Scalar avg;

    private double graphLastXValue = 5d;
    private LineGraphSeries<DataPoint> mSeries;
    private final int GRAPH_SIZE = 80;

    private Mat mIntermediateMat;
    private Mat rgba;
    private Mat val;
    private LowPassFilter filter = new LowPassFilter();
    private int histIndex = 0;
    private static final int HIST_SIZE = 40;
    private int [] hist = new int [HIST_SIZE];
    private long [] periods = new long[12];
    private int pIndex = 0;
    private long lastPeak = 0;

    private long currentPeakTime = 0;
    private int currentPeakVal = 0;
    private GraphView graph;
    private boolean flashOn = false;
    private int thresh;
    private int averageValueFiltered;

    private BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public MeasureActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_measure);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbarMeasure);
        toolbar.setTitle(getString(R.string.heart_rate_measure));
        setSupportActionBar(toolbar);
        getSupportActionBar().setHomeButtonEnabled(true);


        graph = (GraphView) findViewById(R.id.graphBpm);
        mSeries = new LineGraphSeries<>(generateData());
        mSeries.setColor(ContextCompat.getColor(getApplicationContext(),R.color.colorPrimary));
        graph.addSeries(mSeries);

        
        graph.getViewport().setMinX(0);
        graph.getViewport().setMaxX(GRAPH_SIZE);
        graph.getViewport().setXAxisBoundsManual(true);
        graph.setBackgroundColor(ContextCompat.getColor(getApplicationContext(),R.color.darkBackground));
        graph.getGridLabelRenderer().setVerticalLabelsVisible(false);
        graph.getGridLabelRenderer().setHorizontalLabelsVisible(false);
        graph.getGridLabelRenderer().setGridColor(ContextCompat.getColor(getApplicationContext(), R.color.darkBackground));

        mOpenCvCameraView = (JavaCameraView) findViewById(R.id.image_manipulations_activity_surface_view);
        mOpenCvCameraView.setVisibility(CameraBridgeViewBase.VISIBLE);
        mOpenCvCameraView.setAlpha(0);
        mOpenCvCameraView.setMaxFrameSize(640,480);
        mOpenCvCameraView.setCvCameraViewListener(this);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
        rgba = new Mat(height, width, CvType.CV_8UC4);
        mIntermediateMat = new Mat(height, width, CvType.CV_8UC4);
        val = new Mat(height, width, CvType.CV_8UC1);
    }

    public void onCameraViewStopped() {
        // Explicitly deallocate Mats
        if (mIntermediateMat != null)
            mIntermediateMat.release();

        mIntermediateMat = null;
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        rgba = inputFrame.rgba();

        Imgproc.cvtColor(rgba,rgba,Imgproc.COLOR_BGR2HSV);

        List<Mat> planes = new ArrayList<>();

        Core.split(rgba,planes);

        val = planes.get(2);

        Imgproc.equalizeHist(val, val);

        avg = Core.mean(val);

        final int avgVal = (int)avg.val[0]*10;
        filter.put(avgVal);

        averageValueFiltered = filter.get();

        histIndex++;
        histIndex %=HIST_SIZE;
        hist[histIndex] = averageValueFiltered;

        extractBpm();

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                graphLastXValue += 1d;
                mSeries.appendData(new DataPoint(graphLastXValue, averageValueFiltered), true, GRAPH_SIZE);
                graph.getViewport().setMaxY(thresh+30);
                graph.getViewport().setMinY(thresh-70);
            }
        });

        for(Mat m : planes)
            m.release();
        return rgba;
    }

    private void extractBpm(){
        thresh=0;
        for(int x : hist)
            thresh+=x;
        thresh/=HIST_SIZE;
        thresh+=15;

        int auxPointer = histIndex-2;
        if(auxPointer<0)
            auxPointer+=HIST_SIZE;

        if(hist[histIndex]>thresh) {
            if(hist[histIndex]>currentPeakVal){
                currentPeakTime = System.currentTimeMillis();
                currentPeakVal = hist[histIndex];
            }
        }else if(hist[histIndex]<thresh && hist[auxPointer]>=thresh && currentPeakTime!=0) {
            long deltaT = currentPeakTime - lastPeak;
            if(lastPeak != 0){
                if(deltaT > 400 && deltaT < 1500) {
                    periods[pIndex] = deltaT;
                    pIndex++;
                    pIndex %= periods.length;

                    long perAvg=0;
                    long perAvgFiltered = 0;
                    for(long x : periods){
                        perAvg+=x;
                    }
                    perAvg/=periods.length;
                    int prog = 0;
                    for(long x : periods){
                        if(x-perAvg < 50 && perAvg-x < 50) {
                            perAvgFiltered+=x;
                            prog++;
                        }
                    }
                    if(prog>0) {
                        perAvgFiltered /= prog;
                        prog = 0;
                        for (long x : periods) {
                            if (x - perAvgFiltered < 20 && perAvgFiltered - x < 20) {
                                prog++;
                            }
                        }
                        if (prog > 3) {
                            float bpm = 1000f / perAvg;
                            bpm *= 60;
                            if(bpm < 600 && bpm > 20)
                                saveMeasurement(bpm);
                        }
                    }
                }
            }
            lastPeak = currentPeakTime;
        }else {
            currentPeakVal=0;
//            calcPeak = false;
        }
    }

    private void saveMeasurement(float bpm){
        SharedPreferences settings = getSharedPreferences(STORAGE_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();

        Set<String> monthsString = settings.getStringSet("months", new HashSet<String>());

        String month = ConversionUtil.getMonthString();

        String date = ConversionUtil.getDateString();

        if(!monthsString.contains(month)){
            monthsString.add(month);
            editor.putStringSet("months",monthsString);
            editor.apply();
        }
        List<BpmRecord> entries = ConversionUtil.toRecordsList(settings.getStringSet(month, new HashSet<String>()));
        RadioGroup rGroup = (RadioGroup) findViewById(R.id.typesGroup);
        RadioButton checked = (RadioButton) findViewById(rGroup.getCheckedRadioButtonId());
        String state = checked.getText().toString();
        entries.add(new BpmRecord((int)bpm,state,date));
        Set<String> entriesSet = ConversionUtil.toRecordsStringSet(entries);
        editor.putStringSet(month,entriesSet);
        String message = getString(R.string.measureResp1)+" "+String.valueOf((int)bpm)+" "+getString(R.string.measureResp2);
        editor.apply();
        Intent intent=new Intent();
        intent.putExtra("MESSAGE",message);
        setResult(RESULT_OK,intent);
        finish();//finishing activity
    }

    private DataPoint[] generateData() {
        int count = GRAPH_SIZE;
        DataPoint[] values = new DataPoint[count];
        for (int i=0; i<count; i++) {
            double f = 25;
            DataPoint v = new DataPoint(i, f);
            values[i] = v;
            graphLastXValue = i;
        }
        return values;
    }

    // Menu icons are inflated just as they were with actionbar
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_measure, menu);
        if(flashOn){
            menu.findItem(R.id.flashToggle).setIcon(R.drawable.flash_off);
        }else{
            menu.findItem(R.id.flashToggle).setIcon(R.drawable.flash_on);
        }
        return true;
    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.flashToggle:
                if (flashOn) {
                    flashOn = false;
                    mOpenCvCameraView.setFlash(false);
                    item.setIcon(R.drawable.flash_on);
                    return true;
                } else {
                    flashOn = true;
                    mOpenCvCameraView.setFlash(true);
                    item.setIcon(R.drawable.flash_off);
                    return true;
                }
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}