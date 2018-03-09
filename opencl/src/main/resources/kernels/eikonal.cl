__constant sampler_t sampler = CLK_NORMALIZED_COORDS_FALSE | CLK_ADDRESS_CLAMP_TO_EDGE | CLK_FILTER_NEAREST;

void __kernel iterate_eikonal(
    read_only image2d_t closest_points,
    write_only image2d_t closest_points_output,
    int dx,
    int dy
) {
    const int2 pos = {get_global_id(0), get_global_id(1)};

    float4 currentValues = read_imagef(closest_points, sampler, pos);
    float currentDistance2 = currentValues.z;

    float4 leftNeighbor = read_imagef(closest_points, sampler, pos + int2(dx, dy));
    float candidateDx = leftNeighbor.x - float(pos.x);
    float candidateDy = leftNeighbor.y - float(pos.y);
    float candidateDistance2 = candidateDx * candidateDx + candidateDy * candidateDy;

    if (candidateDistance2 < currentDistance2) {
        float4 newValue = {leftNeighbor.x, leftNeighbor.y, candidateDistance2, 0.0};
        write_imagef(closest_points_output, pos, newValue);
    }
}