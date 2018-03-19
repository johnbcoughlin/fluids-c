void __kernel generate_segments__5(
    int levelCount,
    __global float* vertex_buffer,
    __read_only image2d_t phi,
    __read_only image2d_t hp_0,
    __read_only image2d_t hp_1,
    __read_only image2d_t hp_2,
    __read_only image2d_t hp_3,
    __read_only image2d_t hp_4,
    __read_only image2d_t hp_5,
    __read_only image2d_t hp_6,
    __read_only image2d_t hp_7,
    __read_only image2d_t hp_8,
    __read_only image2d_t hp_9,
    __read_only image2d_t hp_10,
    __read_only image2d_t hp_11
) {
    int i = get_global_id(0);

    segment_pointer ptr = traverse_hp(levelCount,
    hp_0,
    hp_1,
    hp_2,
    hp_3,
    hp_4,
    hp_5,
    hp_6,
    hp_7,
    hp_8,
    hp_9,
    hp_10,
    hp_11);

    int2 pos = ptr.pos;
    int quad_lower_bound = ptr.quad_lower_bound;

    float phi0 = read_imagef(phi, sampler, pos).x;
    float phi1 = read_imagef(phi, sampler, pos + (int2)(1, 0)).x;
    float phi2 = read_imagef(phi, sampler, pos + (int2)(0, 1)).x;
    float phi3 = read_imagef(phi, sampler, pos + (int2)(1, 1)).x;

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
        vertex_buffer[i * 4] = dot_(a_x.numerator, phi_vec) / denom + pos.x;
    } else {
        vertex_buffer[i * 4] = a_x.offset + pos.x;
    }

    vertex_spec a_y = seg.a_y;
    denom = dot_(a_y.denominator, phi_vec);
    if (denom > 0.0) {
        vertex_buffer[i * 4 + 1] = dot_(a_y.numerator, phi_vec) / denom + pos.y;
    } else {
        vertex_buffer[i * 4 + 1] = a_y.offset + pos.y;
    }

    vertex_spec b_x = seg.b_x;
    denom = dot_(b_x.denominator, phi_vec);
    if (denom > 0.0) {
        vertex_buffer[i * 4 + 2] = dot_(b_x.numerator, phi_vec) / denom + pos.x;
    } else {
        vertex_buffer[i * 4 + 2] = b_x.offset + pos.x;
    }

    vertex_spec b_y = seg.b_y;
    denom = dot_(b_y.denominator, phi_vec);
    if (denom > 0.0) {
        vertex_buffer[i * 4 + 3] = dot_(b_y.numerator, phi_vec) / denom + pos.y;
    } else {
        vertex_buffer[i * 4 + 3] = b_y.offset + pos.y;
    }
}
