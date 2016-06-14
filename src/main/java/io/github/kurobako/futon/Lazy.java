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

import static io.github.kurobako.futon.Pair.pair;
import static java.util.Objects.requireNonNull;

@FunctionalInterface
public interface Lazy<A> {
  A extract();

  default @Nonnull Lazy<? extends Lazy<A>> duplicate() {
    return () -> this;
  }

  default @Nonnull <B> Lazy<B> extend(final @Nonnull Function<? super Lazy<A>, ? extends B> function) {
    requireNonNull(function);
    return () -> function.$(this);
  }

  default @Nonnull <B> Lazy<B> bind(final @Nonnull Function<? super A, ? extends Lazy<B>> function) {
    requireNonNull(function);
    return () -> function.$(this.extract()).extract();
  }

  default @Nonnull <B, C> Lazy<C> zip(final @Nonnull Lazy<B> lazy, final @Nonnull Function<? super Pair<A, B>, ? extends C> function) {
    requireNonNull(lazy);
    requireNonNull(function);
    return () -> function.$(pair(this.extract(), lazy.extract()));
  }

  default @Nonnull <B, C> Pair<? extends Lazy<B>, ? extends Lazy<C>> unzip(final @Nonnull Function<? super A, Pair<? extends B, ? extends C>> function) {
    requireNonNull(function);
    return pair(() -> function.$(this.extract()).first, () -> function.$(this.extract()).second);
  }

  default @Nonnull <B> Lazy<B> apply(final @Nonnull Lazy<? extends Function<? super A, ? extends B>> lazy) {
    requireNonNull(lazy);
    return () -> lazy.extract().$(this.extract());
  }

  default @Nonnull <B> Lazy<B> map(final @Nonnull Function<? super A, ? extends B> function) {
    requireNonNull(function);
    return () -> function.$(this.extract());
  }

  static @Nonnull <A> Lazy<A> join(final @Nonnull Lazy<? extends Lazy<A>> lazy) {
    requireNonNull(lazy);
    return lazy.extract();
  }

  static @Nonnull <A> Lazy<A> lazy(final A value) {
    return () -> value;
  }
}
