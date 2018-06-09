package jack.fluids.slow.coords;

import org.immutables.value.Value;

@Value.Immutable
public interface VCellCoords extends Coords {
  static VCellCoords of(int i, int j) {
    return ImmutableVCellCoords.of(i, j);
  }

  default String variable() {
    return String.format("v-%d-%d", i(), j());
  }
}
