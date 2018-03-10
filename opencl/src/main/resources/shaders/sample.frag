#version 410

in vec4 v_coords;

uniform sampler2D u_texture;

out vec4 outColor;

void main() {
  vec2 texcoords = (v_coords.xy / 2.0) + 0.5;
  outColor = texture(u_texture, v_coords);
}