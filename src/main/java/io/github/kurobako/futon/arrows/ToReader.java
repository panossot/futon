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
import io.github.kurobako.futon.Reader;

import javax.annotation.Nonnull;

import static io.github.kurobako.futon.Pair.pair;
import static io.github.kurobako.futon.Reader.reader;
import static io.github.kurobako.futon.Reader.unit;
import static io.github.kurobako.futon.arrows.Util.nonNull;

public interface ToReader<A, B, E> {
  @Nonnull Reader<E, B> run(A a);

  default @Nonnull <Z> ToReader<Z, B, E> precompose(final @Nonnull ToReader<Z, ? extends A, E> kleisli) {
    nonNull(kleisli);
    return z -> kleisli.run(z).bind(this::run);
  }

  default @Nonnull <Z> ToReader<Z, B, E> precompose(final @Nonnull Function<? super Z, ? extends A> function) {
    nonNull(function);
    return z -> run(function.$(z));
  }

  default @Nonnull <C> ToReader<A, C, E> postcompose(final @Nonnull ToReader<? super B, C, E> kleisli) {
    nonNull(kleisli);
    return a -> run(a).bind(kleisli::run);
  }

  default @Nonnull <C> ToReader<A, C, E> postcompose(final @Nonnull Function<? super B, ? extends C> function) {
    nonNull(function);
    return a -> run(a).map(function);
  }

  default @Nonnull <C> ToReader<Either<A, C>, Either<B, C>, E> left() {
    return ac -> ac.either(a -> run(a).map(Either::left), c -> unit(Either.right(c)));
  }

  default @Nonnull <C> ToReader<Either<C, A>, Either<C, B>, E> right() {
    return ca -> ca.either(c -> unit(Either.left(c)), a -> run(a).map(Either::right));
  }

  default @Nonnull <C> ToReader<Pair<A, C>, Pair<B, C>, E> first() {
    return ac -> run(ac.first).map(b -> pair(b, ac.second));
  }

  default @Nonnull <C> ToReader<Pair<C, A>, Pair<C, B>, E> second() {
    return ca -> run(ca.second).map(b -> pair(ca.first, b));
  }

  default @Nonnull <C, D> ToReader<Either<A, C>, Either<B, D>, E> sum(final @Nonnull ToReader<? super C, ? extends D, E> kleisli) {
    nonNull(kleisli);
    return ac -> ac.either(a -> run(a).map(Either::left), c -> kleisli.run(c).map(Either::right));
  }

  default @Nonnull <C, D> ToReader<Pair<A, C>, Pair<B, D>, E> product(final @Nonnull ToReader<? super C, ? extends D, E> kleisli) {
    nonNull(kleisli);
    return ac -> run(ac.first).apply(kleisli.run(ac.second).map(d -> b -> pair(b, d)));
  }

  default @Nonnull <C> ToReader<Either<A, C>, B, E> fanIn(final @Nonnull ToReader<? super C, B, E> kleisli) {
    nonNull(kleisli);
    return ac -> ac.either(ToReader.this::run, kleisli::run);
  }

  default @Nonnull <C> ToReader<A, Pair<B, C>, E> fanOut(final @Nonnull ToReader<? super A, ? extends C, E> kleisli) {
    nonNull(kleisli);
    return a -> run(a).apply(kleisli.run(a).map(c -> b -> pair(b, c)));
  }

  static @Nonnull <A, B, E> ToReader<A, B, E> lift(final @Nonnull Function<? super A, ? extends B> function) {
    nonNull(function);
    return a -> unit(function.$(a));
  }

  static @Nonnull <A, E> ToReader<A, A, E> id() {
    return Reader::unit;
  }

  static @Nonnull <A, B, E> ToReader<Pair<ToReader<A, B, E>, A>, B, E> apply() {
    return ka -> ka.first.run(ka.second);
  }
}