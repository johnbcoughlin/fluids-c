// Returns an error code of -1 if there are too many horizontal or vertical intersections.

void __kernel count_boundary_points(
    unsigned int inv_mesh_size,
    __global float* vertices,
    __global int* per_segment_boundary_point_counts,
    __global int* boundary_point_counts_prefix_sum,
    __global float* boundary_points,
    int write_points
) {
    unsigned int i = get_global_id(0);
    float ax = vertices[2 * i] * inv_mesh_size;
    float ay = vertices[2 * i + 1] * inv_mesh_size;
    float bx = vertices[2 * i + 2] * inv_mesh_size;
    float by = vertices[2 * i + 3] * inv_mesh_size;

    bool x_pos = bx - ax > 0.0;
    bool y_pos = by - ay > 0.0;
    float x_inc = x_pos > 0.0 ? 1.0 : -1.0;
    float y_inc = y_pos > 0.0 ? 1.0 : -1.0;

    float x_start = x_pos ? ceil(ax) : floor(ax);
    float x_end = x_pos ? floor(bx) : ceil(bx);
    float x_distance = bx - ax;
    float y_start = y_pos ? ceil(ay) : floor(ay);
    float y_end = y_pos ? floor(by) : ceil(by);
    float y_distance = by - ay;
    float epsilon = 1.0e-3;

    int vert_count = 0;
    float verticals[128];
    if (fabs(x_distance) > epsilon) {
        for (float x = x_start; (x_pos && x < bx) || (!x_pos && x > bx); x += x_inc) {
            if (x < 0.0 || x > float(inv_mesh_size)) {
                continue;
            }
            float y = ay + y_distance * (x - ax) / x_distance;
            if (y < 0.0 || y > float(inv_mesh_size)) {
                continue;
            }
            verticals[2 * vert_count] = x;
            verticals[2 * vert_count + 1] = y;
            vert_count += 1;
            if (vert_count > 64) {
                per_segment_boundary_point_counts[i] = -1;
                return;
            }
        }
    }

    int horiz_count = 0;
    float horizontals[100];
    if (fabs(y_distance) > epsilon) {
        for (float y = y_start; (y_pos && y < by) || (!y_pos && y > by); y += y_inc) {
            if (y < 0.0 || y > float(inv_mesh_size)) {
                continue;
            }
            float x = ax + x_distance * (y - ay) / y_distance;
            if (x < 0.0 || x > float(inv_mesh_size)) {
                continue;
            }
            horizontals[2 * horiz_count] = x;
            horizontals[2 * horiz_count + 1] = y;
            horiz_count += 1;
            if (horiz_count > 64) {
                per_segment_boundary_point_counts[i] = -1;
                return;
            }
        }
    }

    // now iterate through the horizontal and vertical intersections and order them
    int ipoint;
    if (write_points) {
        ipoint = (i == 0 ? 0 : boundary_point_counts_prefix_sum[i-1]);
    } else {
        ipoint = 0;
    }
    int ivert = 0;
    int ihoriz = 0;
    float xvert, yvert, xhoriz, yhoriz;
    while (ivert < vert_count && ihoriz < horiz_count) {
        xvert = verticals[2 * ivert];
        yvert = verticals[2 * ivert + 1];
        xhoriz = horizontals[2 * ihoriz];
        yhoriz = horizontals[2 * ihoriz + 1];

        // first check to see if the points are far enough apart
        if ((xhoriz - xvert) * (xhoriz - xvert) + (yhoriz - yvert) * (yhoriz - yvert) < epsilon * epsilon) {
            // if they're too close, skip the horizontal intersection
            ihoriz += 1;
        } else if ((x_pos && xvert <= xhoriz) || (!x_pos && xvert >= xhoriz)) {
            if (write_points) {
                boundary_points[2 * ipoint] = xvert;
                boundary_points[2 * ipoint + 1] = yvert;
            }
            ivert += 1;
            ipoint += 1;
        } else if ((x_pos && xvert >= xhoriz) || (!x_pos && xvert <= xhoriz)) {
            if (write_points) {
                boundary_points[2 * ipoint] = xhoriz;
                boundary_points[2 * ipoint + 1] = yhoriz;
            }
            ihoriz += 1;
            ipoint += 1;
        }
    }
    while (ivert < vert_count) {
        xvert = verticals[2 * ivert];
        yvert = verticals[2 * ivert + 1];
        if (write_points) {
            boundary_points[2 * ipoint] = xvert;
            boundary_points[2 * ipoint + 1] = yvert;
        }
        ivert += 1;
        ipoint += 1;
    }
    while (ihoriz < horiz_count) {
        xhoriz = horizontals[2 * ihoriz];
        yhoriz = horizontals[2 * ihoriz + 1];
        if (write_points) {
            boundary_points[2 * ipoint] = xhoriz;
            boundary_points[2 * ipoint + 1] = yhoriz;
        }
        ihoriz += 1;
        ipoint += 1;
    }
    if (write_points == 0) {
        per_segment_boundary_point_counts[i] = ipoint;
    }
}

