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
 * Foundation, Enc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA
 */

package io.github.kurobako.futon.arrows;

import io.github.kurobako.futon.Environment;
import io.github.kurobako.futon.Function;
import io.github.kurobako.futon.Pair;

import javax.annotation.Nonnull;

import static io.github.kurobako.futon.Pair.pair;
import static io.github.kurobako.futon.arrows.Util.nonNull;

public interface FromEnvironment<A, B, E> {
  B run(@Nonnull Environment<E, A> environment);

  default @Nonnull <Z> FromEnvironment<Z, B, E> precompose(final @Nonnull FromEnvironment<? super Z, A, E> cokleisli) {
    nonNull(cokleisli);
    return z -> run(z.extend(cokleisli::run));
  }

  default @Nonnull <Z> FromEnvironment<Z, B, E> precompose(final @Nonnull Function<? super Z, ? extends A> function) {
    nonNull(function);
    return z -> run(z.map(function));
  }

  default @Nonnull <C> FromEnvironment<A, C, E> postcompose(final @Nonnull FromEnvironment<B, ? extends C, E> cokleisli) {
    nonNull(cokleisli);
    return a -> cokleisli.run(a.extend(this::run));
  }

  default @Nonnull <C> FromEnvironment<A, C, E> postcompose(final @Nonnull Function<? super B, ? extends C> function) {
    nonNull(function);
    return a -> function.$(run(a));
  }

  default @Nonnull <C> FromEnvironment<A, C, E> compose(final @Nonnull FromEnvironment<B, ? extends C, E> cokleisli) {
    nonNull(cokleisli);
    return a -> cokleisli.run(a.extend(this::run));
  }

  default @Nonnull <C> FromEnvironment<A, C, E> compose(final @Nonnull Function<? super B, ? extends C> function) {
    nonNull(function);
    return a -> function.$(run(a));
  }

  default @Nonnull <C> FromEnvironment<Pair<A, C>, Pair<B, C>, E> first() {
    return ac -> pair(ac.map(p -> p.first), ac.map(p -> p.second)).biMap(this::run, Environment::extract);
  }

  default @Nonnull <C> FromEnvironment<Pair<C, A>, Pair<C, B>, E> second() {
    return ca -> pair(ca.map(p -> p.first), ca.map(p -> p.second)).biMap(Environment::extract, this::run);
  }

  default @Nonnull <C, D> FromEnvironment<Pair<A, C>, Pair<B, D>, E> product(final @Nonnull FromEnvironment<C, ? extends D, E> cokleisli) {
    nonNull(cokleisli);
    return ac -> pair(ac.map(p -> p.first), ac.map(p -> p.second)).biMap(this::run, cokleisli::run);
  }

  default @Nonnull <C> FromEnvironment<A, Pair<B, C>, E> fanOut(final @Nonnull FromEnvironment<A, ? extends C, E> cokleisli) {
    nonNull(cokleisli);
    return a -> pair(run(a), cokleisli.run(a));
  }

  static @Nonnull <A, B, E> FromEnvironment<A, B, E> lift(final @Nonnull Function<? super A, ? extends B> function) {
    nonNull(function);
    return a -> function.$(a.extract());
  }

  static @Nonnull <A, E> FromEnvironment<A, A, E> id() {
    return Environment::extract;
  }
}
