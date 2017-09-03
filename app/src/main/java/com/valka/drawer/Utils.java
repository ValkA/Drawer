package com.valka.drawer;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by valentid on 03/09/2017.
 */

public class Utils {

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
        List<List<Point>> paths = new LinkedList<>();
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
}
