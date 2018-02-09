//
// Created by Jack Coughlin on 2/7/18.
//

#include "Simulation.h"
#include "utils.h"
#include <cmath>

void Simulation::advectU_x() {
    /* first figure out the preimages of each u_x grid point. */
    float **preimages = initArray<float>((nx + 1) * ny, 2);
    for (int i = 0; i < nx + 1; i++) {
        for (int j = 0; j < ny; j++) {
            float u_xij = u_x[i][j];
            float u_yij;
            if (i == 0) {
                u_yij = (u_y[i][j] + u_y[i][j + 1]) / 2;
            } else if (i == nx) {
                u_yij = (u_y[i - 1][j] + u_y[i - 1][j + 1]) / 2;
            } else {
                u_yij = (u_y[i][j] + u_y[i][j + 1] + u_y[i - 1][j] + u_y[i - 1][j + 1]) / 4;
            }
            float preimage_x = dx * (i - 0.5f) - u_xij * dt;
            float preimage_y = dy * (j) - u_yij * dt;
            preimages[i * ny + j] = new float[2] {preimage_x, preimage_y};
        }
    }

    /* Now interpolate the value of u_x at each preimage point and write it back to u_x */
    for (int i = 0; i < nx + 1; i++) {
        for (int j = 0; j < ny; j++) {
            float *preimage = preimages[i*ny+j];
            float x = preimage[0];
            float y = preimage[1];
            auto i1 = (int) floorf((x + 0.5f) / dx);
            float x1 = dx * (i1 - 0.5f);
            int i2 = i1 + 1;
            float x2 = dx * (i2 - 0.5f);
            auto j1 = (int) floorf(y / dy);
            float y1 = dy * j1;
            int j2 = j1 + 1;
            float y2 = dy * j2;

            float u_xxy;
            // we are off in the direction of a corner. use the speed at that corner.
            if (i1 < 0 && j1 < 0) {
                u_xxy = u_x[0][0];
            } else if (i1 < 0 && j2 > ny-1) {
                u_xxy = u_x[0][ny-1];
            } else if (i2 > nx && j1 < 0) {
                u_xxy = u_x[nx][0];
            } else if (i2 > nx && j2 > ny-1) {
                u_xxy = u_x[nx][ny-1];
            }

            // we are off an edge. use a weighted average of the 2 nearest grid points
            if (i1 < 0) {
                u_xxy = ((y2 - y) / dy) * u_x[0][j1] + ((y - y1) / dy) * u_x[0][j2];
            }
        }
    }

}

void Simulation::advectU_y() {

}

void Simulation::advectParticles() {

}
