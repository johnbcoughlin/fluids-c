package jack.fluids;

import org.jocl.*;

import java.util.Arrays;

import static jack.fluids.JOCLUtils.check;
import static org.jocl.CL.*;

public class SolidObjects {
  private final cl_context context;
  private final cl_command_queue queue;

  public SolidObjects(cl_context context, cl_command_queue queue) {
    this.context = context;
    this.queue = queue;
  }

  public cl_mem initVertexBuffer() {
    int[] errorCode = new int[1];
    float[] vertices = new float[202];
    for (int i = 0; i < 100; i++) {
      double theta = i * 2 * Math.PI / 100;
      vertices[2 * i] = (float) Math.cos(theta);
      vertices[2 * i + 1] = (float) Math.sin(theta);
    }
    vertices[200] = vertices[0];
    vertices[201] = vertices[1];
    System.out.println(Arrays.toString(vertices));
    Pointer vertexPointer = Pointer.to(vertices);
    cl_mem result = clCreateBuffer(context, CL_MEM_READ_WRITE | CL_MEM_COPY_HOST_PTR,
        Sizeof.cl_float * 202, vertexPointer, errorCode);
    check(errorCode);
    return result;
  }

}
