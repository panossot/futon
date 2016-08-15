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

import java.util.Objects;

import static io.github.kurobako.futon.BiFunction.curry;
import static io.github.kurobako.futon.Function.id;
import static io.github.kurobako.futon.Option.none;
import static io.github.kurobako.futon.Option.some;
import static io.github.kurobako.futon.Pair.pair;
import static java.util.Objects.requireNonNull;

public abstract class Either<L, R> implements Foldable<R> {
  Either() {}

  public abstract <X> X either(final @Nonnull Function<? super L, ? extends X> leftFn, final @Nonnull Function<? super R, ? extends X> rightFn);

  public abstract @Nonnull Either<R, L> swap();

  public abstract @Nonnull <X> Either<L, X> bind(@Nonnull Function<? super R, ? extends Either<L, X>> function);

  public abstract @Nonnull <X, Y> Either<L, Y> zip(@Nonnull Either<L, X> either, @Nonnull BiFunction<? super R, ? super X, ? extends Y> biFunction);

  public abstract @Nonnull <X, Y> Pair<? extends Either<L, X>, ? extends Either<L, Y>> unzip(@Nonnull Function<? super R, Pair<X, Y>> function);

  public abstract @Nonnull <X> Either<L, X> apply(@Nonnull Either<L, ? extends Function<? super R, ? extends X>> either);

  public abstract @Nonnull <X> Either<L, X> map(@Nonnull Function<? super R, ? extends X> function);

  public abstract @Nonnull <X, Y> Either<X, Y> biMap(final @Nonnull Function<? super L, ? extends X> leftFn, final @Nonnull Function<? super R, ? extends Y> rightFn);

  public abstract @Nonnull Option<Left<L, R>> caseLeft();

  public abstract @Nonnull Option<Right<L, R>> caseRight();

  public abstract boolean isLeft();

  public abstract boolean isRight();

  public static @Nonnull <L, R> Either<L, R> join(final @Nonnull Either<L, ? extends Either<L, R>> either) {
    requireNonNull(either);
    return either.bind(id());
  }

  public static @Nonnull <L, R> Left<L, R> left(final L value) {
    return new Left<>(value);
  }

  public static @Nonnull <L, R> Right<L, R> right(final R value) {
    return new Right<>(value);
  }

  public static final class Left<L, R> extends Either<L, R> {
    public final L left;

    private Left(final L left) {
      this.left = left;
    }

    @Override
    public <X> X either(final @Nonnull Function<? super L, ? extends X> leftFn, final @Nonnull Function<? super R, ? extends X> rightFn) {
      requireNonNull(leftFn);
      requireNonNull(rightFn);
      return leftFn.$(left);
    }

    @Override
    public @Nonnull Right<R, L> swap() {
      return right(left);
    }

    @Override
    public @Nonnull <X> Left<L, X> bind(final @Nonnull Function<? super R, ? extends Either<L, X>> function) {
      requireNonNull(function);
      return self();
    }

    @Override
    public @Nonnull <X, Y> Left<L, Y> zip(final @Nonnull Either<L, X> either, final @Nonnull BiFunction<? super R, ? super X, ? extends Y> biFunction) {
      requireNonNull(either);
      requireNonNull(biFunction);
      return self();
    }

    @Override
    public @Nonnull <X, Y> Pair<Left<L, X>, Left<L, Y>> unzip(final @Nonnull Function<? super R, Pair<X, Y>> function) {
      requireNonNull(function);
      return pair(self(), self());
    }

    @Override
    public @Nonnull <X> Left<L, X> apply(final @Nonnull Either<L, ? extends Function<? super R, ? extends X>> either) {
      requireNonNull(either);
      return self();
    }

    @Override
    public @Nonnull <X> Left<L, X> map(final @Nonnull Function<? super R, ? extends X> function) {
      requireNonNull(function);
      return self();
    }

    @SuppressWarnings("unchecked")
    private <X> Left<L, X> self() {
      return (Left<L, X>) this;
    }

    @Override
    public @Nonnull <X, Y> Left<X, Y> biMap(final @Nonnull Function<? super L, ? extends X> leftFn, @Nonnull Function<? super R, ? extends Y> rightFn) {
      requireNonNull(leftFn);
      requireNonNull(rightFn);
      return left(leftFn.$(left));
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
      requireNonNull(biFunction);
      return initial;
    }

    @Override
    public <B> B foldLeft(final @Nonnull BiFunction<? super B, ? super R, ? extends B> biFunction, final B initial) {
      requireNonNull(biFunction);
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
      requireNonNull(leftFn);
      requireNonNull(rightFn);
      return rightFn.$(right);
    }

    @Override
    public @Nonnull Left<R, L> swap() {
      return left(right);
    }

    @Override
    public @Nonnull <X> Either<L, X> bind(final @Nonnull Function<? super R, ? extends Either<L, X>> function) {
      requireNonNull(function);
      return function.$(right);
    }

    @Override
    public @Nonnull <X, Y> Either<L, Y> zip(final @Nonnull Either<L, X> either, final @Nonnull BiFunction<? super R, ? super X, ? extends Y> biFunction) {
      requireNonNull(either);
      requireNonNull(biFunction);
      return either.map(curry(biFunction).$(right));
    }

    @Override
    public @Nonnull <X, Y> Pair<Right<L, X>, Right<L, Y>> unzip(final @Nonnull Function<? super R, Pair<X, Y>> function) {
      requireNonNull(function);
      final Pair<? extends X, ? extends Y> xy = function.$(right);
      return pair(right(xy.first), right(xy.second));
    }

    @Override
    public @Nonnull <X> Either<L, X> apply(final @Nonnull Either<L, ? extends Function<? super R, ? extends X>> either) {
      requireNonNull(either);
      return either.map(f -> f.$(right));
    }

    @Override
    public @Nonnull <X> Right<L, X> map(final @Nonnull Function<? super R, ? extends X> function) {
      requireNonNull(function);
      return right(function.$(right));
    }

    @Override
    public @Nonnull <X, Y> Either<X, Y> biMap(final @Nonnull Function<? super L, ? extends X> leftFn, final @Nonnull Function<? super R, ? extends Y> rightFn) {
      requireNonNull(leftFn);
      requireNonNull(rightFn);
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
      requireNonNull(biFunction);
      return biFunction.$(right, initial);
    }

    @Override
    public <B> B foldLeft(final @Nonnull BiFunction<? super B, ? super R, ? extends B> biFunction, final B initial) {
      requireNonNull(biFunction);
      return biFunction.$(initial, right);
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
    public @Nonnull String toString() {
      return "Right " + String.valueOf(right);
    }
  }

  @FunctionalInterface
  interface Kleisli<A, L, R> {

    @Nonnull Either<L, R> run(A a);

    default @Nonnull <X> Kleisli<A, L, X> compose(final @Nonnull Kleisli<? super R, L, X> kleisli) {
      requireNonNull(kleisli);
      return a -> run(a).bind(kleisli::run);
    }

    default @Nonnull <X> Kleisli<A, L, X> compose(final @Nonnull Function<? super R, ? extends X> function) {
      requireNonNull(function);
      return a -> run(a).map(function);
    }

    default @Nonnull <X> Kleisli<Either<A, X>, L, Either<R, X>> left() {
      return ax -> ax.either(a -> run(a).map(Either::left), x -> Either.right(Either.right(x)));
    }

    default @Nonnull <X> Kleisli<Either<X, A>, L, Either<X, R>> right() {
      return xa -> xa.either(x -> Either.right(Either.left(x)), a -> run(a).map(Either::right));
    }

    default @Nonnull <X> Kleisli<Pair<A, X>, L, Pair<R, X>> first() {
      return ax -> run(ax.first).zip(Either.right(ax.second), Pair::pair);
    }

    default @Nonnull <X> Kleisli<Pair<X, A>, L, Pair<X, R>> second() {
      return xa -> Either.<L, X>right(xa.first).zip(Kleisli.this.run(xa.second), Pair::pair);
    }

    default @Nonnull <X, Y> Kleisli<Either<A, X>, L, Either<R, Y>> sum(final @Nonnull Kleisli<? super X, L, Y> kleisli) {
      requireNonNull(kleisli);
      return ax -> ax.either(a -> Kleisli.this.run(a).map(Either::left), x -> kleisli.run(x).map(Either::right));
    }

    default @Nonnull <X, Y> Kleisli<Pair<A, X>, L, Pair<R, Y>> product(final @Nonnull Kleisli<? super X, L, Y> kleisli) {
      requireNonNull(kleisli);
      return ax -> run(ax.first).zip(kleisli.run(ax.second), Pair::pair);
    }

    default @Nonnull <X> Kleisli<Either<A, X>, L, R> fanIn(final @Nonnull Kleisli<? super X, L, R> kleisli) {
      requireNonNull(kleisli);
      return ax -> ax.either(Kleisli.this::run, kleisli::run);
    }

    default @Nonnull <X> Kleisli<A, L, Pair<R, X>> fanOut(final @Nonnull Kleisli<? super A, L, X> kleisli) {
      requireNonNull(kleisli);
      return a -> run(a).zip(kleisli.run(a), Pair::pair);
    }

    static @Nonnull <A, L, R> Kleisli<A, L, R> lift(final @Nonnull Function<? super A, ? extends R> function) {
      requireNonNull(function);
      return a -> right(function.$(a));
    }

    static @Nonnull <A, L> Kleisli<A, L, A> id() {
      return Either::right;
    }

    static @Nonnull <A, L, R> Kleisli<Pair<Kleisli<A, L, R>, A>, L, R> apply() {
      return ka -> ka.first.run(ka.second);
    }
  }
}
