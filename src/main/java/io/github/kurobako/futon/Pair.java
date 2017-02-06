/*
 * Copyright (C) 2017 Fedor Gavrilov
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

import static io.github.kurobako.futon.Util.nonNull;

/**
 * <b>Pair is a tuple of two non-null values.</b>
 * <b>Unlike {@link Either}, both values have to be present and may be mapped using {@link #biMap(Function, Function)}
 * simultaneously.</b>
 * @param <A> first value type.
 * @param <B> second value type.
 */
public class Pair<A, B> {
  /**
   * First value. Can't be null.
   */
  public final @Nonnull A first;

  /**
   * Second value. Can't be null.
   */
  public final @Nonnull B second;

  private Pair(final A first, final B second) {
    this.first = first;
    this.second = second;
  }

  /**
   * Applies the given binary function to values of this pair and returns its result.
   * @param function <b>A -&gt; B -&gt; C</b> transformation. Can't be null.
   * @param <X> result type.
   * @return function application result. Can't be null.
   * @throws NullPointerException if the argument was null.
   */
  public @Nonnull <X> X squeeze(final BiFunction<? super A, ? super B, ? extends X> function) {
    return nonNull(function).$(first, second);
  }

  /**
   * Creates a new Pair, transforming first value of this Pair using firstFunction and second value using secondFunction.
   * @param firstFunction <b>A -&gt; X</b> transformation. Can't be null.
   * @param secondFunction <b>B -&gt; Y</b> transformation. Can't be null.
   * @param <X> new first value type.
   * @param <Y> new second value type.
   * @return transformed Pair. Can't be null.
   * @throws NullPointerException is any argument was null.
   */
  public @Nonnull <X, Y> Pair<X, Y> biMap(final Function<? super A, ? extends X> firstFunction, final Function<? super B, ? extends Y> secondFunction) {
    return pair(nonNull(firstFunction).$(first), nonNull(secondFunction).$(second));
  }

  /**
   * Returns a new Pair which holds this first value as its second value or this second value as its first value.
   * @return new Pair with <b>A</b> and <b>B</b> types swapped. Can't be null.
   */
  public @Nonnull Pair<B, A> swap() {
    return new Pair<>(second, first);
  }

  @Override
  public int hashCode() {
    final int l = first.hashCode();
    final int r = second.hashCode();
    return l ^ (((r & 0xFFFF) << 16) | (r >> 16));
  }

  @Override
  public boolean equals(final Object o) {
    if (!(o instanceof Pair)) return false;
    final Pair that = (Pair) o;
    return this.first.equals(that.first) && this.second.equals(that.second);
  }

  @Override
  public @Nonnull String toString() {
    return "(" + first + ", " + second + ")";
  }

  /**
   * Creates a new Pair with the given first and second values.
   * @param first first value. Can'e be null.
   * @param second second value. Can't be null/
   * @param <A> first value type.
   * @param <B> second value type.
   * @return new Pair. Can't be null.
   * @throws NullPointerException if any argument was null.
   */
  public static <A, B> Pair<A, B> pair(final A first, final B second) {
    return new Pair<>(nonNull(first), nonNull(second));
  }
}
