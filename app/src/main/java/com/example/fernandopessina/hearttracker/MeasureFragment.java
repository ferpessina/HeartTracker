package com.example.fernandopessina.hearttracker;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.android.Utils;

import java.util.ArrayList;
import java.util.List;


public class MeasureFragment extends Fragment{

    private int var1 = 0;
    private int var2 = 0;
    private int var3 = 0;

    private Mat mIntermediateMat;
    private Mat rgba;
    private Mat gray;

    private ImageView imView;

    private SeekBar sBar1;
    private SeekBar sBar2;
    private SeekBar sBar3;

    private TextView tv1;
    private TextView tv2;
    private TextView tv3;

    public MeasureFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.measure_fragment, container, false);
        imView = (ImageView) view.findViewById(R.id.imageView);
        sBar1 = (SeekBar) view.findViewById(R.id.seekBar);
        sBar2 = (SeekBar) view.findViewById(R.id.seekBar2);
        sBar3 = (SeekBar) view.findViewById(R.id.seekBar3);
        sBar1.setMax(255);
        sBar2.setMax(255);
        sBar3.setMax(255);

        tv1 = (TextView) view.findViewById(R.id.textView);
        tv2 = (TextView) view.findViewById(R.id.textView2);
        tv3 = (TextView) view.findViewById(R.id.textView3);

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

        return view;
    }

    private void updateImage(){
        int var1 = sBar1.getProgress();
        int var2 = sBar2.getProgress();
        int var3 = sBar3.getProgress();

        tv1.setText(String.valueOf(var1));
        tv2.setText(String.valueOf(var2));
        tv3.setText(String.valueOf(var3));

        Bitmap bm = BitmapFactory.decodeResource(getResources(), R.drawable.wrist1);

        Mat rgba = new Mat (bm.getWidth(), bm.getHeight(), CvType.CV_8UC1);

        Utils.bitmapToMat(bm, rgba);

        Imgproc.cvtColor(rgba,rgba,Imgproc.COLOR_BGR2HSV);

        List<Mat> planes = new ArrayList<>();

        Core.split(rgba,planes);

        gray = planes.get(var1);

        Imgproc.equalizeHist(gray,gray);

        Imgproc.threshold(gray, gray, var2, 255, Imgproc.THRESH_BINARY);

        Imgproc.threshold(gray, gray, var2, 255, Imgproc.THRESH_BINARY);

        Imgproc.cvtColor(gray, rgba, Imgproc.COLOR_GRAY2RGBA, 4);

        for(Mat m : planes)
            m.release();

        // convert to bitmap:
        Utils.matToBitmap(rgba, bm);

        imView.setImageBitmap(bm);
    }
}