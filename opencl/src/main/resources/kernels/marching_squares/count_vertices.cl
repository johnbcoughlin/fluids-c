__constant sampler_t sampler =
    CLK_NORMALIZED_COORDS_FALSE |
    CLK_ADDRESS_CLAMP_TO_EDGE |
    CLK_FILTER_NEAREST;

__constant int vertex_count_lookup[] = {
    0, 2, 2, 2, 2, 2,
    4, 2, 2, 4, 2, 2,
    2, 2, 2, 0
};

void __kernel count_vertices(
    __read_only image2d_t phi,
    __write_only image2d_t hp_0
) {
    const int2 pos = {get_global_id(0), get_global_id(1)};
    float phi0 = read_imagef(phi, sampler, pos).x;
    float phi1 = read_imagef(phi, sampler, pos + (int2)(0, 1)).x;
    float phi2 = read_imagef(phi, sampler, pos + (int2)(1, 0)).x;
    float phi3 = read_imagef(phi, sampler, pos + (int2)(1, 1)).x;

    int key = 0;
    key += phi0 > 0 ? 8 : 0;
    key += phi1 > 0 ? 4 : 0;
    key += phi2 > 0 ? 2 : 0;
    key += phi3 > 0 ? 1 : 0;

    write_imagei(hp_0, pos, vertex_count_lookup[key]);
}
