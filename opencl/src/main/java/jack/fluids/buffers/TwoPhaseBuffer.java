package jack.fluids.buffers;

import org.jocl.cl_mem;

public class TwoPhaseBuffer {
  private cl_mem front;
  private cl_mem back;

  public TwoPhaseBuffer(cl_mem front, cl_mem back) {
    this.front = front;
    this.back = back;
  }

  public cl_mem front() {
    return front;
  }

  public cl_mem back() {
    return back;
  }

  void swap() {
    cl_mem tmp = front;
    front = back;
    back = tmp;
  }
}
