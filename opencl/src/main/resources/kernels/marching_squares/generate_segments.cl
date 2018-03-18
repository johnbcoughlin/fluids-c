__constant sampler_t sampler3 =
    CLK_NORMALIZED_COORDS_FALSE |
    CLK_ADDRESS_CLAMP |
    CLK_FILTER_NEAREST;

__constant int2 A = {0, 0};
__constant int2 B = {1, 0};
__constant int2 C = {0, 1};
__constant int2 D = {1, 1};

void __kernel generate_segments__5(
    int levelCount,
    __global float* vertex_buffer,
    __read_only image2d_t phi,
    __read_only image2d_t hp_0,
    __read_only image2d_t hp_1,
    __read_only image2d_t hp_2,
    __read_only image2d_t hp_3,
    __read_only image2d_t hp_4
) {
    int i = get_global_id(0);

    int quad_lower_bound = 0;
    int next_quad_lower_bound = quad_lower_bound;
    int2 pos4 = {0, 0};
    int2 pos3 = pos4 * 2;

    int a = read_imagei(hp_3, sampler3, pos3 + A).x;
    int b = read_imagei(hp_3, sampler3, pos3 + B).x;
    int c = read_imagei(hp_3, sampler3, pos3 + C).x;
    int d = read_imagei(hp_3, sampler3, pos3 + D).x;
    if (quad_lower_bound + a > i) {
        pos3 = pos3 + A;
    } else if (quad_lower_bound + a + b > i) {
        quad_lower_bound = quad_lower_bound + a;
        pos3 = pos3 + B;
    } else if (quad_lower_bound + a + b + c > i) {
        quad_lower_bound = quad_lower_bound + a + b;
        pos3 = pos3 + C;
    } else {
        quad_lower_bound = quad_lower_bound + a + b + c;
        pos3 = pos3 + D;
    }

    int2 pos2 = pos3 * 2;
    a = read_imagei(hp_2, sampler3, pos2 + A).x;
    b = read_imagei(hp_2, sampler3, pos2 + B).x;
    c = read_imagei(hp_2, sampler3, pos2 + C).x;
    d = read_imagei(hp_2, sampler3, pos2 + D).x;
    if (quad_lower_bound + a > i) {
        pos2 = pos2 + A;
    } else if (quad_lower_bound + a + b > i) {
        quad_lower_bound = quad_lower_bound + a;
        pos2 = pos2 + B;
    } else if (quad_lower_bound + a + b + c > i) {
        quad_lower_bound = quad_lower_bound + a + b;
        pos2 = pos2 + C;
    } else {
        quad_lower_bound = quad_lower_bound + a + b + c;
        pos2 = pos2 + D;
    }

    int2 pos1 = pos2 * 2;
    a = read_imagei(hp_1, sampler3, pos1 + A).x;
    b = read_imagei(hp_1, sampler3, pos1 + B).x;
    c = read_imagei(hp_1, sampler3, pos1 + C).x;
    d = read_imagei(hp_1, sampler3, pos1 + D).x;
    if (quad_lower_bound + a > i) {
        pos1 = pos1 + A;
    } else if (quad_lower_bound + a + b > i) {
        quad_lower_bound = quad_lower_bound + a;
        pos1 = pos1 + B;
    } else if (quad_lower_bound + a + b + c > i) {
        quad_lower_bound = quad_lower_bound + a + b;
        pos1 = pos1 + C;
    } else {
        quad_lower_bound = quad_lower_bound + a + b + c;
        pos1 = pos1 + D;
    }

    int2 pos0 = pos1 * 2;
    a = read_imagei(hp_0, sampler3, pos0 + A).x;
    b = read_imagei(hp_0, sampler3, pos0 + B).x;
    c = read_imagei(hp_0, sampler3, pos0 + C).x;
    d = read_imagei(hp_0, sampler3, pos0 + D).x;
    if (quad_lower_bound + a > i) {
        pos0 = pos0 + A;
    } else if (quad_lower_bound + a + b > i) {
        quad_lower_bound = quad_lower_bound + a;
        pos0 = pos0 + B;
    } else if (quad_lower_bound + a + b + c > i) {
        quad_lower_bound = quad_lower_bound + a + b;
        pos0 = pos0 + C;
    } else {
        quad_lower_bound = quad_lower_bound + a + b + c;
        pos0 = pos0 + D;
    }

    float phi0 = read_imagef(phi, sampler, pos0).x;
    float phi1 = read_imagef(phi, sampler, pos0 + (int2)(1, 0)).x;
    float phi2 = read_imagef(phi, sampler, pos0 + (int2)(0, 1)).x;
    float phi3 = read_imagef(phi, sampler, pos0 + (int2)(1, 1)).x;

    int key = 0;
    key += phi0 > 0 ? 8 : 0;
    key += phi1 > 0 ? 4 : 0;
    key += phi2 > 0 ? 2 : 0;
    key += phi3 > 0 ? 1 : 0;

    // the two scenarios with 2 segments need to be split up.
    if (key == 6 && quad_lower_bound < i) {
        key = 0;
    }
    if (key == 9 && quad_lower_bound < i) {
        key = 15;
    }

    float4 phi_vec = {phi0, phi1, phi2, phi3};

    segment_lookup seg = lookup_table[key];

    vertex_spec a_x = seg.a_x;
    float denom = dot_(a_x.denominator, phi_vec);
    if (denom > 0.0) {
        vertex_buffer[i * 4] = dot_(a_x.numerator, phi_vec) / denom + pos0.x;
    } else {
        vertex_buffer[i * 4] = a_x.offset + pos0.x;
    }

    vertex_spec a_y = seg.a_y;
    denom = dot_(a_y.denominator, phi_vec);
    if (denom > 0.0) {
        vertex_buffer[i * 4 + 1] = dot_(a_y.numerator, phi_vec) / denom + pos0.y;
    } else {
        vertex_buffer[i * 4 + 1] = a_y.offset + pos0.y;
    }

    vertex_spec b_x = seg.b_x;
    denom = dot_(b_x.denominator, phi_vec);
    if (denom > 0.0) {
        vertex_buffer[i * 4 + 2] = dot_(b_x.numerator, phi_vec) / denom + pos0.x;
    } else {
        vertex_buffer[i * 4 + 2] = b_x.offset + pos0.x;
    }

    vertex_spec b_y = seg.b_y;
    denom = dot_(b_y.denominator, phi_vec);
    if (denom > 0.0) {
        vertex_buffer[i * 4 + 3] = dot_(b_y.numerator, phi_vec) / denom + pos0.y;
    } else {
        vertex_buffer[i * 4 + 3] = b_y.offset + pos0.y;
    }
    vertex_buffer[i * 4] = key;
}
