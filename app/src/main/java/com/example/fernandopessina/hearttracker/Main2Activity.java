package com.example.fernandopessina.hearttracker;

import android.graphics.Bitmap;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.util.Log;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class Main2Activity extends AppCompatActivity {

    private int var1 = 0;
    private int var2 = 0;
    private int var3 = 0;

    private Mat mIntermediateMat;
    private Mat rgba;
    private Mat gray;

    private ImageView imView;
    private ImageView imView2;
    private static final String TAG = "main2";

    private SeekBar sBar1;
    private SeekBar sBar2;
    private SeekBar sBar3;

    private TextView tv1;
    private TextView tv2;
    private TextView tv3;

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

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    init();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
    }

    private void init(){
        // Inflate the layout for this fragment
        imView = (ImageView) findViewById(R.id.imageView);
        imView2 = (ImageView) findViewById(R.id.imageView2);
        sBar1 = (SeekBar) findViewById(R.id.seekBar);
        sBar2 = (SeekBar) findViewById(R.id.seekBar2);
        sBar3 = (SeekBar) findViewById(R.id.seekBar3);
        sBar1.setMax(2);
        sBar1.setProgress(2);
        sBar2.setMax(245);
        sBar1.setProgress(162);
        sBar3.setMax(255);
        sBar1.setProgress(139);

        tv1 = (TextView) findViewById(R.id.textView);
        tv2 = (TextView) findViewById(R.id.textView2);
        tv3 = (TextView) findViewById(R.id.textView3);

        sBar1.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateImage();
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        sBar2.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateImage();
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        sBar3.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateImage();
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        updateImage();
    }

    private void updateImage(){
        int var1 = sBar1.getProgress();
        int var2 = sBar2.getProgress();
        int var3 = sBar3.getProgress();

        tv1.setText(String.valueOf(var1));
        tv2.setText(String.valueOf(var2));
        tv3.setText(String.valueOf(var3));

        Mat rgba = null;
        try {
            rgba = Utils.loadResource(this, R.drawable.wrist1, CvType.CV_8UC4);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Mat rgba2 = null;
        try {
            rgba2 = Utils.loadResource(this, R.drawable.wrist1, CvType.CV_8UC4);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Imgproc.cvtColor(rgba,rgba,Imgproc.COLOR_BGR2HSV);

        List<Mat> planes = new ArrayList<>();

        Core.split(rgba,planes);

        gray = planes.get(var1);

        Imgproc.equalizeHist(gray,gray);

        Imgproc.threshold(gray, gray, var2+10, 255, Imgproc.THRESH_TOZERO_INV);

        Imgproc.threshold(gray, gray, var2, 255, Imgproc.THRESH_BINARY);

        //Imgproc.equalizeHist(gray,gray);

        Imgproc.cvtColor(gray, rgba, Imgproc.COLOR_GRAY2RGBA, 4);

        Imgproc.cvtColor(rgba2, rgba2, Imgproc.COLOR_BGR2RGB);

        for(Mat m : planes)
            m.release();

        // convert to bitmap:
        Bitmap bm = Bitmap.createBitmap(rgba.cols(), rgba.rows(),Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(rgba, bm);
        // find the imageview and draw it!
        imView.setImageBitmap(bm);

        Bitmap bm2 = Bitmap.createBitmap(rgba2.cols(), rgba2.rows(),Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(rgba2, bm2);
        // find the imageview and draw it!
        imView2.setImageBitmap(bm2);
    }
}