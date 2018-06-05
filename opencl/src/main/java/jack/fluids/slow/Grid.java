package jack.fluids.slow;

import jack.fluids.slow.mesh.Mesh;
import jack.fluids.slow.mesh.Segment;
import org.nd4j.linalg.api.ndarray.INDArray;

import java.util.Optional;

public class Grid {
  public static final double MU = 1.0;

  // Dimensions of the grid of p-cells
  private final int nx;
  private final int ny;

  private final double dx;
  private final double dy;

  private final Mesh mesh;

  /*
   * P-cell volumes and faces
   */
  INDArray pCellsFluid;
  INDArray pCellsX;
  INDArray pCellsY;
  INDArray pCellsVolume;
  INDArray pFacesArea;

  /*
   * U-cell volumes and faces
   */
  INDArray uCellsFluid;
  INDArray uCellsValue;
  INDArray uCellsX;
  INDArray uCellsY;
  INDArray uCellsVolume;
  INDArray uCellsVerticalBoundaryDistance;

  INDArray uFacesArea;
  INDArray uFacesExistenceFlag;
  INDArray uFacesPositiveDirectionFluid;
  INDArray uFacesPositiveDirectionDistance;
  INDArray uFacesNegativeDirectionFluid;
  INDArray uFacesNegativeDirectionDistance;
  INDArray uFacesTheta;
  INDArray uFacesCrosswisePositiveDirectionFluid;
  INDArray uFacesCrosswiseNegativeDirectionFluid;
  INDArray uFacesCrosswiseTheta;

  /*
   * V-cell volumes and faces
   */
  INDArray vCellsFluid;
  INDArray vCellsValue;
  INDArray vCellsVolume;
  INDArray vCellsVerticalBoundaryDistance;

  public Grid(int nx, int ny, double dx, double dy, Mesh mesh) {
    this.nx = nx;
    this.ny = ny;
    this.dx = dx;
    this.dy = dy;
    this.mesh = mesh;
  }

  Optional<ControlPoint> uControlPoint(int i, int j) {
    Optional<Segment> principalSegment = Grids.uPrincipalSegmentLocation(dx, dy, i, j, mesh);
    if (!principalSegment.isPresent()) {
      return Optional.empty();
    }
    Segment segment = principalSegment.get();
    return Optional.of(ControlPoint.of(
        segment.midpoint(),
        "u-" + i + "-" + j,
        uCellsValue.getDouble(i, j)));
  }

  ControlPoint uControlPoint

//  Neighborhood uNeighborhood(int i, int j) {
//    return ImmutableNeighborhood.builder()
//        .P(ControlPoint.of(Point.of(uCellsX.getDouble(i, j), uCellsY.getDouble(i, j)),
//            uCellsValue.getDouble(i, j))
//        .n(uCellFace(i, j, NORTH))
//        .s(uCellFace(i, j, SOUTH))
//        .e(uCellFace(i, j, EAST))
//        .w(uCellFace(i, j, WEST))
//        .N(uCellsValue.getDouble(i + 1, j))
//        .S(uCellsValue.getDouble(i - 1, j))
//        .E(uCellsValue.getDouble(i, j + 1))
//        .W(uCellsValue.getDouble(i, j - 1))
//        .NN(uCellsValue.getDouble(i + 2, j))
//        .SS(uCellsValue.getDouble(i - 2, j))
//        .EE(uCellsValue.getDouble(i, j + 2))
//        .WW(uCellsValue.getDouble(i, j - 2))
//        .ne(vCellsValue.getDouble(i + 1, j))
//        .nw(vCellsValue.getDouble(i + 1, j - 1))
//        .se(vCellsValue.getDouble(i, j))
//        .sw(vCellsValue.getDouble(i, j - 1))
//        .build();
//  }
//
//  StaggeredCellFace uCellFace(int i, int j, Direction direction) {
//    int fi = direction.toFacei(i);
//    int fj = direction.toFacej(j);
//    return ImmutableStaggeredCellFace.builder()
//        .area(uFacesArea.getDouble(i, j))
//        .positiveDirectionFluid(uFacesPositiveDirectionFluid.getInt(fi, fj) == 1)
//        .positiveDirectionDistance(uFacesPositiveDirectionDistance.getDouble(fi, fj))
//        .negativeDirectionFluid(uFacesNegativeDirectionFluid.getInt(fi, fj) == 1)
//        .negativeDirectionDistance(uFacesNegativeDirectionDistance.getDouble(fi, fj))
//        .theta(uFacesTheta.getDouble(fi, fj))
//        .crosswisePositiveDirectionFluid(uFacesCrosswisePositiveDirectionFluid.getInt(fi, fj) == 1)
//        .crosswiseNegativeDirectionFluid(uFacesCrosswiseNegativeDirectionFluid.getInt(fi, fj) == 1)
//        .crosswiseTheta(uFacesCrosswiseTheta.getDouble(fi, fj))
//        .build();
//  }
}
