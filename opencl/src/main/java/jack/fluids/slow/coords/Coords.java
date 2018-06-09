package jack.fluids.slow.coords;

import org.immutables.value.Value;

public interface Coords {
  @Value.Parameter
  int i();

  @Value.Parameter
  int j();
}
