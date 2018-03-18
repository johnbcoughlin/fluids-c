# Development journal

## 2018-03-11

I found the issue that was causing image renders not to work.
It turned out to be a mis-specified buffer length---I was passing a non-null `event_wait_list`, but forgot to also pass a non-zero length.
Next: testing the Eikonal equation smoother and implementing a decently fast version.

## 2018-03-15

Was seeing weird output when doing the histogram pyramid roll up test.
In particular, the image didn't seem to be clamping out of range coordinates correctly, so `read_imagei(... int2(0, 8))` was non-zero,
even when the image wasn't that big.
Bug: was trying to construct a vector in a kernel like: `int2(0, 1)`. It's actually `(int2)(0, 1)`.

## 2018-03-16

Kernel was not doing anything to output buffer.
Ascertained that it was because I was passing invalid values (null) as kernel arguments,
but I wasn't checking the return codes.
Lesson: always check the return codes.

## 2018-03-17

Completed marching squares impl, nearly.
Circle is nearly complete, except for a couple of the 16 cases which are misconfigured, leaving wholes in the output.
There was a bug where the x and y coordinates were transposed in sampling the corners of the voxel,
leading all segments to point more or less in and out of the circle rather than forming edges.
I found it by inserting "debug" statements, i.e. writing bogus values to the output buffer and observing them when they showed up in logs.
Now just to fix the remaining buggy cases.
