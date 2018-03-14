package jack.fluids.buffers;

import com.google.common.base.MoreObjects;
import jack.fluids.cl.Session;
import org.jocl.cl_mem;

import java.util.ArrayList;
import java.util.List;

public class HistogramPyramid {
  private final List<cl_mem> levels;
  private final List<Integer> widths;
  private final List<Integer> heights;

  public HistogramPyramid(List<cl_mem> levels,
                          List<Integer> widths,
                          List<Integer> heights) {
    this.levels = levels;
    this.widths = widths;
    this.heights = heights;
  }

  public cl_mem bottom() {
    return levels.get(0);
  }

  public int width(int level) {
    return widths.get(level);
  }

  public int height(int level) {
    return heights.get(level);
  }

  public static HistogramPyramid create(Session session,  int gridWidth, int gridHeight) {
    // the histogram pyramid stores values for voxels (cells), not grid points.
    // thus the dimensions should be (width - 1, height - 1)

    int width = gridWidth - 1;
    int height = gridHeight - 1;

    List<cl_mem> levels = new ArrayList<>();
    List<Integer> widths = new ArrayList<>();
    List<Integer> heights = new ArrayList<>();

    while (width > 0 || height > 0) {
      cl_mem level = session.createInt2DImageFromEmpty(width, height);
      levels.add(level);
      widths.add(width);
      heights.add(height);

      if (width == 1 && height == 1) {
        break;
      }

      width = (int) Math.ceil((double) width / 2);
      height = (int) Math.ceil((double) height / 2);
    }

    return new HistogramPyramid(levels, widths, heights);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("levels", levels)
        .add("widths", widths)
        .add("heights", heights)
        .toString();
  }
}
