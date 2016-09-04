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
import io.github.kurobako.futon.Pair;

import javax.annotation.Nonnull;

import static io.github.kurobako.futon.arrows.Util.nonNull;

public interface ToEither<A, B, L> {
  @Nonnull Either<L, B> run(A arg);

  default @Nonnull <Z> ToEither<Z, B, L> precompose(final @Nonnull ToEither<Z, ? extends A, L> kleisli) {
    nonNull(kleisli);
    return z -> kleisli.run(z).bind(this::run);
  }

  default @Nonnull <Z> ToEither<Z, B, L> precompose(final @Nonnull Function<? super Z, ? extends A> function) {
    nonNull(function);
    return z -> run(function.$(z));
  }

  default @Nonnull <C> ToEither<A, C, L> postcompose(final @Nonnull ToEither<? super B, C, L> kleisli) {
    nonNull(kleisli);
    return a -> run(a).bind(kleisli::run);
  }

  default @Nonnull <C> ToEither<A, C, L> postcompose(final @Nonnull Function<? super B, ? extends C> function) {
    nonNull(function);
    return a -> run(a).map(function);
  }

  default @Nonnull <C> ToEither<Either<A, C>, Either<B, C>, L> left() {
    return ac -> ac.either(a -> run(a).map(Either::left), c -> Either.unit(Either.right(c)));
  }

  default @Nonnull <C> ToEither<Either<C, A>, Either<C, B>, L> right() {
    return ca -> ca.either(c -> Either.unit(Either.left(c)), a -> run(a).map(Either::right));
  }

  default @Nonnull <C> ToEither<Pair<A, C>, Pair<B, C>, L> first() {
    return ac -> run(ac.first).zip(Either.right(ac.second), Pair::pair);
  }

  default @Nonnull <C> ToEither<Pair<C, A>, Pair<C, B>, L> second() {
    return ca -> Either.<L, C>right(ca.first).zip(run(ca.second), Pair::pair);
  }

  default @Nonnull <C, D> ToEither<Either<A, C>, Either<B, D>, L> sum(final @Nonnull ToEither<? super C, ? extends D, L> kleisli) {
    nonNull(kleisli);
    return ac -> ac.either(a -> run(a).map(Either::left), c -> kleisli.run(c).map(Either::right));
  }

  default @Nonnull <C, D> ToEither<Pair<A, C>, Pair<B, D>, L> product(final @Nonnull ToEither<? super C, ? extends D, L> kleisli) {
    nonNull(kleisli);
    return ac -> run(ac.first).zip(kleisli.run(ac.second), Pair::pair);
  }

  default @Nonnull <C> ToEither<Either<A, C>, B, L> fanIn(final @Nonnull ToEither<? super C, B, L> kleisli) {
    nonNull(kleisli);
    return ac -> ac.either(this::run, kleisli::run);
  }

  default @Nonnull <C> ToEither<A, Pair<B, C>, L> fanOut(final @Nonnull ToEither<? super A, ? extends C, L> kleisli) {
    nonNull(kleisli);
    return a -> run(a).zip(kleisli.run(a), Pair::pair);
  }

  static @Nonnull <A, B, L> ToEither<A, B, L> lift(final @Nonnull Function<? super A, ? extends B> function) {
    nonNull(function);
    return a -> Either.unit(function.$(a));
  }

  static @Nonnull <A, L> ToEither<A, A, L> id() {
    return Either::unit;
  }

  static @Nonnull <A, B, L> ToEither<Pair<ToEither<A, B, L>, A>, B, L> apply() {
    return ka -> ka.first.run(ka.second);
  }
}
