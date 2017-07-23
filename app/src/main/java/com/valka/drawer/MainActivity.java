package com.valka.drawer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import com.valka.drawer.Algorithms.CannyEdges;
import com.valka.drawer.DataStructures.Edge;
import com.valka.drawer.DataStructures.Graph;

public class MainActivity extends AppCompatActivity {
    final static String TAG = "Main";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inScaled = true;
        opt.inSampleSize = 4;
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.raw.car, opt);
        Log.e(TAG,"Canny Start");
        CannyEdges cannyEdges = new CannyEdges();
        cannyEdges.setSourceImage(bitmap);
        cannyEdges.process();
        Bitmap cannyBitmap = cannyEdges.getEdgesBitmap();
        Log.e(TAG,"Canny Done");

        Bitmap blank = Bitmap.createBitmap(cannyBitmap.getWidth(), cannyBitmap.getHeight(), cannyBitmap.getConfig());
        final Canvas canvas = new Canvas(blank);
        canvas.drawColor(Color.BLACK);
        final Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setAlpha(128);
        paint.setStrokeWidth(1f);
        ((ImageView)findViewById(R.id.img)).setImageBitmap(blank);

        final Graph g = new Graph(cannyBitmap);
        Log.e(TAG,"Graph Built");
        new Thread(){
            @Override
            public void run(){
                g.createDfsGCode(new Graph.TMP(){
                    @Override
                    public void onEdge(Edge e) {
                        canvas.drawLine((float)e.u.x,(float)e.u.y,(float)e.v.x,(float)e.v.y, paint);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ((ImageView)findViewById(R.id.img)).invalidate();

                            }
                        });
                        try {
                            Thread.sleep(1);
                        } catch (InterruptedException e1) {
                            e1.printStackTrace();
                        }
                    }
                });
            }
        }.start();

        Log.e(TAG,"DFS Done");
    }
}
