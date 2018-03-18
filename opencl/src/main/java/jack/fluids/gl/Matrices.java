package jack.fluids.gl;

import java.nio.FloatBuffer;

public class Matrices {
  public static FloatBuffer toClipcoords(float width, float height) {
    return FloatBuffer.wrap(new float[]{
        2.0f / width, 0, 0, 0,
        0, 2.0f / height, 0, 0,
        0, 0, 1, 0,
        -1 + 1.0f / width, -1 + 1.0f / height, 0, 1
    });
  }
}
