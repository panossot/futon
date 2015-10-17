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
import java.util.Objects;

public final class Pair<L, R> {
  public final L left;
  public final R right;

  private Pair(final L left, final R right) {
    this.left = left;
    this.right = right;
  }

  public @Nonnull Pair<R, L> swap() {
    return pair(right, left);
  }

  public static @Nonnull <L, R> Pair<L, R> pair(final L left, final R right) {
    return new Pair<>(left, right);
  }

  @Override
  public int hashCode() {
    int l = Objects.hashCode(left);
    int r = Objects.hashCode(right);
    return l ^ (((r & 0xFFFF) << 16) | (r >> 16));
  }

  @Override
  public boolean equals(final Object o) {
    if (!(o instanceof Pair)) return false;
    Pair that = (Pair) o;
    return Objects.equals(this.left, that.left) && Objects.equals(this.right, that.right);
  }

  @Override
  public String toString() {
    return String.format("(%s, %s)", left, right);
  }
}
