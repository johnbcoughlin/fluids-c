package jack.fluids.buffers;

import org.immutables.value.Value;
import org.jocl.cl_mem;

@Value.Immutable
public interface IntBuffer1D {
  cl_mem buffer();
  int length();

  static IntBuffer1D of(cl_mem buffer, int length) {
    return ImmutableIntBuffer1D.builder()
        .buffer(buffer)
        .length(length)
        .build();
  }
}
