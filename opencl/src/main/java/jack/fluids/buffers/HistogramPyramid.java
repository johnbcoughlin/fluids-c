package jack.fluids.buffers;

import org.jocl.cl_mem;

import java.util.List;

public class HistogramPyramid {
  private final List<cl_mem> levels;

  public HistogramPyramid(List<cl_mem> levels) {
    this.levels = levels;
  }

  public cl_mem bottom() {
    return levels.get(0);
  }

  public static HistogramPyramid create(int gridWidth, int gridHeight) {
    
  }
}
