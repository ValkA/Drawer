#ifndef VECTOR_H
#define VECTOR_H

#define PI 3.14159265359

class Vector{
public:
    double x,y;
    Vector() : x(0), y(0) {};
    Vector(const Vector& v) : x(v.x), y(v.y) {};
    Vector(Vector& v) : x(v.x), y(v.y) {};
    Vector(double x, double y) : x(x), y(y) {};
    void setPolar(double r, double t){
        this->x = r*cos(t);
        this->y = r*sin(t);
    }
    double len() const{
        return sqrt(this->x*this->x + this->y*this->y);
    }
    // void print(char* txt=nullptr){
    //     if(txt != nullptr) Serial.print(txt);
    //     Serial.print(" [");
    //     Serial.print(x);
    //     Serial.print(",");
    //     Serial.print(y);
    //     Serial.print("] len=");
    //     Serial.println(this->len());
    // }
};

const Vector operator-(const Vector& v1, const Vector& v2){
    return Vector(v1.x-v2.x, v1.y-v2.y);
}

const Vector operator+(const Vector& v1, const Vector& v2){
    return Vector(v1.x+v2.x, v1.y+v2.y);
}

double dot(const Vector& v1, const Vector& v2){
    return v1.x*v2.x+v1.y*v2.y;
}

double theta(const Vector& v1, const Vector& v2){
    return acos(dot(v1,v2)/(v1.len()*v2.len()));
}

//angle that points on c on a,b,c trig
double C(double a, double c, double b){
    return acos((a*a+b*b-c*c)/(2.0*a*b));
}

double C(const Vector& v1, const Vector& v2, const Vector& v3){
    return C(v1.len(), v2.len(), v3.len());
}

//len of c when angle C points on it in a,b,c trig
double c(double a, double C, double b){
    return sqrt(a*a+b*b-2*a*b*cos(C));
}

//t=0 will return v1, t=1 will return v2
Vector interpolate(const Vector& v1, const Vector& v2, double t){
    return Vector(v1.x*(1-t)+v2.x*t, v1.y*(1-t)+v2.y*t);
}


#endif
