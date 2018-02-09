//
// Created by Jack Coughlin on 2/7/18.
//

#ifndef FLUIDS_UTILS_H
#define FLUIDS_UTILS_H

template <class T> T **initArray(int nx, int ny) {
    auto **result = new T *[nx];
    for (int i = 0; i < nx; i++) {
        result[i] = new T[ny];
        for (int j = 0; j < ny; j++) {
            result[i][j] = 0;
        }
    }
    return result;
}

template <class T> void releaseArray(T **array, int nx) {
    for (int i = 0; i < nx; i++) {
        delete[] array[i];
    }
    delete[] array;
}

float dot(float **a, float **b, int nx, int ny);

#endif //FLUIDS_UTILS_H
