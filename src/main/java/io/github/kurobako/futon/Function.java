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

package io.github.kurobako.futon;

import javax.annotation.Nonnull;
import static java.util.Objects.requireNonNull;

import static io.github.kurobako.futon.Pair.pair;

@FunctionalInterface
public interface Function<A, B> {
  B $(A argument);

  default @Nonnull <C> Function<A, C> compose(final @Nonnull Function<? super B, ? extends C> function) {
    requireNonNull(function);
    return a -> function.$(this.$(a));
  }

  default @Nonnull <C> Function<Either<A, C>, Either<B, C>> left() {
    return ac -> ac.biMap(this::$, id());
  }

  default @Nonnull <C> Function<Either<C, A>, Either<C, B>> right() {
    return ca -> ca.biMap(id(), this::$);
  }

  default @Nonnull <C> Function<Pair<A, C>, Pair<B, C>> first() {
    return ac -> ac.biMap(this::$, id());
  }

  default @Nonnull <C> Function<Pair<C, A>, Pair<C, B>> second() {
    return ca -> ca.biMap(id(), this::$);
  }

  default @Nonnull <C, D> Function<Either<A, C>, Either<B, D>> sum(final @Nonnull Function<? super C, ? extends D> function) {
    requireNonNull(function);
    return ac -> ac.biMap(this::$, function::$);
  }

  default @Nonnull <C, D> Function<Pair<A, C>, Pair<B, D>> product(final @Nonnull Function<? super C, ? extends D> function) {
    requireNonNull(function);
    return ac -> ac.biMap(this::$, function::$);
  }

  default @Nonnull <C> Function<Either<A, C>, B> fanIn(final @Nonnull Function<? super C, ? extends B> function) {
    requireNonNull(function);
    return ac -> ac.either(this::$, function::$);
  }

  default @Nonnull <C> Function<A, Pair<B, C>> fanOut(final @Nonnull Function<? super A, ? extends C> function) {
    requireNonNull(function);
    return a -> pair(this.$(a), function.$(a));
  }

  static @Nonnull <A> Function<A, A> id() {
    return a -> a;
  }

  static @Nonnull <A, B> Function<A, B> constant(final B value) {
    return ignoredArgument -> value;
  }
}
