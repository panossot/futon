/*
 * Copyright (C) 2015 Fedor Gavrilov
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

@FunctionalInterface
public interface Function<A, B> {
  B $(A argument);

  default @Nonnull <C> Function<A, C> bind(final @Nonnull
                                           Function<? super B, ? extends Function<? super A, ? extends C>> function) {
    requireNonNull(function, "function");
    return a -> function.$($(a)).$(a);
  }

  default @Nonnull <C> Function<A, C> apply(final @Nonnull
                                            Function<? super B, ? extends Function<? super B, ? extends C>> function) {
    requireNonNull(function, "function");
    return a -> {
      B b = $(a);
      return function.$(b).$(b);
    };
  }

  default @Nonnull <C> Function<A, C> map(final @Nonnull Function<? super B, ? extends C> function) {
    requireNonNull(function, "function");
    return a -> function.$($(a));
  }

  default @Nonnull <Z> Function<Z, B> of(final @Nonnull Function<? super Z, ? extends A> function) {
    requireNonNull(function, "function");
    return z -> $(function.$(z));
  }

  static @Nonnull <A, B> Function<A, B> join(final @Nonnull
                                             Function<A, ? extends Function<? super A, ? extends B>> function) {
    requireNonNull(function, "function");
    return function.bind(id());
  }

  static @Nonnull <V> Function<?, V> constant(final V value) {
    return any -> value;
  }

  static @Nonnull <A> Function<A, A> id() {
    return a -> a;
  }
}
