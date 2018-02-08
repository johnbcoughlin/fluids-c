//
// Created by Jack Coughlin on 2/7/18.
//

#include "Simulation.h"
#include <iostream>

using namespace std;

void Simulation::show() {
    cout << "[\n";
    for (int i=0; i < nx; i++) {
        cout << "\t[";
        for (int j=0; j < ny; j++) {
            cout << precon[i][j] << ",";
        }
        cout << "],\n";
    }
    cout << "]";
}

