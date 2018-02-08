//
// Created by Jack Coughlin on 2/7/18.
//

#ifndef FLUIDS_UTILS_H
#define FLUIDS_UTILS_H

float **initArray(int nx, int ny);
void releaseArray(float **array, int nx);

float dot(float **a, float **b, int nx, int ny);

#endif //FLUIDS_UTILS_H
