void __kernel write_boundary_points(
    unsigned int inv_mesh_size,
    __global float* vertices,
    __global int* per_segment_boundary_point_prefix_sums,
    __global float* boundary_points
) {
    unsigned int i = get_global_id(0);
    float ax = vertices[2 * i] * inv_mesh_size;
    float ay = vertices[2 * i + 1] * inv_mesh_size;
    float bx = vertices[2 * i + 2] * inv_mesh_size;
    float by = vertices[2 * i + 3] * inv_mesh_size;

    bool y_pos = by - ay > 0.0;
    bool x_pos = bx - ax > 0.0;
    float x_inc = bx - ax > 0.0 ? 1.0 : -1.0;
    float y_inc = by - ay > 0.0 ? 1.0 : -1.0;

    int index = i == 0 ? 0 : per_segment_boundary_point_prefix_sums[i - 1];
    float x = ax;
    float y = ay;
    // the minimum distance between boundary points that we'll allow
    float epsilon = 1.0e-3;
    int count = 0;
    while (((x_pos && x < bx) || (!x_pos && x > bx))) {
        // find the next intersection point
        float next_horizontal_intersection_y = y_pos ? ceil(y) : floor(y);
        float next_horizontal_intersection_x = ax + (bx - ax) * (next_horizontal_intersection_y - ay) / (by - ay);

        float next_vertical_intersection_x = x_pos ? ceil(x) : floor(x);
        float next_vertical_intersection_y = ay + (by - ay) * (next_vertical_intersection_x - ax) / (bx - ax);


        if (x_pos) {
            if (next_vertical_intersection_x < next_horizontal_intersection_x) {
                boundary_points[2 * index] = next_vertical_intersection_x;
                boundary_points[2 * index + 1] = next_vertical_intersection_y;
                x = next_vertical_intersection_x + epsilon;
                y = next_vertical_intersection_y + epsilon * (y_pos ? 1.0 : -1.0);
            } else {
                boundary_points[2 * index] = next_horizontal_intersection_x;
                boundary_points[2 * index + 1] = next_horizontal_intersection_y;
                x = next_horizontal_intersection_x + epsilon;
                y = next_horizontal_intersection_y + epsilon * (y_pos ? 1.0 : -1.0);
            }
        } else {
            if (next_vertical_intersection_x > next_horizontal_intersection_x) {
                boundary_points[2 * index] = next_vertical_intersection_x;
                boundary_points[2 * index + 1] = next_vertical_intersection_y;
                x = next_vertical_intersection_x - epsilon;
                y = next_vertical_intersection_y + epsilon * (y_pos ? 1.0 : -1.0);
            } else {
                boundary_points[2 * index] = next_horizontal_intersection_x;
                boundary_points[2 * index + 1] = next_horizontal_intersection_y;
                x = next_horizontal_intersection_x - epsilon;
                y = next_horizontal_intersection_y + epsilon * (y_pos ? 1.0 : -1.0);
            }
        }

        index += 1;
        count += 1;
    }
}
