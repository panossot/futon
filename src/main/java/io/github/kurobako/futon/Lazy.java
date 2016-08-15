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

import static io.github.kurobako.futon.Pair.pair;
import static java.util.Objects.requireNonNull;

@FunctionalInterface
public interface Lazy<A> {
  A extract();

  default @Nonnull Lazy<? extends Lazy<A>> duplicate() {
    return () -> this;
  }

  default @Nonnull <B> Lazy<B> extend(final @Nonnull Function<? super Lazy<A>, ? extends B> function) {
    requireNonNull(function);
    return () -> function.$(this);
  }

  default @Nonnull <B> Lazy<B> bind(final @Nonnull Function<? super A, ? extends Lazy<B>> function) {
    requireNonNull(function);
    return () -> function.$(this.extract()).extract();
  }

  default @Nonnull <B, C> Lazy<C> zip(final @Nonnull Lazy<B> lazy, final @Nonnull BiFunction<? super A, ? super B, ? extends C> biFunction) {
    requireNonNull(lazy);
    requireNonNull(biFunction);
    return () -> biFunction.$(this.extract(), lazy.extract());
  }

  default @Nonnull <B, C> Pair<? extends Lazy<B>, ? extends Lazy<C>> unzip(final @Nonnull Function<? super A, Pair<B, C>> function) {
    requireNonNull(function);
    return pair(() -> function.$(this.extract()).first, () -> function.$(this.extract()).second);
  }

  default @Nonnull <B> Lazy<B> apply(final @Nonnull Lazy<? extends Function<? super A, ? extends B>> lazy) {
    requireNonNull(lazy);
    return () -> lazy.extract().$(this.extract());
  }

  default @Nonnull <B> Lazy<B> map(final @Nonnull Function<? super A, ? extends B> function) {
    requireNonNull(function);
    return () -> function.$(this.extract());
  }

  static @Nonnull <A> Lazy<A> join(final @Nonnull Lazy<? extends Lazy<A>> lazy) {
    requireNonNull(lazy);
    return lazy.extract();
  }

  static @Nonnull <A> Lazy<A> unit(final A value) {
    return () -> value;
  }

  @FunctionalInterface
  interface Kleisli<A, B> {

    @Nonnull Lazy<B> run(A a);

    default @Nonnull <C> Kleisli<A, C> compose(final @Nonnull Kleisli<? super B, C> kleisli) {
      requireNonNull(kleisli);
      return a -> run(a).bind(kleisli::run);
    }

    default @Nonnull <C> Kleisli<A, C> compose(final @Nonnull Function<? super B, ? extends C> function) {
      requireNonNull(function);
      return a -> run(a).map(function);
    }

    default @Nonnull <C> Kleisli<Either<A, C>, Either<B, C>> left() {
      return ac -> ac.either(a -> run(a).map(Either::left), c -> unit(Either.right(c)));
    }

    default @Nonnull <C> Kleisli<Either<C, A>, Either<C, B>> right() {
      return ca -> ca.either(c -> unit(Either.left(c)), a -> run(a).map(Either::right));
    }

    default @Nonnull <C> Kleisli<Pair<A, C>, Pair<B, C>> first() {
      return ac -> run(ac.first).zip(unit(ac.second), Pair::pair);
    }

    default @Nonnull <C> Kleisli<Pair<C, A>, Pair<C, B>> second() {
      return ca -> unit(ca.first).zip(run(ca.second), Pair::pair);
    }

    default @Nonnull <C, D> Kleisli<Either<A, C>, Either<B, D>> sum(final @Nonnull Kleisli<? super C, ? extends D> kleisli) {
      requireNonNull(kleisli);
      return ac -> ac.either(a -> run(a).map(Either::left), c -> kleisli.run(c).map(Either::right));
    }

    default @Nonnull <C, D> Kleisli<Pair<A, C>, Pair<B, D>> product(final @Nonnull Kleisli<? super C, ? extends D> kleisli) {
      requireNonNull(kleisli);
      return ac -> run(ac.first).zip(kleisli.run(ac.second), Pair::pair);
    }

    default @Nonnull <C> Kleisli<Either<A, C>, B> fanIn(final @Nonnull Kleisli<? super C, B> kleisli) {
      requireNonNull(kleisli);
      return ac -> ac.either(Kleisli.this::run, kleisli::run);
    }

    default @Nonnull <C> Kleisli<A, Pair<B, C>> fanOut(final @Nonnull Kleisli<? super A, ? extends C> kleisli) {
      requireNonNull(kleisli);
      return a -> run(a).zip(kleisli.run(a), Pair::pair);
    }

    static @Nonnull <A, B> Kleisli<A, B> lift(final @Nonnull Function<? super A, ? extends B> function) {
      requireNonNull(function);
      return a -> unit(function.$(a));
    }

    static @Nonnull <A> Kleisli<A, A> id() {
      return Lazy::unit;
    }

    static @Nonnull <A, B> Kleisli<Pair<Kleisli<A, B>, A>, B> apply() {
      return ka -> ka.first.run(ka.second);
    }
  }

  @FunctionalInterface
  interface CoKleisli<A, B> {
    B run(@Nonnull Lazy<A> lazy);

    default @Nonnull <C> CoKleisli<A, C> compose(final @Nonnull CoKleisli<B, ? extends C> coKleisli) {
      requireNonNull(coKleisli);
      return a -> coKleisli.run(a.extend(this::run));
    }

    default @Nonnull <C> CoKleisli<A, C> compose(final @Nonnull Function<? super B, ? extends C> function) {
      requireNonNull(function);
      return a -> function.$(run(a));
    }

    default @Nonnull <C> CoKleisli<Pair<A, C>, Pair<B, C>> first() {
      return ac -> ac.unzip(Function.id()).biMap(this::run, Lazy::extract);
    }

    default @Nonnull <C> CoKleisli<Pair<C, A>, Pair<C, B>> second() {
      return ca -> ca.unzip(Function.id()).biMap(Lazy::extract, this::run);
    }

    default @Nonnull <C, D> CoKleisli<Pair<A, C>, Pair<B, D>> product(final @Nonnull CoKleisli<C, ? extends D> coKleisli) {
      requireNonNull(coKleisli);
      return ac -> ac.unzip(Function.id()).biMap(this::run, coKleisli::run);
    }

    default @Nonnull <C> CoKleisli<A, Pair<B, C>> fanOut(final @Nonnull CoKleisli<A, ? extends C> coKleisli) {
      requireNonNull(coKleisli);
      return a -> pair(run(a), coKleisli.run(a));
    }

    static @Nonnull <A, B> CoKleisli<A, B> lift(final @Nonnull Function<? super A, ? extends B> function) {
      requireNonNull(function);
      return a -> function.$(a.extract());
    }

    static @Nonnull <A> CoKleisli<A, A> id() {
      return Lazy::extract;
    }
  }
}
