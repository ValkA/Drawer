package com.valka.drawer;

import android.graphics.Canvas;
import android.graphics.Paint;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by valentid on 03/09/2017.
 */

public class DrawerUtils {

    //algo to iterate on the canny lines:
    //curves = {}
    //for x,y in domain:
    //if I(x,y)>0 curves.add(travelFrom(x,y));

    //travelFrom(x,y){
    //steps={(1,1)...}
    //curve={}
    //curr = (x,y)
    //for i=0 to steps.len{
    //  step = steps[i]
    //  if(I(curr+step)>0){
    //      curr+=step
    //      I(curr) = 0
    //      curve.add(curr)
    //      i=0
    //  }
    //return curve

    //then approximate with C++:
    //void approxPolyDP(InputArray curve, OutputArray approxCurve, double epsilon, bool closed)¶
    static public List<List<Point>> getAsListOfPaths(Mat bitmapPaths){
        List<List<Point>> paths = new ArrayList<>();
        for(int i=0; i<bitmapPaths.rows(); ++i){
            for (int j=0; j<bitmapPaths.cols(); ++j){
                double[] p = bitmapPaths.get(i,j);
                if(p[0]>0) paths.add(travelFrom(bitmapPaths, i,j));
            }
        }

        return paths;
    }

    static private int[] stepsI = {1,1,0,-1,-1,-1, 0, 1};//y
    static private int[] stepsJ = {0,1,1, 1, 0,-1,-1,-1};//x

    static private List<Point> travelFrom(Mat bitmapPaths, int i, int j){
        List<Point> path = new LinkedList<>();
        double[] p = bitmapPaths.get(i,j);
        int cols = bitmapPaths.cols();
        int rows = bitmapPaths.rows();
        while(p[0]>0){
            p[0] = 0;
            bitmapPaths.put(i,j,p);
            path.add(new Point(j,i));
            for(int s=0; s<stepsI.length; ++s){
                int I = i+stepsI[s];
                int J = j+stepsJ[s];
                if(I<0||I>=rows||J<0||J>=cols) continue;
                p = bitmapPaths.get(I,J);
                if(p[0]>0){
                    i=I;
                    j=J;
                    break;
                }
            }
        }
        return path;
    }


    static public List<List<Point>> sortContours(List<List<Point>> unsorted){
        LinkedList<List<Point>> _unsorted = new LinkedList<>(unsorted);
        int contoursCount = unsorted.size();
        List<List<Point>> sorted = new ArrayList<>(unsorted.size());

        List<Point> p1 = _unsorted.getFirst();
        sorted.add(p1);
        _unsorted.removeFirst();
        //find nearest path
        double[] ret = new double[2];
        for(int i=0; i<contoursCount-1; ++i){
            //find the one with closest beginning to p's end
            double min = Double.POSITIVE_INFINITY;
            boolean flip = false;
            List<Point> closest = _unsorted.getFirst();
            for(List<Point> p2 : _unsorted){
                distBetweenPaths(p1,p2,ret);
                if(ret[0]<min){
                    closest = p2;
                    min = ret[0];
                    flip = (ret[1] != 0.0);
                }
            }
            if(flip) flip(closest);
            p1 = closest;
            sorted.add(closest);
            _unsorted.remove(closest);
        }
        return sorted;
    }

    //dist[0] = distance, dist[1] = 0 iff flip of p2 is not needed
    static private void distBetweenPaths(List<Point> p1, List<Point> p2, double dist[]){
        Point a1 = p1.get(0);
        Point b1 = p1.get(p1.size()-1);
        Point a2 = p2.get(0);
        Point b2 = p2.get(p2.size()-1);

        double nonFlippedDist = dist(b1,a2);
        double flippedDist = dist(b1,b2);

        dist[0] = Math.min(flippedDist,nonFlippedDist);
        dist[1] = (nonFlippedDist <= flippedDist ? 0.0 : 1.0);
    }

    static private void flip(List<Point> p){
        Point tmp;
        for(int i=0; i<p.size()/2; ++i){
            tmp = p.get(i);
            p.set(i,p.get(p.size()-1-i));
            p.set(p.size()-1-i, tmp);
        }
    }

    static private double dist(Point a, Point b){
        double dx = a.x - b.x;
        double dy = a.y - b.y;
        return Math.sqrt(dx*dx+dy*dy);
    }

    static public List<List<Point>> filterShortPaths(List<List<Point>> paths, double minPathLength) {
        List<List<Point>> filtered = new LinkedList<>();
        for(List<Point> path : paths){
            if(getPathLength(path) >= minPathLength){
                filtered.add(path);
            }
        }
        return filtered;
    }

    static public double getPathLength(List<Point> path) {
        if(path.size() == 0){
            return 0.0;
        }
        double length = 0.0;
        Iterator<Point> it = path.iterator();
        Point p1 = it.next();
        while(it.hasNext()){
            Point p2 = it.next();
            length += dist(p1,p2);
            p1 = p2;
        }
        return length;
    }



    static public List<List<Point>> approxPolyDP(List<List<Point>> paths, double epsilon, boolean closed){
        List<List<Point>> approx = new LinkedList<>();
        for(List<Point> lPath : paths){
            MatOfPoint2f mPath = new MatOfPoint2f();
            MatOfPoint2f mApprox = new MatOfPoint2f();
            mPath.fromList(lPath);
            Imgproc.approxPolyDP(mPath,mApprox,epsilon,closed);
            approx.add(mApprox.toList());
        }
        return approx;
    }

    static public void drawPath(Canvas canvas, List<Point> path, Paint paint){
        Iterator<Point> i = path.iterator();
        Point p1 = null;
        Point p2 = i.next();
        while(i.hasNext()){
            p1 = p2;
            p2 = i.next();
            canvas.drawLine((float)p1.x,(float)p1.y,(float)p2.x,(float)p2.y, paint);
        }
    }
}
