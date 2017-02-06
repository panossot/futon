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
 * <p>Environment comonad is a computation which returns a value <b>A</b> together with some context <b>C</b>. Also known
 * as Coreader since it is dual to {@link Reader} monad.</p>
 * <p>Environment contains methods to modify both the context and the produced value.</p>
 * <p>{@link #map(Function)} makes Environment a functor.</p>
 * <p>{@link #extend(Function)} and {@link #get()} form a comonad.</p>
 * @param <C> context type.
 * @param <A> value type.
 */
public interface Environment<C, A> {
  /**
   * Runs the computation, producing a value <b>A</b> together with a context <b>A</b>.
   * @return <b>(C, A)</b> {@link Pair}. Can't be null.
   */
  @Nonnull Pair<C, A> run();

  /**
   * Returns the context of this computation.
   * @return computation context. Can't be null;
   */
  default @Nonnull C ask() {
    return run().first;
  }

  /**
   * Returns a value of this computation.
   * @return value produced by computaion. Can't be null.
   */
  default @Nonnull A get() {
    return run().second;
  }

  /**
   * Returns the context of this computation transformed by the given function.
   * @param function <b>C -&gt; F</b> transformation. Can't be null.
   * @param <F> transformed context type.
   * @return transformed context. Can't be null.
   * @throws NullPointerException if the argument was null.
   */
  default @Nonnull <F> F asks(final Function<? super C, ? extends F> function) {
    return nonNull(function).$(run().first);
  }

  /**
   * Returns a new Environment with its context transformed by the given function.
   * @param function <b>C -&gt; F</b> transformation. Can't be null.
   * @param <F> transformed context type.
   * @return an Environment with its context transformed. Can't be null.
   * @throws NullPointerException if the argument was null.
   */
  default <F> Environment<F, A> local(final Function<? super C, ? extends F> function) {
    nonNull(function);
    final Pair<F, A> fa = run().biMap(function, arg -> arg);
    return () -> fa;
  }

  /**
   * Returns a new Environment whose produced value is the product of applying the given function to this Environment's
   * produced value.
   * @param function <b>A -&gt; B</b> transformation. Can't be null.
   * @param <B> new value type.
   * @return new Environment. Can't be null.
   * @throws NullPointerException if the argument was null.
   */
  default @Nonnull <B> Environment<C, B> map(final Function<? super A, ? extends B> function) {
    nonNull(function);
    final Pair<C, A> ca = run();
    return environment(ca.first, function.$(run().second));
  }

  /**
   * Wraps this Environment into another layer as the wrapper's produced value. The context is the same for both
   * inner and outer Environments.
   * @return wrapped Environment. Can't be null.
   */
  default @Nonnull Environment<C, Environment<C, A>> duplicate() {
    return extend(arg -> arg);
  }

  /**
   * Returns a new Environment with the same context as this one, but its return value replaced by application of the
   * given function to this Environment.
   * @param function <b>Environment&lt;C, A&gt; -&gt; B</b> transformation. Can't be null.
   * @param <B> new value type.
   * @return new Environment. Can't be null.
   * @throws NullPointerException if the argument was null.
   */
  default @Nonnull <B> Environment<C, B> extend(final @Nonnull Function<? super Environment<C, A>, ? extends B> function) {
    nonNull(function);
    return environment(run().first, function.$(this));
  }

  /**
   * Creates a new Environment with the given context and produced values.
   * @param context new Environment's context value.
   * @param value new Environment's produced value.
   * @param <E> context type.
   * @param <A> value type.
   * @return new Environment. Can't be null.
   * @throws NullPointerException if any argument was null.
   */
  static @Nonnull <E, A> Environment<E, A> environment(final E context, final A value) {
    final Pair<E, A> ca = pair(context, value);
    return () -> ca;
  }
}