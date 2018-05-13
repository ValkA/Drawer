package com.valka.drawer.DataStructures;

/**
 * Created by valentid on 23/07/2017.
 */

public class Edge{
    public Vector u, v;
    public Edge(Vector u, Vector v){
        this.u = u;
        this.v = v;
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Edge)) return false;
        Edge e = (Edge) o;
        return ((u==e.u) && (v==e.v)) || ((u==e.v) && (v==e.u));
    }
    public Vector getV(Vector u){
        if(this.u.equals(u)){
            return this.v;
        }
        if(this.v.equals(u)){
            return this.u;
        }
        return null;
    }
    @Override
    public int hashCode() {
        return u.hashCode() ^ v.hashCode();
    }
    @Override
    public String toString(){
        return String.format("(%f,%f)->(%f,%f)", u.x, u.y, v.x, v.y);
    }
}
