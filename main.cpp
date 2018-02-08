#include <iostream>
#include <SDL2/SDL.h>

#include "visualization.h"
#include "Simulation.h"
#include <thread>

using namespace std;

int runUI();
int runSimulation();
int loop(SDL_Window *window);

int main() {
    std::thread simulationThread(runSimulation);
    //runUI();
    simulationThread.join();
    return 0;
}

int runSimulation() {
    Simulation *sim = new Simulation(40, 40, 1.0, 1.0, 0.01);
    sim->step();
    sim->show();
    sim->cleanup();

    return 0;
}

int runUI() {
    SDL_Init(SDL_INIT_VIDEO);
    SDL_GL_SetAttribute(SDL_GL_CONTEXT_PROFILE_MASK, SDL_GL_CONTEXT_PROFILE_CORE);
    SDL_GL_SetAttribute(SDL_GL_CONTEXT_MAJOR_VERSION, 3);
    SDL_GL_SetAttribute(SDL_GL_CONTEXT_MINOR_VERSION, 2);
    SDL_GL_SetAttribute(SDL_GL_STENCIL_SIZE, 8);
    SDL_Window *window = SDL_CreateWindow("OpenGL", 100, 100, 800, 600, SDL_WINDOW_OPENGL);

    SDL_GLContext context = SDL_GL_CreateContext(window);

    initializeShaderProgram();
    loop(window);

    SDL_GL_DeleteContext(context);
    SDL_Quit();

    return 0;
}

int loop(SDL_Window *window) {
    SDL_Event windowEvent;
    while (true) {
        if (SDL_PollEvent(&windowEvent)) {
            if (windowEvent.type == SDL_QUIT) break;
        }

        SDL_GL_SwapWindow(window);
    }
    return 0;
}