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

package io.github.kurobako.futon.arrows;

import io.github.kurobako.futon.Either;
import io.github.kurobako.futon.Function;
import io.github.kurobako.futon.Option;
import io.github.kurobako.futon.Pair;

import javax.annotation.Nonnull;

import static io.github.kurobako.futon.Option.unit;
import static io.github.kurobako.futon.arrows.Util.nonNull;

public interface ToOption<A, B> {
  @Nonnull Option<B> run(A a);

  default @Nonnull <Z> ToOption<Z, B> precompose(final @Nonnull ToOption<Z, ? extends A> kleisli) {
    nonNull(kleisli);
    return z -> kleisli.run(z).bind(this::run);
  }

  default @Nonnull <Z> ToOption<Z, B> precompose(final @Nonnull Function<? super Z, ? extends A> function) {
    nonNull(function);
    return z -> run(function.$(z));
  }

  default @Nonnull <C> ToOption<A, C> postcompose(final @Nonnull ToOption<? super B, C> kleisli) {
    nonNull(kleisli);
    return a -> run(a).bind(kleisli::run);
  }

  default @Nonnull <C> ToOption<A, C> postcompose(final @Nonnull Function<? super B, ? extends C> function) {
    nonNull(function);
    return a -> run(a).map(function);
  }

  default @Nonnull <C> ToOption<Either<A, C>, Either<B, C>> left() {
    return ac -> ac.either(a -> run(a).map(Either::left), c -> unit(Either.right(c)));
  }

  default @Nonnull <C> ToOption<Either<C, A>, Either<C, B>> right() {
    return ca -> ca.either(c -> unit(Either.left(c)), a -> run(a).map(Either::right));
  }

  default @Nonnull <C> ToOption<Pair<A, C>, Pair<B, C>> first() {
    return ac -> run(ac.first).zip(unit(ac.second), Pair::pair);
  }

  default @Nonnull <C> ToOption<Pair<C, A>, Pair<C, B>> second() {
    return ca -> unit(ca.first).zip(run(ca.second), Pair::pair);
  }

  default @Nonnull <C, D> ToOption<Either<A, C>, Either<B, D>> sum(final @Nonnull ToOption<? super C, ? extends D> kleisli) {
    nonNull(kleisli);
    return ac -> ac.either(a -> run(a).map(Either::left), c -> kleisli.run(c).map(Either::right));
  }

  default @Nonnull <C, D> ToOption<Pair<A, C>, Pair<B, D>> product(final @Nonnull ToOption<? super C, ? extends D> kleisli) {
    nonNull(kleisli);
    return ac -> run(ac.first).zip(kleisli.run(ac.second), Pair::pair);
  }

  default @Nonnull <C> ToOption<Either<A, C>, B> fanIn(final @Nonnull ToOption<? super C, B> kleisli) {
    nonNull(kleisli);
    return ac -> ac.either(this::run, kleisli::run);
  }

  default @Nonnull <C> ToOption<A, Pair<B, C>> fanOut(final @Nonnull ToOption<? super A, ? extends C> kleisli) {
    nonNull(kleisli);
    return a -> run(a).zip(kleisli.run(a), Pair::pair);
  }

  static @Nonnull <A, B> ToOption<A, B> lift(final @Nonnull Function<? super A, ? extends B> function) {
    nonNull(function);
    return a -> unit(function.$(a));
  }

  static @Nonnull <A> ToOption<A, A> id() {
    return Option::unit;
  }

  static @Nonnull <A, B> ToOption<Pair<ToOption<A, B>, A>, B> apply() {
    return ka -> ka.first.run(ka.second);
  }
}
