#version 410

in vec4 v_coords;

uniform mediump isampler2D u_texture;

out vec4 outColor;

void main() {
  vec2 texcoords = (v_coords.xy / 2.0) + 0.5;
  ivec4 val = texture(u_texture, texcoords);
  outColor = vec4(val.x / 2.0, 1.0, 0, 1.0);
}