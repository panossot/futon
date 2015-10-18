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

import static io.github.kurobako.futon.Function.id;
import static java.util.Objects.requireNonNull;

@FunctionalInterface
public interface Lazy<A> extends Functor<A>, Foldable<A> {
  A $();

  default @Nonnull <B> Lazy<B> bind(final @Nonnull Function<? super A, ? extends Lazy<B>> function) {
    requireNonNull(function, "function");
    Lazy<B> result = function.$($());
    return requireNonNull(result, "result");
  }

  default @Nonnull <B, C> Lazy<C> zip(final @Nonnull Lazy<B> another,
                                      final @Nonnull BiFunction<? super A, ? super B, ? extends Lazy<C>> merge) {
    requireNonNull(another, "another");
    requireNonNull(merge, "merge");
    Lazy<C> result = merge.$($(), another.$());
    return requireNonNull(result, "result");
  }

  @Override
  default @Nonnull  <B> Lazy<B> map(final @Nonnull Function<? super A, ? extends B> function) {
    requireNonNull(function, "function");
    return bind(a -> lazy(function.$(a)));
  }

  @Override
  default <B> B foldRight(final @Nonnull BiFunction<A, B, B> function, B initial) {
    requireNonNull(function, "function");
    return function.$($(), initial);
  }

  @Override
  default <B> B foldLeft(final @Nonnull BiFunction<B, A, B> function, B initial) {
    requireNonNull(function, "function");
    return function.$(initial, $());
  }

  static @Nonnull <A> Lazy<A> lazy(A value) {
    return () -> value;
  }

  static @Nonnull <A> Lazy<A> join(@Nonnull Lazy<? extends Lazy<A>> lazy) {
    requireNonNull(lazy, "lazy");
    return lazy.bind(id());
  }
}
