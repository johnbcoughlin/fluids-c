package jack.fluids.slow;

import org.immutables.gson.Gson;
import org.immutables.value.Value;

@Value.Immutable
@Gson.TypeAdapters
public interface PointVal extends Point {
}
