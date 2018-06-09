package jack.fluids.slow.coords;

import org.immutables.value.Value;

@Value.Immutable
public interface UCellCoords extends Coords {
  default VCellCoords ne() {
    return VCellCoords.of(i(), j() + 1);
  }

  default VCellCoords nw() {
    return VCellCoords.of(i() - 1, j() + 1);
  }

  default VCellCoords se() {
    return VCellCoords.of(i(), j());
  }

  default VCellCoords sw() {
    return VCellCoords.of(i() - 1, j());
  }

  static UCellCoords of(int i, int j) {
    return ImmutableUCellCoords.of(i, j);
  }
}
