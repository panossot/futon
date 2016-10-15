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
import io.github.kurobako.futon.Traced;

import javax.annotation.Nonnull;

import static io.github.kurobako.futon.Pair.pair;
import static io.github.kurobako.futon.arrows.Util.nonNull;

public interface FromTraced<A, B, O> {
  B run(@Nonnull Traced<O, A> store);

  default @Nonnull <Z> FromTraced<Z, B, O> precompose(final @Nonnull FromTraced<? super Z, A, O> cokleisli) {
    nonNull(cokleisli);
    return z -> run(z.extend(cokleisli::run));
  }

  default @Nonnull <Z> FromTraced<Z, B, O> precompose(final @Nonnull Function<? super Z, ? extends A> function) {
    nonNull(function);
    return z -> run(z.map(function));
  }

  default @Nonnull <C> FromTraced<A, C, O> postcompose(final @Nonnull FromTraced<B, ? extends C, O> cokleisli) {
    nonNull(cokleisli);
    return a -> cokleisli.run(a.extend(this::run));
  }

  default @Nonnull <C> FromTraced<A, C, O> postcompose(final @Nonnull Function<? super B, ? extends C> function) {
    nonNull(function);
    return a -> function.$(run(a));
  }

  default @Nonnull <C> FromTraced<Pair<A, C>, Pair<B, C>, O> first() {
    return ac -> pair(ac.map(p -> p.first), ac.map(p -> p.second)).biMap(this::run, Traced::extract);
  }

  default @Nonnull <C> FromTraced<Pair<C, A>, Pair<C, B>, O> second() {
    return ca -> pair(ca.map(p -> p.first), ca.map(p -> p.second)).biMap(Traced::extract, this::run);
  }

  default @Nonnull <C, D> FromTraced<Pair<A, C>, Pair<B, D>, O> product(final @Nonnull FromTraced<C, ? extends D, O> cokleisli) {
    nonNull(cokleisli);
    return ac -> pair(ac.map(p -> p.first), ac.map(p -> p.second)).biMap(this::run, cokleisli::run);
  }

  default @Nonnull <C> FromTraced<A, Pair<B, C>, O> fanOut(final @Nonnull FromTraced<A, ? extends C, O> cokleisli) {
    nonNull(cokleisli);
    return a -> pair(run(a), cokleisli.run(a));
  }

  static @Nonnull <A, B, O> FromTraced<A, B, O> lift(final @Nonnull Function<? super A, ? extends B> function) {
    nonNull(function);
    return a -> function.$(a.extract());
  }

  static @Nonnull <O, A> FromTraced<A, A, O> id() {
    return Traced::extract;
  }
}
