__constant sampler_t sampler2 = CLK_NORMALIZED_COORDS_FALSE | CLK_ADDRESS_CLAMP | CLK_FILTER_NEAREST;

void __kernel roll_up_histogram(
    __read_only image2d_t lower_level,
    __write_only image2d_t upper_level,
    int lower_level_width,
    int lower_level_height
) {
    int2 pos = {get_global_id(0), get_global_id(1)};
    int2 pos2 = {pos.x * 2, pos.y * 2};
    int4 sum = {0, 0, 0, 0};
    int2 dim = get_image_dim(lower_level);

    sum += read_imagei(lower_level, sampler2, pos2);
    if (pos2.y + 1 < dim.y) {
        sum += read_imagei(lower_level, sampler2, pos2 + (int2)(0, 1));
    }
    if (pos2.x + 1 < dim.x) {
        sum += read_imagei(lower_level, sampler2, pos2 + (int2)(1, 0));
    }
    if (pos2.x + 1 < dim.x && pos2.y + 1 < dim.y) {
        sum += read_imagei(lower_level, sampler2, pos2 + (int2)(1, 1));
    }
    write_imagei(upper_level, pos, sum);
}
