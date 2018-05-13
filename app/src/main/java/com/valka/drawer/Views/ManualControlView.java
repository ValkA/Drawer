package com.valka.drawer.Views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.valka.drawer.DataStructures.Vector;

import static android.view.MotionEvent.ACTION_DOWN;
import static android.view.MotionEvent.ACTION_MOVE;
import static android.view.MotionEvent.ACTION_UP;

/**
 * Created by valentid on 09/05/2018.
 */

/**
 * Created by valentid on 09/05/2018.
 */

public class ManualControlView extends View {
    public interface OnPositionListener {
        void onPosition(Vector p);
    }
    Paint paint = new Paint();
    OnPositionListener onPositionListener;

    //Drawer frame of reference (milimeters)
    double viewWidth = 138.0, viewHeight = 180;
    Vector pen = new Vector();
    Vector m1 = new Vector(59.0,175.0);
    Vector m2 = new Vector(79.0,175.0);

    //View frame of reference (pixels)
    double _viewWidth, _viewHeight;
    Vector _pen = new Vector();
    Vector _m1 = new Vector();
    Vector _m2 = new Vector();

    double scale = 1.0;

    public ManualControlView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public ManualControlView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(!(event.getAction() == ACTION_MOVE || event.getAction() == ACTION_DOWN || event.getAction() == ACTION_UP)){
            return super.onTouchEvent(event);
        }
        if(onPositionListener != null) {
            _pen.x = event.getX();
            _pen.y = event.getY();
            _pen.z = (event.getAction() == ACTION_UP ? 1 : 0); //TODO:_pen.z = event.getPressure();
            androidToDrawer(pen.assign(_pen));
            if(!inView(pen)) return false;

            invalidate();
            if(onPositionListener!=null){
                onPositionListener.onPosition(pen);
            }
            return true;
        }
        return false;
    }

    /**
     * @param p - drawer coordinates of pen
     * @return true iff in the range of drawer
     */
    private boolean inView(Vector p){
        return (0<=p.x && p.x<=viewWidth && 0<=p.y && p.y<=viewHeight);
    }
    
    /**
     * converts androids x,y to drawer frame of reference
     *                    0_______
     *  |                 |
     *  |          <=     |
     *  0______           |
     *
     * @param pixels
     * @return same Vector for chaining
     */
    private Vector androidToDrawer(Vector pixels){
        Vector milimeters = pixels;
        milimeters.y = (+ 0.5 * (_viewHeight + viewHeight * scale) - pixels.y) / scale;
        milimeters.x = (- 0.5 * (_viewWidth - viewWidth * scale) + pixels.x) / scale;
        return milimeters;
    }

    /**
     * converts drawers x,y to androids frame of reference
     *                    0_______
     *  |                 |
     *  |          =>     |
     *  0______           |
     *
     * @param milimeters
     * @return same Vector for chaining
     */
    private Vector drawerToAndroid(Vector milimeters){
        Vector pixels = milimeters;
        pixels.y = 0.5 * (_viewHeight + viewHeight * scale) - scale * milimeters.y;
        pixels.x = 0.5 * (_viewWidth - viewWidth * scale) + scale * milimeters.x;
        return pixels;
    }


    Bitmap drawingBitmap;
    Canvas drawingCanvas;

    @Override
    protected void onSizeChanged(int xNew, int yNew, int xOld, int yOld){
        super.onSizeChanged(xNew, yNew, xOld, yOld);
        _viewWidth = xNew;
        _viewHeight = yNew;

        scale = Math.min(_viewHeight/viewHeight, _viewWidth/viewWidth) * 0.9;

        drawerToAndroid(_pen.assign(pen));
        drawerToAndroid(_m1.assign(m1));
        drawerToAndroid(_m2.assign(m2));
        c1.x = 0; c1.y = 0; drawerToAndroid(c1);
        c2.x = viewWidth; c2.y = 0; drawerToAndroid(c2);
        c3.x = viewWidth; c3.y = viewHeight; drawerToAndroid(c3);
        c4.x = 0; c4.y = viewHeight; drawerToAndroid(c4);

        drawingBitmap = Bitmap.createBitmap((int)_viewHeight, (int)_viewHeight, Bitmap.Config.ARGB_8888);
        drawingCanvas = new Canvas(drawingBitmap);
        drawingCanvas.drawColor(Color.BLACK);
    }


    Vector c1 = new Vector();
    Vector c2 = new Vector();
    Vector c3 = new Vector();
    Vector c4 = new Vector();
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        //drawing
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        drawingCanvas.drawCircle((float)_pen.x,(float)_pen.y,10f,paint);
        canvas.drawBitmap(drawingBitmap, 0, 0, paint);

        //motors
        paint.setColor(Color.GREEN);
        canvas.drawCircle((float)_m1.x,(float)_m1.y,10f,paint);
        canvas.drawCircle((float)_m2.x,(float)_m2.y,10f,paint);

        //frame
        paint.setColor(Color.RED);
        canvas.drawLine((float)c1.x, (float)c1.y, (float)c2.x, (float)c2.y, paint);
        canvas.drawLine((float)c2.x, (float)c2.y, (float)c3.x, (float)c3.y, paint);
        canvas.drawLine((float)c3.x, (float)c3.y, (float)c4.x, (float)c4.y, paint);
        canvas.drawLine((float)c4.x, (float)c4.y, (float)c1.x, (float)c1.y, paint);
    }

    public void setOnPositionListener(OnPositionListener onPositionListener) {
        this.onPositionListener = onPositionListener;
    }
}
