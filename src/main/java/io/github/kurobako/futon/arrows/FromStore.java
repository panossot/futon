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

import io.github.kurobako.futon.Function;
import io.github.kurobako.futon.Pair;
import io.github.kurobako.futon.Store;

import javax.annotation.Nonnull;

import static io.github.kurobako.futon.Pair.pair;
import static io.github.kurobako.futon.arrows.Util.nonNull;

public interface FromStore<A, B, I> {
  B run(@Nonnull Store<I, A> store);

  default @Nonnull <Z> FromStore<Z, B, I> precompose(final @Nonnull FromStore<? super Z, A, I> cokleisli) {
    nonNull(cokleisli);
    return z -> run(z.extend(cokleisli::run));
  }

  default @Nonnull <Z> FromStore<Z, B, I> precompose(final @Nonnull Function<? super Z, ? extends A> function) {
    nonNull(function);
    return z -> run(z.map(function));
  }

  default @Nonnull <C> FromStore<A, C, I> postcompose(final @Nonnull FromStore<B, ? extends C, I> cokleisli) {
    nonNull(cokleisli);
    return a -> cokleisli.run(a.extend(this::run));
  }

  default @Nonnull <C> FromStore<A, C, I> postcompose(final @Nonnull Function<? super B, ? extends C> function) {
    nonNull(function);
    return a -> function.$(run(a));
  }
  
  default @Nonnull <C> FromStore<Pair<A, C>, Pair<B, C>, I> first() {
    return ac -> pair(ac.map(p -> p.first), ac.map(p -> p.second)).biMap(this::run, Store::extract);
  }

  default @Nonnull <C> FromStore<Pair<C, A>, Pair<C, B>, I> second() {
    return ca -> pair(ca.map(p -> p.first), ca.map(p -> p.second)).biMap(Store::extract, this::run);
  }

  default @Nonnull <C, D> FromStore<Pair<A, C>, Pair<B, D>, I> product(final @Nonnull FromStore<C, ? extends D, I> cokleisli) {
    nonNull(cokleisli);
    return ac -> pair(ac.map(p -> p.first), ac.map(p -> p.second)).biMap(this::run, cokleisli::run);
  }

  default @Nonnull <C> FromStore<A, Pair<B, C>, I> fanOut(final @Nonnull FromStore<A, ? extends C, I> cokleisli) {
    nonNull(cokleisli);
    return a -> pair(run(a), cokleisli.run(a));
  }

  static @Nonnull <A, B, I> FromStore<A, B, I> lift(final @Nonnull Function<? super A, ? extends B> function) {
    nonNull(function);
    return a -> function.$(a.extract());
  }

  static @Nonnull <I, A> FromStore<A, A, I> id() {
    return Store::extract;
  }
}