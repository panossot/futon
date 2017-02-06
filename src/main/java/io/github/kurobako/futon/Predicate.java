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
 * <p>Predicate is a <b>A -&gt; Boolean</b> {@link Function} with additional logical operations.</p>
 * <p>{@link #contraMap(Function)} makes Predicate a contravariant functor.</p>
 * @param <A> argument type.
 */
public interface Predicate<A> extends Function<A, Boolean> {

  /**
   * Returns new Predicate combining this and the given predicate using AND (short-circuit) operator.
   * @param predicate predicate to combine with. Can't be null.
   * @return new Predicate. Can't be null.
   * @throws NullPointerException is the argument was null.
   */
  default @Nonnull Predicate<A> and(final @Nonnull Predicate<? super A> predicate) {
    nonNull(predicate);
    return a -> this.$(a) && predicate.$(a);
  }

  /**
   * Returns new Predicate combining this and the given predicate using OR (short-circuit) operator.
   * @param predicate predicate to combine with. Can't be null.
   * @return new Predicate. Can't be null.
   * @throws NullPointerException is the argument was null.
   */
  default @Nonnull Predicate<A> or(final Predicate<? super A> predicate) {
    nonNull(predicate);
    return a -> this.$(a) || predicate.$(a);
  }

  /**
   * Returns new Predicate combining this and the given predicate using XOR operator.
   * @param predicate predicate to combine with. Can't be null.
   * @return new Predicate. Can't be null.
   * @throws NullPointerException is the argument was null.
   */
  default @Nonnull Predicate<A> xor(final Predicate<? super A> predicate) {
    nonNull(predicate);
    return a -> this.$(a) ^ predicate.$(a);
  }

  /**
   * Returns a new Predicate operating on arguments of type <b>B</b> which are first transformed using the given
   * function and then fed to this predicate to get the result.
   * @param function <b>B -&gt; A</b> transformation. Can't be null.
   * @param <B> new predicate argument type.
   * @return new Predicate. Can't be null.
   * @throws NullPointerException is the argument was null.
   */
  default @Nonnull <B> Predicate<B> contraMap(Function<? super B, ? extends A> function) {
    nonNull(function);
    return b -> this.$(function.$(b));
  }

  /**
   * Returns new Predicate inverting the given predicate return value.
   * @param predicate predicate to combine with. Can't be null.
   * @return new Predicate. Can't be null.
   * @throws NullPointerException is the argument was null.
   */
  static @Nonnull <A> Predicate<A> not(final @Nonnull Predicate<? super A> predicate) {
    nonNull(predicate);
    return a -> !predicate.$(a);
  }

  /**
   * Returns a predicate which always returns <b>true</b>.
   * @return reader true predicate. Can't be null.
   */
  static @Nonnull <A> Predicate<A> TRUE() {
    return a -> Boolean.TRUE;
  }

  /**
   * Returns a predicate which always returns <b>false</b>.
   * @return reader false predicate. Can't be null.
   */
  static @Nonnull <A> Predicate<A> FALSE() {
    return a -> Boolean.FALSE;
  }
}
