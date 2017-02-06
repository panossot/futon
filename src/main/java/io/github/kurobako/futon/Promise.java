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
 * <p>Promise is a readable part of a {@link Future}-Promise pair similar to those in scala standard library representing
 * a computation which might be not completed yet and may result in a failure.</p>
 * <p>Promise might be completed with either a result value, in which case it is considered to be a success, or with a
 * {@link Throwable}, in which it is considered a failure. Subsequent attempts to complete this Promise will result
 * in an {@link IllegalStateException} being thrown. {@link #future()} method might be used to obtain associated
 * future and pass it to another thread. {@link #tryComplete(Future)} method might be used to chain futures with promises,
 *  however, it introduces nondeterminism in a program since attached futures will be racing to complete push their
 *  results into the promise.</p>
 *  <p>{@link #contraMap(Function)} makes Predicate a contravariant functor.</p>
 * @param <A> result type.
 */
public interface Promise<A> {
  /**
   * Returns a readable part of this future-promise pair.
   * @return associated Future. Can't be null.
   */
  @Nonnull Future<A> future();

  /**
   * Completes this Promise with a value.
   * @param value completion value. Can't be null.
   * @return associated Future. Can't be null.
   * @throws NullPointerException if the argument was null.
   * @throws IllegalStateException if already completed.
   */
  @Nonnull Future<A> success(final A value);

  /**
   * Completes this Promise with an exception.
   * @param cause failure cause. Can't be null.
   * @return associated Future. Can't be null.
   * @throws NullPointerException if the argument was null.
   * @throws IllegalStateException if already completed.
   */
  @Nonnull Future<A> failure(final Throwable cause);

  /**
   * Completes this Promise with an {@link Either} which might holds either a left failure cause or a right success value.
   * @param completion <b>Throwable | A</b>: either success or a failure. Can't be null.
   * @return associated Future. Can't be null.
   * @throws NullPointerException if the argument was null.
   * @throws IllegalStateException if already completed.
   */
  @Nonnull Future<A> complete(final Either<? extends Throwable, ? extends A> completion);

  /**
   * Returns a new Promise accepting values of type <b>B</b> which are first transformed using the given function and
   * then fed to this promise. Attempts to complete already completed promise will result in a usual {@link IllegalStateException}.
   * @param function <b>B -&gt; A</b> transformation. Can't be null.
   * @param <B> new promise result type.
   * @return new Promise. Can't be null.
   * @throws NullPointerException is the argument was null.
   */
  @Nonnull <B> Promise<B> contraMap(Function<? super B, ? extends A> function);

  /**
   * Attempts to complete this Promise with the given future's result, be it a success or failure, when it will be done.
   * Should be used with caution. This method does so by attaching a callback to the future which tries to push its value
   * to the promise and checks if the promise is already complete only at the time of calling. Thus, it is possible to
   * for futures to be attached to one promise and the first one to arrive will complete it while others' attempts to
   * complete will be silently ignored.
   * @param future {@link Future} to complete with.
   * @return associated Future. Can't be null.
   * @throws NullPointerException is the argument was null.
   * @throws IllegalStateException if already completed.
   */
  @Nonnull Future<A> tryComplete(Future<? extends A> future);

  /**
   * Creates a new Promise to be complete with either result value or a failure cause.
   * @return new Promise. Can't be null.
   */
  static @Nonnull <A> Promise<A> promise() {
    return new PromisedFuture<>();
  }
}
