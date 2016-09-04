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
import static io.github.kurobako.futon.Util.nonNull;

public interface Value<A> {
  A extract();

  default @Nonnull <B> Value<B> extend(final @Nonnull Function<? super Value<A>, B> function) {
    final B b = nonNull(function).$(this);
    return () -> b;
  }

  default @Nonnull Value<? extends Value<A>> duplicate() {
    return () -> this;
  }

  default @Nonnull <B, C> Value<C> zip(final @Nonnull Value<B> value, final @Nonnull BiFunction<? super A, ? super B, ? extends C> biFunction) {
    final C c = nonNull(biFunction).$(extract(), nonNull(value).extract());
    return () -> c;
  }

  default @Nonnull <B, C> Pair<? extends Value<B>, ? extends Value<C>> unzip(final @Nonnull Function<? super A, Pair<B, C>> function) {
    final Pair<B, C> bc = nonNull(function).$(extract());
    return pair(() -> bc.first, () -> bc.second);
  }

  default @Nonnull <B> Value<B> bind(final @Nonnull Function<? super A, ? extends Value<B>> function) {
    return nonNull(function).$(extract());
  }

  default @Nonnull <B> Value<B> apply(final @Nonnull Value<? extends Function<? super A, ? extends B>> value) {
    final B b = nonNull(value).extract().$(extract());
    return () -> b;
  }

  default @Nonnull <B> Value<B> map(final @Nonnull Function<? super A, ? extends B> function) {
    final B b = nonNull(function).$(extract());
    return () -> b;
  }

  static @Nonnull <A> Value<A> unit(final A value) {
    return () -> value;
  }

  static <A> Value<A> join(final @Nonnull Value<? extends Value<A>> value) {
    return nonNull(value).extract();
  }
}