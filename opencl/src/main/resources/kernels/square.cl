void __kernel square(
    __global float* input,
    __global float* output
) {
    unsigned int gid = get_global_id(0);
    output[1] = foo;
    output[gid] = input[gid] * input[gid];
}
