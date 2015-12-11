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

import java.util.Objects;

import static io.github.kurobako.futon.Function.id;
import static java.util.Objects.requireNonNull;

public abstract class Either<L, R> implements Foldable<R> {
  public abstract @Nonnull <X> Either<L, X> bind(@Nonnull Function<? super R, Either<L, X>> function);

  public abstract @Nonnull <X> Either<L, X> apply(@Nonnull Either<L, ? extends Function<? super R, ? extends X>> either);

  public abstract @Nonnull <X> Either<L, X> map(@Nonnull Function<? super R, ? extends X> function);

  public abstract @Nonnull <X, Y> Either<X, Y> biMap(@Nonnull Function<? super L, ? extends X> ifLeft,
                                                     @Nonnull Function<? super R, ? extends Y> ifRight);

  public abstract @Nonnull Either<R, L> swap();

  public abstract @Nonnull Optional<L> caseLeft();

  public abstract @Nonnull Optional<R> caseRight();

  public abstract <X> X either(@Nonnull Function<? super L, ? extends X> ifLeft,
                               @Nonnull Function<? super R, ? extends X> ifRight);

  public static @Nonnull <L, R> Either<L, R> join(final @Nonnull Either<L, Either<L, R>> either) {
    requireNonNull(either, "either");
    return either.bind(id());
  }

  public static @Nonnull <L, R> Either<L, R> left(final L value) {
    return new Left<>(new Optional.Some<>(value));
  }

  public static @Nonnull <L, R> Either<L, R> right(final R value) {
    return new Right<>(new Optional.Some<>(value));
  }

  abstract void NOT_FOR_EXTENSION();

  final static class Left<L, R> extends Either<L, R> {
    final Optional.Some<L> someL;

    Left(final @Nonnull Optional.Some<L> someL) {
      assert someL !=  null;
      this.someL = someL;
    }

    @Override
    @SuppressWarnings("unchecked")
    public @Nonnull<X> Either<L, X> bind(final @Nonnull Function<? super R, Either<L, X>> function) {
      requireNonNull(function, "function");
      return (Either<L, X>) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public @Nonnull<X> Either<L, X> apply(final @Nonnull Either<L, ? extends Function<? super R, ? extends X>> either) {
      requireNonNull(either, "either");
      return (Either<L, X>) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public @Nonnull<X> Either<L, X> map(final @Nonnull Function<? super R, ? extends X> function) {
      requireNonNull(function, "function");
      return (Either<L, X>) this;
    }

    @Override
    public @Nonnull <X, Y> Either<X, Y> biMap(final @Nonnull Function<? super L, ? extends X> ifLeft,
                                              final @Nonnull Function<? super R, ? extends Y> ifRight) {
      requireNonNull(ifLeft, "ifLeft");
      requireNonNull(ifRight, "ifRight");
      return left(ifLeft.$(someL.value));
    }

    @Override
    public @Nonnull Either<R, L> swap() {
      return new Right<>(someL);
    }

    @Override
    public @Nonnull Optional<L> caseLeft() {
      return someL;
    }

    @Override
    public @Nonnull Optional<R> caseRight() {
      return Optional.none();
    }

    @Override
    public <X> X either(final @Nonnull Function<? super L, ? extends X> ifLeft,
                        final @Nonnull Function<? super R, ? extends X> ifRight) {
      requireNonNull(ifLeft, "left");
      requireNonNull(ifRight, "right");
      return ifLeft.$(someL.value);
    }

    @Override
    public <B> B foldRight(final @Nonnull BiFunction<? super R, ? super B, ? extends B> function, final B initial) {
      requireNonNull(function, "function");
      return initial;
    }

    @Override
    public <B> B foldLeft(final @Nonnull BiFunction<? super B, ? super R, ? extends B> function, final B initial) {
      requireNonNull(function, "function");
      return initial;
    }

    @Override
    public int hashCode() {
      return someL.hashCode();
    }

    @Override
    public boolean equals(final Object o) {
      if (!(o instanceof Left)) return false;
      Left that = (Left) o;
      return Objects.equals(this.someL, that.someL);
    }

    @Override
    public String toString() {
      return "Left " + someL.value;
    }

    @Override
    void NOT_FOR_EXTENSION() {}
  }

  final static class Right<L, R> extends Either<L, R> {
    final Optional.Some<R> someR;

    Right(final @Nonnull Optional.Some<R> someR) {
      assert someR != null;
      this.someR = someR;
    }

    @Override
    public @Nonnull<X> Either<L, X> bind(final @Nonnull Function<? super R, Either<L, X>> function) {
      requireNonNull(function, "function");
      return function.$(someR.value);
    }

    @Override
    public @Nonnull<X> Either<L, X> apply(final @Nonnull Either<L, ? extends Function<? super R, ? extends X>> either) {
      requireNonNull(either, "either");
      return either.map(f -> f.$(someR.value));
    }

    @Override
    public @Nonnull<X> Either<L, X> map(final @Nonnull Function<? super R, ? extends X> function) {
      requireNonNull(function, "function");
      return new Right<>(new Optional.Some<>(function.$(someR.value)));
    }

    @Override
    public @Nonnull <X, Y> Either<X, Y> biMap(final @Nonnull Function<? super L, ? extends X> ifLeft,
                                              final @Nonnull Function<? super R, ? extends Y> ifRight) {
      requireNonNull(ifLeft, "ifLeft");
      requireNonNull(ifRight, "ifRight");
      return right(ifRight.$(someR.value));
    }

    @Override
    public @Nonnull Either<R, L> swap() {
      return new Left<>(someR);
    }

    @Override
    public @Nonnull Optional<L> caseLeft() {
      return Optional.none();
    }

    @Override
    public @Nonnull Optional<R> caseRight() {
      return someR;
    }

    @Override
    public <X> X either(final @Nonnull Function<? super L, ? extends X> ifLeft,
                        final @Nonnull Function<? super R, ? extends X> ifRight) {
      requireNonNull(ifLeft, "left");
      requireNonNull(ifRight, "right");
      return ifRight.$(someR.value);
    }

    @Override
    public <B> B foldRight(final @Nonnull BiFunction<? super R, ? super B, ? extends B> function, final B initial) {
      requireNonNull(function, "function");
      return function.$(someR.value, initial);
    }

    @Override
    public <B> B foldLeft(final @Nonnull BiFunction<? super B, ? super R, ? extends B> function, final B initial) {
      requireNonNull(function, "function");
      return function.$(initial, someR.value);
    }

    @Override
    public int hashCode() {
      return someR.hashCode();
    }

    @Override
    public boolean equals(final Object o) {
      if (!(o instanceof Right)) return false;
      Right that = (Right) o;
      return Objects.equals(this.someR, that.someR);
    }

    @Override
    public String toString() {
      return "Right " + someR.value;
    }

    @Override
    void NOT_FOR_EXTENSION() {}
  }
}
