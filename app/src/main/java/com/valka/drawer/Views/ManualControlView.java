package com.valka.drawer.Views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

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
        /**
         * called when new x,y event happens
         * @param x in millimeters
         * @param y in millimeters
         * @param z in pressure units (0-1)
         */
        void onPosition(float x, float y, float z);
    }

    float x = 0, xMillimeters;
    float y = 0, yMillimeters;
    float z, zPressure;

    Paint paint = new Paint();
    OnPositionListener onPositionListener;

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
            if(!inView(event.getX(),event.getY())) return false;
            x = event.getX();
            y = event.getY();
            z = event.getPressure();

            xMillimeters = x/10;
            yMillimeters = y/10;
            if(event.getAction() == ACTION_UP){
                zPressure = 1;
            } else {
                zPressure = 0;//TODO: = z ?
            }

            invalidate();
            onPositionListener.onPosition(xMillimeters,yMillimeters,zPressure);
            return true;
        }
        return false;
    }

    private boolean inView(float x, float y){
        return (0<=x && x<=getWidth() && 0<=y && y<=getHeight());
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(Color.BLACK);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        canvas.drawCircle(x,y,10f,paint);
    }

    public void setOnPositionListener(OnPositionListener onPositionListener) {
        this.onPositionListener = onPositionListener;
    }
}
