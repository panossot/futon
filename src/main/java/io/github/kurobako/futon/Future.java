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
 * <p>Future is a readable part of a Future-{@link Promise} pair similar to those in scala standard library representing
 * a computation which might be not completed yet and may result in a failure.</p>
 * <p>Unlike java {@link java.util.concurrent.Future}, there is no cancel method since there is no running task associated
 * with this Future: Promise, the writable part of the pair, might be completed with result or failure anytime from any
 * thread or remain incomplet. This breaks the dependency on the {@link java.util.concurrent.Executor} and its {@link java.util.Queue}
 * and allows this primitive to be used in remoting with no threads blocked or with non-standard inter-thread communication mechanisms
 * like <a href="https://github.com/LMAX-Exchange/disruptor">LMAX Disruptor</a></p>.
 * <p>All transformations attached to the Future will run only once when its Promise is complete by the thread completing
 * it. If transformation functions are called after this Future's Promise is complete they will be executed immediately
 * by the calling thread.</p>
 * <p>{@link #map(Function)} makes Future a functor.</p>
 * <p>{@link #apply(Future)} and {@link #successful(A)}} form an applicative functor.</p>
 * <p>{@link #bind(Function)} and {@link #successful(A)} form a monad.</p>
 * @see Promise
 * @see <a href="http://www.scala-lang.org/api/2.12.1/scala/concurrent/Future.html">http://www.scala-lang.org/api/2.12.1/scala/concurrent/Future.html</a>
 * @param <A> result type.
 */
public interface Future<A> {
  /**
   * Returns a new Future whose result value is the product of applying the given function to this Future's result value.
   * If this Future is a failure, returned Future fails with the same exception. RuntimeException thrown by map won't be
   * caught in a Future and will be thrown in a thread performing the map. If you want a version of map which can throw
   * any exceptions and/or execute side-effects, check {@link #then(Effect)};
   * @param function <b>A -&gt; B</b> transformation. Can't be null.
   * @param <B> new result type.
   * @return transformed Future. Can't be null.
   * @throws NullPointerException if the argument was null.
   */
  @Nonnull <B> Future<B> map(Function<? super A, ? extends B> function);

  /**
   * Returns a new Future whose result value is the product of applying the function which is the result of the given
   * Future to the result of this. If this Future is a failure, returned Future fails with the same exception. If this
   * Future is successful and argument Future is a failure, returned future fails with argument's exception.
   * @param future <b>Future&lt;A -&gt; B&gt;</b> transformation inside the Future. Can't be null.
   * @param <B> new result type.
   * @return transformed Future. Can't be null.
   * @throws NullPointerException if the argument was null.
   */
  @Nonnull <B> Future<B> apply(Future<? extends Function<? super A, ? extends B>> future);

  /**
   * Returns a new Future which is the product of applying the given function to this Future's result. If this Future is
   * a failure, returned Future fails with the same exception. If this Future is successful and the Future returned by
   * the function is a failure, returned future fails with its exception.
   * @param function <b>A -&gt; Future&lt;B&gt;</b> transformation. Can't be null.
   * @param <B> new result type.
   * @return transformed Future. Can't be null.
   * @throws NullPointerException if the argument was null.
   */
  @Nonnull <B> Future<B> bind(Function<? super A, ? extends Future<B>> function);

  /**
   * Returns a new Future whose result value remains the same, however it becomes complete only after the given {@link Procedure}
   * is executed. If this Future is a failure, returned Future fails with the same exception. If side-effect throws an
   * exception, it becomes returned Future's failure cause.
   * @param procedure side-effecting action to execute.
   * @return side-effecting Future. Can't be null.
   * @throws NullPointerException if the argument was null.
   */
  @Nonnull Future<A> then(Procedure<? super A> procedure);

  /**
   * Zips this Future with the given future using the given function. If this Future is a failure, returned Future fails
   * with the same exception. If this Future us successful and the Future returned by the function is a failure, returned
   * future fails with its exception.
   * @param future future to zip with. Can't be null.
   * @param function <b>A -&gt; B -&gt; C</b> transformation: combine both results into a new value. Can't be null.
   * @param <B> argument future's result type.
   * @param <C> new result type.
   * @return zipped Future. Can't be null.
   * @throws NullPointerException if the argument was null.
   */
  @Nonnull <B, C> Future<C> zip(Future<B> future, BiFunction<? super A, ? super B, ? extends C> function);

  /**
   * Filters this Future's result: if the predicate holds, the Future returned by this method remains the same,
   * if it does not, it fails with {@link java.util.NoSuchElementException}. If this Future is a failure to begin with,
   * predicate is not applied and the result holds the same exception.
   * @param predicate filtering function. Can't be null.
   * @return filtered Future. Can't be null.
   * @throws NullPointerException if the argument was null.
   */
  @Nonnull Future<A> filter(Predicate<? super A> predicate);

  /**
   * Recovers this Future: is this is a success, future with the same value is returned; if this is a failure, the given
   * function is applied and its returned value is the successful result of the future returned.
   * @param function recovery function: <b>Throwable -&gt; A</b>. Can't be null.
   * @return recovered Future. Can't be null.
   * @throws NullPointerException if the argument was null.
   */
  @Nonnull Future<A> recover(Function<? super Throwable, A> function);

  /**
   * Add a fallback mechanism to this Future: is this is a success, future with the same value is returned; if this is a
   * failure, the given function is applied and its returned future is the result.
   * @param function fallback function: <b>Throwable -&gt; Future&lt;A&gt;</b>. Can't be null.
   * @return new Future. Can't be null.
   * @throws NullPointerException if the argument was null.
   */
  @Nonnull Future<A> fallback(Function<? super Throwable, Future<A>> function);

  /**
   * Creates a new Future already complete with the given value as success.
   * @param value result to complete the future with.
   * @param <A> result type.
   * @return new Future. Can't be null.
   * @throws NullPointerException if the argument was null.
   */
  static @Nonnull <A> Future<A> successful(final A value) {
    return new PromisedFuture<>(nonNull(value));
  }

  /**
   * Creates a new Future already complete with the given exception as its failure cause.
   * @param cause exception to fail with.
   * @return new Future. Can't be null.
   * @throws NullPointerException if the argument was null.
   */
  static @Nonnull <A> Future<A> failed(final Throwable cause) {
    return new PromisedFuture<>(nonNull(cause));
  }

  /**
   * Flattens nested Futures without blocking. The value of the returned Future depends on the structure of the argument:
   * <ul>
   *   <li>outer was successful, inner was successful: inner success value</li>
   *   <li>outer was successful, inner was a failure: inner failure cause</li>
   *   <li>outer was a failure: outer failure cause</li>
   * </ul>
   * @param wrapped Future whose result value might be another Future.
   * @param <A> result type.
   * @return new Future. Can't be null.
   * @throws NullPointerException if the argument was null.
   */
  static @Nonnull <A> Future<A> unwrap(final Future<? extends Future<A>> wrapped) {
    nonNull(wrapped);
    return wrapped.bind(arg -> arg);
  }
}