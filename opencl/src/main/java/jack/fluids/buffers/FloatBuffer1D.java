package jack.fluids.buffers;

import org.immutables.value.Value;
import org.jocl.cl_mem;

@Value.Immutable
public interface FloatBuffer1D {
  cl_mem buffer();
  int length();

  static FloatBuffer1D of(cl_mem buffer, int length) {
    return ImmutableFloatBuffer1D.builder()
        .buffer(buffer)
        .length(length)
        .build();
  }
}
