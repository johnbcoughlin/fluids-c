package jack.fluids.buffers;

import jack.fluids.cl.JOCLUtils;
import org.jocl.cl_buffer_region;
import org.jocl.cl_mem;

import static org.jocl.CL.CL_BUFFER_CREATE_TYPE_REGION;
import static org.jocl.CL.clCreateSubBuffer;

public class SplitBuffer {
  private final cl_mem underlyingBuffer;
  private final long totalSizeBytes;
  long openRegionPointer;

  public SplitBuffer(cl_mem underlyingBuffer, long totalSizeBytes) {
    this.underlyingBuffer = underlyingBuffer;
    this.totalSizeBytes = totalSizeBytes;
  }

  public cl_mem requestSubBuffer(long length, long flags) {
    int[] error_code_ret = new int[1];
    cl_buffer_region region = new cl_buffer_region(openRegionPointer, length);
    cl_mem result = clCreateSubBuffer(underlyingBuffer, flags, CL_BUFFER_CREATE_TYPE_REGION, region, error_code_ret);
    JOCLUtils.check(error_code_ret);
    return result;
  }
}
