package com.valka.drawer.DataStructures;

/**
 * Created by valentid on 23/07/2017.
 */

public class Vertex{
    public double x,y;

    public Vertex(double x, double y){
        this.x = x;
        this.y = y;
    }

    public double sqrDist(Vertex v){
        double dx = v.x - x;
        double dy = v.y - y;
        return dx*dx + dy*dy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Vertex)) return false;
        Vertex v = (Vertex) o;
        return x == v.x && y == v.y;
    }

    @Override
    public int hashCode() {
        return 31 * Double.valueOf(x).hashCode() + Double.valueOf(y).hashCode();
    }
}
