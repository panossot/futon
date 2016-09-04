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

import static io.github.kurobako.futon.Util.equal;
import static io.github.kurobako.futon.Util.hash;
import static io.github.kurobako.futon.Util.nonNull;

public class Pair<A, B> {
  public final A first;
  public final B second;

  private Pair(final A first, final B second) {
    this.first = first;
    this.second = second;
  }

  public @Nonnull <X, Y> Pair<X, Y> biMap(final @Nonnull Function<? super A, ? extends X> firstFunction, final @Nonnull Function<? super B, ? extends Y> secondFunction) {
    nonNull(firstFunction);
    nonNull(secondFunction);
    return pair(firstFunction.$(first), secondFunction.$(second));
  }

  public @Nonnull Pair<B, A> swap() {
    return pair(second, first);
  }

  @Override
  public int hashCode() {
    final int l = hash(first);
    final int r = hash(second);
    return l ^ (((r & 0xFFFF) << 16) | (r >> 16));
  }

  @Override
  public boolean equals(final Object o) {
    if (!(o instanceof Pair)) return false;
    final Pair that = (Pair) o;
    return equal(first, that.first) && equal(second, that.second);
  }

  @Override
  public @Nonnull String toString() {
    return String.format("(%s, %s)", first, second);
  }

  public static <A, B> Pair<A, B> pair(final A first, final B second) {
    return new Pair<>(first, second);
  }
}
