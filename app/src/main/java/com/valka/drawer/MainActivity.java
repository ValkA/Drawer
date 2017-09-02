package com.valka.drawer;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Toast;

import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;
import com.valka.drawer.DrawerDevice.BTDrawerDevice;
import com.valka.drawer.DrawerDevice.BaseDrawerDevice;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    final static String TAG = "Main";

    ImageView image;
    Uri choseImageUri;
    ProgressBar progressBarHorizontal;
    ProgressBar progressBarCircular;
    Button chooseImageButton;
    SeekBar lowThresBar;
    SeekBar hiThresBar;

    BTDrawerDevice btDrawerDevice = new BTDrawerDevice(this);


    static {
        if(!OpenCVLoader.initDebug()){
            Log.d(TAG, "OpenCV not loaded");
        } else {
            Log.d(TAG, "OpenCV loaded");
        }
    }

    Mat edgesMat = null;
    Bitmap edgesBitmap = null;
    Thread imageThread = new Thread(){
            @Override
            public void run(){
                if (choseImageUri == null || choseImageUri.equals(Uri.EMPTY)) return;

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {progressBarCircular.setVisibility(View.VISIBLE);}
                });

                Bitmap bitmap;
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(),choseImageUri);
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }

                Mat mat = new Mat(bitmap.getHeight(), bitmap.getWidth(), CvType.CV_8UC4);
                Utils.bitmapToMat(bitmap,mat);
                Mat grayMat = new Mat(mat.size(), CvType.CV_8UC1);
                Imgproc.cvtColor(mat, grayMat, Imgproc.COLOR_RGB2GRAY);
                edgesMat = new Mat(mat.size(), CvType.CV_8UC1);
                Imgproc.Canny(grayMat, edgesMat, 255.0*lowThresBar.getProgress()/100.0, 255.0*hiThresBar.getProgress()/100.0);
                //invert colors
                Mat invertcolormatrix= new Mat(edgesMat.rows(),edgesMat.cols(), edgesMat.type(), new Scalar(255,255,255));
                Core.subtract(invertcolormatrix, edgesMat, edgesMat);


                //draw opencv edges mat
                edgesBitmap = Bitmap.createBitmap(edgesMat.width(), edgesMat.height(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(edgesMat, edgesBitmap);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        image.setImageBitmap(edgesBitmap);
                        progressBarCircular.setVisibility(View.GONE);
                    }
                });
            }
        };

        Thread drawThread = new Thread(){
            @Override
            public void run(){
                if (edgesMat == null || edgesBitmap == null) return;

                //get contours and simutae drawing them
                Mat hierarchy = new Mat();
                List<MatOfPoint> contoursMats = new ArrayList<>();
                Imgproc.findContours(edgesMat,contoursMats,hierarchy,Imgproc.RETR_LIST,Imgproc.CHAIN_APPROX_SIMPLE);

                List<List<Point>> contours = sortContours(contoursMats);

                final Canvas canvas = new Canvas(edgesBitmap);
                final Paint paint = new Paint();
                paint.setColor(Color.RED);
                paint.setAlpha(255);
                paint.setStrokeWidth(1f);

                for(int j=0; j<contours.size(); ++j){
                    paint.setColor(j%3==0 ? Color.RED : j%3==1 ? Color.GREEN : j%3==2 ? Color.BLUE : Color.WHITE);

                    final List<Point> contour = contours.get(j);
                    for(int i=1; i<contour.size(); ++i){
                        Point p1 = contour.get(i-1);
                        Point p2 = contour.get(i);
                        //simulate on lcd
                        canvas.drawLine((float)p1.x,(float)p1.y,(float)p2.x,(float)p2.y, paint);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                image.invalidate();
                            }
                        });
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        //send to drawer
//                        if(!lastPos.equals(e.u)){
//                            btDrawerDevice.sendGCodeCommand("G0 Z0");//pen up
//                            btDrawerDevice.sendGCodeCommand(String.format("G0 X%.2f Y%.2f", e.u.x*scaleRatio, (drawing.getHeight()-e.u.y)*scaleRatio));//goto u
//                            btDrawerDevice.sendGCodeCommand("G0 Z1");//pen down
//
//                        }
//                        btDrawerDevice.sendGCodeCommand(String.format("G0 X%.2f Y%.2f", e.v.x*scaleRatio, (drawing.getHeight()-e.v.y)*scaleRatio));//draw u->v line
//                        lastPos.x = e.v.x;
//                        lastPos.y = e.v.y;

                    }
                }

            }
        };

    List<List<Point>> sortContours(List<MatOfPoint> _unsorted){
        List<List<Point>> unsorted = new ArrayList<>();
        for(MatOfPoint matOfPoint : _unsorted){
            unsorted.add(matOfPoint.toList());
        }

        int contoursCount = unsorted.size();
        List<List<Point>> sorted = new LinkedList<>();

        List<Point> p = unsorted.get(0);
        sorted.add(p);
        unsorted.remove(p);
        for(int i=0; i<contoursCount-1; ++i){
            Point last = p.get(p.size()-1);
            //find the one with closest beginning to p's end
            double min = Double.POSITIVE_INFINITY;
            List<Point> closest = unsorted.get(0);
            for(int j=0; j<unsorted.size(); ++j){
                Point first = unsorted.get(j).get(0);
                double dx = first.x - last.x;
                double dy = first.y - last.y;
                double d = Math.sqrt(dx*dx+dy*dy);
                if(d<min){
                    closest = unsorted.get(j);
                    min=d;
                }
            }
            p = closest;
            sorted.add(closest);
            unsorted.remove(closest);
        }
        return sorted;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        image = (ImageView) findViewById(R.id.img);
        progressBarHorizontal = (ProgressBar) findViewById(R.id.progress_horizontal);
        progressBarCircular = (ProgressBar) findViewById(R.id.progress_circular);

        chooseImageButton = (Button)findViewById(R.id.choose_image_button);
        chooseImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CropImage.activity()
                        .setAllowRotation(true)
                        .setAllowCounterRotation(true)
                        .setAspectRatio(1,1)
                        .setRequestedSize(512,512)
                        .setGuidelines(CropImageView.Guidelines.ON)
                        .start(MainActivity.this);
            }
        });
//        chooseImageButton.setEnabled(false);

        findViewById(R.id.choose_device_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                btDrawerDevice.open(MainActivity.this, new BaseDrawerDevice.OnOpenListener() {
                    @Override
                    public void onOpen(boolean success) {
                    if(!success){
                        Toast.makeText(MainActivity.this, "Can't connect to device", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(MainActivity.this, "Connected successfully", Toast.LENGTH_LONG).show();
                    }
//                    chooseImageButton.setEnabled(success);
                    }
                });
            }
        });

        findViewById(R.id.draw_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { drawThread.start(); }
        });

        lowThresBar = (SeekBar)findViewById(R.id.seekbar_low_thres);
        hiThresBar = (SeekBar)findViewById(R.id.seekbar_hi_thres);
        SeekBar.OnSeekBarChangeListener onSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                imageThread.start();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        };
        lowThresBar.setOnSeekBarChangeListener(onSeekBarChangeListener);
        hiThresBar.setOnSeekBarChangeListener(onSeekBarChangeListener);


    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                choseImageUri = result.getUri();
                imageThread.start();
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }
    }
}
