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
 * <p>{@link #bind(Kleisli)} and {@link #writer(A)} form a monad.</p>
 * @see <a href="http://web.cecs.pdx.edu/~mpj/pubs/springschool95.pdf">http://web.cecs.pdx.edu/~mpj/pubs/springschool95.pdf</a>
 * @param <O> output type.
 * @param <A> result type.
 */
@FunctionalInterface
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
   * @param kleisli <b>A -&gt; Writer&lt;O, B&gt;</b> transformation. Can't be null.
   * @param <B> new result type.
   * @return new Writer. Can't be null.
   * @throws NullPointerException if the argument was null.
   */
  default @Nonnull <B> Writer<O, B> bind(final Kleisli<? super A, B, O> kleisli) {
    nonNull(kleisli);
    final Pair<Sequence<O>, A> initial = run();
    final Pair<Sequence<O>, B> returned = kleisli.run(initial.second).run();
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
    return writer(sequence(output), Unit.INSTANCE);
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

  /**
   * <p>Kleisli arrow is a pure function from an argument of type <b>A</b> to <b>Writer&lt;O, B&gt;</b>. </p>
   * <p>It can be combined with other arrows of the same type (but parameterized differently) in ways similar to how
   * {@link Function}s can be combined with other functions.</p>
   * @param <A> argument type.
   * @param <B> return type parameter.
   */
  @FunctionalInterface
  interface Kleisli<A, B, O> {
    /**
     * Run the computation, producing a monad.
     * @param arg computation argument. Can't be null.
     * @return new monad. Can't be null.
     */
    @Nonnull Writer<O, B> run(A arg);

    /**
     * Returns an arrow combining this arrow with the given arrow: <b>Z -&gt; A -&gt; B</b>.
     * @param kleisli <b>Z -&gt; A</b> arrow. Can't be null.
     * @param <Z> argument type for the new arrow.
     * @return new <b>Z -&gt; A</b> arrow. Can't be null.
     * @throws NullPointerException if the argument was null.
     */
    default @Nonnull <Z> Kleisli<Z, B, O> precomposeKleisli(final Kleisli<? super Z, A, O> kleisli) {
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
    default @Nonnull <Z> Kleisli<Z, B, O> precomposeFunction(final Function<? super Z, ? extends A> function) {
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
    default @Nonnull <C> Kleisli<A, C, O> postcomposeKleisli(final Kleisli<? super B, C, O> kleisli) {
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
    default @Nonnull <C> Kleisli<A, C, O> postcomposeFunction(final Function<? super B, ? extends C> function) {
      nonNull(function);
      return a -> run(a).map(function);
    }

    /**
     * Returns an arrow which maps its input using this arrow of it is {@link Either.Left} and passes it
     * unchanged otherwise.
     * @param <C> right component type.
     * @return new arrow. Can't be null.
     */
    default @Nonnull <C> Kleisli<Either<A, C>, Either<B, C>, O> left() {
      return ac -> ac.either(a -> run(a).map(Either::left), c -> writer(Either.right(c)));
    }

    /**
     * Returns an arrow which maps its input using this arrow of it is {@link Either.Right} and passes it
     * unchanged otherwise.
     * @param <C> left component type.
     * @return new arrow. Can't be null.
     */
    default @Nonnull <C> Kleisli<Either<C, A>, Either<C, B>, O> right() {
      return ca -> ca.either(c -> writer(Either.left(c)), a -> run(a).map(Either::right));
    }

    /**
     * Returns an arrow which maps first part of its input and passes the second part unchanged.
     * @param <C> right component type.
     * @return new arrow. Can't be null.
     */
    default @Nonnull <C> Kleisli<Pair<A, C>, Pair<B, C>, O> first() {
      return ac -> {
        final Pair<Sequence<O>, B> ob = Kleisli.this.run(ac.first).run();
        return writer(ob.first, pair(ob.second, ac.second));
      };
    }

    /**
     * Returns an arrow which maps second part of its input and passes the first part unchanged.
     * @param <C> left component type.
     * @return new arrow. Can't be null.
     */
    default @Nonnull <C> Kleisli<Pair<C, A>, Pair<C, B>, O> second() {
      return ca -> {
        final Pair<Sequence<O>, B> ob = Kleisli.this.run(ca.second).run();
        return writer(ob.first, pair(ca.first, ob.second));
      };
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
    default @Nonnull <C, D> Kleisli<Either<A, C>, Either<B, D>, O> sum(final Kleisli<? super C, D, O> kleisli) {
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
    default @Nonnull <C, D> Kleisli<Pair<A, C>, Pair<B, D>, O> product(final Kleisli<? super C, D, O> kleisli) {
      nonNull(kleisli);
      return ac -> {
        final Pair<Sequence<O>, B> ob = Kleisli.this.run(ac.first).run();
        final Pair<Sequence<O>, D> od = kleisli.run(ac.second).run();
        return writer(ob.first.append(od.first), pair(ob.second, od.second));
      };
    }

    /**
     * Returns an arrow which maps input using this arrow if it is {@link Either.Left} or the given arrow if it is {@link Either.Right}.
     * @param kleisli left <b>C -&gt; B</b> mapping. Can't be null.
     * @param <C> right argument type.
     * @return new arrow. Can't be null.
     * @throws NullPointerException if the argument is null.
     */
    default @Nonnull <C> Kleisli<Either<A, C>, B, O> fanIn(final Kleisli<? super C, B, O> kleisli) {
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
    default @Nonnull <C> Kleisli<A, Pair<B, C>, O> fanOut(final Kleisli<? super A, C, O> kleisli) {
      nonNull(kleisli);
      return a -> {
        final Pair<Sequence<O>, B> ob = Kleisli.this.run(a).run();
        final Pair<Sequence<O>, C> oc = kleisli.run(a).run();
        return writer(ob.first.append(oc.first), pair(ob.second, oc.second));
      };
    }

    /**
     * Returns an arrow wrapping the given function.
     * @param function <b>A -&gt; B</b> function to wrap. Can't be null.
     * @param <A> argument type.
     * @param <B> return type parameter.
     * @return an arrow. Can't be null.
     * @throws NullPointerException if the argument is null.
     */
    static @Nonnull <A, B, O> Kleisli<A, B, O> lift(final Function<? super A, ? extends B> function) {
      nonNull(function);
      return a -> writer(function.$(a));
    }

    /**
     * Returns an arrow <b>(A -&gt; B, B) -&gt; B</b> which applies its input arrow (<b>A -&gt; B</b>) to its input
     * value (<b>A</b>) and returns the result (<b>B</b>).
     * @param <A> argument type.
     * @param <B> return type.
     * @return an arrow. Can't be null.
     */
    static @Nonnull <A, B, O> Kleisli<Pair<Kleisli<A, B, O>, A>, B, O> apply() {
      return ka -> ka.first.run(ka.second);
    }
  }
}
