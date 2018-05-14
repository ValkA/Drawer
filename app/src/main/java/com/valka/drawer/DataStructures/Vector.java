package com.valka.drawer.DataStructures;

/**
 * Created by valentid on 23/07/2017.
 */

public class Vector {
    public double x,y,z;
    public Vector(){}
    public Vector(double x, double y){
        this.x = x;
        this.y = y;
        this.z = 0.0;
    }
    public Vector(double x, double y, double z){
        this.x = x;
        this.y = y;
        this.z = z;
    }
    public Vector(Vector p){
        this.x = p.x;
        this.y = p.y;
        this.z = p.z;
    }

    public double sqrDist(Vector v){
        double dx = v.x - x;
        double dy = v.y - y;
        double dz = v.z - z;
        return dx*dx + dy*dy + dz*dz;
    }

    public Vector assign(Vector v){
        x = v.x;
        y = v.y;
        z = v.z;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Vector)) return false;
        Vector v = (Vector) o;
        return x == v.x && y == v.y && z == v.z;
    }

    @Override
    public int hashCode() {
        return 31 * Double.valueOf(x).hashCode() + Double.valueOf(y).hashCode() + 57 * Double.valueOf(z).hashCode();
    }
}
