__constant sampler_t sampler3 = CLK_NORMALIZED_COORDS_FALSE | CLK_ADDRESS_CLAMP | CLK_FILTER_NEAREST;

typedef struct {
    int2 pos;
    int quad_lower_bound;
} segment_pointer;

segment_pointer traverse_hp(int levelCount,
    __read_only image2d_t hp_0,
    __read_only image2d_t hp_1,
    __read_only image2d_t hp_2,
    __read_only image2d_t hp_3,
    __read_only image2d_t hp_4,
    __read_only image2d_t hp_5,
    __read_only image2d_t hp_6,
    __read_only image2d_t hp_7,
    __read_only image2d_t hp_8,
    __read_only image2d_t hp_9,
    __read_only image2d_t hp_10,
    __read_only image2d_t hp_11);

int2 traverse_level(
    int2 upper_pos,
    __read_only image2d_t level,
    int *quad_lower_bound,
    int i);

segment_pointer traverse_hp(
    int levelCount,
    __read_only image2d_t hp_0,
    __read_only image2d_t hp_1,
    __read_only image2d_t hp_2,
    __read_only image2d_t hp_3,
    __read_only image2d_t hp_4,
    __read_only image2d_t hp_5,
    __read_only image2d_t hp_6,
    __read_only image2d_t hp_7,
    __read_only image2d_t hp_8,
    __read_only image2d_t hp_9,
    __read_only image2d_t hp_10,
    __read_only image2d_t hp_11
) {
    int i = get_global_id(0);

    int quad_lower_bound = 0;
    int next_quad_lower_bound = quad_lower_bound;

    int2 pos_11 = {0, 0};
    int2 pos_10, pos_9, pos_8, pos_7, pos_6, pos_5, pos_4, pos_3, pos_2, pos_1, pos_0;

    if (levelCount >= 12) {
        pos_10 = traverse_level(pos_11, hp_10, &quad_lower_bound, i);
    } else {
        pos_10 = (int2)(0);
    }

    if (levelCount >= 11) {
        pos_9 = traverse_level(pos_10, hp_9, &quad_lower_bound, i);
    } else {
        pos_9 =(int2)(0);
    }

    if (levelCount >= 10) {
        pos_8 = traverse_level(pos_9, hp_8, &quad_lower_bound, i);
    } else {
        pos_8 =(int2)(0);
    }

    if (levelCount >= 9) {
        pos_7 = traverse_level(pos_8, hp_7, &quad_lower_bound, i);
    } else {
        pos_7 =(int2)(0);
    }

    if (levelCount >= 8) {
        pos_6 = traverse_level(pos_7, hp_6, &quad_lower_bound, i);
    } else {
        pos_6 =(int2)(0);
    }

    if (levelCount >= 7) {
        pos_5 = traverse_level(pos_6, hp_5, &quad_lower_bound, i);
    } else {
        pos_5 =(int2)(0);
    }

    if (levelCount >= 6) {
        pos_4 = traverse_level(pos_5, hp_4, &quad_lower_bound, i);
    } else {
        pos_4 =(int2)(0);
    }

    if (levelCount >= 5) {
        pos_3 = traverse_level(pos_4, hp_3, &quad_lower_bound, i);
    } else {
        pos_3 =(int2)(0);
    }

    if (levelCount >= 4) {
        pos_2 = traverse_level(pos_3, hp_2, &quad_lower_bound, i);
    } else {
        pos_2 =(int2)(0);
    }

    if (levelCount >= 3) {
        pos_1 = traverse_level(pos_2, hp_1, &quad_lower_bound, i);
    } else {
        pos_1 =(int2)(0);
    }

    if (levelCount >= 2) {
        pos_0 = traverse_level(pos_1, hp_0, &quad_lower_bound, i);
    } else {
        pos_0 =(int2)(0);
    }

    segment_pointer result = {pos_0, quad_lower_bound};
    return (result);
}

__constant int2 A = {0, 0};
__constant int2 B = {1, 0};
__constant int2 C = {0, 1};
__constant int2 D = {1, 1};

int2 traverse_level(
    int2 upper_pos,
    __read_only image2d_t level,
    int *quad_lower_bound,
    int i
) {
    float a, b, c, d;
    int2 pos = upper_pos * 2;
    a = read_imagei(level, sampler3, pos + A).x;
    b = read_imagei(level, sampler3, pos + B).x;
    c = read_imagei(level, sampler3, pos + C).x;
    d = read_imagei(level, sampler3, pos + D).x;

    if (*quad_lower_bound + a > i) {
        pos = pos + A;
    } else if (*quad_lower_bound + a + b > i) {
        *quad_lower_bound = *quad_lower_bound + a;
        pos = pos + B;
    } else if (*quad_lower_bound + a + b + c > i) {
        *quad_lower_bound = *quad_lower_bound + a + b;
        pos = pos + C;
    } else {
        *quad_lower_bound = *quad_lower_bound + a + b + c;
        pos = pos + D;
    }
    return pos;
}
