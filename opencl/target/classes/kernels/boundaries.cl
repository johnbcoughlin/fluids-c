void __kernel count_boundary_points(
    __global unsigned int inv_mesh_size,
    __global float* vertices,
    __global unsigned int vertex_count,
    __global int* per_segment_boundary_point_counts
) {
    unsigned int i = get_global_id(0);
    float ax = vertices[2 * i];
    float ay = vertices[2 * i + 1];
    float bx;
    float by;
    if (i >= vertexCount) {
        bx = vertices[0];
        by = vertices[1];
    } else {
        bx = vertices[2 * i + 2];
        by = vertices[2 * i + 3];
    }
}