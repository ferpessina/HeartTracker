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
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.example.fernandopessina.hearttracker.model.BpmRecord;
import com.example.fernandopessina.hearttracker.utils.ConversionUtil;
import com.example.fernandopessina.hearttracker.utils.FftWindows;
import com.example.fernandopessina.hearttracker.utils.GeneralFFT;
import com.example.fernandopessina.hearttracker.utils.FirFilter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.BarGraphSeries;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

public class MeasureActivity extends AppCompatActivity implements CvCameraViewListener2 {
    private static final String  TAG                 = "OCVSample::Activity";

    private JavaCameraView mOpenCvCameraView;

    public static final String STORAGE_NAME = "HRHist";
    private static final int FFT_INTERVAL = 20;
    Scalar avg;

    private GraphView graph;
    private GraphView graphFft;
    private double graphLastXValue = 5d;
    private LineGraphSeries<DataPoint> mSeries;
    private BarGraphSeries<DataPoint> mSeriesFFT;
    private final int GRAPH_SIZE = 80;

    private Mat mIntermediateMat;
    private Mat rgba;
    private Mat val;
    private FirFilter filter = new FirFilter();


    private int histIndex = 0;
    private static final int HIST_SIZE = 256;
    private double[] hist = new double[HIST_SIZE];
    private double[] window = FftWindows.square(HIST_SIZE);
    private double [] ssSpectrum = new double[HIST_SIZE/2];
    private int maxBin;
    private boolean initPassed = false;

    private long lastTime = 0;

    private boolean flashOn = false;
    private double bpm;

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

        graphFft = (GraphView) findViewById(R.id.graphFFT);
        mSeriesFFT = new BarGraphSeries<>(updateData());
        mSeriesFFT.setSpacing(50);
        graphFft.addSeries(mSeriesFFT);
        
        graph.getViewport().setMinX(0);
        graph.getViewport().setMaxX(GRAPH_SIZE);
        graph.getViewport().setXAxisBoundsManual(true);
        graph.setBackgroundColor(ContextCompat.getColor(getApplicationContext(),R.color.darkBackground));
        graph.getGridLabelRenderer().setVerticalLabelsVisible(false);
        graph.getGridLabelRenderer().setHorizontalLabelsVisible(false);
        graph.getGridLabelRenderer().setGridColor(ContextCompat.getColor(getApplicationContext(), R.color.darkBackground));

        graphFft.getViewport().setMinX(0);
        graphFft.getViewport().setMaxX(HIST_SIZE/6);
        graphFft.getViewport().setXAxisBoundsManual(true);
        graphFft.setBackgroundColor(ContextCompat.getColor(getApplicationContext(),R.color.darkBackground));
        graphFft.getGridLabelRenderer().setVerticalLabelsVisible(false);
        graphFft.getGridLabelRenderer().setHorizontalLabelsVisible(false);
        graphFft.getGridLabelRenderer().setGridColor(ContextCompat.getColor(getApplicationContext(), R.color.darkBackground));

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

        val = planes.get(2); // 1=Green BGR 2=Value HSV

        Imgproc.equalizeHist(val, val);

        avg = Core.mean(val);

        filter.put(avg.val[0]*10);

        histIndex++;
        if(histIndex==HIST_SIZE)
            initPassed = true;
        histIndex %=HIST_SIZE;
        hist[histIndex] = filter.get();
        //hist[histIndex] = avg.val[0]*10;

//        for(int i =0;i<hist.length;i++){
//            hist[i] = Math.sin(0.1d*Math.PI*i);
//        }

        if(histIndex%FFT_INTERVAL == 0 && initPassed) {
            double[]real = new double[HIST_SIZE];

            for (int i=0,p=histIndex+1;i<HIST_SIZE;i++,p++){
                p%=HIST_SIZE;
                real[i]=hist[p]*window[i];
            }

            double []im = new double[HIST_SIZE];

            GeneralFFT.transform(real, im);

            for (int i = 0; i < HIST_SIZE/2; i++) {
                ssSpectrum[i] = Math.sqrt(((real[i+1] * real[i+1]) + (im[i+1] * im[i+1]))/HIST_SIZE);
            }
        }

        if(histIndex%FFT_INTERVAL == 0 && initPassed) {
            calculateBpm();
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                graphLastXValue += 1d;
                mSeries.appendData(new DataPoint(graphLastXValue,hist[histIndex]),true,GRAPH_SIZE);
                if(histIndex%FFT_INTERVAL == 0 && initPassed) {
                    TextView bpmView = (TextView) findViewById(R.id.bpmView);
                    bpmView.setText("Bpm: "+String.valueOf((int)bpm));
                    mSeriesFFT.resetData(updateData());
                }
                //.getViewport().setMaxY(thresh+30);
                //graph.getViewport().setMinY(thresh-70);
            }
        });

        for(Mat m : planes)
            m.release();
        return rgba;
    }

    private void calculateBpm(){
        long currentTime = System.currentTimeMillis();
        if(lastTime != 0){
            double max = 0;
            maxBin = 0;
            for(int i=13;i<85;i++){
                if(ssSpectrum[i]>max){
                    maxBin = i;
                    max = ssSpectrum[i];
                }
            }
            double binsAvg = (ssSpectrum[maxBin-1]+ssSpectrum[maxBin]+ssSpectrum[maxBin+1])/3;
            double peakLocation = (maxBin*ssSpectrum[maxBin]+(maxBin-1)*ssSpectrum[maxBin-1]+(maxBin+1)*ssSpectrum[maxBin+1])/(3*binsAvg);
            peakLocation+=0.5d;
            long deltaT = (currentTime-lastTime)/FFT_INTERVAL;
            lastTime = currentTime;
            bpm = (peakLocation*60) * 1000/(deltaT*HIST_SIZE);
        }else{
            lastTime = currentTime;
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
    private DataPoint[] updateData(){
        DataPoint[] ret = new DataPoint[HIST_SIZE/6];
        for (int i=0;i<HIST_SIZE/6;i++){
            DataPoint v = new DataPoint(i, ssSpectrum[i]);
            ret[i] = v;
        }
        return ret;
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