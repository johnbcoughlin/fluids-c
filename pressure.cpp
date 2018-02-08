//
// Created by Jack Coughlin on 2/7/18.
//

#include "Simulation.h"
#include <iostream>
#include <cmath>
#include "utils.h"

using namespace std;

void Simulation::correctPressure() {
    updateLaplacian();
    calculatePreconditioner();

    /* current guess for the pressure */
    float **p = initArray(nx, ny);
    /* residual vector */
    float **r = initArray(nx, ny);
    float **z = initArray(nx, ny);
    float **s = initArray(nx, ny);

    cout << "here";
    for (int i = 0; i < nx; i++) {
        for (int j = 0; j < ny; j++) {
            r[i][j] = divergence[i][j];
        }
    }

    applyPreconditioner(r, z);
    for (int i = 0; i < nx; i++) {
        for (int j = 0; j < nx; j++) {
            s[i][j] = z[i][j];
        }
    }

    float sigma = dot(r, z, nx, ny);
    for (int iteration = 0; iteration < 10; iteration++) {
        applyLaplacian(s, z);
        float alpha = 1.0f / dot(z, s, nx, ny);

        for (int i = 0; i < nx; i++) {
            for (int j = 0; j < nx; j++) {
                p[i][j] += s[i][j] * alpha;
                r[i][j] -= z[i][j] * alpha;
            }
        }

        cout << "residual:" << "\n";
        for (int i = 0; i < nx; i++) {
            cout << "[";
            for (int j = 0; j < nx; j++) {
                cout << s[i][j] << ",";
            }
            cout << "]\n,";
        }
        cout << "alpha: " << alpha;

        applyPreconditioner(r, z);
        sigma = dot(r, z, nx, ny);
        float beta = sigma;

        /* Set s = z + beta * s */
        for (int i = 0; i < nx; i++) {
            for (int j = 0; j < nx; j++) {
                s[i][j] = z[i][j] + beta * s[i][j];
            }
        }
    }

    releaseArray(z, nx);
}


void Simulation::updateLaplacian() {
    auto **water_or_air_mask = new short *[nx];
    for (int i = 0; i < nx; i++) {
        water_or_air_mask[i] = new short[ny];
        for (int j = 0; j < ny; j++) {
            water_or_air_mask[i][j] = water_mask[i][j] | air_mask[i][j];
        }
    }

    Adiag[0][0] = water_or_air_mask[0][1] + water_or_air_mask[1][0];
    Aplusi[0][0] = -water_mask[1][0];
    Aplusj[0][0] = -water_mask[0][1];
    for (int j = 1; j < ny - 1; j++) {
        Adiag[0][j] = water_or_air_mask[1][j] + water_or_air_mask[0][j - 1] + water_or_air_mask[0][j + 1];
        Aplusi[0][j] = -water_mask[1][j];
        Aplusj[0][j] = -water_mask[0][j + 1];
    }
    for (int i = 1; i < nx - 1; i++) {
        Adiag[i][0] = water_or_air_mask[i - 1][0] + water_or_air_mask[i + 1][0] + water_or_air_mask[i][1];
        Aplusi[i][0] = -water_mask[i + 1][0];
        Aplusj[i][0] = -water_mask[i][1];
        for (int j = 1; j < ny - 1; j++) {
            Adiag[i][j] = water_or_air_mask[i - 1][j] +
                          water_or_air_mask[i + 1][j] +
                          water_or_air_mask[i][j - 1] +
                          water_or_air_mask[i][j + 1];
            Aplusi[i][j] = -water_mask[i + 1][j];
            Aplusj[i][j] = -water_mask[i][j + 1];
        }
        Adiag[i][ny - 1] =
                water_or_air_mask[i - 1][ny - 1] + water_or_air_mask[i + 1][ny - 1] + water_or_air_mask[i][ny - 2];
        Aplusi[i][ny - 1] = -water_mask[i + 1][ny - 1];
        /* there are solids beyond the grid */
        Aplusj[i][ny - 1] = 0;
    }
    Adiag[nx - 1][0] = water_or_air_mask[nx - 2][0] + water_or_air_mask[nx - 1][1];
    Aplusi[nx - 1][0] = 0;
    Aplusj[nx - 1][0] = -water_mask[nx - 1][1];
    for (int j = 1; j < ny - 1; j++) {
        Adiag[nx - 1][j] =
                water_or_air_mask[nx - 2][j] + water_or_air_mask[nx - 1][j - 1] + water_or_air_mask[nx - 1][j + 1];
        Aplusi[nx - 1][j] = 0;
        Aplusj[nx - 1][j] = -water_mask[nx - 1][j + 1];
    }
    Adiag[nx - 1][ny - 1] = -water_mask[nx - 2][ny - 1] + -water_mask[nx - 1][ny - 2];
    /* there are solids beyond the grid */
    Aplusi[nx - 1][ny - 1] = 0;
    Aplusj[nx - 1][ny - 1] = 0;

    for (int i = 0; i < nx; i++) {
        delete[] water_or_air_mask[i];
    }
    delete[] water_or_air_mask;
}

void Simulation::calculatePreconditioner() {
    double tau = 0.97;
    double epsilon = 1.0e-10;
    int i = 1;
    int j = 1;
    double e = Adiag[i][j];
    precon[i][j] = water_mask[i][j] * 1.0 / sqrt(e + epsilon);
    for (j = 1; j < ny; j++) {
        e = Adiag[i][j] -
            (pow(Aplusj[i][j - 1] * precon[i][j - 1], 2) * water_mask[i][j - 1]) -
            tau * (
                    (Aplusj[i][j - 1] * (Aplusi[i][j - 1]) * precon[i][j - 1] * precon[i][j - 1]) *
                    water_mask[i][j - 1]
            );
        precon[i][j] = water_mask[i][j] * 1.0 / sqrt(e + epsilon);
    }
    for (i = 1; i < nx; i++) {
        j = 0;
        e = Adiag[i][j] -
            (pow(Aplusi[i - 1][j] * precon[i - 1][j], 2) * water_mask[i - 1][j]) -
            tau * (
                    (Aplusi[i - 1][j] * (Aplusj[i - 1][j]) * precon[i - 1][j] * precon[i - 1][j]) *
                    water_mask[i - 1][j]
            );
        precon[i][j] = water_mask[i][j] * 1.0 / sqrt(e + epsilon);
        for (j = 1; j < ny; j++) {
            e = Adiag[i][j] -
                (pow(Aplusi[i - 1][j] * precon[i - 1][j], 2) * water_mask[i - 1][j]) -
                (pow(Aplusj[i][j - 1] * precon[i][j - 1], 2) * water_mask[i - 1][j]) -
                tau * (
                        (Aplusi[i - 1][j] * (Aplusj[i - 1][j]) * precon[i - 1][j] * precon[i - 1][j]) *
                        water_mask[i - 1][j] +
                        (Aplusj[i][j - 1] * (Aplusi[i][j - 1]) * precon[i][j - 1] * precon[i][j - 1]) *
                        water_mask[i][j - 1]
                );
            precon[i][j] = water_mask[i][j] * 1.0 / sqrt(e + epsilon);
        }
    }
}

void Simulation::applyPreconditioner(float **r, float **z) {
    float **q = initArray(nx, ny);
    float t;

    /* First solve Lq = r */
    for (int i = 0; i < nx; i++) {
        for (int j = 0; j < ny; j++) {
            if (water_mask[i][j]) {
                t = r[i][j];
                if (i > 0) {
                    t -= Aplusi[i - 1][j] * q[i - 1][j];
                }
                if (j > 0) {
                    t -= Aplusj[i][j - 1] * q[i][j - 1];
                }
                q[i][j] = (float) (t * precon[i][j]);
            }
        }
    }

    /* Now solve L^Tz = q */
    for (int i = nx - 1; i >= 0; i--) {
        for (int j = ny - 1; j >= 0; j--) {
            if (water_mask[i][j]) {
                t = q[i][j];
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
