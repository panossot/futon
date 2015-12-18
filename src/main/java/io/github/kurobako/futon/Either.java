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
import static io.github.kurobako.futon.Optional.none;
import static io.github.kurobako.futon.Optional.some;
import static java.util.Objects.requireNonNull;

public abstract class Either<L, R> implements Foldable<R> {
  Either() {}

  public abstract @Nonnull <X> Either<L, X> bind(@Nonnull Function<? super R, ? extends Either<L, X>> bind);

  public abstract @Nonnull <X> Either<L, X> apply(@Nonnull Either<L, ? extends Function<? super R, ? extends X>> either);

  public abstract @Nonnull <X> Either<L, X> map(@Nonnull Function<? super R, ? extends X> map);

  public abstract @Nonnull <X, Y> Either<L, Y> zip(@Nonnull Either<L, X> another,
                                                   @Nonnull BiFunction<? super R, ? super X, ? extends Y> zip);

  public abstract @Nonnull <X, Y> Either<X, Y> biMap(@Nonnull Function<? super L, ? extends X> ifLeft,
                                                     @Nonnull Function<? super R, ? extends Y> ifRight);

  public abstract @Nonnull Either<R, L> swap();

  public abstract <X> X either(@Nonnull Function<? super L, ? extends X> ifLeft,
                               @Nonnull Function<? super R, ? extends X> ifRight);

  public abstract @Nonnull Optional<Left<L, R>> caseLeft();

  public abstract @Nonnull Optional<Right<L, R>> caseRight();

  public static @Nonnull <L, R> Either<L, R> join(final @Nonnull Either<L, ? extends Either<L, R>> either) {
    requireNonNull(either, "either");
    return either.bind(id());
  }

  public static @Nonnull <L, R> Either.Right<L, R> right(final R value) {
    return new Right<>(value);
  }

  public static @Nonnull <L, R> Either.Left<L, R> left(final L value) {
    return new Left<>(value);
  }

  public static final class Right<L, R> extends Either<L, R> {
    public final R right;

    Right(final R value) {
      this.right = value;
    }

    @Override
    public @Nonnull<X> Either<L, X> bind(final @Nonnull Function<? super R, ? extends Either<L, X>> bind) {
      requireNonNull(bind, "bind");
      return bind.$(right);
    }

    @Override
    public @Nonnull<X> Either<L, X> apply(final @Nonnull Either<L, ? extends Function<? super R, ? extends X>> either) {
      requireNonNull(either, "either");
      return either.map(f -> f.$(right));
    }

    @Override
    public @Nonnull<X> Right<L, X> map(final @Nonnull Function<? super R, ? extends X> map) {
      requireNonNull(map, "map");
      return right(map.$(right));
    }

    @Override
    public @Nonnull <X, Y> Either<L, Y> zip(final @Nonnull Either<L, X> another,
                                            final @Nonnull BiFunction<? super R, ? super X, ? extends Y> zip) {
      requireNonNull(another, "another");
      requireNonNull(zip, "zip");
      if (!(another instanceof Right)) return (Left<L, Y>) another;
      Right<L, X> that = (Right<L, X>) another;
      return right(zip.$(this.right, that.right));
    }

    @Override
    public @Nonnull <X, Y> Right<X, Y> biMap(final @Nonnull Function<? super L, ? extends X> ifLeft,
                                             final @Nonnull Function<? super R, ? extends Y> ifRight) {
      requireNonNull(ifLeft, "ifLeft");
      requireNonNull(ifRight, "ifRight");
      return right(ifRight.$(right));
    }

    @Override
    public @Nonnull Left<R, L> swap() {
      return left(right);
    }


    @Override
    public <X> X either(final @Nonnull Function<? super L, ? extends X> ifLeft,
                        final @Nonnull Function<? super R, ? extends X> ifRight) {
      requireNonNull(ifLeft, "left");
      requireNonNull(ifRight, "right");
      return ifRight.$(right);
    }

    public @Nonnull Optional.None<Left<L, R>> caseLeft() {
      return none();
    }

    @Override
    public @Nonnull Optional.Some<Right<L, R>> caseRight() {
      return some(this);
    }

    @Override
    public <B> B foldRight(final @Nonnull BiFunction<? super R, ? super B, ? extends B> fold, final B initial) {
      requireNonNull(fold, "fold");
      return fold.$(right, initial);
    }

    @Override
    public <B> B foldLeft(final @Nonnull BiFunction<? super B, ? super R, ? extends B> fold, final B initial) {
      requireNonNull(fold, "fold");
      return fold.$(initial, right);
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(right);
    }

    @Override
    public boolean equals(final Object o) {
      if (!(o instanceof Right)) return false;
      final Right that = (Right) o;
      return Objects.equals(this.right, that.right);
    }

    @Override
    public String toString() {
      return "Right " + right;
    }
  }

  public static final class Left<L, R> extends Either<L, R> {
    public final L left;

    Left(final L value) {
      this.left = value;
    }

    @Override
    @SuppressWarnings("unchecked")
    public @Nonnull<X> Left<L, X> bind(final @Nonnull Function<? super R, ? extends Either<L, X>> bind) {
      requireNonNull(bind, "bind");
      return (Left<L, X>) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public @Nonnull<X> Left<L, X> apply(final @Nonnull Either<L, ? extends Function<? super R, ? extends X>> either) {
      requireNonNull(either, "either");
      return (Left<L, X>) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public @Nonnull<X> Left<L, X> map(final @Nonnull Function<? super R, ? extends X> map) {
      requireNonNull(map, "map");
      return (Left<L, X>) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public @Nonnull <X, Y> Left<L, Y> zip(final @Nonnull Either<L, X> another,
                                          final @Nonnull BiFunction<? super R, ? super X, ? extends Y> zip) {
      requireNonNull(another, "another");
      requireNonNull(zip, "zip");
      return (Left<L, Y>) this;
    }

    @Override
    public @Nonnull <X, Y> Left<X, Y> biMap(final @Nonnull Function<? super L, ? extends X> ifLeft,
                                            final @Nonnull Function<? super R, ? extends Y> ifRight) {
      requireNonNull(ifLeft, "ifLeft");
      requireNonNull(ifRight, "ifRight");
      return left(ifLeft.$(left));
    }

    @Override
    public @Nonnull Right<R, L> swap() {
      return right(left);
    }

    @Override
    public <X> X either(final @Nonnull Function<? super L, ? extends X> ifLeft,
                        final @Nonnull Function<? super R, ? extends X> ifRight) {
      requireNonNull(ifLeft, "left");
      requireNonNull(ifRight, "right");
      return ifLeft.$(left);
    }

    @Override
    public @Nonnull Optional.Some<Left<L, R>> caseLeft() {
      return some(this);
    }

    @Override
    public @Nonnull Optional.None<Right<L, R>> caseRight() {
      return none();
    }

    @Override
    public <B> B foldRight(final @Nonnull BiFunction<? super R, ? super B, ? extends B> fold, final B initial) {
      requireNonNull(fold, "fold");
      return initial;
    }

    @Override
    public <B> B foldLeft(final @Nonnull BiFunction<? super B, ? super R, ? extends B> fold, final B initial) {
      requireNonNull(fold, "fold");
      return initial;
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(left);
    }

    @Override
    public boolean equals(final Object o) {
      if (!(o instanceof Left)) return false;
      final Left that = (Left) o;
      return Objects.equals(this.left, that.left);
    }

    @Override
    public String toString() {
      return "Left " + left;
    }
  }
}