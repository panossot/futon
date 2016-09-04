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
import io.github.kurobako.futon.Value;

import javax.annotation.Nonnull;

import static io.github.kurobako.futon.Pair.pair;
import static io.github.kurobako.futon.arrows.Util.nonNull;

public interface FromValue<A, B> {
  B run(@Nonnull Value<A> arg);

  default @Nonnull <Z> FromValue<Z, B> precompose(final @Nonnull FromValue<? super Z, A> cokleisli) {
    nonNull(cokleisli);
    return z -> run(z.extend(cokleisli::run));
  }

  default @Nonnull <Z> FromValue<Z, B> precompose(final @Nonnull Function<? super Z, ? extends A> function) {
    nonNull(function);
    return z -> run(z.map(function));
  }

  default @Nonnull <C> FromValue<A, C> postcompose(final @Nonnull FromValue<B, ? extends C> cokleisli) {
    nonNull(cokleisli);
    return a -> cokleisli.run(a.extend(this::run));
  }

  default @Nonnull <C> FromValue<A, C> postcompose(final @Nonnull Function<? super B, ? extends C> function) {
    nonNull(function);
    return a -> function.$(run(a));
  }

  default @Nonnull <C> FromValue<Pair<A, C>, Pair<B, C>> first() {
    return ac -> ac.unzip(Function.id()).biMap(this::run, Value::extract);
  }

  default @Nonnull <C> FromValue<Pair<C, A>, Pair<C, B>> second() {
    return ca -> ca.unzip(Function.id()).biMap(Value::extract, this::run);
  }

  default @Nonnull <C, D> FromValue<Pair<A, C>, Pair<B, D>> product(final @Nonnull FromValue<C, ? extends D> cokleisli) {
    nonNull(cokleisli);
    return ac -> ac.unzip(Function.id()).biMap(this::run, cokleisli::run);
  }

  default @Nonnull <C> FromValue<A, Pair<B, C>> fanOut(final @Nonnull FromValue<A, ? extends C> cokleisli) {
    nonNull(cokleisli);
    return a -> pair(run(a), cokleisli.run(a));
  }

  static @Nonnull <A, B> FromValue<A, B> lift(final @Nonnull Function<? super A, ? extends B> function) {
    nonNull(function);
    return a -> function.$(a.extract());
  }

  static @Nonnull <A> FromValue<A, A> id() {
    return Value::extract;
  }
}
