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
 * <p>Store comonad is an index <b>I</b> together with a function from such index to a value: <b>I -&gt; A</b>.</p>
 * <p>It might be used as a window into an immutable data structure which shape is not known to the user. Alternatively,
 * Store might be a value <b>I</b> together with a transforming function <b>I -> A</b> providing a focused view into
 * its internals.</p>
 * <p>{@link #map(Function)} makes Store a functor.</p>
 * <p>{@link #extend(Function)} and {@link #get()} form a comonad.</p>
 * @param <I> index type.
 * @param <A> value type.
 */
@FunctionalInterface
public interface Store<I, A> {
  /**
   * Returns both index and a function this Store contains.
   * @return a {@link Pair} of and index and a function from an index to a value. Can't be null.
   */
  @Nonnull Pair<Function<? super I, ? extends A>, I> run();

  /**
   * Returns the index of this Store.
   * @return index. Can't be null.
   */
  default @Nonnull I pos() {
    return run().second;
  }

  /**
   * Applies the function of this Store to the given index and returns the result.
   * @param index argument to this Store's function. Can't be null.
   * @return application result. Can't be null.
   * @throws NullPointerException if the argument was null.
   */
  default @Nonnull A peek(final I index) {
    return run().first.$(index);
  }

  /**
   * First transforms the index of this Store with the given function, then feeds it to this Store's function.
   * @param function <b>I -&gt; I</b> transformation. Can't be null.
   * @return application result. Can't be null.
   * @throws NullPointerException if the argument was null.
   */
  default A peeks(final Function<? super I, ? extends I> function) {
    nonNull(function);
    final Pair<Function<? super I, ? extends A>, I> fi = run();
    return fi.first.$(function.$(fi.second));
  }

  /**
   * Returns a new Store with index replaced by the given index.
   * @param index new index value. Can't be null.
   * @return new Store. Can't be null.
   * @throws NullPointerException if the argument was null.
   */
  default @Nonnull Store<I, A> seek(final I index) {
    final Pair<Function<? super I, ? extends A>, I> fi = run();
    return store(fi.first, index);
  }

  /**
   * Returns a new Store with index transformed by the given function.
   * @param function <b>I -&gt; I</b> transformation. Can't be null.
   * @return new Store. Can't be null.
   * @throws NullPointerException if the argument was null.
   */
  default @Nonnull Store<I, A> seeks(final Function<? super I, ? extends I> function) {
    nonNull(function);
    final Pair<Function<? super I, ? extends A>, I> fi = run();
    return store(fi.first, function.$(fi.second));
  }

  /**
   * Applies this Store's function to its index and returns the result.
   * @return application result. Can't be null.
   */
  default @Nonnull A get() {
    final Pair<Function<? super I, ? extends A>, I> fi = run();
    return fi.first.$(fi.second);
  }

  /**
   * Duplicates this Store, wrapping it into a new Store.
   * @return wrapped Store. Can't be null.
   */
  default @Nonnull Store<I, Store<I, A>> duplicate() {
    return extend(arg -> arg);
  }

  /**
   * Returns a new Store whose produced value is the product of applying the given function to this Store's produced
   * value. Index remains the same.
   * @param function <b>A -&gt; B</b> transformation. Can't be null.
   * @param <B> new value type.
   * @return new Store. Can't be null.
   * @throws NullPointerException if the argument was null.
   */
  default @Nonnull <B> Store<I, B> map(final Function<? super A, ? extends B> function) {
    nonNull(function);
    final Pair<Function<? super I, ? extends A>, I> fi = run();
    return store(function.precompose(fi.first), fi.second);
  }

  /**
   * Returns a new Store with the same index as this one, but its function replaced by application of the given function
   * to this Store with the index provided by the caller.
   * @param function <b>Store&lt;I, A&gt; -&gt; B</b> transformation. Can't be null.
   * @param <B> new value type.
   * @return new Store. Can't be null.
   * @throws NullPointerException if the argument was null.
   */
  default @Nonnull <B> Store<I, B> extend(final Function<? super Store<I, A>, ? extends B> function) {
    nonNull(function);
    final Pair<Function<? super I, ? extends A>, I> fi = run();
    return store(index -> {
      Store<I, A> s = store(fi.first, index);
      return function.$(s);
    }, fi.second);
  }

  /**
   * Creates a new Store with the given function and index.
   * @param function new Store's <b>I -&gt; A</b> function. Can't be null.
   * @param index new Store's index value. Can't be null.
   * @param <I> index type.
   * @param <A> value type.
   * @return new Store. Can't be null.
   * @throws NullPointerException if the argument was null.
   */
  static @Nonnull <I, A> Store<I, A> store(final Function<? super I, ? extends A> function, final I index) {
    nonNull(function);
    final Pair<Function<? super I, ? extends A>, I> fi = pair(function, index);
    return () -> fi;
  }
}