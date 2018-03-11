void __kernel test(
    __read_only image2d_t input,
    __write_only image2d_t output
) {
    int2 pos = {get_global_id(0), get_global_id(1)};
    float4 val = {1.0, 2.3, 1.8, 8.9};
    write_imagef(output, pos, val);
}