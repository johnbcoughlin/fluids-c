package jack.fluids.util;

public class Pair<L, R> {
  private final L left;
  private final R right;

  private Pair(L left, R right) {
    this.left = left;
    this.right = right;
  }

  public static <L, R> Pair<L, R> of(L left, R right) {
    return new Pair(left, right);
  }

  public L left() {
    return left;
  }

  public R right() {
    return right;
  }
}
