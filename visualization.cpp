//
// Created by Jack Coughlin on 2/7/18.
//

#include "visualization.h"
#include <OpenGL/gl.h>
#include <OpenGL/gl3.h>
#include <OpenGL/glu.h>
#include <OpenGL/glext.h>
#include <OpenGL/gl3ext.h>
#include <cstdlib>
#include <cstdio>
#include <iostream>

using namespace std;

void checkShaderStatus(GLuint shader);

void checkProgramStatus(GLuint program);

void messageCallback(GLenum source,
                     GLenum type,
                     GLuint id,
                     GLenum severity,
                     GLsizei length,
                     const GLchar *message,
                     const void *userParam);

int initializeShaderProgram() {
    float vertices[] = {
            0.0f, 0.5f, // Vertex 1 (X, Y)
            0.5f, -0.5f, // Vertex 2 (X, Y)
            -0.5f, -0.5f  // Vertex 3 (X, Y)
    };

    GLuint vbo;
    glGenBuffers(1, &vbo); // Generate 1 buffer
    glBindBuffer(GL_ARRAY_BUFFER, vbo);
    glBufferData(GL_ARRAY_BUFFER, sizeof(vertices), vertices, GL_STATIC_DRAW);

    GLuint vao;
    glGenVertexArrays(1, &vao);
    glBindVertexArray(vao);

    const char *vertexShaderSource = R"glsl(
        #version 150 core

        in vec2 position;

        void main() {
            gl_Position = vec4(position.x, position.y, 0.0, 1.0);
        }
    )glsl";
    GLuint vertexShader = glCreateShader(GL_VERTEX_SHADER);
    glShaderSource(vertexShader, 1, &vertexShaderSource, NULL);
    glCompileShader(vertexShader);
    checkShaderStatus(vertexShader);

    const char *fragmentShaderSource = R"glsl(
        #version 150 core

        out vec4 outColor;

        void main() {
            outColor = vec4(1.0, 1.0, 1.0, 1.0);
        }
    )glsl";
    GLuint fragmentShader = glCreateShader(GL_FRAGMENT_SHADER);
    glShaderSource(fragmentShader, 1, &fragmentShaderSource, NULL);
    glCompileShader(fragmentShader);
    checkShaderStatus(fragmentShader);

    GLuint shaderProgram = glCreateProgram();
    glAttachShader(shaderProgram, vertexShader);
    glAttachShader(shaderProgram, fragmentShader);

    glBindFragDataLocationEXT(shaderProgram, 0, "outColor");

    glLinkProgram(shaderProgram);
    glUseProgram(shaderProgram);
    glValidateProgram(shaderProgram);

    checkProgramStatus(shaderProgram);

    GLint posAttrib = glGetAttribLocation(shaderProgram, "position");
    glVertexAttribPointer(posAttrib, 2, GL_FLOAT, GL_FALSE, 0, 0);
    glEnableVertexAttribArray(posAttrib);

    glPointSize(10.0);
    glDrawArrays(GL_POINTS, 0, 3);

    checkProgramStatus(shaderProgram);

    return 0;
}

void checkShaderStatus(GLuint shader) {
    GLint status;
    glGetShaderiv(shader, GL_COMPILE_STATUS, &status);
    char buffer[512];
    glGetShaderInfoLog(shader, 512, NULL, buffer);
    cout << buffer;
}

void checkProgramStatus(GLuint program) {
    char buffer[512];
    GLsizei length;
    glGetProgramInfoLog(program, 512, &length, buffer);
    cout << buffer;
}

void checkGLStatus() {
    GLuint status = glGetError();
    if (status != GL_NO_ERROR) {

    }
}

void messageCallback(GLenum source,
                     GLenum type,
                     GLuint id,
                     GLenum severity,
                     GLsizei length,
                     const GLchar *message,
                     const void *userParam) {
    fprintf(stderr, "GL CALLBACK: %s type = 0x%x, severity = 0x%x, message = %s\n",
            type, severity, message);
}
