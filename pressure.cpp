//
// Created by Jack Coughlin on 2/7/18.
//

#include "Simulation.h"
#include <iostream>
#include <cmath>
#include "utils.h"
#include <time.h>

using namespace std;

void Simulation::correctPressure() {
    clock_t t = clock();

    updateLaplacian();
    calculatePreconditioner();

    /* current guess for the pressure */
    float **p = initArray<float>(nx, ny);
    /* residual vector */
    float **r = initArray<float>(nx, ny);
    float **z = initArray<float>(nx, ny);
    float **s = initArray<float>(nx, ny);

    for (int i = 0; i < nx; i++) {
        for (int j = 0; j < ny; j++) {
            r[i][j] = divergence[i][j] * (dx * dx / dt);
        }
    }

    applyPreconditioner(r, z);
    for (int i = 0; i < nx; i++) {
        for (int j = 0; j < nx; j++) {
            s[i][j] = z[i][j];
        }
    }

    float sigma = dot(r, z, nx, ny);

    clock_t iter_t = clock();
    for (int iteration = 0; iteration < 100; iteration++) {
        applyLaplacian(s, z);
        float alpha = sigma / dot(z, s, nx, ny);

        for (int i = 0; i < nx; i++) {
            for (int j = 0; j < nx; j++) {
                p[i][j] += s[i][j] * alpha;
                r[i][j] -= z[i][j] * alpha;
            }
        }

        float max_residual = 0;
        for (int i = 0; i < nx; i++) {
            for (int j = 0; j < ny; j++) {
                if (abs(r[i][j]) > max_residual) {
                    max_residual = abs(r[i][j]);
                }
            }
        }
        if (max_residual < 1.0e-15) {
            cout << "breaking after " << iteration << "iterations.\n";
            break;
        }

        applyPreconditioner(r, z);

        float sigma_new = dot(r, z, nx, ny);
        float beta = sigma_new / sigma;
        sigma = sigma_new;

        /* Set s = z + beta * s */
        for (int i = 0; i < nx; i++) {
            for (int j = 0; j < nx; j++) {
                s[i][j] = z[i][j] + beta * s[i][j];
            }
        }
    }
    iter_t = clock() - iter_t;
    printf("Pressure correction iterations: %dus\n", (int) iter_t);

    releaseArray(r, nx);
    releaseArray(z, nx);
    releaseArray(s, nx);

    releaseArray(this->p, nx);

    t = clock() - t;
    printf("Pressure correction total: %d\n", (int) t);
    this->p = p;
}


void Simulation::updateLaplacian() {
    auto **water_or_air_mask = new short *[nx];
    for (int i = 0; i < nx; i++) {
        water_or_air_mask[i] = new short[ny];
        for (int j = 0; j < ny; j++) {
            water_or_air_mask[i][j] = water_mask[i][j] | air_mask[i][j];
        }
    }

    for (int i = 0; i < nx; i++) {
        for (int j = 0; j < ny; j++) {
            if (water_mask[i][j] != 1) {
                Adiag[i][j] = 0;
                Aplusi[i][j] = 0;
                Aplusj[i][j] = 0;
                continue;
            }
            if (i > 0) {
                Adiag[i][j] += water_or_air_mask[i - 1][j];
            }
            if (j > 0) {
                Adiag[i][j] += water_or_air_mask[i][j - 1];
            }
            if (i < nx - 1) {
                Adiag[i][j] += water_or_air_mask[i + 1][j];
                Aplusi[i][j] = -water_mask[i + 1][j];
            }
            if (j < ny - 1) {
                Adiag[i][j] += water_or_air_mask[i][j + 1];
                Aplusj[i][j] = -water_mask[i][j + 1];
            }
        }
    }

    for (int i = 0; i < nx; i++) {
        delete[] water_or_air_mask[i];
    }
    delete[] water_or_air_mask;
}

void Simulation::calculatePreconditioner() {
    double tau = 0.97;
    double epsilon = 1.0e-30;

    for (int i = 0; i < nx; i++) {
        for (int j = 0; j < ny; j++) {
            double e = Adiag[i][j];
            if (i > 0) {
                e -= (pow(Aplusi[i - 1][j] * precon[i - 1][j], 2) +
                      tau * (Aplusi[i - 1][j] * (Aplusj[i - 1][j]) * precon[i - 1][j] * precon[i - 1][j]));
            }
            if (j > 0) {
                e -= (pow(Aplusj[i][j - 1] * precon[i][j - 1], 2) +
                      tau * (Aplusj[i][j - 1] * (Aplusi[i][j - 1]) * precon[i][j - 1] * precon[i][j - 1]));
            }
            if (water_mask[i][j] != 1 && e != 0.0) {
                cout << "is not water but e=" << e;
            }
            precon[i][j] = water_mask[i][j] ? 1.0 / sqrt(e + epsilon) : 0.0;
        }
    }
}

void Simulation::applyPreconditioner(float **r, float **z) {
    float **q = initArray<float>(nx, ny);

    /* First solve Lq = r */
    for (int i = 0; i < nx; i++) {
        for (int j = 0; j < ny; j++) {
            if (water_mask[i][j]) {
                float t = r[i][j];
                if (i > 0) {
                    t -= Aplusi[i - 1][j] * precon[i-1][j] * q[i - 1][j];
                }
                if (j > 0) {
                    t -= Aplusj[i][j - 1] * precon[i][j-1] * q[i][j - 1];
                }
                q[i][j] = (float) (t * precon[i][j]);
            }
        }
    }

    /* Now solve L^Tz = q */
    for (int i = nx - 1; i >= 0; i--) {
        for (int j = ny - 1; j >= 0; j--) {
            if (water_mask[i][j]) {
                float t = q[i][j];
                if (i < nx - 1) {
                    t -= Aplusi[i][j] * precon[i][j] * z[i + 1][j];
                }
                if (j < ny - 1) {
                    t -= Aplusj[i][j] * precon[i][j] * z[i][j + 1];
                }
                z[i][j] = (float) (t * precon[i][j]);
            }
        }
    }

    for (int i = 0; i < nx; i++) {
        delete[] q[i];
    }
    delete[] q;
}


void Simulation::applyLaplacian(float **s, float **dest) {
    for (int i = 0; i < nx; i++) {
        for (int j = 0; j < ny; j++) {
            if (water_mask[i][j] != 1) {
                dest[i][j] = 0;
                continue;
            }
            dest[i][j] = Adiag[i][j] * s[i][j];
            if (i > 0) {
                dest[i][j] += Aplusi[i - 1][j] * s[i - 1][j];
            }
            if (i < nx - 1) {
                dest[i][j] += Aplusi[i][j] * s[i + 1][j];
            }
            if (j > 0) {
                dest[i][j] += Aplusj[i][j - 1] * s[i][j - 1];
            }
            if (j < ny - 1) {
                dest[i][j] += Aplusj[i][j] * s[i][j + 1];
            }
        }
    }
}
