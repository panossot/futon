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
 * <p>Reader monad is a computation that reads a value <b>A</b> from an environment <b>E</b> that can be transformed and
 * executed in a modified environment.</p>
 * <p>It is dual to {@link Environment} comonad. It might be seen as a less powerful {@link State} monad which, on the
 * other hand, might be better at expressing programmer's intent when the computation does not change the environment.</p>
 * <p>{@link #map(Function)} makes Reader a functor.</p>
 * <p>{@link #apply(Reader)} and {@link #reader(A)} form an applicative functor.</p>
 * <p>{@link #bind(Function)} and {@link #reader(A)} form a monad.</p>
 * @see <a href="http://web.cecs.pdx.edu/~mpj/pubs/springschool95.pdf">http://web.cecs.pdx.edu/~mpj/pubs/springschool95.pdf</a>
 * @param <E> environment type.
 * @param <A> result type.
 */
public interface Reader<E, A> {
  /**
   * Runs the computation, reading a value of type <b>A</b> from the given environment <b>E</b>.
   * @param environment an environment to read from. Can't be null.
   * @return computation result. Can't be null.
   */
  @Nonnull A run(E environment);

  /**
   * Returns a new Reader which runs the computation in an environment modified by the given function.
   * @param function <b>F -&gt; E</b> environment transformation.
   * @param <F> new environment type.
   * @return transformed Reader. Can't be null.
   * @throws NullPointerException if the argument was null.
   */
  default @Nonnull <F> Reader<F, A> local(final Function<? super F, ? extends E> function) {
    nonNull(function);
    return function.postcompose(this::run)::$;
  }

  /**
   * Returns a new Reader which runs this computation in a given environment, ignoring the one passed to it.
   * @param environment an environment to run this computation in.
   * @return transformed Reader. Can't be null.
   * @throws NullPointerException if the argument was null.
   */
  default @Nonnull Reader<E, A> scope(final E environment) {
    return ignoredArg -> run(environment);
  }

  /**
   * Returns a new Reader whose result value is the product of applying the given function to this Reader's result value.
   * @param function <b>A -&gt; B</b> transformation. Can't be null.
   * @param <B> new result type.
   * @return new Reader. Can't be null.
   * @throws NullPointerException if the argument was null.
   */
  default @Nonnull <B> Reader<E, B> map(final Function<? super A, ? extends B> function) {
    nonNull(function);
    final Function<E, A> run = this::run;
    return run.postcompose(function)::$;
  }

  /**
   * Returns a new Reader whose result value is the product of applying the function produced by the given Reader to
   * this result value.
   * @param reader <b>Reader&lt;S, A -&gt; B&gt</b>: transformation inside the Reader. Can't be null.
   * @param <B> new result type.
   * @return new Reader. Can't be null.
   * @throws NullPointerException if the argument was null.
   */
  default @Nonnull <B> Reader<E, B> apply(final Reader<E, ? extends Function<? super A, ? extends B>> reader) {
    nonNull(reader);
    return e -> reader.run(e).$(run(e));
  }

  /**
   * Returns a new Reader which is the product of applying the given function to this Reader's return value.
   * @param function <b>A -&gt; Reader&lt;S, B&gt;</b> transformation. Can't be null.
   * @param <B> new result type.
   * @return new Reader. Can't be null.
   * @throws NullPointerException if the argument was null.
   */
  default @Nonnull <B> Reader<E, B> bind(final Function<? super A, ? extends Reader<E, B>> function) {
    nonNull(function);
    return e -> function.$(run(e)).run(e);
  }

  /**
   * Creates a Reader which passes the environment is was provided with as its result without change.
   * @param <E> environment type.
   * @return new Reader. Can't be null.
   */
  static @Nonnull <E> Reader<E, E> ask() {
    return e -> e;
  }

  /**
   * Wraps the given function into Reader instance.
   * @param function <b>E -&gt; A</b> function to wrap. Can't be null.
   * @param <E> environment type.
   * @param <A> value type.
   * @return new Reader. Can't be null.
   * @throws NullPointerException if the argument was null.
   */
  static @Nonnull <E, A> Reader<E, A> reader(final Function<? super E, ? extends A> function) {
    nonNull(function);
    return function::$;
  }

  /**
   * Flattens nested Readers, first running the outer computation, then the inner one.
   * @param reader Reader returning another Reader as its result value.
   * @param <E> environment type.
   * @param <A> result value type.
   * @return new Reader. Can't be null.
   * @throws NullPointerException if the argument was null.
   */
  static @Nonnull <E, A> Reader<E, A> unwrap(final Reader<E, ? extends Reader<E, A>> reader) {
    return nonNull(reader).bind(arg -> arg);
  }

  /**
   * Creates a new Reader which always returns the given value no matter what environment was provided.
   * @param value to return. Can't be null.
   * @param <A> return value type.
   * @return new Reader. Can't be null.
   * @throws NullPointerException if the argument was null.
   */
  static @Nonnull <E, A> Reader<E, A> reader(final A value) {
    return ignoredArg -> value;
  }
}