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

import static io.github.kurobako.futon.Function.id;
import static io.github.kurobako.futon.Maybe.*;
import static io.github.kurobako.futon.Pair.pair;
import static java.util.Objects.requireNonNull;

public interface Either<L, R> extends Foldable<R> {
  @Nonnull <X> Either<L, X> bind(@Nonnull Function<? super R, ? extends Either<L, X>> function);

  @Nonnull <X, Y> Either<L, Y> zip(@Nonnull Either<L, X> either, @Nonnull BiFunction<? super R, ? super X, ? extends Y> biFunction);

  @Nonnull <X, Y> Pair<? extends Either<L, X>, ? extends Either<L, Y>> unzip(@Nonnull Function<? super R, Pair<? extends X, ? extends Y>> function);

  @Nonnull <X> Either<L, X> apply(@Nonnull Either<L, ? extends Function<? super R, ? extends X>> either);

  @Nonnull <X> Either<L, X> map(@Nonnull Function<? super R, ? extends X> function);

  @Nonnull <X, Y> Either<X, Y> biMap(final @Nonnull Function<? super L, ? extends X> left, final @Nonnull Function<? super R, ? extends Y> right);

  @Nonnull Either<R, L> swap();

  <X> X either(final @Nonnull Function<? super L, ? extends X> left, final @Nonnull Function<? super R, ? extends X> right);

  @Nonnull Maybe<Left<L, R>> caseLeft();

  @Nonnull Maybe<Right<L, R>> caseRight();

  static @Nonnull <L, R> Either<L, R> join(final @Nonnull Either<L, ? extends Either<L, R>> either) {
    requireNonNull(either);
    return either.bind(id());
  }

  static @Nonnull <L, R> Left<L, R> left(final L value) {
    return new Left<L, R>() {
      @Override
      public L value() {
        return value;
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
      public @Nonnull <X, Y> Pair<? extends Left<L, X>, ? extends Left<L, Y>> unzip(final @Nonnull Function<? super R, Pair<? extends X, ? extends Y>> function) {
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

      @Override
      public @Nonnull <X, Y> Left<X, Y> biMap(final @Nonnull Function<? super L, ? extends X> left, final @Nonnull Function<? super R, ? extends Y> right) {
        requireNonNull(left);
        requireNonNull(right);
        return left(left.$(value));
      }

      @SuppressWarnings("unchecked")
      private <X> Left<L, X> self() {
        return (Left<L, X>) this;
      }

      @Override
      public @Nonnull Right<R, L> swap() {
        return right(value);
      }

      @Override
      public @Nonnull Just<Left<L, R>> caseLeft() {
        return just(this);
      }

      @Override
      public @Nonnull Nothing<Right<L, R>> caseRight() {
        return nothing();
      }

      @Override
      public <X> X either(final @Nonnull Function<? super L, ? extends X> left, final @Nonnull Function<? super R, ? extends X> right) {
        requireNonNull(left);
        requireNonNull(right);
        return left.$(value);
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
        return Objects.hashCode(value);
      }

      @Override
      public boolean equals(Object obj) {
        if (!(obj instanceof Left)) return false;
        Left<?, ?> that = (Left<?, ?>) obj;
        return Objects.equals(value, that.value());
      }

      @Override
      public @Nonnull String toString() {
        return "Left " + String.valueOf(value);
      }
    };
  }

  static @Nonnull <L, R> Right<L, R> right(final R value) {
    return new Right<L, R>() {
      @Override
      public R value() {
        return value;
      }

      @Override
      public @Nonnull <X> Either<L, X> bind(final @Nonnull Function<? super R, ? extends Either<L, X>> function) {
        requireNonNull(function);
        return function.$(value);
      }

      @Override
      public @Nonnull <X, Y> Either<L, Y> zip(final @Nonnull Either<L, X> either, final @Nonnull BiFunction<? super R, ? super X, ? extends Y> biFunction) {
        requireNonNull(either);
        requireNonNull(biFunction);
        return either.map(x -> biFunction.$(value, x));
      }

      @Override
      public @Nonnull <X, Y> Pair<? extends Right<L, X>, ? extends Right<L, Y>> unzip(final @Nonnull Function<? super R, Pair<? extends X, ? extends Y>> function) {
        requireNonNull(function);
        Pair<? extends X, ? extends Y> xy = function.$(value);
        return pair(right(xy.first), right(xy.second));
      }

      @Override
      public @Nonnull <X> Either<L, X> apply(final @Nonnull Either<L, ? extends Function<? super R, ? extends X>> either) {
        requireNonNull(either);
        return either.map(f -> f.$(value));
      }

      @Override
      public @Nonnull <X> Right<L, X> map(final @Nonnull Function<? super R, ? extends X> function) {
        requireNonNull(function);
        return right(function.$(value));
      }

      @Override
      public @Nonnull <X, Y> Right<X, Y> biMap(final @Nonnull Function<? super L, ? extends X> left, final @Nonnull Function<? super R, ? extends Y> right) {
        requireNonNull(left);
        requireNonNull(right);
        return right(right.$(value));
      }

      @Override
      public @Nonnull Left<R, L> swap() {
        return left(value);
      }

      @Override
      public @Nonnull Nothing<Left<L, R>> caseLeft() {
        return nothing();
      }

      @Override
      public @Nonnull Just<Right<L, R>> caseRight() {
        return just(this);
      }

      @Override
      public <X> X either(final @Nonnull Function<? super L, ? extends X> left, final @Nonnull Function<? super R, ? extends X> right) {
        requireNonNull(left);
        requireNonNull(right);
        return right.$(value);
      }

      @Override
      public <B> B foldRight(final @Nonnull BiFunction<? super R, ? super B, ? extends B> biFunction, final B initial) {
        requireNonNull(biFunction);
        return biFunction.$(value, initial);
      }

      @Override
      public <B> B foldLeft(final @Nonnull BiFunction<? super B, ? super R, ? extends B> biFunction, final B initial) {
        requireNonNull(biFunction);
        return biFunction.$(initial, value);
      }

      @Override
      public int hashCode() {
        return Objects.hashCode(value);
      }

      @Override
      public boolean equals(Object obj) {
        if (!(obj instanceof Right)) return false;
        Right<?, ?> that = (Right) obj;
        return Objects.equals(value, that.value());
      }

      @Override
      public @Nonnull String toString() {
        return "Right " + String.valueOf(value);
      }
    };
  }

  interface Left<L, R> extends Either<L, R> {
    L value();

    @Override
    @Nonnull <X> Left<L, X> bind(@Nonnull Function<? super R, ? extends Either<L, X>> function);

    @Override
    @Nonnull<X, Y> Left<L, Y> zip(@Nonnull Either<L, X> either, @Nonnull BiFunction<? super R, ? super X, ? extends Y> biFunction);

    @Override
    @Nonnull <X, Y> Pair<? extends Left<L, X>, ? extends Left<L, Y>> unzip(@Nonnull Function<? super R, Pair<? extends X, ? extends Y>> function);

    @Override
    @Nonnull <X> Left<L, X> apply(@Nonnull Either<L, ? extends Function<? super R, ? extends X>> either);

    @Override
    @Nonnull <X> Left<L, X> map(@Nonnull Function<? super R, ? extends X> function);

    @Override
    @Nonnull <X, Y> Left<X, Y> biMap(@Nonnull final Function<? super L, ? extends X> left, @Nonnull final Function<? super R, ? extends Y> right);

    @Override
    @Nonnull Right<R, L> swap();

    @Override
    @Nonnull Just<Left<L, R>> caseLeft();

    @Override
    @Nonnull Nothing<Right<L, R>> caseRight();
  }

  interface Right<L, R> extends Either<L, R> {
    R value();

    @Override
    @Nonnull <X> Either<L, X> bind(@Nonnull Function<? super R, ? extends Either<L, X>> function);

    @Override
    @Nonnull <X, Y> Either<L, Y> zip(@Nonnull Either<L, X> either, @Nonnull BiFunction<? super R, ? super X, ? extends Y> biFunction);

    @Override
    @Nonnull <X, Y> Pair<? extends Right<L, X>, ? extends Right<L, Y>> unzip(@Nonnull Function<? super R, Pair<? extends X, ? extends Y>> function);

    @Override
    @Nonnull <X> Either<L, X> apply(@Nonnull Either<L, ? extends Function<? super R, ? extends X>> either);

    @Override
    @Nonnull <X> Right<L, X> map(@Nonnull Function<? super R, ? extends X> function);

    @Override
    @Nonnull <X, Y> Right<X, Y> biMap(@Nonnull final Function<? super L, ? extends X> left, @Nonnull final Function<? super R, ? extends Y> right);

    @Override
    @Nonnull Left<R, L> swap();

    @Override
    @Nonnull Nothing<Left<L, R>> caseLeft();

    @Override
    @Nonnull Just<Right<L, R>> caseRight();
  }
}
