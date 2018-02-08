cmake_minimum_required(VERSION 3.9)
project(fluids)

set(CMAKE_CXX_STANDARD 11)

find_library(SDL_LIBRARY SDL2)
find_package(OpenGL REQUIRED)

include_directories(${SDL2_INCLUDE_DIRS})

add_executable(fluids main.cpp visualization.cpp visualization.h Simulation.cpp Simulation.h simulation_vis.cpp advection.cpp pressure.cpp utils.cpp utils.h)

target_link_libraries(fluids ${SDL_LIBRARY})
target_link_libraries(fluids ${OPENGL_LIBRARY})