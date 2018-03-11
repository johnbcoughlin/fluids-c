__constant sampler_t sampler = CLK_NORMALIZED_COORDS_FALSE | CLK_ADDRESS_REPEAT | CLK_FILTER_NEAREST;

void __kernel iterate_eikonal(
    __read_only image2d_t closest_points,
    __write_only image2d_t closest_points_output,
    int dx,
    int dy
) {
    const int2 pos = {get_global_id(0), get_global_id(1)};
    float4 newValue = {4.2, 1.7, 0.6, 1.5};
    write_imagef(closest_points_output, pos, newValue);
}