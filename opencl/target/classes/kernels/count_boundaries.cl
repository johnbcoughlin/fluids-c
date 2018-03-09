// Returns an error code of -1 if there are too many horizontal or vertical intersections.

void __kernel count_boundary_points(
    unsigned int inv_mesh_size,
    unsigned int max_intersections,
    __global float* vertices,
    __global int* per_segment_boundary_point_counts
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
    float x_end = x_pos ? floor(bx) : ceil(ax);
    float x_distance = bx - ax;
    float y_start = y_pos ? ceil(ay) : floor(ay);
    float y_end = y_pos ? floor(by) : ceil(by);
    float y_distance = by - ay;
    float epsilon = 1.0e-3;

    int vert_count = 0;
    float verticals[100];
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
    float horizontals[100];
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
    float points[100];
    int ipoint = 0;
    int ivert = 0;
    int ihoriz = 0;
    while (ivert < vert_count && ihoriz < horiz_count) {
        float xvert = verticals[2 * ivert];
        float yvert = verticals[2 * ivert + 1];
        float xhoriz = verticals[2 * ihoriz];
        float yhoriz = verticals[2 * ihoriz + 1];

        // first check to see if the points are far enough apart
        if ((xhoriz - xvert) * (xhoriz - xvert) + (yhoriz - yvert) * (yhoriz * yvert) < epsilon * epsilon) {
            // if they're too close, skip the horizontal intersection
            ihoriz += 1;
        } else if ((x_pos && xvert <= xhoriz) || (!x_pos && xvert >= xhoriz)) {
            points[2 * ipoint] = xvert;
            points[2 * ipoint + 1] = yvert;
            ivert += 1;
        } else if ((x_pos && xvert > xhoriz) || (!x_pos && xvert < xhoriz)) {
            points[2 * ipoint] = xhoriz;
            points[2 * ipoint + 1] = yhoriz;
            ihoriz += 1;
        }
        ipoint += 1;
    }
    while (ivert < vert_count) {
        float xvert = verticals[2 * ivert];
        float yvert = verticals[2 * ivert + 1];
        points[2 * ipoint] = xvert;
        points[2 * ipoint + 1] = yvert;
        ivert += 1;
        ipoint += 1;
    }
    while (ihoriz < horiz_count) {
        float xhoriz = verticals[2 * ihoriz];
        float yhoriz = verticals[2 * ihoriz + 1];
        points[2 * ipoint] = xhoriz;
        points[2 * ipoint + 1] = yhoriz;
        ihoriz += 1;
        ipoint += 1;
    }

    per_segment_boundary_point_counts[i] = ipoint;
}

