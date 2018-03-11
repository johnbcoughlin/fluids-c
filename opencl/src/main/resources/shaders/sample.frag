#version 410

in vec4 v_coords;

uniform mediump sampler2D u_texture;

out vec4 outColor;

void main() {
  vec2 texcoords = (v_coords.xy / 2.0) + 0.5;
  vec3 val = texture(u_texture, texcoords).xyz;
  outColor = vec4(texture(u_texture, texcoords).xyz * 100.0, 1.0);
}