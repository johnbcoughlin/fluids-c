# Development journal

## 2018-03-11

I found the issue that was causing image renders not to work.
It turned out to be a mis-specified buffer length---I was passing a non-null `event_wait_list`, but forgot to also pass a non-zero length.
Next: testing the Eikonal equation smoother and implementing a decently fast version.
