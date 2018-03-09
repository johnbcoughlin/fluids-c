void __kernel serial_prefix_sum(
    __global int* input,
    __global int* output,
    unsigned int count
) {
    int sum = 0;
    for (unsigned int i = 0; i < count; i++) {
        int a = input[i];
        sum += a;
        output[i] = sum;
    }
}
