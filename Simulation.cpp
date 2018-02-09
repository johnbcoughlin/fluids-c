//
// Created by Jack Coughlin on 2/7/18.
//

#include "Simulation.h"
#include "utils.h"

Simulation::Simulation(int nx, int ny, float dx, float dy, float dt) {
    this->nx = nx;
    this->ny = ny;
    this->dx = dx;
    this->rdx = 1.0 / dx;
    this->dy = dy;
    this->dt = dt;
    allocateArrays();
}

void Simulation::cleanup() {
    for (int i = 0; i < nx; i++) {
        delete[] p[i];
        delete[] u_y[i];
        delete[] solid_mask[i];
        delete[] air_mask[i];
        delete[] water_mask[i];
        delete[] divergence[i];
        delete[] Adiag[i];
        delete[] Aplusi[i];
        delete[] Aplusj[i];
        delete[] precon[i];
    }
    delete[] p;
    delete[] u_y;
    delete[] solid_mask;
    delete[] air_mask;
    delete[] water_mask;
    delete[] divergence;
    delete[] Adiag;
    delete[] Aplusi;
    delete[] Aplusj;
    delete[] precon;

    for (int i = 0; i < nx + 1; i++) {
        delete[] u_x[i];
    }
    delete[] u_x;
}

void Simulation::allocateArrays() {
    u_x = initArray<float>(nx+1, ny);
    u_y = initArray<float>(nx, ny+1);

    p = initArray<float>(nx, ny);
    solid_mask = initArray<short>(nx, ny);
    air_mask = initArray<short>(nx, ny);
    water_mask = initArray<short>(nx, ny);
    divergence = initArray<float>(nx, ny);
    Adiag = initArray<short>(nx, ny);
    Aplusi = initArray<short>(nx, ny);
    Aplusj = initArray<short>(nx, ny);
    precon = initArray<double>(nx, ny);

    for (int i = 0; i < nx; i++) {
        for (int j = 0; j < ny; j++) {
            if (i == 0 || j == ny-1 || i == nx-1) {
                solid_mask[i][j] = 1;
            } else if (j == 0) {
                air_mask[i][j] = 1;
            } else {
                water_mask[i][j] = 1;
            }
        }
    }

    particles = initArray<float>(nx*ny, 2);
    for (int i = 0; i < nx; i++) {
        for (int j = 0; j < ny; j++) {
            particles[i*ny+j][0] = i * dx;
            particles[i*ny+j][1] = j * dy;
        }
    }
}

void Simulation::step() {
    applyBodyForces();
    computeDivergence();
    correctPressure();
    advectParticles();
    advectU_x();
    advectU_y();
}

void Simulation::applyBodyForces() {
    for (int i = 1; i < nx; i++) {
        for (int j = 0; j < ny; j++) {
            if (water_mask[i][j-1] == 1 && water_mask[i][j] == 1) {
                u_y[i][j] -= 9.8 * dt;
            }
        }
    }
}

void Simulation::computeDivergence() {
    for (int i = 0; i < nx; i++) {
        for (int j = 0; j < ny; j++) {
            if (water_mask[i][j]) {
                divergence[i][j] = rdx * (u_x[i + 1][j] - u_x[i][j] + u_y[i][j + 1] - u_y[i][j]);
            } else {
                divergence[i][j] = 0;
            }
        }
    }
}
