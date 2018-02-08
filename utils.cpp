//
// Created by Jack Coughlin on 2/7/18.
//

#include "utils.h"

float **initArray(int nx, int ny) {
    float **result = new float *[nx];
    for (int i = 0; i < nx; i++) {
        result[i] = new float[ny];
        for (int j = 0; j < ny; j++) {
            result[i][j] = 0;
        }
    }
    return result;
}

void releaseArray(float **array, int nx) {
    for (int i = 0; i < nx; i++) {
        delete[] array[i];
    }
    delete[] array;
}

float dot(float **a, float **b, int nx, int ny) {
    float result = 0.0;
    for (int i = 0; i < nx; i++) {
        for (int j = 0; j < ny; j++) {
            result += a[i][j] * b[i][j];
        }
    }
    return result;
}
