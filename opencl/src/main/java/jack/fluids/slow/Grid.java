package jack.fluids.slow;

import org.nd4j.linalg.api.ndarray.INDArray;

import static jack.fluids.slow.Direction.*;

public class Grid {
  public static final double MU = 1.0;

  // Dimensions of the grid of p-cells
  private final int nx;
  private final int ny;

  /*
   * P-cell volumes and faces
   */
  INDArray pCellsVolume;
  INDArray pFacesArea;

  /*
   * U-cell volumes and faces
   */
  INDArray uCellsFluid;
  INDArray uCellsValue;
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

  public Grid(int nx, int ny) {
    this.nx = nx;
    this.ny = ny;
  }

  Neighborhood uNeighborhood(int i, int j) {
    return ImmutableNeighborhood.builder()
        .P(uCellsValue.getDouble(i, j))
        .n(uCellFace(i, j, NORTH))
        .s(uCellFace(i, j, SOUTH))
        .e(uCellFace(i, j, EAST))
        .w(uCellFace(i, j, WEST))
        .N(uCellsValue.getDouble(i + 1, j))
        .S(uCellsValue.getDouble(i - 1, j))
        .E(uCellsValue.getDouble(i, j + 1))
        .W(uCellsValue.getDouble(i, j - 1))
        .NN(uCellsValue.getDouble(i + 2, j))
        .SS(uCellsValue.getDouble(i - 2, j))
        .EE(uCellsValue.getDouble(i, j + 2))
        .WW(uCellsValue.getDouble(i, j - 2))
        .ne(vCellsValue.getDouble(i + 1, j))
        .nw(vCellsValue.getDouble(i + 1, j - 1))
        .se(vCellsValue.getDouble(i, j))
        .sw(vCellsValue.getDouble(i, j - 1))
        .build();
  }

  StaggeredCellFace uCellFace(int i, int j, Direction direction) {
    int fi = direction.toFacei(i);
    int fj = direction.toFacej(j);
    return ImmutableStaggeredCellFace.builder()
        .area(uFacesArea.getDouble(i, j))
        .positiveDirectionFluid(uFacesPositiveDirectionFluid.getInt(fi, fj) == 1)
        .positiveDirectionDistance(uFacesPositiveDirectionDistance.getDouble(fi, fj))
        .negativeDirectionFluid(uFacesNegativeDirectionFluid.getInt(fi, fj) == 1)
        .negativeDirectionDistance(uFacesNegativeDirectionDistance.getDouble(fi, fj))
        .theta(uFacesTheta.getDouble(fi, fj))
        .crosswisePositiveDirectionFluid(uFacesCrosswisePositiveDirectionFluid.getInt(fi, fj) == 1)
        .crosswiseNegativeDirectionFluid(uFacesCrosswiseNegativeDirectionFluid.getInt(fi, fj) == 1)
        .crosswiseTheta(uFacesCrosswiseTheta.getDouble(fi, fj))
        .build();
  }

  public boolean pNorthmost(int j) {
    return j == nx - 1;
  }

  public boolean pSouthmost(int j) {
    return j == 0;
  }

  public boolean pEastmost(int i) {
    return i == nx - 1;
  }

  public boolean pWestmost(int i) {
    return i == 0;
  }

  public boolean uFurthest(int i, int j, Direction direction) {
    switch (direction) {
      case NORTH:
        return uNorthmost(j);
      case SOUTH:
        return uSouthmost(j);
      case EAST:
        return uEastmost(i);
      case WEST:
        return uWestmost(i);
    }
  }

  public boolean uNorthmost(int j) {
    return j == nx - 1;
  }

  public boolean uSouthmost(int j) {
    return j == 0;
  }

  public boolean uEastmost(int i) {
    return i == nx - 2;
  }

  public boolean uWestmost(int i) {
    return i == 0;
  }
}
