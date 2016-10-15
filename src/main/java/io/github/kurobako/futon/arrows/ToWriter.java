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
import io.github.kurobako.futon.Writer;

import javax.annotation.Nonnull;

import static io.github.kurobako.futon.Pair.pair;
import static io.github.kurobako.futon.Writer.unit;
import static io.github.kurobako.futon.arrows.Util.nonNull;

public interface ToWriter<A, B, O> {
  @Nonnull Writer<O, B> run(A arg);

  default @Nonnull <Z> ToWriter<Z, B, O> precompose(final @Nonnull ToWriter<Z, ? extends A, O> kleisli) {
    nonNull(kleisli);
    return z -> kleisli.run(z).bind(this::run);
  }

  default @Nonnull <Z> ToWriter<Z, B, O> precompose(final @Nonnull Function<? super Z, ? extends A> function) {
    nonNull(function);
    return z -> run(function.$(z));
  }

  default @Nonnull <C> ToWriter<A, C, O> postcompose(final @Nonnull ToWriter<? super B, C, O> kleisli) {
    nonNull(kleisli);
    return a -> run(a).bind(kleisli::run);
  }

  default @Nonnull <C> ToWriter<A, C, O> postcompose(final @Nonnull Function<? super B, ? extends C> function) {
    nonNull(function);
    return a -> run(a).map(function);
  }

  default @Nonnull <C> ToWriter<Either<A, C>, Either<B, C>, O> left() {
    return ac -> ac.either(a -> run(a).map(Either::left), c -> unit(Either.right(c)));
  }

  default @Nonnull <C> ToWriter<Either<C, A>, Either<C, B>, O> right() {
    return ca -> ca.either(c -> unit(Either.left(c)), a -> run(a).map(Either::right));
  }

  default @Nonnull <C> ToWriter<Pair<A, C>, Pair<B, C>, O> first() {
    return ac -> run(ac.first).map(b -> pair(b, ac.second));
  }

  default @Nonnull <C> ToWriter<Pair<C, A>, Pair<C, B>, O> second() {
    return ca -> run(ca.second).map(b -> pair(ca.first, b));
  }

  default @Nonnull <C, D> ToWriter<Either<A, C>, Either<B, D>, O> sum(final @Nonnull ToWriter<? super C, ? extends D, O> kleisli) {
    nonNull(kleisli);
    return ac -> ac.either(a -> run(a).map(Either::left), c -> kleisli.run(c).map(Either::right));
  }

  default @Nonnull <C, D> ToWriter<Pair<A, C>, Pair<B, D>, O> product(final @Nonnull ToWriter<? super C, ? extends D, O> kleisli) {
    nonNull(kleisli);
    return ac -> run(ac.first).apply(kleisli.run(ac.second).map(d -> b -> pair(b, d)));
  }

  default @Nonnull <C> ToWriter<Either<A, C>, B, O> fanIn(final @Nonnull ToWriter<? super C, B, O> kleisli) {
    nonNull(kleisli);
    return ac -> ac.either(this::run, kleisli::run);
  }

  default @Nonnull <C> ToWriter<A, Pair<B, C>, O> fanOut(final @Nonnull ToWriter<? super A, ? extends C, O> kleisli) {
    nonNull(kleisli);
    return a -> run(a).apply(kleisli.run(a).map(c -> b -> pair(b, c)));
  }

  static @Nonnull <A, B, O> ToWriter<A, B, O> lift(final @Nonnull Function<? super A, ? extends B> function) {
    nonNull(function);
    return a -> unit(function.$(a));
  }

  static @Nonnull <A, O> ToWriter<A, A, O> id() {
    return Writer::unit;
  }

  static @Nonnull <A, B, O> ToWriter<Pair<ToWriter<A, B, O>, A>, B, O> apply() {
    return ka -> ka.first.run(ka.second);
  }
}
