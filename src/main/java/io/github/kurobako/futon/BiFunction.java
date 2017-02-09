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
 * <p>A pure function of two arguments. Neither arguments nor return value can be null.</p>
 * <p>This interface extends {@link Function} and can be combined with other functions as a function which sole
 * argument is <b>(A, B)</b> pair.</p>
 * @param <A> first argument type.
 * @param <B> second argument type.
 * @param <C> return type.
 */
@FunctionalInterface
public interface BiFunction<A, B, C> extends Function<Pair<A, B>, C> {
  /**
   * Applies the function to given arguments.
   * @param first first argument. Can't be null.
   * @param second second argument. Can't be null.
   * @return the result. Can't be null.
   */
  @Nonnull C $(A first, B second);

  /**
   * Applies the function to given arguments wrapped in a pair.
   * @param args a pair of the first and the second argument. Can't be null.
   * @return the result. Can't be null.
   * @throws NullPointerException if the argument was null.
   */
  @Override
  default @Nonnull C $(final Pair<A, B> args) {
    nonNull(args);
    return $(args.first, args.second);
  }

  /**
   * Flips the function, making a new function which accepts the first and the second argument in reverse order.
   * @param function binary function to flip. Can't be null.
   * @param <A> first argument type of the given function.
   * @param <B> second argument type of the given function.
   * @param <C> return type.
   * @return new Function. Can't be null.
   * @throws NullPointerException if the argument was null.
   */
  static @Nonnull <A, B, C> BiFunction<B, A, C> flip(final BiFunction<? super A, ? super B, ? extends C> function) {
    nonNull(function);
    return (b, a) -> function.$(a, b);
  }

  /**
   * Curry the given binary function so that it would accept one argument at a time, returning intermediate result as a
   * function from the remaining argument to the result.
   * @param function binary function to curry. Can't be null.
   * @param <A> first argument type.
   * @param <B> second argument type.
   * @param <C> return type.
   * @return curried function. Can't be null.
   * @throws NullPointerException if the argument was null.
   */
  static @Nonnull <A, B, C> Function<A, Function<B, C>> curry(final BiFunction<? super A, ? super B, ? extends C> function) {
    nonNull(function);
    return a -> b -> function.$(a, b);
  }

  /**
   * Uncurry the function, making curried function to accept all of its argument at the same time.
   * @param function binary function to uncurry. Can't be null.
   * @param <A> first argument type.
   * @param <B> second argument type.
   * @param <C> return type.
   * @return uncurried function. Can't be null.
   * @throws NullPointerException if the argument was null.
   */
  static @Nonnull <A, B, C> BiFunction<A, B, C> uncurry(final Function<? super A, ? extends Function<? super B, ? extends C>> function) {
    return (a, b) -> function.$(a).$(b);
  }
}
