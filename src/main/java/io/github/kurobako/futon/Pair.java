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
import java.util.Objects;

import static java.util.Objects.requireNonNull;

public final class Pair<L, R> {
  public final L first;
  public final R second;

  private Pair(final L first, final R second) {
    this.first = first;
    this.second = second;
  }

  public @Nonnull <X, Y> Pair<X, Y> biMap(final @Nonnull Function<? super L, ? extends X> first, final @Nonnull Function<? super R, ? extends Y> second) {
    requireNonNull(first);
    requireNonNull(second);
    return pair(first.$(this.first), second.$(this.second));
  }

  public @Nonnull Pair<R, L> swap() {
    return pair(this.second, this.first);
  }

  @Override
  public int hashCode() {
    final int l = Objects.hashCode(this.first);
    final int r = Objects.hashCode(this.second);
    return l ^ (((r & 0xFFFF) << 16) | (r >> 16));
  }

  @Override
  public boolean equals(final Object o) {
    if (!(o instanceof Pair)) return false;
    final Pair that = (Pair) o;
    return Objects.equals(this.first, that.first) && Objects.equals(this.second, that.second);
  }

  @Override
  public @Nonnull String toString() {
    return String.format("(%s, %s)", this.first, this.second);
  }

  public static @Nonnull <L, R> Pair<L, R> pair(final L first, final R second) {
    return new Pair<>(first, second);
  }
}
