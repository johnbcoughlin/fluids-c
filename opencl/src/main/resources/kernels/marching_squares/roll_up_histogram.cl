void __kernel roll_up_histogram(
    __read_only image2d_t lower_level,
    __write_only image2d_t upper_level,
    int lower_level_width,
    int lower_level_height
) {
    int2 pos = {get_global_id(0), get_global_id(1)};
    int2 pos2 = pos * 2;
    int sum = 0;
    sum += read_imagei(
}