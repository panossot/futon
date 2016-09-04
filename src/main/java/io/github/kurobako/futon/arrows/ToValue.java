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
import io.github.kurobako.futon.Value;

import javax.annotation.Nonnull;

import static io.github.kurobako.futon.Value.unit;
import static io.github.kurobako.futon.arrows.Util.nonNull;

public interface ToValue<A, B> {
  @Nonnull Value<B> run(A arg);

  default @Nonnull <Z> ToValue<Z, B> precompose(final @Nonnull ToValue<Z, ? extends A> kleisli) {
    nonNull(kleisli);
    return z -> kleisli.run(z).bind(this::run);
  }

  default @Nonnull <Z> ToValue<Z, B> precompose(final @Nonnull Function<? super Z, ? extends A> function) {
    nonNull(function);
    return z -> run(function.$(z));
  }

  default @Nonnull <C> ToValue<A, C> postcompose(final @Nonnull ToValue<? super B, C> kleisli) {
    nonNull(kleisli);
    return a -> run(a).bind(kleisli::run);
  }

  default @Nonnull <C> ToValue<A, C> postcompose(final @Nonnull Function<? super B, ? extends C> function) {
    nonNull(function);
    return a -> run(a).map(function);
  }

  default @Nonnull <C> ToValue<Either<A, C>, Either<B, C>> left() {
    return ac -> ac.either(a -> run(a).map(Either::left), c -> unit(Either.right(c)));
  }

  default @Nonnull <C> ToValue<Either<C, A>, Either<C, B>> right() {
    return ca -> ca.either(c -> unit(Either.left(c)), a -> run(a).map(Either::right));
  }

  default @Nonnull <C> ToValue<Pair<A, C>, Pair<B, C>> first() {
    return ac -> run(ac.first).zip(unit(ac.second), Pair::pair);
  }

  default @Nonnull <C> ToValue<Pair<C, A>, Pair<C, B>> second() {
    return ca -> unit(ca.first).zip(run(ca.second), Pair::pair);
  }

  default @Nonnull <C, D> ToValue<Either<A, C>, Either<B, D>> sum(final @Nonnull ToValue<? super C, ? extends D> kleisli) {
    nonNull(kleisli);
    return ac -> ac.either(a -> run(a).map(Either::left), c -> kleisli.run(c).map(Either::right));
  }

  default @Nonnull <C, D> ToValue<Pair<A, C>, Pair<B, D>> product(final @Nonnull ToValue<? super C, ? extends D> kleisli) {
    nonNull(kleisli);
    return ac -> run(ac.first).zip(kleisli.run(ac.second), Pair::pair);
  }

  default @Nonnull <C> ToValue<Either<A, C>, B> fanIn(final @Nonnull ToValue<? super C, B> kleisli) {
    nonNull(kleisli);
    return ac -> ac.either(this::run, kleisli::run);
  }

  default @Nonnull <C> ToValue<A, Pair<B, C>> fanOut(final @Nonnull ToValue<? super A, ? extends C> kleisli) {
    nonNull(kleisli);
    return a -> run(a).zip(kleisli.run(a), Pair::pair);
  }

  static @Nonnull <A, B> ToValue<A, B> lift(final @Nonnull Function<? super A, ? extends B> function) {
    nonNull(function);
    return a -> unit(function.$(a));
  }

  static @Nonnull <A> ToValue<A, A> id() {
    return Value::unit;
  }

  static @Nonnull <A, B> ToValue<Pair<ToValue<A, B>, A>, B> apply() {
    return ka -> ka.first.run(ka.second);
  }
}
