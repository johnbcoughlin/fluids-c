//-no-import-rewrite
package jack.fluids.buffers;

import java.lang.Object;
import java.lang.String;
import java.lang.Float;
import java.lang.Double;

/**
 * Immutable implementation of {@link FloatBuffer1D}.
 * <p>
 * Use the builder to create immutable instances:
 * {@code ImmutableFloatBuffer1D.builder()}.
 */
@SuppressWarnings({"all"})
@javax.annotation.ParametersAreNonnullByDefault
@javax.annotation.Generated({"Immutables.generator", "FloatBuffer1D"})
@javax.annotation.concurrent.Immutable
@javax.annotation.CheckReturnValue
public final class ImmutableFloatBuffer1D implements jack.fluids.buffers.FloatBuffer1D {
  private final org.jocl.cl_mem buffer;
  private final int length;

  private ImmutableFloatBuffer1D(org.jocl.cl_mem buffer, int length) {
    this.buffer = buffer;
    this.length = length;
  }

  /**
   * @return The value of the {@code buffer} attribute
   */
  @Override
  public org.jocl.cl_mem buffer() {
    return buffer;
  }

  /**
   * @return The value of the {@code length} attribute
   */
  @Override
  public int length() {
    return length;
  }

  /**
   * Copy the current immutable object by setting a value for the {@link FloatBuffer1D#buffer() buffer} attribute.
   * A shallow reference equality check is used to prevent copying of the same value by returning {@code this}.
   * @param value A new value for buffer
   * @return A modified copy of the {@code this} object
   */
  public final ImmutableFloatBuffer1D withBuffer(org.jocl.cl_mem value) {
    if (this.buffer == value) return this;
    org.jocl.cl_mem newValue = java.util.Objects.requireNonNull(value, "buffer");
    return new ImmutableFloatBuffer1D(newValue, this.length);
  }

  /**
   * Copy the current immutable object by setting a value for the {@link FloatBuffer1D#length() length} attribute.
   * A value equality check is used to prevent copying of the same value by returning {@code this}.
   * @param value A new value for length
   * @return A modified copy of the {@code this} object
   */
  public final ImmutableFloatBuffer1D withLength(int value) {
    if (this.length == value) return this;
    return new ImmutableFloatBuffer1D(this.buffer, value);
  }

  /**
   * This instance is equal to all instances of {@code ImmutableFloatBuffer1D} that have equal attribute values.
   * @return {@code true} if {@code this} is equal to {@code another} instance
   */
  @Override
  public boolean equals(@javax.annotation.Nullable Object another) {
    if (this == another) return true;
    return another instanceof ImmutableFloatBuffer1D
        && equalTo((ImmutableFloatBuffer1D) another);
  }

  private boolean equalTo(ImmutableFloatBuffer1D another) {
    return buffer.equals(another.buffer)
        && length == another.length;
  }

  /**
   * Computes a hash code from attributes: {@code buffer}, {@code length}.
   * @return hashCode value
   */
  @Override
  public int hashCode() {
    int h = 5381;
    h += (h << 5) + buffer.hashCode();
    h += (h << 5) + length;
    return h;
  }

  /**
   * Prints the immutable value {@code FloatBuffer1D} with attribute values.
   * @return A string representation of the value
   */
  @Override
  public String toString() {
    return com.google.common.base.MoreObjects.toStringHelper("FloatBuffer1D")
        .omitNullValues()
        .add("buffer", buffer)
        .add("length", length)
        .toString();
  }

  /**
   * Creates an immutable copy of a {@link FloatBuffer1D} value.
   * Uses accessors to get values to initialize the new immutable instance.
   * If an instance is already immutable, it is returned as is.
   * @param instance The instance to copy
   * @return A copied immutable FloatBuffer1D instance
   */
  public static ImmutableFloatBuffer1D copyOf(FloatBuffer1D instance) {
    if (instance instanceof ImmutableFloatBuffer1D) {
      return (ImmutableFloatBuffer1D) instance;
    }
    return ImmutableFloatBuffer1D.builder()
        .from(instance)
        .build();
  }

  /**
   * Creates a builder for {@link ImmutableFloatBuffer1D ImmutableFloatBuffer1D}.
   * @return A new ImmutableFloatBuffer1D builder
   */
  public static ImmutableFloatBuffer1D.Builder builder() {
    return new ImmutableFloatBuffer1D.Builder();
  }

  /**
   * Builds instances of type {@link ImmutableFloatBuffer1D ImmutableFloatBuffer1D}.
   * Initialize attributes and then invoke the {@link #build()} method to create an
   * immutable instance.
   * <p><em>{@code Builder} is not thread-safe and generally should not be stored in a field or collection,
   * but instead used immediately to create instances.</em>
   */
  @javax.annotation.concurrent.NotThreadSafe
  public static final class Builder {
    private static final long INIT_BIT_BUFFER = 0x1L;
    private static final long INIT_BIT_LENGTH = 0x2L;
    private long initBits = 0x3L;

    private @javax.annotation.Nullable org.jocl.cl_mem buffer;
    private int length;

    private Builder() {
    }

    /**
     * Fill a builder with attribute values from the provided {@code FloatBuffer1D} instance.
     * Regular attribute values will be replaced with those from the given instance.
     * Absent optional values will not replace present values.
     * @param instance The instance from which to copy values
     * @return {@code this} builder for use in a chained invocation
     */
    @com.google.errorprone.annotations.CanIgnoreReturnValue 
    public final Builder from(FloatBuffer1D instance) {
      java.util.Objects.requireNonNull(instance, "instance");
      buffer(instance.buffer());
      length(instance.length());
      return this;
    }

    /**
     * Initializes the value for the {@link FloatBuffer1D#buffer() buffer} attribute.
     * @param buffer The value for buffer 
     * @return {@code this} builder for use in a chained invocation
     */
    @com.google.errorprone.annotations.CanIgnoreReturnValue 
    public final Builder buffer(org.jocl.cl_mem buffer) {
      this.buffer = java.util.Objects.requireNonNull(buffer, "buffer");
      initBits &= ~INIT_BIT_BUFFER;
      return this;
    }

    /**
     * Initializes the value for the {@link FloatBuffer1D#length() length} attribute.
     * @param length The value for length 
     * @return {@code this} builder for use in a chained invocation
     */
    @com.google.errorprone.annotations.CanIgnoreReturnValue 
    public final Builder length(int length) {
      this.length = length;
      initBits &= ~INIT_BIT_LENGTH;
      return this;
    }

    /**
     * Builds a new {@link ImmutableFloatBuffer1D ImmutableFloatBuffer1D}.
     * @return An immutable instance of FloatBuffer1D
     * @throws java.lang.IllegalStateException if any required attributes are missing
     */
    public ImmutableFloatBuffer1D build() {
      if (initBits != 0) {
        throw new java.lang.IllegalStateException(formatRequiredAttributesMessage());
      }
      return new ImmutableFloatBuffer1D(buffer, length);
    }

    private String formatRequiredAttributesMessage() {
      java.util.List<String> attributes = com.google.common.collect.Lists.newArrayList();
      if ((initBits & INIT_BIT_BUFFER) != 0) attributes.add("buffer");
      if ((initBits & INIT_BIT_LENGTH) != 0) attributes.add("length");
      return "Cannot build FloatBuffer1D, some of required attributes are not set " + attributes;
    }
  }
}
