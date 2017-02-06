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

import static io.github.kurobako.futon.Pair.pair;
import static io.github.kurobako.futon.Util.nonNull;

/**
 * <p>A pure function from <b>A</b> to <b>B</b> which argument and return values can't be null.</p>
 * <p>Functions are also arrows and can be combined as such.</p>
 * @param <A> argument type.
 * @param <B> return type.
 */
public interface Function<A, B> {
  /**
   * Applies the function to the given argument.
   * @param arg the argument. Can't be null.
   * @return the result. Can't be null.
   */
  @Nonnull B $(A arg);

  /**
   * Returns a function combining this function with the given function: <b>Z -&gt; A -&gt; B</b>.
   * @param function <b>Z -&gt; A</b> function. Can't be null.
   * @param <Z> argument type for the new function.
   * @return new <b>Z -&gt; A</b> function. Can't be null.
   * @throws NullPointerException if argument was null.
   */
  default @Nonnull <Z> Function<Z, B> precompose(final Function<? super Z, ? extends A> function) {
    nonNull(function);
    return z -> $(function.$(z));
  }

  /**
   * Returns a function combining this function with the given function: <b>A -&gt; B -&gt; C</b>.
   * @param function <b>B -&gt; C</b> function. Can't be null.
   * @param <C> return type for the new function.
   * @return new function: <b>A -&gt; C</b>. Can't be null.
   * @throws NullPointerException if argument was null.
   */
  default @Nonnull <C> Function<A, C> postcompose(final Function<? super B, ? extends C> function) {
    nonNull(function);
    return a -> function.$($(a));
  }

  /**
   * Returns a function which maps its input using this function of it is {@link Either.Left} and passes it
   * unchanged otherwise.
   * @param <C> right component type.
   * @return new Function. Can't be null.
   */
  default @Nonnull <C> Function<Either<A, C>, Either<B, C>> left() {
    return ac -> ac.biMap(this, arg -> arg);
  }

  /**
   * Returns a function which maps its input using this function of it is {@link Either.Right} and passes it
   * unchanged otherwise.
   * @param <C> left component type.
   * @return new Function. Can't be null.
   */
  default @Nonnull <C> Function<Either<C, A>, Either<C, B>> right() {
    return ca -> ca.biMap(arg -> arg, this);
  }

  /**
   * Returns a function which maps first part of its input and passes the second part unchanged.
   * @param <C> right component type.
   * @return new Function. Can't be null.
   */
  default @Nonnull <C> Function<Pair<A, C>, Pair<B, C>> first() {
    return ac -> ac.biMap(this, arg -> arg);
  }
  /**
   * Returns a function which maps second part of its input and passes the first part unchanged.
   * @param <C> left component type.
   * @return new Function. Can't be null.
   */
  default @Nonnull <C> Function<Pair<C, A>, Pair<C, B>> second() {
    return ca -> ca.biMap(arg -> arg, this);
  }

  /**
   * Returns a function which maps its input using this function if it is {@link Either.Left} and using the given function if it is {@link Either.Right}.
   * @param function right <b>C -&gt; D</b> mapping. Can't be null.
   * @param <C> right argument type.
   * @param <D> right return type.
   * @return new Function. Can't be null.
   * @throws NullPointerException if argument is null.
   */
  default @Nonnull <C, D> Function<Either<A, C>, Either<B, D>> sum(final Function<? super C, ? extends D> function) {
    nonNull(function);
    return ac -> ac.biMap(this, function::$);
  }

  /**
   * Returns a function which maps the first part of its input using this function and the second part using the given function.
   * @param function second <b>C -&gt; D</b> mapping. Can't be null.
   * @param <C> second argument type.
   * @param <D> second return type.
   * @return new Function. Can't be null.
   * @throws NullPointerException if argument is null.
   */
  default @Nonnull <C, D> Function<Pair<A, C>, Pair<B, D>> product(final Function<? super C, ? extends D> function) {
    nonNull(function);
    return ac -> ac.biMap(this, function::$);
  }

  /**
   * Returns a function which maps input using this function if it is {@link Either.Left} or the given function if it is {@link Either.Right}.
   * @param function left <b>C -&gt; B</b> mapping. Can't be null.
   * @param <C> right argument type.
   * @return new Function. Can't be null.
   * @throws NullPointerException if argument is null.
   */
  default @Nonnull <C> Function<Either<A, C>, B> fanIn(final Function<? super C, ? extends B> function) {
    nonNull(function);
    return ac -> ac.either(this, function::$);
  }

  /**
   * Returns a function which maps its input using this function and the given function and returns two resulting values as a pair.
   * @param function second <b>A -&gt; C</b> mapping. Can't be null.
   * @param <C> second return type.
   * @return new Function. Can't be null.
   * @throws NullPointerException if argument is null.
   */
  default @Nonnull <C> Function<A, Pair<B, C>> fanOut(final Function<? super A, ? extends C> function) {
    nonNull(function);
    return a -> pair(this.$(a), function.$(a));
  }

  /**
   * Returns a function <b>(A -&gt; B, B) -&gt; B</b> which applies its input function (<b>A -&gt; B</b>) to its input
   * value (<b>A</b>) and returns the result (<b>B</b>).
   * @param <A> argument type.
   * @param <B> return type.
   * @return a Function. Can't be null.
   */
  static @Nonnull <A, B> Function<Pair<Function<A, B>, A>, B> apply() {
    return fa -> fa.first.$(fa.second);
  }
}