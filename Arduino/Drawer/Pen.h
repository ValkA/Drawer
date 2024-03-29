#ifndef PEN_H
#define PEN_H

#include "Config.h"
#include "Vector.h"
#include "InverseKinematics.h"
#include <AccelStepper.h>
#include <MultiStepper.h>
#include <Servo.h>


AccelStepper leftStepper(8, LEFT_MOTOR_1_PIN, LEFT_MOTOR_3_PIN, LEFT_MOTOR_2_PIN, LEFT_MOTOR_4_PIN);
AccelStepper rightStepper(8, RIGHT_MOTOR_2_PIN, RIGHT_MOTOR_4_PIN, RIGHT_MOTOR_1_PIN, RIGHT_MOTOR_3_PIN);
MultiStepper steppers;
Servo penServo;

Vector currentPos;
double currentZ = 0;


long toSteps(double rads){
    return (long)((double)STEPS_PER_TURN)*(rads/(2.0*PI));
}

void initPen(){
    leftStepper.setMaxSpeed(500);
    rightStepper.setMaxSpeed(500);

    leftStepper.setCurrentPosition(toSteps(PI));
    rightStepper.setCurrentPosition(toSteps(PI));

    steppers.addStepper(leftStepper);
    steppers.addStepper(rightStepper);

    penServo.attach(PEN_SERVO_PIN);
}

inline void _penGoto(const Vector& cartesian){
    double polar[2];
    ik(cartesian, polar);
    long steps[2] = {toSteps(polar[0]),toSteps(polar[1])};
    steppers.moveTo(steps);
    steppers.runSpeedToPosition();
    currentPos = cartesian;
}

//move pen to p (centimeters)
void penGotoInterpolated(const Vector& cartesian, double z){
    double dist = (currentPos-cartesian).len();
    int segments = dist/10;//each 10mm will be a segment
    if(segments<1) segments = 1;

    for(int i=1; i<=segments; ++i){
        _penGoto(interpolate(currentPos, cartesian, ((double)i)/segments));
    }
    currentPos = cartesian;

    z = constrain(z, 0, 1);
    if(z != currentZ){
        //transforms z=[0,1] to [PEN_SERVO_DOWN, PEN_SERVO_UP]
        penServo.write((PEN_SERVO_UP-PEN_SERVO_DOWN)*z+PEN_SERVO_DOWN);
        delay(500);
    }
    currentZ = z;
}

void penGoto(const Vector& cartesian){
    _penGoto(cartesian);
    currentPos = cartesian;
}

#endif
