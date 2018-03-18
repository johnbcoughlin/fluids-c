# List of optimizations to try out

## Marching Cubes with Histogram Pyramids

- Reference whole 2x2 sub-square at once as an int4
- don't schedule final level roll-up of histogram pyramid, because adding 4 numbers in CPU should be faster
  - whatever amount can fit in one cache line
- Bottom levels of histogram pyramids can use smaller datatypes to hold counts.
- explicitly pull upper levels of histogram pyramid into local memory
- pull boxes of lower levels into local memory as well

## GL

- Try a different usage hint than `GL_STATIC_DRAW` for marching cubes vertices.
