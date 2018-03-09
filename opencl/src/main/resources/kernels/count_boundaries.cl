// Returns an error code of -1 if there are too many horizontal or vertical intersections.

void __kernel count_boundary_points(
    unsigned int inv_mesh_size,
    __global float* vertices,
    __global int* per_segment_boundary_point_counts
) {
    unsigned int i = get_global_id(0);
    float ax = vertices[2 * i] * inv_mesh_size;
    float ay = vertices[2 * i + 1] * inv_mesh_size;
    float bx = vertices[2 * i + 2] * inv_mesh_size;
    float by = vertices[2 * i + 3] * inv_mesh_size;

    int count = 0;

    bool x_pos = bx - ax > 0.0;
    bool y_pos = by - ay > 0.0;
    float x_inc = x_pos > 0.0 ? 1.0 : -1.0;
    float y_inc = y_pos > 0.0 ? 1.0 : -1.0;

    float x_start = x_pos ? ceil(ax) : floor(ax);
    float x_end = x_pos ? floor(bx) : ceil(ax);
    float x_distance = bx - ax;
    float y_start = y_pos ? ceil(ay) : floor(ay);
    float y_end = y_pos ? floor(by) : ceil(by);
    float y_distance = by - ay;
    float epsilon = 1.0e-4;

    int max_intersections = max(16, int(inv_mesh_size / 4));

    int vert_count = 0;
    float verticals[max_intersections * 2];
    if (fabs(x_distance) > epsilon) {
        for (float x = x_start; (x_pos && x <= x_end) || (!x_pos && x >= x_end); x += x_inc) {
            if (x < 0.0 || x > float(inv_mesh_size)) {
                continue;
            }
            float y = y_start + y_distance * (x - x_start) / x_distance;
            if (y < 0.0 || y > float(inv_mesh_size)) {
                continue;
            }
            verticals[2 * vert_count] = x;
            verticals[2 * vert_count + 1] = y;
            vert_count += 1;
        }
    }

    int horiz_count = 0;
    float horizontals[max_intersections * 2];
    if (fabs(y_distance) > epsilon) {
        for (float y = y_start; (y_pos && y <= y_end) || (!y_pos && y >= y_end); y += y_inc) {
            if (y < 0.0 || y > float(inv_mesh_size)) {
                continue;
            }
            float x = x_start + x_distance * (y - y_start) / y_distance;
            if (x < 0.0 || x > float(inv_mesh_size)) {
                continue;
            }
            horizontals[2 * horiz_count] = x;
            horizontals[2 * horiz_count + 1] = y;
            horiz_count += 1;
        }
    }

    // now iterate through the horizontal and vertical intersections and order them
    float points[max_intersections * 4];
    while (


    per_segment_boundary_point_counts[i] = ;
}

