package jack.fluids.buffers;

import org.jocl.cl_mem;

public interface SizedBuffer1D {
  int length();

  cl_mem buffer();
}
