package jack.fluids.slow;

import com.google.inject.AbstractModule;

public class SlowModule extends AbstractModule {
  private final Grid grid;

  public SlowModule(Grid grid) {
    this.grid = grid;
  }

  @Override
  public void configure() {
    bind(Grid.class).toInstance(grid);
  }
}
