__constant sampler_t sampler =
    CLK_NORMALIZED_COORDS_FALSE |
    CLK_ADDRESS_CLAMP |
    CLK_FILTER_NEAREST;

bool between(float a, float test, float b);

void __kernel compute_winding_number(
    __global float* vbo,
    int segmentCount,
    int segmentStride,
    __read_only image2d_t grid_from,
    __write_only image2d_t grid_to,
    float2 origin
) {
    int2 pos = {get_global_id(0), get_global_id(1)};
    int current = read_imagei(grid_from, sampler, pos).x;

    for (int i = 0; i < segmentCount; i++) {

        float4 segment = vload4(i, vbo);
        float cross1 = segment.x * segment.w - segment.y * segment.z;
        float cross2 = origin.x * pos.y - origin.y * pos.x;
        float xdist1 = segment.x - segment.z;
        float ydist1 = segment.y - segment.w;
        float xdist2 = origin.x - pos.x;
        float ydist2 = origin.y - pos.y;

        float denom = xdist1 * ydist2 - ydist1 * xdist2;

        if (denom != 0.0) {
            float intersection_x = (cross1 * xdist2 - xdist1 * cross2) / denom;
            float intersection_y = (cross1 * ydist2 - ydist1 * cross2) / denom;
            //current = intersection_x * 10;
            //break;
            if ((between(segment.x, intersection_x, segment.z) ||
                 between(segment.y, intersection_y, segment.w)) &&
                (between(origin.x, intersection_x, (float) pos.x) ||
                 between(origin.y, intersection_y, (float) pos.y))) {
                current = (current + 1) % 2;
            } else {
            }
        } else {
        }
    }
    write_imagei(grid_to, pos, current);
}

bool between(float a, float test, float b) {
    return (a <= test && test < b) ||
           (a >= test && test > b);
}