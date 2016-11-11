package com.example.fernandopessina.hearttracker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.BarGraphSeries;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

public class MeasureActivity extends Activity implements CvCameraViewListener2 {
    private static final String  TAG                 = "OCVSample::Activity";

    private CameraBridgeViewBase mOpenCvCameraView;

    Scalar avg;

    private LineGraphSeries<DataPoint> mSeries;
    private double graphLastXValue = 5d;

    private final int GRAPH_SIZE = 100;

    private Mat                  mIntermediateMat;
    private Mat                  rgba;
    private Mat                  gray;

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

        GraphView graph = (GraphView) findViewById(R.id.graphBpm);
        mSeries = new LineGraphSeries<>(generateData());
        graph.addSeries(mSeries);
        graph.getViewport().setMinX(0);
        graph.getViewport().setMaxX(GRAPH_SIZE);
        graph.getViewport().setXAxisBoundsManual(true);

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
        gray = new Mat(height, width, CvType.CV_8UC1);
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

        gray = planes.get(2);

        Imgproc.equalizeHist(gray,gray);

        avg = Core.mean(gray);

        ProgressBar avgBar = (ProgressBar) findViewById(R.id.progressBar2);

        int avgVal = (int)avg.val[0]*10;
        avgBar.setMax(255);

        avgBar.setProgress((int)avg.val[0]);


        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView bpmTextView = (TextView) findViewById(R.id.textViewBpm);
                bpmTextView.setText(String.valueOf(avg.val[0]));
            }
        });

        Imgproc.cvtColor(gray, rgba, Imgproc.COLOR_GRAY2RGBA, 4);

        graphLastXValue += 1d;
        mSeries.appendData(new DataPoint(graphLastXValue, (int)avg.val[0]), true, 40);


        for(Mat m : planes)
            m.release();
        return rgba;
    }

    private DataPoint[] generateData() {
        int count = GRAPH_SIZE;
        DataPoint[] values = new DataPoint[count];
        for (int i=0; i<count; i++) {
            double x = i;
            double f = 25;
            DataPoint v = new DataPoint(x, f);
            values[i] = v;
            graphLastXValue = i;
        }
        return values;
    }
}