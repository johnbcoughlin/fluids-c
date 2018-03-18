//-no-import-rewrite
package jack.fluids.buffers;

import java.lang.Object;
import java.lang.String;
import java.lang.Float;
import java.lang.Double;

/**
 * Immutable implementation of {@link SharedVBO}.
 * <p>
 * Use the builder to create immutable instances:
 * {@code ImmutableSharedVBO.builder()}.
 */
@SuppressWarnings({"all"})
@javax.annotation.ParametersAreNonnullByDefault
@javax.annotation.Generated({"Immutables.generator", "SharedVBO"})
@javax.annotation.concurrent.Immutable
@javax.annotation.CheckReturnValue
public final class ImmutableSharedVBO implements jack.fluids.buffers.SharedVBO {
  private final int length;
  private final int glBufferName;
  private final org.jocl.cl_mem clBufferHandle;

  private ImmutableSharedVBO(int length, int glBufferName, org.jocl.cl_mem clBufferHandle) {
    this.length = length;
    this.glBufferName = glBufferName;
    this.clBufferHandle = clBufferHandle;
  }

  /**
   * The length of the buffer, in floats
   */
  @Override
  public int length() {
    return length;
  }

  /**
   * @return The value of the {@code glBufferName} attribute
   */
  @Override
  public int glBufferName() {
    return glBufferName;
  }

  /**
   * @return The value of the {@code clBufferHandle} attribute
   */
  @Override
  public org.jocl.cl_mem clBufferHandle() {
    return clBufferHandle;
  }

  /**
   * Copy the current immutable object by setting a value for the {@link SharedVBO#length() length} attribute.
   * A value equality check is used to prevent copying of the same value by returning {@code this}.
   * @param value A new value for length
   * @return A modified copy of the {@code this} object
   */
  public final ImmutableSharedVBO withLength(int value) {
    if (this.length == value) return this;
    return new ImmutableSharedVBO(value, this.glBufferName, this.clBufferHandle);
  }

  /**
   * Copy the current immutable object by setting a value for the {@link SharedVBO#glBufferName() glBufferName} attribute.
   * A value equality check is used to prevent copying of the same value by returning {@code this}.
   * @param value A new value for glBufferName
   * @return A modified copy of the {@code this} object
   */
  public final ImmutableSharedVBO withGlBufferName(int value) {
    if (this.glBufferName == value) return this;
    return new ImmutableSharedVBO(this.length, value, this.clBufferHandle);
  }

  /**
   * Copy the current immutable object by setting a value for the {@link SharedVBO#clBufferHandle() clBufferHandle} attribute.
   * A shallow reference equality check is used to prevent copying of the same value by returning {@code this}.
   * @param value A new value for clBufferHandle
   * @return A modified copy of the {@code this} object
   */
  public final ImmutableSharedVBO withClBufferHandle(org.jocl.cl_mem value) {
    if (this.clBufferHandle == value) return this;
    org.jocl.cl_mem newValue = java.util.Objects.requireNonNull(value, "clBufferHandle");
    return new ImmutableSharedVBO(this.length, this.glBufferName, newValue);
  }

  /**
   * This instance is equal to all instances of {@code ImmutableSharedVBO} that have equal attribute values.
   * @return {@code true} if {@code this} is equal to {@code another} instance
   */
  @Override
  public boolean equals(@javax.annotation.Nullable Object another) {
    if (this == another) return true;
    return another instanceof ImmutableSharedVBO
        && equalTo((ImmutableSharedVBO) another);
  }

  private boolean equalTo(ImmutableSharedVBO another) {
    return length == another.length
        && glBufferName == another.glBufferName
        && clBufferHandle.equals(another.clBufferHandle);
  }

  /**
   * Computes a hash code from attributes: {@code length}, {@code glBufferName}, {@code clBufferHandle}.
   * @return hashCode value
   */
  @Override
  public int hashCode() {
    int h = 5381;
    h += (h << 5) + length;
    h += (h << 5) + glBufferName;
    h += (h << 5) + clBufferHandle.hashCode();
    return h;
  }

  /**
   * Prints the immutable value {@code SharedVBO} with attribute values.
   * @return A string representation of the value
   */
  @Override
  public String toString() {
    return com.google.common.base.MoreObjects.toStringHelper("SharedVBO")
        .omitNullValues()
        .add("length", length)
        .add("glBufferName", glBufferName)
        .add("clBufferHandle", clBufferHandle)
        .toString();
  }

  /**
   * Creates an immutable copy of a {@link SharedVBO} value.
   * Uses accessors to get values to initialize the new immutable instance.
   * If an instance is already immutable, it is returned as is.
   * @param instance The instance to copy
   * @return A copied immutable SharedVBO instance
   */
  public static ImmutableSharedVBO copyOf(SharedVBO instance) {
    if (instance instanceof ImmutableSharedVBO) {
      return (ImmutableSharedVBO) instance;
    }
    return ImmutableSharedVBO.builder()
        .from(instance)
        .build();
  }

  /**
   * Creates a builder for {@link ImmutableSharedVBO ImmutableSharedVBO}.
   * @return A new ImmutableSharedVBO builder
   */
  public static ImmutableSharedVBO.Builder builder() {
    return new ImmutableSharedVBO.Builder();
  }

  /**
   * Builds instances of type {@link ImmutableSharedVBO ImmutableSharedVBO}.
   * Initialize attributes and then invoke the {@link #build()} method to create an
   * immutable instance.
   * <p><em>{@code Builder} is not thread-safe and generally should not be stored in a field or collection,
   * but instead used immediately to create instances.</em>
   */
  @javax.annotation.concurrent.NotThreadSafe
  public static final class Builder {
    private static final long INIT_BIT_LENGTH = 0x1L;
    private static final long INIT_BIT_GL_BUFFER_NAME = 0x2L;
    private static final long INIT_BIT_CL_BUFFER_HANDLE = 0x4L;
    private long initBits = 0x7L;

    private int length;
    private int glBufferName;
    private @javax.annotation.Nullable org.jocl.cl_mem clBufferHandle;

    private Builder() {
    }

    /**
     * Fill a builder with attribute values from the provided {@code jack.fluids.buffers.SharedVBO} instance.
     * @param instance The instance from which to copy values
     * @return {@code this} builder for use in a chained invocation
     */
    @com.google.errorprone.annotations.CanIgnoreReturnValue 
    public final Builder from(jack.fluids.buffers.SharedVBO instance) {
      java.util.Objects.requireNonNull(instance, "instance");
      from((Object) instance);
      return this;
    }

    /**
     * Fill a builder with attribute values from the provided {@code jack.fluids.buffers.SizedBuffer1D} instance.
     * @param instance The instance from which to copy values
     * @return {@code this} builder for use in a chained invocation
     */
    @com.google.errorprone.annotations.CanIgnoreReturnValue 
    public final Builder from(jack.fluids.buffers.SizedBuffer1D instance) {
      java.util.Objects.requireNonNull(instance, "instance");
      from((Object) instance);
      return this;
    }

    private void from(Object object) {
      long bits = 0;
      if (object instanceof jack.fluids.buffers.SharedVBO) {
        jack.fluids.buffers.SharedVBO instance = (jack.fluids.buffers.SharedVBO) object;
        if ((bits & 0x1L) == 0) {
          length(instance.length());
          bits |= 0x1L;
        }
        clBufferHandle(instance.clBufferHandle());
        glBufferName(instance.glBufferName());
      }
      if (object instanceof jack.fluids.buffers.SizedBuffer1D) {
        jack.fluids.buffers.SizedBuffer1D instance = (jack.fluids.buffers.SizedBuffer1D) object;
        if ((bits & 0x1L) == 0) {
          length(instance.length());
          bits |= 0x1L;
        }
      }
    }

    /**
     * Initializes the value for the {@link SharedVBO#length() length} attribute.
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
     * Initializes the value for the {@link SharedVBO#glBufferName() glBufferName} attribute.
     * @param glBufferName The value for glBufferName 
     * @return {@code this} builder for use in a chained invocation
     */
    @com.google.errorprone.annotations.CanIgnoreReturnValue 
    public final Builder glBufferName(int glBufferName) {
      this.glBufferName = glBufferName;
      initBits &= ~INIT_BIT_GL_BUFFER_NAME;
      return this;
    }

    /**
     * Initializes the value for the {@link SharedVBO#clBufferHandle() clBufferHandle} attribute.
     * @param clBufferHandle The value for clBufferHandle 
     * @return {@code this} builder for use in a chained invocation
     */
    @com.google.errorprone.annotations.CanIgnoreReturnValue 
    public final Builder clBufferHandle(org.jocl.cl_mem clBufferHandle) {
      this.clBufferHandle = java.util.Objects.requireNonNull(clBufferHandle, "clBufferHandle");
      initBits &= ~INIT_BIT_CL_BUFFER_HANDLE;
      return this;
    }

    /**
     * Builds a new {@link ImmutableSharedVBO ImmutableSharedVBO}.
     * @return An immutable instance of SharedVBO
     * @throws java.lang.IllegalStateException if any required attributes are missing
     */
    public ImmutableSharedVBO build() {
      if (initBits != 0) {
        throw new java.lang.IllegalStateException(formatRequiredAttributesMessage());
      }
      return new ImmutableSharedVBO(length, glBufferName, clBufferHandle);
    }

    private String formatRequiredAttributesMessage() {
      java.util.List<String> attributes = com.google.common.collect.Lists.newArrayList();
      if ((initBits & INIT_BIT_LENGTH) != 0) attributes.add("length");
      if ((initBits & INIT_BIT_GL_BUFFER_NAME) != 0) attributes.add("glBufferName");
      if ((initBits & INIT_BIT_CL_BUFFER_HANDLE) != 0) attributes.add("clBufferHandle");
      return "Cannot build SharedVBO, some of required attributes are not set " + attributes;
    }
  }
}
