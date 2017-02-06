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

/**
 * A generic interface for containers of values which might be folded from left to right or from right to left using the
 * reducer function and initial value provided as arguments. For some containers implementations of this methods might
 * give identical results while others, like {@link Sequence}, order of reduction will matter. Folding an empty container
 * will just return the initial value provided to the function.
 * @param <A> contained value type.
 */
public interface Foldable<A> {
  /**
   * Reduces this container from right ('end') to left ('start') using the given binary function and initial value.
   * @param function <b>A -&gt; B -&gt; B</b>: function accepting accumulated result and a new value, producing new accumulated value.
   * @param initial initial accumulator.
   * @param <B> accumulator type.
   * @return accumulated result. Can' be null.
   * @throws NullPointerException if the argument was null.
   */
  @Nonnull <B> B foldRight(BiFunction<? super A, ? super B, ? extends B> function, B initial);

  /**
   * Reduces this container from left ('start') to right ('end') using the given binary function and initial value.
   * @param function <b>B -&gt; A -&gt; B</b>: function accepting accumulated result and a new value, producing new accumulated value.
   * @param initial initial accumulator.
   * @param <B> accumulator type.
   * @return accumulated result. Can' be null.
   * @throws NullPointerException if the argument was null.
   */
  @Nonnull <B> B foldLeft(BiFunction<? super B, ? super A, ? extends B> function, B initial);
}
