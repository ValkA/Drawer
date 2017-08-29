package com.valka.drawer;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;
import com.valka.drawer.Algorithms.CannyEdges;
import com.valka.drawer.DataStructures.Edge;
import com.valka.drawer.DataStructures.Graph;
import com.valka.drawer.DataStructures.Vertex;
import com.valka.drawer.DrawerDevice.BTDrawerDevice;
import com.valka.drawer.DrawerDevice.BaseDrawerDevice;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    final static String TAG = "Main";

    ImageView image;
    Uri choseImageUri;
    ProgressBar progressBarHorizontal;
    ProgressBar progressBarCircular;
    Button chooseImageButton;

    BTDrawerDevice btDrawerDevice = new BTDrawerDevice(this);

    Thread drawThread = new Thread(){
            @Override
            public void run(){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {progressBarCircular.setVisibility(View.VISIBLE);}
                });
                if (choseImageUri == null || choseImageUri.equals(Uri.EMPTY)) return;
                Bitmap bitmap;
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(),choseImageUri);
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }

                Log.e(TAG,"Canny Start");
                CannyEdges cannyEdges = new CannyEdges();
                cannyEdges.setSourceImage(bitmap);
                cannyEdges.process();
                Bitmap cannyBitmap = cannyEdges.getEdgesBitmap();
                Log.e(TAG,"Canny Done");

                final Bitmap drawing = Bitmap.createBitmap(cannyBitmap.getWidth(), cannyBitmap.getHeight(), cannyBitmap.getConfig());
                final Canvas canvas = new Canvas(drawing);
                canvas.drawBitmap(cannyBitmap,0,0,null);
                final Paint paint = new Paint();
                paint.setColor(Color.RED);
                paint.setAlpha(255);
                paint.setStrokeWidth(1f);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        image.setImageBitmap(drawing);
                    }
                });

                Log.e(TAG,"Graph Build Start");
                final Graph g = new Graph(cannyBitmap);
                Log.e(TAG,"Graph Build Done");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {progressBarCircular.setVisibility(View.GONE);}
                });
                final Vertex lastPos = new Vertex(-1.0, -1.0);
                final double scaleRatio = 120.0/Math.max(cannyBitmap.getWidth(),cannyBitmap.getHeight());

                g.createDfsGCode(new Graph.onEdgeListener(){
                    @Override
                    public void onEdge(Edge e, final double progress) {
                        //draw simulation on ImageView
                        canvas.drawLine((float)e.u.x,(float)e.u.y,(float)e.v.x,(float)e.v.y, paint);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                image.invalidate();
                                progressBarHorizontal.setProgress((int)(progress*100));
                            }
                        });

                        //send to drawer
                        if(!lastPos.equals(e.u)){
                            btDrawerDevice.sendGCodeCommand("G0 Z0");//pen up
                            btDrawerDevice.sendGCodeCommand(String.format("G0 X%.2f Y%.2f", e.u.x*scaleRatio, (drawing.getHeight()-e.u.y)*scaleRatio));//goto u
                            btDrawerDevice.sendGCodeCommand("G0 Z1");//pen down

                        }
                        btDrawerDevice.sendGCodeCommand(String.format("G0 X%.2f Y%.2f", e.v.x*scaleRatio, (drawing.getHeight()-e.v.y)*scaleRatio));//draw u->v line
                        lastPos.x = e.v.x;
                        lastPos.y = e.v.y;
                    }
                });

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
                        .setRequestedSize(128,128)
                        .setGuidelines(CropImageView.Guidelines.ON)
                        .start(MainActivity.this);
            }
        });
        chooseImageButton.setEnabled(false);

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
                    chooseImageButton.setEnabled(success);
                    }
                });
            }
        });

        findViewById(R.id.draw_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                drawThread.start();
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                choseImageUri = result.getUri();
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }
    }
}
