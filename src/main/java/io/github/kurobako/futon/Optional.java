/*
 * Copyright (C) 2015 Fedor Gavrilov
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA
 */

package io.github.kurobako.futon;

import javax.annotation.Nonnull;

import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

import static io.github.kurobako.futon.Function.id;
import static java.util.Objects.requireNonNull;

public abstract class Optional<A> implements Foldable<A>, Iterable<A> {
  Optional() {}

  public abstract @Nonnull <B> Optional<B> bind(@Nonnull Function<? super A, ? extends Optional<B>> function);

  public abstract @Nonnull <B> Optional<B> apply(@Nonnull Optional<? extends Function<? super A, ? extends B>> optional);

  public abstract @Nonnull <B> Optional<B> map(@Nonnull Function<? super A, ? extends B> function);

  public abstract @Nonnull Optional<A> filter(@Nonnull Predicate<A> predicate);

  public abstract @Nonnull Optional<Some<A>> caseSome();

  public abstract @Nonnull Optional<None> caseNone();

  public static @Nonnull <A> Optional<A> join(final @Nonnull Optional<? extends Optional<A>> optional) {
    requireNonNull(optional, "optional");
    return optional.bind(id());
  }

  public static @Nonnull <A> Optional.Some<A> some(final A value) {
    return new Some<>(value);
  }

  @SuppressWarnings("unchecked")
  public static @Nonnull <A> Optional.None<A> none() {
    return None.INSTANCE;
  }

  public static final class Some<A> extends Optional<A> {
    public final A value;

    Some(final A value) {
      this.value = value;
    }

    @Override
    public @Nonnull<B> Optional<B> bind(final @Nonnull Function<? super A, ? extends Optional<B>> function) {
      requireNonNull(function, "function");
      return function.$(value);
    }

    @Override
    public @Nonnull <B> Optional<B> apply(final @Nonnull
                                          Optional<? extends Function<? super A, ? extends B>> optional) {
      requireNonNull(optional, "optional");
      return optional.bind(f -> some(f.$(value)));
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
    public <B> B foldRight(final @Nonnull BiFunction<? super A, ? super B, ? extends B> function, final B initial) {
      requireNonNull(function, "function");
      return function.$(value, initial);
    }

    @Override
    public <B> B foldLeft(final @Nonnull BiFunction<? super B, ? super A, ? extends B> function, final B initial) {
      requireNonNull(function, "function");
      return function.$(initial, value);
    }

    public @Nonnull Optional<Some<A>> caseSome() {
      return some(this);
    }

    @Override
    @SuppressWarnings("unchecked")
    public @Nonnull Optional<None> caseNone() {
      return none();
    }

    @Override
    public Iterator<A> iterator() {
      return new Iterator<A>() {
        private boolean wasConsumed;

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

  @SuppressWarnings("unchecked")
  public static final class None<A> extends Optional<A> {
    private static final None INSTANCE = new None();
    private static final Optional.Some<None> SOME_NONE = some(INSTANCE);

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
    public Object foldRight(final @Nonnull BiFunction function, final Object initial) {
      requireNonNull(function, "function");
      return initial;
    }

    @Override
    public Object foldLeft(final @Nonnull BiFunction function, final Object initial) {
      requireNonNull(function, "function");
      return initial;
    }

    @Override
    public @Nonnull Optional<Some<A>> caseSome() {
      return INSTANCE;
    }

    @Override
    public @Nonnull Optional<None> caseNone() {
      return SOME_NONE;
    }

    @Override
    public Iterator iterator() {
      return Collections.emptyIterator();
    }

    @Override
    public String toString() {
      return "None";
    }
  }
}