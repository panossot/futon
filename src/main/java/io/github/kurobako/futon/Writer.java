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
import static io.github.kurobako.futon.Sequence.sequence;
import static io.github.kurobako.futon.Util.nonNull;

/**
 * <p>Writer monad may execute computations while also maintaining a {@link Sequence} of output values recorded per action.</p>
 * <p>The sequence can be retrieved and transformed as well as the computation result itself. Writer may be used to
 * explicitly model an chain of actions which also maintain a log.</p>
 * <p>Writer might be seen as a less powerful {@link State} monad which, on the
 * other hand, might be better at expressing programmer's intent when the computation does not depend on the input.</p>
 * <p>{@link #map(Function)} makes Writer a functor.</p>
 * <p>{@link #apply(Writer)} and {@link #writer(A)} form an applicative functor.</p>
 * <p>{@link #bind(Function)} and {@link #writer(A)} form a monad.</p>
 * @see <a href="http://web.cecs.pdx.edu/~mpj/pubs/springschool95.pdf">http://web.cecs.pdx.edu/~mpj/pubs/springschool95.pdf</a>
 * @param <O> output type.
 * @param <A> result type.
 */
public interface Writer<O, A> {
  /**
   * Runs the computation, providing a {@link Pair} of output log and the result value.
   * @return output-result pair. Can't be null.
   */
  @Nonnull Pair<Sequence<O>, A> run();

  /**
   * Executes the action and returns the sequence of output values.
   * @return output sequence. Can't be null.
   */
  default @Nonnull Sequence<O> exec() {
    return run().first;
  }

  /**
   * Returns a Writer which allows its output to be accessed as a value.
   * @return new Writer. Can't be null.
   */
  default @Nonnull Writer<O, Pair<Sequence<O>, A>> listen() {
    final Pair<Sequence<O>, A> initial = run();
    final Pair<Sequence<O>, Pair<Sequence<O>, A>> result = pair(initial.first, initial);
    return () -> result;
  }

  /**
   * Version of {@link #listen()} which also modifies the output using the given function before providing access to it.
   * @param function <b>O -&gt; P</b> output transformation. Can't be null.
   * @param <P> new output type.
   * @return new Writer. Can't be null.
   * @throws NullPointerException if the argument was null.
   */
  default @Nonnull <P> Writer<O, Pair<Sequence<P>, A>> listens(final Function<? super O, ? extends P> function) {
    nonNull(function);
    final Pair<Sequence<O>, A> initial = run();
    return () -> pair(initial.first, pair(initial.first.map(function), initial.second));
  }

  /**
   * Returns a Writer which transforms the output of this Writer using the given function. Result value remains unchanged.
   * @param function <b>O -&gt; O</b> output transformation.
   * @return new Writer. Can't be null.
   * @throws NullPointerException if the argument was null.
   */
  default @Nonnull Writer<O, A> censor(final Function<? super O, ? extends O> function) {
    nonNull(function);
    final Pair<Sequence<O>, A> initial = run();
    return () -> pair(initial.first.map(function), initial.second);
  }

  /**
   * Returns a Writer whose result value is the product of applying the given function to this Writer's result value.
   * @param function <b>A -&gt; B</b> transformation. Can't be null.
   * @param <B> new result type.
   * @return new Writer. Can't be null.
   * @throws NullPointerException if the argument was null.
   */
  default @Nonnull <B> Writer<O, B> map(final Function<? super A, ? extends B> function) {
    nonNull(function);
    final Pair<Sequence<O>, A> initial = run();
    return () -> pair(initial.first, function.$(initial.second));
  }

  /**
   * Returns a Writer whose result value is the product of applying the function produced by the given Writer to the
   * result of this Writer.
   * @param writer <b>Writer&lt;O, A -&gt; B&gt:</b> transformation inside the State. Can't be null.
   * @param <B> new result type.
   * @return new Writer. Can't be null.
   * @throws NullPointerException if the argument was null.
   */
  default @Nonnull <B> Writer<O, B> apply(final Writer<O, ? extends Function<? super A, ? extends B>> writer) {
    nonNull(writer);
    final Pair<Sequence<O>, A> initial = run();
    final Pair<Sequence<O>, ? extends Function<? super A, ? extends B>> application = writer.run();
    return () -> pair(application.first.append(initial.first), application.second.$(initial.second));
  }

  /**
   * Returns a new Writer which is the product of applying the given function to this Writer's result value.
   * @param function <b>A -&gt; Writer&lt;O, B&gt;</b> transformation. Can't be null.
   * @param <B> new result type.
   * @return new Writer. Can't be null.
   * @throws NullPointerException if the argument was null.
   */
  default @Nonnull <B> Writer<O, B> bind(final Function<? super A, ? extends Writer<O, B>> function) {
    nonNull(function);
    final Pair<Sequence<O>, A> initial = run();
    final Pair<Sequence<O>, B> returned = function.$(initial.second).run();
    final Pair<Sequence<O>, B> result = pair(initial.first.append(returned.first), returned.second);
    return () -> result;
  }

  /**
   * Transforms a Writer which applies output transforming function to the output stored in the given Writer.
   * @param writer writer containing a {@link Pair} of a result value and an output transformation. Can't be null.
   * @param <O> output type.
   * @param <A> result type.
   * @return new Writer. Can't be null.
   * @throws NullPointerException if the argument was null.
   */
  static @Nonnull <O, A> Writer<O, A> pass(final Writer<O, Pair<A, ? extends Function<? super O, ? extends O>>> writer) {
    nonNull(writer);
    final Pair<Sequence<O>, Pair<A, ? extends Function<? super O, ? extends O>>> initial = writer.run();
    final Pair<Sequence<O>, A> result = pair(initial.first.map(initial.second.second), initial.second.first);
    return () -> result;
  }

  /**
   * Creates a Writer that produces the given output sequence.
   * @param output produced output. Can't be null.
   * @param <O> output type.
   * @return new Writer. Can't be null.
   * @throws NullPointerException if the argument was null.
   */
  static @Nonnull <O> Writer<O, Unit> tell(final Sequence<O> output) {
    return writer(output, Unit.INSTANCE);
  }

  /**
   * Creates a Writer which produces the given output entry.
   * The same as calling {@link #tell(Sequence)} with {@link Sequence#sequence(O))};
   * @param output a single entry of output. Can't be null.
   * @param <O> output type.
   * @return new Writer. Can't be null.
   * @throws NullPointerException if the argument was null.
   */
  static @Nonnull <O> Writer<O, Unit> tell(final O output) {
    return writer(output, Unit.INSTANCE);
  }

  /**
   * Creates a Writer that produces the given output sequence and the given value.
   * @param output produced output. Can't be null.
   * @param value produced value. Can't be null.
   * @param <O> output type.
   * @param <A> result type.
   * @return new Writer. Can't be null.
   * @throws NullPointerException if any argument was null.
   */
  static @Nonnull <O, A> Writer<O, A> writer(final Sequence<O> output, final A value) {
    final Pair<Sequence<O>, A> result = pair(nonNull(output), value);
    return () -> result;
  }

  /**
   * Creates a Writer that produces the given output entry and the given value.
   * The same as calling {@link #writer(Sequence, A)} with {@link Sequence#sequence(O))};
   * @param output produced output. Can't be null.
   * @param value produced value. Can't be null.
   * @param <O> output type.
   * @param <A> result type.
   * @return new Writer. Can't be null.
   * @throws NullPointerException if any argument was null.
   */
  static @Nonnull <O, A> Writer<O, A> writer(final O output, final A value) {
    return writer(sequence(output), value);
  }

  /**
   * Flattens nested Writers, returning a new Writer containing the innermost computation.
   * @param writer Writer containing another Writer as its result value.
   * @param <O> output type.
   * @param <A> inner result value type.
   * @return new Writer. Can't be null.
   * @throws NullPointerException if the argument was null.
   */
  static @Nonnull <O, A> Writer<O, A> unwrap(final Writer<O, ? extends Writer<O, A>> writer) {
    return nonNull(writer).bind(arg -> arg);
  }

  /**
   * Creates a Writer that produces an empty output and the given value.
   * The same as calling {@link #writer(Sequence, A)} with {@link Sequence#sequence())};
   * @param value produced value. Can't be null.
   * @param <A> result type.
   * @return new Writer. Can't be null.
   * @throws NullPointerException if the argument was null.
   */
  static @Nonnull <O, A> Writer<O, A> writer(final A value) {
    return writer(sequence(), value);
  }
}
