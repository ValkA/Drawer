#ifndef DRAWERS_H
#define DRAWERS_H

#include "Vector.h"
#include "Pen.h"

void drawRect(const Vector& center, double width, double height){
    double poolar[2] = {0,0};
    Vector corners[4] = {
        Vector(center.x-width/2.0, center.y+height/2.0),
        Vector(center.x+width/2.0, center.y+height/2.0),
        Vector(center.x+width/2.0, center.y-height/2.0),
        Vector(center.x-width/2.0, center.y-height/2.0)
    };
    for(int i=0; i<4; i++){
        Vector& curr = corners[i];
        Vector& next = corners[(i+1)%4];
        for(int j=0; j<10 || (i==4&&j==10); ++j){
            penGoto(interpolate(curr, next, j/10.0));
        }
    }
    delay(50);
}

void drawCircle(const Vector& center, double radius){
    double poolar[2] = {0,0};
    for(int t=0; t<=360; t+=4){
        Vector r;
        r.setPolar(radius,PI*(t/180.0));
        penGoto(center+r);
    }
    delay(50);
}

void drawSpiral(const Vector& center, double maxRadius, int revs){
    double poolar[2] = {0,0};
    for(int t=0; t<=360*revs; t+=4){
        Vector r;
        r.setPolar(maxRadius*(t/((double)360*revs)),PI*(t/180.0));
        penGoto(center+r);
    }
    delay(50);
}
#endif
