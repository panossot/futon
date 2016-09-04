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
import io.github.kurobako.futon.State;

import javax.annotation.Nonnull;

import static io.github.kurobako.futon.Pair.pair;
import static io.github.kurobako.futon.State.unit;
import static io.github.kurobako.futon.arrows.Util.nonNull;

public interface ToState<A, B, S> {
  @Nonnull
  State<S, B> run(A arg);

  default @Nonnull <Z> ToState<Z, B, S> precompose(final @Nonnull ToState<Z, ? extends A, S> kleisli) {
    nonNull(kleisli);
    return z -> kleisli.run(z).bind(this::run);
  }

  default @Nonnull <Z> ToState<Z, B, S> precompose(final @Nonnull Function<? super Z, ? extends A> function) {
    nonNull(function);
    return z -> run(function.$(z));
  }

  default @Nonnull <C> ToState<A, C, S> postcompose(final @Nonnull ToState<? super B, C, S> kleisli) {
    nonNull(kleisli);
    return a -> run(a).bind(kleisli::run);
  }

  default @Nonnull <C> ToState<A, C, S> postcompose(final @Nonnull Function<? super B, ? extends C> function) {
    nonNull(function);
    return a -> run(a).map(function);
  }

  default @Nonnull <C> ToState<Either<A, C>, Either<B, C>, S> left() {
    return ac -> ac.either(a -> run(a).map(Either::left), c -> unit(Either.right(c)));
  }

  default @Nonnull <C> ToState<Either<C, A>, Either<C, B>, S> right() {
    return ca -> ca.either(c -> unit(Either.left(c)), a -> run(a).map(Either::right));
  }

  default @Nonnull <C> ToState<Pair<A, C>, Pair<B, C>, S> first() {
    return ac -> run(ac.first).map(b -> pair(b, ac.second));
  }

  default @Nonnull <C> ToState<Pair<C, A>, Pair<C, B>, S> second() {
    return ca -> run(ca.second).map(b -> pair(ca.first, b));
  }

  default @Nonnull <C, D> ToState<Either<A, C>, Either<B, D>, S> sum(final @Nonnull ToState<? super C, ? extends D, S> kleisli) {
    nonNull(kleisli);
    return ac -> ac.either(a -> run(a).map(Either::left), c -> kleisli.run(c).map(Either::right));
  }

  default @Nonnull <C, D> ToState<Pair<A, C>, Pair<B, D>, S> product(final @Nonnull ToState<? super C, ? extends D, S> kleisli) {
    nonNull(kleisli);
    return ac -> run(ac.first).apply(kleisli.run(ac.second).map(d -> b -> pair(b, d)));
  }

  default @Nonnull <C> ToState<Either<A, C>, B, S> fanIn(final @Nonnull ToState<? super C, B, S> kleisli) {
    nonNull(kleisli);
    return ac -> ac.either(this::run, kleisli::run);
  }

  default @Nonnull <C> ToState<A, Pair<B, C>, S> fanOut(final @Nonnull ToState<? super A, ? extends C, S> kleisli) {
    nonNull(kleisli);
    return a -> run(a).apply(kleisli.run(a).map(c -> b -> pair(b, c)));
  }

  static @Nonnull <A, B, S> ToState<A, B, S> lift(final @Nonnull Function<? super A, ? extends B> function) {
    nonNull(function);
    return a -> unit(function.$(a));
  }

  static @Nonnull <A, S> ToState<A, A, S> id() {
    return State::unit;
  }

  static @Nonnull <A, B, S> ToState<Pair<ToState<A, B, S>, A>, B, S> apply() {
    return ka -> ka.first.run(ka.second);
  }
}
