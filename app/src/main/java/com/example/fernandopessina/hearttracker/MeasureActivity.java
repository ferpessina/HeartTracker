package com.example.fernandopessina.hearttracker;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
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
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.example.fernandopessina.hearttracker.model.BpmRecord;
import com.example.fernandopessina.hearttracker.utils.ConversionUtil;
import com.example.fernandopessina.hearttracker.utils.LowPassFilter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.BarGraphSeries;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

public class MeasureActivity extends AppCompatActivity implements CvCameraViewListener2 {
    private static final String  TAG                 = "OCVSample::Activity";

    private CameraBridgeViewBase mOpenCvCameraView;

    public static final String STORAGE_NAME = "HRHist";

    Scalar avg;

    private double graphLastXValue = 5d;
    private LineGraphSeries<DataPoint> mSeries;
    //private LineGraphSeries<DataPoint> mSeriesAvg;
    //private BarGraphSeries<DataPoint> maxSeries;

    private final int GRAPH_SIZE = 80;

    private Mat mIntermediateMat;
    private Mat rgba;
    private Mat val;
    //private long last = 0;
    //private int []deltas = new int[50];
    //private int deltasIndex = 0;
    private LowPassFilter filter = new LowPassFilter();
    private int histIndex = 0;
    private static final int HIST_SIZE = 40;
    private int [] hist = new int [HIST_SIZE];
    private long [] periods = new long[12];
    private int pIndex = 0;
    private long lastPeak = 0;

    private long currentPeakTime = 0;
    private int currentPeakVal = 0;
    private double currentPeakPos=0;
    private ProgressBar progress;
    private int thresh;
    //private boolean calcPeak = false;
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
        toolbar.setTitle(getString(R.string.heartrate_measure));

        GraphView graph = (GraphView) findViewById(R.id.graphBpm);
        mSeries = new LineGraphSeries<>(generateData());
//        mSeriesAvg = new LineGraphSeries<>(generateData());
//        maxSeries = new BarGraphSeries<>();
//        maxSeries.setSpacing(98);
//        mSeriesAvg.setColor(ContextCompat.getColor(getApplicationContext(),R.color.colorPrimaryDark));
        graph.addSeries(mSeries);
//        graph.addSeries(mSeriesAvg);
//        graph.addSeries(maxSeries);
        
        graph.getViewport().setMinX(0);
        graph.getViewport().setMaxX(GRAPH_SIZE);
        graph.getViewport().setXAxisBoundsManual(true);

        progress = (ProgressBar) findViewById(R.id.measureProgress);
        progress.setMax(periods.length);
        progress.setProgress(0);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.image_manipulations_activity_surface_view);
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

        ProgressBar avgBar = (ProgressBar) findViewById(R.id.progressBar2);

        int avgVal = (int)avg.val[0]*10;
        filter.put(avgVal);

        averageValueFiltered = filter.get();

        histIndex++;
        histIndex %=HIST_SIZE;
        hist[histIndex] = averageValueFiltered;


        avgBar.setProgress(averageValueFiltered);
        //Log.i(TAG,String.valueOf(averageValueFiltered)+" "+String.valueOf(graphLastXValue));
        extractBpm();

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                graphLastXValue += 1d;
                //mSeriesAvg.appendData(new DataPoint(graphLastXValue, thresh), true, GRAPH_SIZE);
                mSeries.appendData(new DataPoint(graphLastXValue, averageValueFiltered), true, GRAPH_SIZE);
//                if(calcPeak)
//                    maxSeries.appendData(new DataPoint(currentPeakPos+1,currentPeakVal),true,GRAPH_SIZE);
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
//            calcPeak = false;
            if(hist[histIndex]>currentPeakVal){
                currentPeakTime = System.currentTimeMillis();
                currentPeakVal = hist[histIndex];
                currentPeakPos = graphLastXValue;
            }
        }else if(hist[histIndex]<thresh && hist[auxPointer]>=thresh && currentPeakTime!=0) {
            long deltaT = currentPeakTime - lastPeak;
            //Log.e(TAG,String.valueOf(thresh)+" "+String.valueOf(hist[histIndex])+" "+String.valueOf(hist[auxPointer]));
//            calcPeak = true;
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
                        this.progress.setProgress(prog);
                        if (prog > 3) {
                            float bpm = 1000f / perAvg;
                            bpm *= 60;
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
}