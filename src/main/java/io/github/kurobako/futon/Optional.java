package io.github.kurobako.futon;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

import static io.github.kurobako.futon.Function.id;
import static java.util.Objects.requireNonNull;

public abstract class Optional<A> implements Iterable<A> {
  public abstract @Nonnull <B> Optional<B> bind(@Nonnull Function<? super A, Optional<B>> function);

  public abstract @Nonnull <B> Optional<B> apply(@Nonnull Optional<? extends Function<? super A, ? extends B>> optional);

  public abstract @Nonnull <B> Optional<B> map(@Nonnull Function<? super A, ? extends B> function);

  public abstract @Nonnull Optional<A> filter(@Nonnull Predicate<A> predicate);

  public abstract @Nullable A value();

  public abstract boolean isPresent();

  public static @Nonnull <A> Optional<A> join(final @Nonnull Optional<Optional<A>> optional) {
    requireNonNull(optional, "optional");
    return optional.bind(id());
  }

  public static @Nonnull <A> Optional<A> some(final A value) {
    return new Some<>(value);
  }

  @SuppressWarnings("unchecked")
  public static @Nonnull <A> Optional<A> none() {
    return None.INSTANCE;
  }

  final static class Some<A> extends Optional<A> {
    final A value;

    Some(final A value) {
      this.value = value;
    }

    @Override
    public @Nonnull<B> Optional<B> bind(final @Nonnull Function<? super A, Optional<B>> function) {
      requireNonNull(function, "function");
      return function.$(value);
    }

    @Override
    public @Nonnull <B> Optional<B> apply(final @Nonnull
                                          Optional<? extends Function<? super A, ? extends B>> optional) {
      requireNonNull(optional, "optional");
      return bind(a -> optional.bind(f -> some(f.$(a))));
    }

    @Override
    public @Nonnull <B> Optional<B> map(final @Nonnull Function<? super A, ? extends B> function) {
      requireNonNull(function, "function");
      return some(function.$(value));
    }

    @Override
    public @Nonnull Optional<A> filter(@Nonnull Predicate<A> predicate) {
      requireNonNull(predicate, "predicate");
      return predicate.$(value) ? this : none();
    }

    @Override
    public A value() {
      return value;
    }

    @Override
    public boolean isPresent() {
      return true;
    }

    @Override
    public Iterator<A> iterator() {
      return new Iterator<A>() {
        boolean wasConsumed;

        @Override
        public boolean hasNext() {
          return !wasConsumed;
        }

        @Override
        public A next() {
          if (wasConsumed) throw new NoSuchElementException();
          wasConsumed = true;
          return value;
        }
      };
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(value);
    }

    @Override
    public boolean equals(final Object o) {
      if (!(o instanceof Some)) return false;
      Some that = (Some) o;
      return Objects.equals(this.value, that.value);
    }

    @Override
    public String toString() {
      return "Some " + value;
    }
  }

  final static class None extends Optional {
    private None() {}

    @Override
    public @Nonnull Optional bind(final @Nonnull Function function) {
      requireNonNull(function, "function");
      return this;
    }

    @Override
    public @Nonnull Optional apply(final @Nonnull Optional optional) {
      requireNonNull(optional, "optional");
      return this;
    }


    @Override
    public @Nonnull Optional map(final @Nonnull Function function) {
      requireNonNull(function, "function");
      return this;
    }

    @Override
    public @Nonnull Optional filter(@Nonnull Predicate predicate) {
      requireNonNull(predicate, "predicate");
      return this;
    }

    @Override
    public @Nullable Object value() {
      return null;
    }

    @Override
    public boolean isPresent() {
      return false;
    }

    @Override
    public Iterator iterator() {
      return Collections.emptyIterator();
    }

    @Override
    public String toString() {
      return "None";
    }

    static final None INSTANCE = new None();
  }
}
