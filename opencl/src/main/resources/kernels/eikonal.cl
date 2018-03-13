__constant sampler_t sampler = CLK_NORMALIZED_COORDS_FALSE | CLK_ADDRESS_CLAMP_TO_EDGE | CLK_FILTER_NEAREST;

void __kernel iterate_eikonal(
    __read_only image2d_t closest_points,
    __write_only image2d_t closest_points_output,
    int dx,
    int dy
) {
    const int2 pos = {get_global_id(0), get_global_id(1)};

    float4 currentValues = read_imagef(closest_points, sampler, pos);
    bool decided = currentValues.w != 0.0;
    float currentDistance2 = currentValues.z;

    float4 neighbor = read_imagef(closest_points, sampler, pos + int2(dx, dy));
    float candidateDx = neighbor.x - float(pos.x);
    float candidateDy = neighbor.y - float(pos.y);
    float candidateSign = neighbor.w;
    bool neighborDecided = candidateSign != 0.0;
    if (!neighborDecided) {
        return;
    }
    float candidateDistance2 = candidateDx * candidateDx + candidateDy * candidateDy;

    if (candidateDistance2 < currentDistance2) {
        float4 newValue = {neighbor.x, neighbor.y, candidateDistance2, candidateSign};
        write_imagef(closest_points_output, pos, newValue);
    } else {
        write_imagef(closest_points_output, pos, currentValues);
    }
}