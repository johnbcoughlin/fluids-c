package jack.fluids.slow;

import jack.fluids.slow.coords.UCellCoords;
import jack.fluids.slow.coords.VCellCoords;
import jack.fluids.slow.mesh.Mesh;
import jack.fluids.slow.mesh.Segment;
import org.nd4j.linalg.api.ndarray.INDArray;

import java.util.Optional;

import static jack.fluids.slow.Direction.EAST;
import static jack.fluids.slow.Direction.NORTH;
import static jack.fluids.slow.Direction.SOUTH;
import static jack.fluids.slow.Direction.WEST;

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

  public Grid(int nx, int ny, double dx, double dy, INDArray uCellsValue,
      INDArray vCellsValue, Mesh mesh) {
    this.nx = nx;
    this.ny = ny;
    this.dx = dx;
    this.dy = dy;
    this.uCellsValue = uCellsValue;
    this.vCellsValue = vCellsValue;
    this.mesh = mesh;
  }

  public int nx() {
    return nx;
  }

  public int ny() {
    return ny;
  }

  Optional<ControlPoint> uuControlPoint(int i, int j) {
    if (i >= nx + 1 || j >= ny) {
      return Optional.empty();
    }
    Optional<Segment> principalSegment = Grids.pCellUFacePrincipalSegment(dx, dy, i, j, mesh);
    if (!principalSegment.isPresent()) {
      return Optional.empty();
    }
    Segment segment = principalSegment.get();
    return Optional.of(ControlPoint.of(
        segment.midpoint(),
        "u-" + i + "-" + j,
        uCellsValue.getDouble(i, j)));
  }

  /**
   * Compute a u control point for a neighborhood centered at the given coordinates.
   */
  ControlPoint uuControlPoint(int i, int j, Direction direction, int distance, double borderU,
      Point P) {
    int ui = i + (direction == EAST ? distance : (direction == WEST ? -distance : 0));
    int uj = j + (direction == NORTH ? distance : (direction == SOUTH ? -distance : 0));
    Optional<ControlPoint> realControlPoint = uuControlPoint(ui, uj);
    if (realControlPoint.isPresent()) {
      return realControlPoint.get();
    }
    Optional<Point> meshIntersection = mesh.intersectionPoint(Segment.of(
        P, Grids.uPointAtCoords(dx, dy, ui, uj)));
    if (!meshIntersection.isPresent()) {
      throw new IllegalStateException("The mesh must not be closed, " +
          "there should be an intersection here");
    }
    return ControlPoint.of(meshIntersection.get(), "", borderU);
  }

  ControlPoint uvControlPoint(UCellCoords coords, Direction nsDirection, Direction ewDirection,
      StaggeredCellFace face, double borderV) {
    if (nsDirection == NORTH) {
      if (ewDirection == EAST) {
        VCellCoords ne = coords.ne();
        Segment naturalNe = Grids.pCellVFace(dx, dy, ne);
        Optional<Segment> actualNe = Grids.principalSegmentLocation(naturalNe, mesh);
        if (actualNe.isPresent()) {
          return ControlPoint.of(actualNe.get().midpoint(), ne.variable(),
              vCellsValue.getDouble(ne.i(), ne.j()));
        } else {
          return ControlPoint.of(face.segment().eastmost(), "", borderV);
        }
      } else if (ewDirection == WEST) {
        VCellCoords nw = coords.nw();
        Segment naturalNw = Grids.pCellVFace(dx, dy, nw);
        Optional<Segment> actualNw = Grids.principalSegmentLocation(naturalNw, mesh);
        if (actualNw.isPresent()) {
          return ControlPoint.of(actualNw.get().midpoint(), nw.variable(),
              vCellsValue.getDouble(nw.i(), nw.j()));
        } else {
          return ControlPoint.of(face.segment().westmost(), "", borderV);
        }
      }
    } else if (nsDirection == SOUTH) {
      if (ewDirection == EAST) {
        VCellCoords se = coords.se();
        Segment naturalSe = Grids.pCellVFace(dx, dy, se);
        Optional<Segment> actualSe = Grids.principalSegmentLocation(naturalSe, mesh);
        if (actualSe.isPresent()) {
          return ControlPoint.of(actualSe.get().midpoint(), se.variable(),
              vCellsValue.getDouble(se.i(), se.j()));
        } else {
          return ControlPoint.of(face.segment().eastmost(), "", borderV);
        }
      } else if (ewDirection == WEST) {
        VCellCoords sw = coords.sw();
        Segment naturalSw = Grids.pCellVFace(dx, dy, sw);
        Optional<Segment> actualNw = Grids.principalSegmentLocation(naturalSw, mesh);
        if (actualNw.isPresent()) {
          return ControlPoint.of(actualNw.get().midpoint(), sw.variable(),
              vCellsValue.getDouble(sw.i(), sw.j()));
        } else {
          return ControlPoint.of(face.segment().westmost(), "", borderV);
        }
      }
    }
    throw new IllegalArgumentException(String.format("Bad ns/ew directions: %s/%s",
        nsDirection, ewDirection));
  }

  Optional<Neighborhood> uNeighborhood(int i, int j) {
    UCellCoords coords = UCellCoords.of(i, j);
    Optional<ControlPoint> maybeP = uuControlPoint(i, j);
    if (!maybeP.isPresent()) {
      return Optional.empty();
    }
    Point naturalPLocation = Grids.uPointAtCoords(dx, dy, i, j);
    ControlPoint P = maybeP.get();
    ControlPoint N = uuControlPoint(i, j, NORTH, 1, 0.0, P);
    ControlPoint S = uuControlPoint(i, j, SOUTH, 1, 0.0, P);
    ControlPoint E = uuControlPoint(i, j, EAST, 1, 0.0, P);
    ControlPoint W = uuControlPoint(i, j, WEST, 1, 0.0, P);
    ControlPoint NN = uuControlPoint(i, j, NORTH, 2, 0.0, P);
    ControlPoint SS = uuControlPoint(i, j, SOUTH, 2, 0.0, P);
    ControlPoint EE = uuControlPoint(i, j, EAST, 2, 0.0, P);
    ControlPoint WW = uuControlPoint(i, j, WEST, 2, 0.0, P);
    Optional<StaggeredCellFace> fn = uCellFace(i, j, NORTH, N, P, NN, S);
    Optional<StaggeredCellFace> fs = uCellFace(i, j, SOUTH, P, S, N, SS);
    Optional<StaggeredCellFace> fe = uCellFace(i, j, EAST, E, P, EE, W);
    Optional<StaggeredCellFace> fw = uCellFace(i, j, WEST, P, W, E, WW);

    Neighborhood.BoundaryDistances boundaryDistances = ImmutableBoundaryDistances.builder()
        .hP(mesh.minimumDistance(P))
        .hN(mesh.minimumDistance(N))
        .hS(mesh.minimumDistance(S))
        .hE(mesh.minimumDistance(E))
        .hW(mesh.minimumDistance(W))
        .hn(fn.map(f -> mesh.minimumDistance(f.point())).orElse(0.0))
        .hs(fs.map(f -> mesh.minimumDistance(f.point())).orElse(0.0))
        .he(fe.map(f -> mesh.minimumDistance(f.point())).orElse(0.0))
        .hw(fw.map(f -> mesh.minimumDistance(f.point())).orElse(0.0))
        .build();

    return Optional.of(ImmutableNeighborhood.builder()
        .P(P)
        .N(N)
        .S(S)
        .E(E)
        .W(W)
        .NN(NN)
        .SS(SS)
        .EE(EE)
        .WW(WW)
        .fn(fn)
        .fs(fs)
        .fe(fe)
        .fw(fw)
        .ne(fn.map(f -> uvControlPoint(coords, NORTH, EAST, f, 0.0)))
        .nw(fn.map(f -> uvControlPoint(coords, NORTH, WEST, f, 0.0)))
        .se(fs.map(f -> uvControlPoint(coords, SOUTH, EAST, f, 0.0)))
        .sw(fs.map(f -> uvControlPoint(coords, SOUTH, WEST, f, 0.0)))
        .boundaryDistances(boundaryDistances)
        .volume(Grids.approximateVolume(dx, dy, naturalPLocation.x() - dx / 2,
            naturalPLocation.y() - dy / 2, mesh))
        .build());
  }

  Optional<StaggeredCellFace> uCellFace(int i, int j, Direction direction,
      ControlPoint pos, ControlPoint neg, ControlPoint posPos, ControlPoint negNeg) {
    Segment naturalSegment = null;
    switch (direction) {
      case NORTH:
        naturalSegment = Grids.uCellNorthWall(dx, dy, i, j);
        break;
      case SOUTH:
        naturalSegment = Grids.uCellSouthWall(dx, dy, i, j);
        break;
      case EAST:
        naturalSegment = Grids.uCellEastWall(dx, dy, i, j);
        break;
      case WEST:
        naturalSegment = Grids.uCellWestWall(dx, dy, i, j);
        break;
    }
    Optional<Segment> maybeSegment = Grids.principalSegmentLocation(naturalSegment, mesh);
    if (!maybeSegment.isPresent()) {
      return Optional.empty();
    }
    Segment segment = maybeSegment.get();
    return Optional.of(ImmutableStaggeredCellFace.builder()
        .segment(segment)
        .positiveDirectionDistance(pos.distance(segment.midpoint()))
        .positivePositiveDirectionDistance(posPos.distance(segment.midpoint()))
        .negativeDirectionDistance(neg.distance(segment.midpoint()))
        .negativeNegativeDirectionDistance(negNeg.distance(segment.midpoint()))
        .build());
  }
}
