package jack.fluids.buffers;

import org.immutables.value.Value;
import org.jocl.cl_mem;

@Value.Immutable
public interface SharedVBO extends SizedBuffer1D {
  /**
   * The length of the buffer, in floats
   */
  int length();

  int glBufferName();

  cl_mem clBufferHandle();

  default cl_mem buffer() {
    return clBufferHandle();
  }

  static SharedVBO of(int length, int glBufferName, cl_mem clBufferHandle) {
    return ImmutableSharedVBO.builder()
        .length(length)
        .glBufferName(glBufferName)
        .clBufferHandle(clBufferHandle)
        .build();
  }
}
