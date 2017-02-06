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
import static io.github.kurobako.futon.Trampoline.delay;
import static io.github.kurobako.futon.Util.nonNull;

/**
 * <p>Value is the simplest possible monad (and comonad): a wrapper around non-null value retrieved via {@link #get()} method.
 * It can be used to model a non-strict evaluation.</p>
 * <p>The interface itself does not specify any strictness policy, however. The default implementation avoids building
 * long series of lazy chunks by evaluating the result of the computation when transformation ({@link #map(Function)} etc.)
 * method is called. The transformation itself is not done eagerly where possible, only the previous value is evaluated
 * and closed over.
 * Default definitions of methods may be overridden in a subinterface to make the value truly lazy. However, care must
 * be taken to not overflow the stack if the chain of computations is long enough.
 * If you want a version of monad which delays the computation until the very last moment possible and executes all
 * transformations in reader stack space, check {@link Trampoline}.</p>
 * <p>Value does not memoize its results by default and will re-evaluate the computation each time. For a version of Value
 * which uses memoization, see {@link Thunk}.</p>
 * <p>{@link #map(Function)} makes Value a functor.</p>
 * <p>{@link #apply(Value)} and {@link #value(A)} form an applicative functor.</p>
 * <p>{@link #bind(Function)} and {@link #value(A)} form a monad.</p>
 * <p>{@link #extend(Function)} and {@link #get()} form a comonad.</p>
 * @param <A> result type.
 */
public interface Value<A> {
  /**
   * Evaluate and return the result.
   * @return computation result. Can't be null.
   */
  @Nonnull A get();

  /**
   * Returns a Value whose result is the product of applying the given function to the result this Value.
   * @param function <b>A -&gt; B</b> transformation. Can't be null.
   * @param <B> new result type.
   * @return transformed Value. Can't be null.
   * @throws NullPointerException if the argument was null.
   */
  default @Nonnull <B> Value<B> map(final Function<? super A, ? extends B> function) {
    nonNull(function);
    final A a = get();
    return () -> function.$(a);
  }

  /**
   * Returns a Value whose result is the product of applying the function stored as the given Value to this value.
   * @param value <b>Value&lt;A -&gt; B&gt;</b>: transformation inside the Value. Can't be null.
   * @param <B> new result type.
   * @return transformed Value. Can't be null.
   * @throws NullPointerException if the argument was null.
   */
  default @Nonnull <B> Value<B> apply(final Value<? extends Function<? super A, ? extends B>> value) {
    final Function<? super A, ? extends B> f = nonNull(value).get();
    final A a = get();
    return () -> f.$(a);
  }

  /**
   * Returns a Value which is the product of applying the given function to the result of this Value.
   * @param function <b>A -&gt; Value&lt;B&gt;</b> transformation. Can't be null.
   * @param <B> new result type.
   * @return transformed Value. Can't be null.
   * @throws NullPointerException if the argument was null.
   */
  default @Nonnull <B> Value<B> bind(final Function<? super A, ? extends Value<B>> function) {
    return nonNull(function).$(get());
  }

  /**
   * Returns a Value with its result evaluated by application of the given function to this Value.
   * @param function <b>Value&lt;A&gt; -&gt; B</b> transformation. Can't be null.
   * @param <B> new result type.
   * @return new Value. Can't be null.
   * @throws NullPointerException if the argument was null.
   */
  default @Nonnull <B> Value<B> extend(final Function<? super Value<A>, B> function) {
    nonNull(function);
    final B b = nonNull(function).$(this);
    return () -> b;
  }

  /**
   * Wraps this Value into another layer of Value, thus delaying the time the computation will be forced.
   * @return wrapped Value. Can't be null.
   */
  default @Nonnull Value<? extends Value<A>> duplicate() {
    return () -> this;
  }

  /**
   * Zips this with the given Value using the given function.
   * @param value a Value to zip with.
   * @param function <b>A -&gt; B -&gt; C</b> transformation. Can't be null.
   * @param <B> result type of the Value to zip with.
   * @param <C> new result type.
   * @return a Value zipped with the given Value. Can't be null.
   * @throws NullPointerException if any argument was null.
   */
  default @Nonnull <B, C> Value<C> zip(final Value<B> value, final BiFunction<? super A, ? super B, ? extends C> function) {
    nonNull(function);
    final B b = nonNull(value).get();
    final A a = get();
    return () -> function.$(a, b);
  }

  /**
   * Unzips this into a {@link Pair} of Values using the given function.
   * @param function <b>A -&gt; (B, C)</b> transformation. Can't be null.
   * @param <B> result type of the first value of the pair.
   * @param <C> result type of the second value of the pair.
   * @return a Pair of unzipped Values.
   * @throws NullPointerException if the argument was null.
   */
  default @Nonnull <B, C> Pair<? extends Value<B>, ? extends Value<C>> unzip(final Function<? super A, Pair<B, C>> function) {
    final Pair<B, C> bc = nonNull(function).$(get());
    return pair(() -> bc.first, () -> bc.second);
  }

  /**
   * Creates a new Value wrapping the argument.
   * @param value a value to wrap. Can't be null.
   * @param <A> result type.
   * @return a new Value. Can't be null.
   * @throws NullPointerException if the argument was null.
   */
  static @Nonnull <A> Value<A> value(final A value) {
    return () -> value;
  }

  /**
   * Flattens nested Values.
   * @param value Value wrapped in another Value.
   * @param <A> result type of the returned Value.
   * @return flattened Value. Can't be null.
   */
  static @Nonnull <A> Value<A> unwrap(final Value<? extends Value<A>> value) {
    return nonNull(value).get();
  }
}