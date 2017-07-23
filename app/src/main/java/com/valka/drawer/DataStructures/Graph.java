package com.valka.drawer.DataStructures;

import android.graphics.Bitmap;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by valentid on 23/07/2017.
 */

public class Graph extends HashMap<Vertex, ArrayList<Edge>> {
    private static final String TAG = "Graph";
    private static final int THRES = 0x000000;
    public Graph(Bitmap bitmap) {
        int[][] steps = {
                { 0,-1},{-1, 0},{ 1, 0},{ 0, 1},//straights
                {-1,-1},{ 1,-1},{-1, 1},{ 1, 1}//diagonals
        };
        int w = bitmap.getWidth() - 1, h = bitmap.getHeight() - 1;
        for (int ux = 1; ux < w; ++ux) {
            for (int uy = 1; uy < h; uy++) {
                int uc = bitmap.getPixel(ux, uy);
                uc = (uc >> 16) & 0xff;
                if (uc <= THRES) continue;
                Vertex u = new Vertex(ux, uy);

                ArrayList<Edge> ue = get(u);
                if (get(u) == null) {
                    ue = new ArrayList<>();
                    put(u, ue);
                }

                for (int[] step : steps) {
                    int vx = ux + step[0];
                    int vy = uy + step[1];
                    int vc = bitmap.getPixel(vx, vy);
                    vc = (vc >> 16) & 0xff;
                    if (vc <= THRES) continue;
                    Vertex v = new Vertex(vx, vy);
                    ArrayList<Edge> ve = get(v);
                    if (get(v) == null) {
                        ve = new ArrayList<>();
                        put(v, ve);
                    }
                    Edge e = new Edge(u, v);
                    ue.add(e);
                    ve.add(e);
                }
            }
        }
    }

    private Set<Vertex> notVisitedV;
    private HashSet<Edge> visitedE;
    public interface TMP{
        void onEdge(Edge e);
    }
    TMP tmp;
    Vertex lastV;
    public void createDfsGCode(TMP tmp){
        this.tmp = tmp;
        notVisitedV = new HashSet(keySet());
        visitedE = new HashSet<>();

        Vertex firstV = notVisitedV.iterator().next();
        dfs(firstV);
        while(!notVisitedV.isEmpty()){
            //find closest to lastV
            double minDist = Double.POSITIVE_INFINITY;
            for(Vertex w : notVisitedV){
                double dist = w.sqrDist(lastV);
                if(dist < minDist){
                    minDist = dist;
                    firstV = w;
                }
            }
            dfs(firstV);
        }
    }

    private void dfs(Vertex u){
        if(!notVisitedV.contains(u)) return;//if visited u
        notVisitedV.remove(u);
        for(Edge uv : get(u)){
            if(visitedE.contains(uv)) continue;
            lastV = uv.getV(u);
            tmp.onEdge(uv);
            visitedE.add(uv);
            dfs(lastV);
        }
    }
}
