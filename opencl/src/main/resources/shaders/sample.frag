#version 410

in vec4 v_coords;

uniform mediump sampler2D u_texture;

out vec4 outColor;

void main() {
  vec2 texcoords = (v_coords.xy / 2.0) + 0.5;
  vec4 val = texture(u_texture, texcoords);
  outColor = vec4(sqrt(val.z) / 10.0, 1.0 - val.w, val.w, 1.0);
}