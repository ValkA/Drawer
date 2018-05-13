package com.valka.drawer.DataStructures;

import android.graphics.Bitmap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by valentid on 23/07/2017.
 */

public class Graph extends HashMap<Vector, ArrayList<Edge>> {
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
                Vector u = new Vector(ux, uy);

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
                    Vector v = new Vector(vx, vy);
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

    private Set<Vector> notVisitedV;
    private HashSet<Edge> visitedE;
    public interface onEdgeListener {
        void onEdge(Edge e, double progress);
    }
    onEdgeListener onEdgeListener;
    Vector lastV;
    public void createDfsGCode(onEdgeListener onEdgeListener){
        this.onEdgeListener = onEdgeListener;
        notVisitedV = new HashSet(keySet());
        visitedE = new HashSet<>();

        Vector firstV = notVisitedV.iterator().next();
        dfs(firstV);
        while(!notVisitedV.isEmpty()){
            //find closest to lastV
            double minDist = Double.POSITIVE_INFINITY;
            for(Vector w : notVisitedV){
                double dist = w.sqrDist(lastV);
                if(dist < minDist){
                    minDist = dist;
                    firstV = w;
                }
            }
            dfs(firstV);
        }
    }

    private void dfs(Vector u){
        if(!notVisitedV.contains(u)) return;//if visited u
        lastV = u;
        notVisitedV.remove(u);
        for(Edge uv : get(u)){
            if(visitedE.contains(uv)) continue;
            onEdgeListener.onEdge(uv, 1d-((double)notVisitedV.size())/size());
            visitedE.add(uv);
            dfs(uv.getV(u));
        }
    }
}
