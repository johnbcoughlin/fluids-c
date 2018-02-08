//
// Created by Jack Coughlin on 2/7/18.
//

#include "Simulation.h"

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
    u_x = new float *[nx+1];
    for (int i = 0; i < nx+1; i++) {
        u_x[i] = new float[ny];
    }

    u_y = new float *[nx];
    for (int i = 0; i < nx; i++) {
        u_y[i] = new float[ny+1];
    }

    p = new float *[nx];
    solid_mask = new short *[nx];
    air_mask = new short *[nx];
    water_mask = new short *[nx];
    divergence = new float *[nx];
    Adiag = new short *[nx];
    Aplusi = new short *[nx];
    Aplusj = new short *[nx];
    precon = new double *[nx];
    for (int i = 0; i < nx; i++) {
        p[i] = new float[ny];
        solid_mask[i] = new short[ny];
        air_mask[i] = new short[ny];
        water_mask[i] = new short[ny];
        divergence[i] = new float[ny];
        Adiag[i] = new short[ny];
        Aplusi[i] = new short[ny];
        Aplusj[i] = new short[ny];
        precon[i] = new double[ny];
    }
    for (int i = 0; i < nx; i++) {
        for (int j = 0; j < ny; j++) {
            p[i][j] = 0;
            solid_mask[i][j] = 0;
            air_mask[i][j] = 0;
            water_mask[i][j] = 0;
            divergence[i][j] = 0;
            Adiag[i][j] = 0;
            Aplusi[i][j] = 0;
            Aplusj[i][j] = 0;
            precon[i][j] = 0;
            if (i == 0 || j == ny-1 || i == nx-1) {
                solid_mask[i][j] = 1;
            } else if (j == 0) {
                air_mask[i][j] = 1;
            } else {
                water_mask[i][j] = 1;
            }
        }
    }

    particles = new float*[nx*ny];
    for (int i = 0; i < nx; i++) {
        for (int j = 0; j < ny; j++) {
            particles[i*ny+j] = new float[2] {(float) j * dx, (float) i * dy};
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
            divergence[i][j] = rdx * (u_x[i+1][j] - u_x[i][j] + u_y[i][j+1] - u_y[i][j]);
        }
    }
}
