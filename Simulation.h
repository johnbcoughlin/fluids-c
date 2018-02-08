//
// Created by Jack Coughlin on 2/7/18.
//

#ifndef FLUIDS_SIMULATION_H
#define FLUIDS_SIMULATION_H

class Simulation {
    int nx;
    int ny;
    float dx;
    float dy;
    float dt;
    float rdx;
    float **p;
    float **u_x;
    float **u_y;

    /* An array of shape (nm, 2) containing xy coordinates
     * of the particles we are tracking
     */
    float **particles;

    /* Arrays of 0/1 to track where water, solid, and air are. */
    short **water_mask;
    short **air_mask;
    short **solid_mask;

    /* Array containing the divergence of each fluid cell. */
    float **divergence;

    short **Adiag;
    short **Aplusi;
    short **Aplusj;

    double **precon;

public:
    Simulation(int nx, int ny, float dx, float dy, float dt);
    void step();
    void cleanup();

    void show();

private:
    void allocateArrays();

    void applyBodyForces();
    void computeDivergence();
    void correctPressure();

    void advectU_x();
    void advectU_y();
    void advectParticles();

    void updateLaplacian();
    void calculatePreconditioner();
    void applyPreconditioner(float **r, float **z);
    void applyLaplacian(float **s, float **dest);
};

#endif //FLUIDS_SIMULATION_H
