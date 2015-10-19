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

import static io.github.kurobako.futon.Function.id;
import static java.util.Objects.requireNonNull;

public interface Either<L, R> extends BiFunctor<L, R>, Foldable<Either<L, R>> {
  @Nonnull <X> Either<L, X> bind(@Nonnull Function<? super R, ? extends Either<L, X>> function);

  @Nonnull <X> Either<L, X> apply(@Nonnull Either<L, ? extends Function<? super R, ? extends X>> transformation);

  <X> X fold(@Nonnull Function<? super L, ? extends X> ifLeft, @Nonnull Function<? super R, ? extends X> ifRight);

  boolean isLeft();

  boolean isRight();

  @Nonnull Maybe<L> left();

  @Nonnull Maybe<R> right();

  @Nonnull Either<R, L> swap();

  @Override
  default @Nonnull <U, V> Either<U, V> biMap(@Nonnull Function<? super L, ? extends U> ifLeft,
                                     @Nonnull Function<? super R, ? extends V> ifRight) {
    requireNonNull(ifLeft, "ifLeft");
    requireNonNull(ifRight, "ifRight");
    return mapFirst(ifLeft).mapSecond(ifRight);
  }

  @Override
  @Nonnull <X> Either<X, R> mapFirst(@Nonnull Function<? super L, ? extends X> function);

  @Override
  @Nonnull <X> Either<L, X> mapSecond(@Nonnull Function<? super R, ? extends X> function);

  @Override
  int hashCode();

  @Override
  boolean equals(Object o);

  static @Nonnull <L, R> Either<L, R> left(final @Nonnull L value) {
    requireNonNull(value, "value");
    return new Either$Left<>(value);
  }

  static @Nonnull <L, R> Either<L, R> right(final @Nonnull R value) {
    requireNonNull(value, "value");
    return new Either$Right<>(value);
  }

  static <L, R> Either<L, R> joinLeft(final @Nonnull Either<? extends Either<L, R>, R> wrapper) {
    requireNonNull(wrapper, "wrapper");
    return wrapper.fold(id(), (Function<R, Either<L, R>>) Either::right);
  }

  static <L, R> Either<L, R> joinRight(final @Nonnull Either<L, ? extends Either<L, R>> wrapper) {
    requireNonNull(wrapper, "wrapper");
    return wrapper.fold((Function<L, Either<L, R>>) Either::left, id());
  }
}

final class Either$Left<L, R> implements Either<L, R> {
  private final @Nonnull L value;

  Either$Left(L value) {
    assert value != null;
    this.value = value;
  }

  @Override
  @SuppressWarnings("unchecked")
  public @Nonnull <X> Either<L, X> bind(final @Nonnull Function<? super R, ? extends Either<L, X>> function) {
    requireNonNull(function, "function");
    return (Either<L, X>) this;
  }


  @Override
  @SuppressWarnings("unchecked")
  public @Nonnull <X> Either<L, X> apply(final @Nonnull Either<L, ? extends Function<? super R, ? extends X>>
                                         transformation) {
    requireNonNull(transformation, "transformation");
    return (Either<L, X>) this;
  }

  @Override
  public @Nonnull <X> Either<X, R> mapFirst(final @Nonnull Function<? super L, ? extends X> function) {
    requireNonNull(function, "function");
    return Either.left(function.$(value));
  }

  @Override
  @SuppressWarnings("unchecked")
  public @Nonnull <X> Either<L, X> mapSecond(final @Nonnull Function<? super R, ? extends X> function) {
    requireNonNull(function, "function");
    return (Either<L, X>) this;
  }

  @Override
  public <X> X fold(final @Nonnull Function<? super L, ? extends X> ifLeft,
                    final @Nonnull Function<? super R, ? extends X> ifRight) {
    requireNonNull(ifLeft, "ifLeft");
    requireNonNull(ifRight, "ifRight");
    return ifLeft.$(value);
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
  public @Nonnull Maybe<L> left() {
    return Maybe.just(value);
  }

  @Override
  public @Nonnull Maybe<R> right() {
    return Maybe.nothing();
  }

  @Override
  public @Nonnull Either<R, L> swap() {
    return Either.right(value);
  }

  @Override
  public <B> B foldRight(final @Nonnull BiFunction<Either<L, R>, B, B> function, B initial) {
    requireNonNull(function, "function");
    return function.$(this, initial);
  }

  @Override
  public <B> B foldLeft(final @Nonnull BiFunction<B, Either<L, R>, B> function, B initial) {
    requireNonNull(function, "function");
    return function.$(initial, this);
  }

  @Override
  public int hashCode() {
    return value.hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Either)) return false;
    Either that = (Either) o;
    return this.value.equals(that.left().value());
  }

  @Override
  public String toString() {
    return "Left " + value;
  }
}

final class Either$Right<L, R> implements Either<L, R> {
  private final @Nonnull R value;

  Either$Right(R value) {
    assert value != null;
    this.value = value;
  }

  @Override
  public @Nonnull <X> Either<L, X> bind(final @Nonnull Function<? super R, ? extends Either<L, X>> function) {
    requireNonNull(function, "function");
    return function.$(value);
  }

  @Override
  public @Nonnull <X> Either<L, X> apply(final @Nonnull Either<L, ? extends Function<? super R, ? extends X>>
                                         transformation) {
    requireNonNull(transformation, "transformation");
    return transformation.bind(f -> Either.right(f.$(value)));
  }

  @Override
  public @Nonnull <U, V> Either<U, V> biMap(final @Nonnull Function<? super L, ? extends U> ifLeft,
                                            final @Nonnull Function<? super R, ? extends V> ifRight) {
    requireNonNull(ifLeft, "ifLeft");
    requireNonNull(ifRight, "ifRight");
    return Either.right(ifRight.$(value));
  }

  @Override
  @SuppressWarnings("unchecked")
  public @Nonnull <X> Either<X, R> mapFirst(final @Nonnull Function<? super L, ? extends X> function) {
    requireNonNull(function, "function");
    return (Either<X, R>) this;
  }

  @Override
  public @Nonnull <X> Either<L, X> mapSecond(final @Nonnull Function<? super R, ? extends X> function) {
    requireNonNull(function, "function");
    return Either.right(function.$(value));
  }

  @Override
  public <X> X fold(final @Nonnull Function<? super L, ? extends X> ifLeft,
                    final @Nonnull Function<? super R, ? extends X> ifRight) {
    requireNonNull(ifLeft, "ifLeft");
    requireNonNull(ifRight, "ifRight");
    return ifRight.$(value);
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
  public @Nonnull Maybe<L> left() {
    return Maybe.nothing();
  }

  @Override
  public @Nonnull Maybe<R> right() {
    return Maybe.just(value);
  }

  @Override
  public @Nonnull Either<R, L> swap() {
    return Either.left(value);
  }

  @Override
  public <B> B foldRight(final @Nonnull BiFunction<Either<L, R>, B, B> function, B initial) {
    requireNonNull(function, "function");
    return function.$(this, initial);
  }

  @Override
  public <B> B foldLeft(final @Nonnull BiFunction<B, Either<L, R>, B> function, B initial) {
    requireNonNull(function, "function");
    return function.$(initial, this);
  }

  @Override
  public int hashCode() {
    return value.hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Either)) return false;
   Either that = (Either) o;
    return this.value.equals(that.right().value());
  }

  @Override
  public String toString() {
    return "Right " + value;
  }
}