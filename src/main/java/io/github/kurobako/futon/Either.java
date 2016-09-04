/*
 * Copyright (C) 2016 Fedor Gavrilov
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

import static io.github.kurobako.futon.BiFunction.curry;
import static io.github.kurobako.futon.Function.id;
import static io.github.kurobako.futon.Option.none;
import static io.github.kurobako.futon.Option.some;
import static io.github.kurobako.futon.Pair.pair;
import static io.github.kurobako.futon.Util.equal;
import static io.github.kurobako.futon.Util.hash;
import static io.github.kurobako.futon.Util.nonNull;

public abstract class Either<L, R> implements Foldable<R> {
  Either() {}

  public abstract <X> X either(@Nonnull Function<? super L, ? extends X> leftFn, final @Nonnull Function<? super R, ? extends X> rightFn);

  public abstract @Nonnull Either<R, L> swap();

  public abstract @Nonnull <X> Either<L, X> bind(@Nonnull Function<? super R, ? extends Either<L, X>> function);

  public abstract @Nonnull <X, Y> Either<L, Y> zip(@Nonnull Either<L, X> either, @Nonnull BiFunction<? super R, ? super X, ? extends Y> biFunction);

  public abstract @Nonnull <X, Y> Pair<? extends Either<L, X>, ? extends Either<L, Y>> unzip(@Nonnull Function<? super R, Pair<X, Y>> function);

  public abstract @Nonnull <X> Either<L, X> apply(@Nonnull Either<L, ? extends Function<? super R, ? extends X>> either);

  public abstract @Nonnull <X> Either<L, X> map(@Nonnull Function<? super R, ? extends X> function);

  public abstract @Nonnull <X, Y> Either<X, Y> biMap(@Nonnull Function<? super L, ? extends X> leftFunction, @Nonnull Function<? super R, ? extends Y> rightFunction);

  public abstract @Nonnull Option<Left<L, R>> caseLeft();

  public abstract @Nonnull Option<Right<L, R>> caseRight();

  public abstract boolean isLeft();

  public abstract boolean isRight();

  public static @Nonnull <L, R> Left<L, R> left(final L value) {
    return new Left<>(value);
  }

  public static @Nonnull <L, R> Right<L, R> right(final R value) {
    return new Right<>(value);
  }

  public static @Nonnull <L, R> Either<L, R> join(final @Nonnull Either<L, ? extends Either<L, R>> either) {
    return nonNull(either).bind(id());
  }

  public static @Nonnull <L, R> Right<L, R> unit(final R value) {
    return right(value);
  }

  public static final class Left<L, R> extends Either<L, R> {
    public final L left;

    private Left(final L left) {
      this.left = left;
    }

    @Override
    public <X> X either(final @Nonnull Function<? super L, ? extends X> leftFn, final @Nonnull Function<? super R, ? extends X> rightFn) {
      nonNull(leftFn);
      nonNull(rightFn);
      return leftFn.$(left);
    }

    @Override
    public @Nonnull Right<R, L> swap() {
      return right(left);
    }

    @Override
    public @Nonnull <X> Left<L, X> bind(final @Nonnull Function<? super R, ? extends Either<L, X>> function) {
      nonNull(function);
      return self();
    }

    @Override
    public @Nonnull <X, Y> Left<L, Y> zip(final @Nonnull Either<L, X> either, final @Nonnull BiFunction<? super R, ? super X, ? extends Y> biFunction) {
      nonNull(either);
      nonNull(biFunction);
      return self();
    }

    @Override
    public @Nonnull <X, Y> Pair<Left<L, X>, Left<L, Y>> unzip(final @Nonnull Function<? super R, Pair<X, Y>> function) {
      nonNull(function);
      return pair(self(), self());
    }

    @Override
    public @Nonnull <X> Left<L, X> apply(final @Nonnull Either<L, ? extends Function<? super R, ? extends X>> either) {
      nonNull(either);
      return self();
    }

    @Override
    public @Nonnull <X> Left<L, X> map(final @Nonnull Function<? super R, ? extends X> function) {
      nonNull(function);
      return self();
    }

    @SuppressWarnings("unchecked")
    private <X> Left<L, X> self() {
      return (Left<L, X>) this;
    }

    @Override
    public @Nonnull <X, Y> Left<X, Y> biMap(final @Nonnull Function<? super L, ? extends X> leftFunction, @Nonnull Function<? super R, ? extends Y> rightFunction) {
      nonNull(leftFunction);
      nonNull(rightFunction);
      return left(leftFunction.$(left));
    }

    @Override
    public @Nonnull Option.Some<Left<L, R>> caseLeft() {
      return some(this);
    }

    @Override
    public @Nonnull Option.None<Right<L, R>> caseRight() {
      return none();
    }

    @Override
    public boolean isLeft() {
      return true;
    }

    @Override
    public boolean isRight() {
      return false;
    }

    @Override
    public <B> B foldRight(final @Nonnull BiFunction<? super R, ? super B, ? extends B> biFunction, final B initial) {
      nonNull(biFunction);
      return initial;
    }

    @Override
    public <B> B foldLeft(final @Nonnull BiFunction<? super B, ? super R, ? extends B> biFunction, final B initial) {
      nonNull(biFunction);
      return initial;
    }

    @Override
    public int hashCode() {
      return hash(left);
    }

    @Override
    public boolean equals(final Object o) {
      if (!(o instanceof Left)) return false;
      final Left that = (Left) o;
      return equal(this.left, that.left);
    }

    @Override
    public @Nonnull String toString() {
      return "Left " + String.valueOf(left);
    }
  }

  public static final class Right<L, R> extends Either<L, R> {
    public final R right;

    private Right(final R right) {
      this.right = right;
    }

    @Override
    public <X> X either(final @Nonnull Function<? super L, ? extends X> leftFn, final @Nonnull Function<? super R, ? extends X> rightFn) {
      nonNull(leftFn);
      nonNull(rightFn);
      return rightFn.$(right);
    }

    @Override
    public @Nonnull Left<R, L> swap() {
      return left(right);
    }

    @Override
    public @Nonnull <X> Either<L, X> bind(final @Nonnull Function<? super R, ? extends Either<L, X>> function) {
      nonNull(function);
      return function.$(right);
    }

    @Override
    public @Nonnull <X, Y> Either<L, Y> zip(final @Nonnull Either<L, X> either, final @Nonnull BiFunction<? super R, ? super X, ? extends Y> biFunction) {
      nonNull(either);
      nonNull(biFunction);
      return either.map(curry(biFunction).$(right));
    }

    @Override
    public @Nonnull <X, Y> Pair<Right<L, X>, Right<L, Y>> unzip(final @Nonnull Function<? super R, Pair<X, Y>> function) {
      final Pair<? extends X, ? extends Y> xy = nonNull(function).$(right);
      return pair(right(xy.first), right(xy.second));
    }

    @Override
    public @Nonnull <X> Either<L, X> apply(final @Nonnull Either<L, ? extends Function<? super R, ? extends X>> either) {
      return nonNull(either).map(f -> f.$(right));
    }

    @Override
    public @Nonnull <X> Right<L, X> map(final @Nonnull Function<? super R, ? extends X> function) {
      return right(nonNull(function).$(right));
    }

    @Override
    public @Nonnull <X, Y> Either<X, Y> biMap(final @Nonnull Function<? super L, ? extends X> leftFn, final @Nonnull Function<? super R, ? extends Y> rightFn) {
      nonNull(leftFn);
      nonNull(rightFn);
      return right(rightFn.$(right));
    }

    @Override
    public @Nonnull Option.None<Left<L, R>> caseLeft() {
      return none();
    }

    @Override
    public @Nonnull Option.Some<Right<L, R>> caseRight() {
      return some(this);
    }

    @Override
    public boolean isLeft() {
      return false;
    }

    @Override
    public boolean isRight() {
      return true;
    }

    @Override
    public <B> B foldRight(final @Nonnull BiFunction<? super R, ? super B, ? extends B> biFunction, final B initial) {
      return nonNull(biFunction).$(right, initial);
    }

    @Override
    public <B> B foldLeft(final @Nonnull BiFunction<? super B, ? super R, ? extends B> biFunction, final B initial) {
      return nonNull(biFunction).$(initial, right);
    }

    @Override
    public int hashCode() {
      return hash(right);
    }

    @Override
    public boolean equals(final Object o) {
      if (!(o instanceof Right)) return false;
      final Right that = (Right) o;
      return equal(this.right, that.right);
    }

    @Override
    public @Nonnull String toString() {
      return "Right " + String.valueOf(right);
    }
  }
}
