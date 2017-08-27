#include "Vector.h"
#include "InverseKinematics.h"
#include "Drawers.h"



void setup() {
    Serial.begin(9600);

    initInverseKinematics();
    initPen();

    delay(1000);
}

char busyRead(Stream& stream){
    while(!stream.available()){}
    return stream.read();
}


#define BUFFER_SIZE 64
void parseLine(Stream& stream){
    char command[BUFFER_SIZE];
    int i=0;
    char* p = command;
    *p = busyRead(stream);
    while(*p == ' ') *p = busyRead(stream); //skip beginning spaces
    bool comment_mode = false;
    while(*p != '\n'){
        if(*p == ';') comment_mode = true;//exclude comments
        if(!comment_mode && *p != '\r'){ //accept char that is not '/r'
            ++p;
            ++i;
            if(i==BUFFER_SIZE){
                //flush this line... too big for us...
                Serial.println("Too big command");
                *p = busyRead(stream);
                while(*p != '\n'){
                    *p = busyRead(stream);
                }
                return;
            }
        }
        *p = busyRead(stream);
    }
    *p = 0;

    if(command[0] != 'G' && (command[1] != '1' || command[1] != '0')){
        Serial.println("non G command");
        return;
    }

    char* X = strchr(command, 'X');
    char* Y = strchr(command, 'Y');
    char* Z = strchr(command, 'Z');

    Vector newPos = currentPos;
    double newZ = currentZ;
    if(X) newPos.x = atof(X+1);
    if(Y) newPos.y = atof(Y+1);
    if(Z) newZ = atof(Z+1);

    currentZ = newZ; //TODO: figure out how to handle "z"
    penGotoInterpolated(newPos);

    stream.println("ok");

}

void loop() {
    // parseLine(Serial);

    Vector center(70,70);
    for(double i=0.0; i<=5; i+=1.0){
        drawRect(center,i,i);
    }
    for(double i=0.0; i<=30; i+=10){
        drawCircle(center,i);
    }
    drawSpiral(center, 30, 10);
}
