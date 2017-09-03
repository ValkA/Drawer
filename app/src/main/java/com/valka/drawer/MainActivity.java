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
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.util.Iterator;
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
    SeekBar epsilonBar;

    BaseDrawerDevice btDrawerDevice = new BTDrawerDevice(this);


    static {
        if(!OpenCVLoader.initDebug()){
            Log.d(TAG, "OpenCV not loaded");
        } else {
            Log.d(TAG, "OpenCV loaded");
        }
    }

    Mat edgesMat = null;
    Bitmap edgesBitmap = null;
    Bitmap approxBitmap = null;
    List<List<Point>> approx = null;
    List<List<Point>> paths = null;
    Thread imageThread = new Thread(){
            @Override
            public void run(){
                Log.i(TAG, "start image thread");
                if (choseImageUri == null || choseImageUri.equals(Uri.EMPTY)) return;

                //create gray Mat of image
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

                //create canny edges mat
                edgesMat = new Mat(mat.size(), CvType.CV_8UC1);
                Imgproc.Canny(grayMat, edgesMat, 255.0*lowThresBar.getProgress()/100.0, 255.0*hiThresBar.getProgress()/100.0);
                //invert colors
//                Mat invertcolormatrix= new Mat(edgesMat.rows(),edgesMat.cols(), edgesMat.type(), new Scalar(255,255,255));
//                Core.subtract(invertcolormatrix, edgesMat, edgesMat);

                //draw opencv edges mat
                edgesBitmap = Bitmap.createBitmap(edgesMat.width(), edgesMat.height(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(edgesMat, edgesBitmap);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        image.setImageBitmap(edgesBitmap);
                    }
                });
                paths = null;
                Log.i(TAG, "canny done");
            }
        };

        Thread approxThread = new Thread(){
            @Override
            public void run(){
                try {
                    drawThread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                Log.i(TAG, "start approx thread");
                //create paths and approx paths
                if (paths == null) paths = DrawerUtils.getAsListOfPaths(edgesMat);
                Log.i(TAG, "getAsListOfPaths done");
                approx = DrawerUtils.approxPolyDP(paths,epsilonBar.getProgress()/25.0,false);
                Log.i(TAG, "approxPolyDP done");

                approxBitmap = Bitmap.createBitmap(edgesMat.width(), edgesMat.height(), Bitmap.Config.ARGB_8888);
                final Canvas canvas = new Canvas(approxBitmap);
                final Paint paint = new Paint();
                canvas.drawColor(Color.BLACK);
                paint.setColor(Color.WHITE);
                paint.setAlpha(255);
                paint.setStrokeWidth(1f);
                for(List<Point> path : approx){
                    DrawerUtils.drawPath(canvas, path, paint);
                }
                Log.i(TAG, "drawing paths done");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        image.setImageBitmap(approxBitmap);
                    }
                });
            }
        };

        Thread drawThread = new Thread(){
            @Override
            public void run(){
                Log.i(TAG, "started draw thread");
                if (approxBitmap == null) {
                    approxThread.start();
                    try {
                        approxThread.join();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        return;
                    }
                }

                approx = DrawerUtils.sortContours(approx);
                Log.i(TAG, "sortContours done");

                final Canvas canvas = new Canvas(approxBitmap);
                final Paint paint = new Paint();
//                canvas.drawColor(Color.BLACK);
                paint.setAlpha(255);
                paint.setStrokeWidth(1f);

                double scaleRatio = 100.0/Math.max(canvas.getWidth(),canvas.getHeight());
                int j = -1;
                Point p1 = null;
                Point p2 = null;
                for(List<Point> path : approx){
                    btDrawerDevice.sendGCodeCommand("G0 Z1");//pen up
                    j++;
                    paint.setColor(j%3==0 ? Color.RED : j%3==1 ? Color.GREEN : j%3==2 ? Color.BLUE : Color.WHITE);

                    Iterator<Point> i = path.iterator();
                    p2 = i.next();

                    //it isnt null! TODO: isConnected() method
                    if(p1!=null&&p2!=null){
                        paint.setColor(Color.BLUE);
                        paint.setAlpha(128);
                        canvas.drawLine((float)p1.x,(float)p1.y,(float)p2.x,(float)p2.y, paint);
                    }
                    btDrawerDevice.sendGCodeCommand(String.format("G0 X%.2f Y%.2f", 20+p2.x*scaleRatio, (canvas.getHeight()-p2.y)*scaleRatio));//goto u
                    btDrawerDevice.sendGCodeCommand("G0 Z0");//pen down

                    while(i.hasNext()){
                        p1 = p2;
                        p2 = i.next();
                        paint.setAlpha(255);
                        paint.setColor(Color.RED);
                        canvas.drawLine((float)p1.x,(float)p1.y,(float)p2.x,(float)p2.y, paint);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                image.invalidate();
                            }
                        });
                        btDrawerDevice.sendGCodeCommand(String.format("G0 X%.2f Y%.2f", 20+p2.x*scaleRatio, (canvas.getHeight()-p2.y)*scaleRatio));//goto u                                                                ~~~~~~~~~~~~~~~~~~~~~~~~~~~
                    }
                }
                Log.i(TAG, "ended draw thread");
            }
        };

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
                        .setRequestedSize(256,256)
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
            public void onClick(View view) {
                if (btDrawerDevice.isConnected()) {
                    drawThread.start();
                } else {
                    Toast.makeText(MainActivity.this, "Connect to a device first", Toast.LENGTH_LONG).show();
                }
            }
        });

        lowThresBar = (SeekBar)findViewById(R.id.seekbar_low_thres);
        hiThresBar = (SeekBar)findViewById(R.id.seekbar_hi_thres);
        SeekBar.OnSeekBarChangeListener onSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if(!imageThread.isAlive() && !approxThread.isAlive()) {
                    imageThread.start();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                approxThread.start();
            }
        };
        lowThresBar.setOnSeekBarChangeListener(onSeekBarChangeListener);
        hiThresBar.setOnSeekBarChangeListener(onSeekBarChangeListener);
        epsilonBar = (SeekBar)findViewById(R.id.seekbar_epsilon);
        epsilonBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if(!approxThread.isAlive()){
                    approxThread.start();
                }
            }
        });


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
