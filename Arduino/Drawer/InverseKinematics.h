#ifndef INVERSEKINEMATICS_H
#define INVERSEKINEMATICS_H

#include "Vector.h"

const Vector m1(65,145+20);
const Vector m2(85,145+20);
const double R1 = 70;
const double R2 = 100;
const double R3 = 20;
const double alpha = 135.0*PI/180.0;
double R2R3;
double beta;

void initInverseKinematics(){
    R2R3 = c(R2,alpha,R3);
    beta = C(R3,R2,R2R3);
}

/**
 * solves the ik problem for the following arm:
 *   m1 *__    __* m2
 *      |t0)  (t1|
 * R1 { |        | } R1
 *      *        *
 *       \      /
 *  R2 {  \   / } R2
 *         \/
 *         *
 *     R3  |
 *         p
 * @param p - the point int cartesian field
 * @param t - array {t0,t1} will be filled with the point in joints field
 */
void ik(const Vector& p, double* t){
    Vector pm2(m2-p);
    Vector pm1(m1-p);
    Vector m1m2(m2-m1);
    // pm2.print("pm2");
    // pm1.print("pm1");
    // m1m2.print("m1m2");
    t[0] = C(pm1.len(), R2R3, R1) + C(pm1, pm2, m1m2); // the angle between m1m2 and motor1's r1
    // Serial.print("t[0]=");
    // Serial.println(t[0]);
    Vector r1;//of the left side
    r1.setPolar(R1, -t[0]);
    // r1.print("r1");
    Vector j1(m1+r1);
    // j1.print("j1");
    Vector r3;
    r3.setPolar(R3, theta((j1-p),Vector(1,0))-beta);
    // r3.print("r3");
    Vector o(p+r3);
    // o.print("o");
    Vector om2(m2-o);
    Vector om1(m1-o);
    t[1] = C(om2, om1, m1m2) + C(R1, R2, om2.len());

    // Serial.print(p.x);
    // Serial.print(",");
    // Serial.print(p.y);
    // Serial.print("=>");
    // Serial.print(t[0]);
    // Serial.print(",");
    // Serial.println(t[1]);
}


/**
 * solves the ik problem for the following arm:
 *   m1 *__    __* m2
 *      |t0)  (t1|
 * R1 { |        | } R1
 *      *        *
 *       \      /
 *  R2 {  \   / } R2
 *         \/
 *         p
 * @param p - the point int cartesian field
 * @param t - array {t0,t1} will be filled with the point in joints field
 */
void ik_cheap(const Vector& p, double* t){
    Vector pm2(m2-p);
    Vector pm1(m1-p);
    Vector m1m2(m2-m1);
    // pm2.print("pm2");
    // pm1.print("pm1");
    // m1m2.print("m1m2");
    t[0] = C(pm1.len(), R2, R1) + C(pm1, pm2, m1m2);
    t[1] = C(pm2, pm1, m1m2) + C(R1, R2, pm2.len());
        // Serial.print(p.x);
        // Serial.print(",");
        // Serial.print(p.y);
        // Serial.print("=>");
        // Serial.print(t[0]);
        // Serial.print(",");
        // Serial.println(t[1]);
}

#endif
