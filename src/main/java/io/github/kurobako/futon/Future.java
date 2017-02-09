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
import javax.annotation.concurrent.Immutable;

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
 * <p>{@link #bind(Kleisli)} and {@link #successful(A)} form a monad.</p>
 * @see Promise
 * @see <a href="http://www.scala-lang.org/api/2.12.1/scala/concurrent/Future.html">http://www.scala-lang.org/api/2.12.1/scala/concurrent/Future.html</a>
 * @param <A> result type.
 */
@Immutable
public interface Future<A> {
  /**
   * Returns a new Future whose result value is the product of applying the given function to this Future's result value.
   * If this Future is a failure, returned Future fails with the same exception. RuntimeException thrown by map won't be
   * caught in a Future and will be thrown in a thread performing the map. If you want a version of map which can throw
   * any exceptions and/or execute side-effects, check {@link #then(Procedure)};
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
   * @param kleisli <b>A -&gt; Future&lt;B&gt;</b> transformation. Can't be null.
   * @param <B> new result type.
   * @return transformed Future. Can't be null.
   * @throws NullPointerException if the argument was null.
   */
  @Nonnull <B> Future<B> bind(Kleisli<? super A, B> kleisli);

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
  @Nonnull Future<A> fallback(Kleisli<? super Throwable, A> function);

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

  /**
   * <p>Kleisli arrow is a pure function from an argument of type <b>A</b> to <b>Future&lt;B&gt;</b>. </p>
   * <p>It can be combined with other arrows of the same type (but parameterized differently) in ways similar to how
   * {@link Function}s can be combined with other functions.</p>
   * @param <A> argument type.
   * @param <B> return type parameter.
   */
  @FunctionalInterface
  interface Kleisli<A, B> {
    /**
     * Run the computation, producing a monad.
     * @param arg computation argument. Can't be null.
     * @return new monad. Can't be null.
     */
    @Nonnull Future<B> run(A arg);

    /**
     * Returns an arrow combining this arrow with the given arrow: <b>Z -&gt; A -&gt; B</b>.
     * @param kleisli <b>Z -&gt; A</b> arrow. Can't be null.
     * @param <Z> argument type for the new arrow.
     * @return new <b>Z -&gt; A</b> arrow. Can't be null.
     * @throws NullPointerException if the argument was null.
     */
    default @Nonnull <Z> Kleisli<Z, B> precomposeKleisli(final Kleisli<? super Z, A> kleisli) {
      nonNull(kleisli);
      return z -> kleisli.run(z).bind(this);
    }

    /**
     * Returns an arrow combining this arrow with the given pure function: <b>Z -&gt; A -&gt; B</b>.
     * @param function <b>Z -&gt; A</b> function. Can't be null.
     * @param <Z> argument type for the new arrow.
     * @return new <b>Z -&gt; A</b> arrow. Can't be null.
     * @throws NullPointerException if the argument was null.
     */
    default @Nonnull <Z> Kleisli<Z, B> precomposeFunction(final Function<? super Z, ? extends A> function) {
      nonNull(function);
      return z -> run(function.$(z));
    }

    /**
     * Returns an arrow combining this arrow with the given arrow: <b>A -&gt; B -&gt; C</b>.
     * @param kleisli <b>B -&gt; C</b> arrow. Can't be null.
     * @param <C> return type for the new arrow.
     * @return new <b>A -&gt; C</b> arrow. Can't be null.
     * @throws NullPointerException if the argument was null.
     */
    default @Nonnull <C> Kleisli<A, C> postcomposeKleisli(final Kleisli<? super B, C> kleisli) {
      nonNull(kleisli);
      return a -> run(a).bind(kleisli);
    }

    /**
     * Returns an arrow combining this arrow with the given pure function: <b>A -&gt; B -&gt; C</b>.
     * @param function <b>B -&gt; C</b> function. Can't be null.
     * @param <C> return type for the new arrow.
     * @return new <b>A -&gt; C</b> arrow. Can't be null.
     * @throws NullPointerException if the argument was null.
     */
    default @Nonnull <C> Kleisli<A, C> postcomposeFunction(final Function<? super B, ? extends C> function) {
      nonNull(function);
      return a -> run(a).map(function);
    }

    /**
     * Returns an arrow which maps its input using this arrow of it is {@link Either.Left} and passes it
     * unchanged otherwise.
     * @param <C> right component type.
     * @return new arrow. Can't be null.
     */
    default @Nonnull <C> Kleisli<Either<A, C>, Either<B, C>> left() {
      return ac -> ac.either(a -> run(a).map(Either::left), c -> successful(Either.right(c)));
    }

    /**
     * Returns an arrow which maps its input using this arrow of it is {@link Either.Right} and passes it
     * unchanged otherwise.
     * @param <C> left component type.
     * @return new arrow. Can't be null.
     */
    default @Nonnull <C> Kleisli<Either<C, A>, Either<C, B>> right() {
      return ca -> ca.either(c -> successful(Either.left(c)), a -> run(a).map(Either::right));
    }

    /**
     * Returns an arrow which maps first part of its input and passes the second part unchanged.
     * @param <C> right component type.
     * @return new arrow. Can't be null.
     */
    default @Nonnull <C> Kleisli<Pair<A, C>, Pair<B, C>> first() {
      return ac -> run(ac.first).zip(successful(ac.second), Pair::pair);
    }

    /**
     * Returns an arrow which maps second part of its input and passes the first part unchanged.
     * @param <C> left component type.
     * @return new arrow. Can't be null.
     */
    default @Nonnull <C> Kleisli<Pair<C, A>, Pair<C, B>> second() {
      return ca -> successful(ca.first).zip(run(ca.second), Pair::pair);
    }

    /**
     * Returns an arrow which maps its input using this arrow if it is {@link Either.Left} and using the given arrow if
     * it is {@link Either.Right}.
     * @param kleisli right <b>C -&gt; D</b> mapping. Can't be null.
     * @param <C> right argument type.
     * @param <D> right return type.
     * @return new arrow. Can't be null.
     * @throws NullPointerException if the argument is null.
     */
    default @Nonnull <C, D> Kleisli<Either<A, C>, Either<B, D>> sum(final Kleisli<? super C, D> kleisli) {
      nonNull(kleisli);
      return ac -> ac.either(a -> run(a).map(Either::left), c -> kleisli.run(c).map(Either::right));
    }

    /**
     * Returns an arrow which maps the first part of its input using this arrow and the second part using the given arrow.
     * @param kleisli second <b>C -&gt; D</b> mapping. Can't be null.
     * @param <C> second argument type.
     * @param <D> second return type.
     * @return new arrow. Can't be null.
     * @throws NullPointerException if the argument is null.
     */
    default @Nonnull <C, D> Kleisli<Pair<A, C>, Pair<B, D>> product(final Kleisli<? super C, D> kleisli) {
      nonNull(kleisli);
      return ac -> run(ac.first).zip(kleisli.run(ac.second), Pair::pair);
    }

    /**
     * Returns an arrow which maps input using this arrow if it is {@link Either.Left} or the given arrow if it is {@link Either.Right}.
     * @param kleisli left <b>C -&gt; B</b> mapping. Can't be null.
     * @param <C> right argument type.
     * @return new arrow. Can't be null.
     * @throws NullPointerException if the argument is null.
     */
    default @Nonnull <C> Kleisli<Either<A, C>, B> fanIn(final Kleisli<? super C, B> kleisli) {
      nonNull(kleisli);
      return ac -> ac.either(this::run, kleisli::run);
    }

    /**
     * Returns an arrow which maps its input using this arrow and the given arrow and returns two resulting values as a pair.
     * @param kleisli second <b>A -&gt; C</b> mapping. Can't be null.
     * @param <C> second return type.
     * @return new arrow. Can't be null.
     * @throws NullPointerException if the argument is null.
     */
    default @Nonnull <C> Kleisli<A, Pair<B, C>> fanOut(final Kleisli<? super A, C> kleisli) {
      nonNull(kleisli);
      return a -> run(a).zip(kleisli.run(a), Pair::pair);
    }

    /**
     * Returns an arrow wrapping the given function.
     * @param function <b>A -&gt; B</b> function to wrap. Can't be null.
     * @param <A> argument type.
     * @param <B> return type parameter.
     * @return an arrow. Can't be null.
     * @throws NullPointerException if the argument is null.
     */
    static @Nonnull <A, B> Kleisli<A, B> lift(final Function<? super A, ? extends B> function) {
      nonNull(function);
      return a -> successful(function.$(a));
    }

    /**
     * Returns an arrow <b>(A -&gt; B, B) -&gt; B</b> which applies its input arrow (<b>A -&gt; B</b>) to its input
     * value (<b>A</b>) and returns the result (<b>B</b>).
     * @param <A> argument type.
     * @param <B> return type.
     * @return an arrow. Can't be null.
     */
    static @Nonnull <A, B> Kleisli<Pair<Kleisli<A, B>, A>, B> apply() {
      return ka -> ka.first.run(ka.second);
    }
  }
}