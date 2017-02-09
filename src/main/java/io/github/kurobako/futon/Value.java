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
 * <p>{@link #bind(Kleisli)} and {@link #value(A)} form a monad.</p>
 * <p>{@link #extend(CoKleisli)} and {@link #get()} form a comonad.</p>
 * @param <A> result type.
 */
@FunctionalInterface
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
   * @param kleisli <b>A -&gt; Value&lt;B&gt;</b> transformation. Can't be null.
   * @param <B> new result type.
   * @return transformed Value. Can't be null.
   * @throws NullPointerException if the argument was null.
   */
  default @Nonnull <B> Value<B> bind(final Kleisli<? super A, B> kleisli) {
    return nonNull(kleisli).run(get());
  }

  /**
   * Returns a Value with its result evaluated by application of the given function to this Value.
   * @param coKleisli <b>Value&lt;A&gt; -&gt; B</b> transformation. Can't be null.
   * @param <B> new result type.
   * @return new Value. Can't be null.
   * @throws NullPointerException if the argument was null.
   */
  default @Nonnull <B> Value<B> extend(final CoKleisli<A, ? extends B> coKleisli) {
    final B b = nonNull(coKleisli).run(this);
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

  /**
   * <p>Kleisli arrow is a pure function from an argument of type <b>A</b> to <b>Value&lt;B&gt;</b>. </p>
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
    @Nonnull Value<B> run(A arg);

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
      return ac -> ac.either(a -> run(a).map(Either::left), c -> value(Either.right(c)));
    }

    /**
     * Returns an arrow which maps its input using this arrow of it is {@link Either.Right} and passes it
     * unchanged otherwise.
     * @param <C> left component type.
     * @return new arrow. Can't be null.
     */
    default @Nonnull <C> Kleisli<Either<C, A>, Either<C, B>> right() {
      return ca -> ca.either(c -> value(Either.left(c)), a -> run(a).map(Either::right));
    }

    /**
     * Returns an arrow which maps first part of its input and passes the second part unchanged.
     * @param <C> right component type.
     * @return new arrow. Can't be null.
     */
    default @Nonnull <C> Kleisli<Pair<A, C>, Pair<B, C>> first() {
      return ac -> run(ac.first).zip(value(ac.second), Pair::pair);
    }

    /**
     * Returns an arrow which maps second part of its input and passes the first part unchanged.
     * @param <C> left component type.
     * @return new arrow. Can't be null.
     */
    default @Nonnull <C> Kleisli<Pair<C, A>, Pair<C, B>> second() {
      return ca -> value(ca.first).zip(run(ca.second), Pair::pair);
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
      return a -> value(function.$(a));
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

  /**
   * <p>CoKleisli arrow is a pure function from an argument of type <b>Value&lt;A&gt;</b> to <b>B</b>. </p>
   * <p>It can be combined with other arrows of the same type (but parameterized differently) in ways similar to how
   * {@link Function}s can be combined with other functions.</p>
   * @param <A> argument type parameter.
   * @param <B> return type.
   */
  @FunctionalInterface
  interface CoKleisli<A, B> {
    /**
     * Run the computation, producing a result.
     * @param arg argument comonad. Can't be null.
     * @return computation result. Can't be null.
     */
    @Nonnull B run(Value<A> arg);

    /**
     * Returns an arrow combining this arrow with the given arrow: <b>Z -&gt; A -&gt; B</b>.
     * @param coKleisli <b>Z -&gt; A</b> arrow. Can't be null.
     * @param <Z> argument type for the new arrow.
     * @return new <b>Z -&gt; A</b> arrow. Can't be null.
     * @throws NullPointerException if the argument was null.
     */
    default @Nonnull <Z> CoKleisli<Z, B> precomposeCoKleisli(final CoKleisli<Z, ? extends A> coKleisli) {
      nonNull(coKleisli);
      return z -> run(z.extend(coKleisli));
    }

    /**
     * Returns an arrow combining this arrow with the given pure function: <b>Z -&gt; A -&gt; B</b>.
     * @param function <b>Z -&gt; A</b> function. Can't be null.
     * @param <Z> argument type for the new arrow.
     * @return new <b>Z -&gt; A</b> arrow. Can't be null.
     * @throws NullPointerException if the argument was null.
     */
    default @Nonnull <Z> CoKleisli<Z, B> precomposeFunction(final Function<? super Z, ? extends A> function) {
      nonNull(function);
      return z -> run(z.map(function));
    }

    /**
     * Returns an arrow combining this arrow with the given arrow: <b>A -&gt; B -&gt; C</b>.
     * @param coKleisli <b>B -&gt; C</b> arrow. Can't be null.
     * @param <C> return type for the new arrow.
     * @return new <b>A -&gt; C</b> arrow. Can't be null.
     * @throws NullPointerException if the argument was null.
     */
    default @Nonnull <C> CoKleisli<A, C> postcomposeCoKleisli(final CoKleisli<B, ? extends C> coKleisli) {
      nonNull(coKleisli);
      return a -> coKleisli.run(a.extend(this));
    }

    /**
     * Returns an arrow combining this arrow with the given pure function: <b>A -&gt; B -&gt; C</b>.
     * @param function <b>B -&gt; C</b> function. Can't be null.
     * @param <C> return type for the new arrow.
     * @return new <b>A -&gt; C</b> arrow. Can't be null.
     * @throws NullPointerException if the argument was null.
     */
    default @Nonnull <C> CoKleisli<A, C> postcomposeFunction(final Function<? super B, ? extends C> function) {
      nonNull(function);
      return a -> function.$(run(a));
    }

    /**
     * Returns an arrow which maps first part of its input and passes the second part unchanged.
     * @param <C> right component type.
     * @return new arrow. Can't be null.
     */
    default @Nonnull <C> CoKleisli<Pair<A, C>, Pair<B, C>> first() {
      return ac -> ac.unzip(arg -> arg).biMap(this::run, Value::get);
    }

    /**
     * Returns an arrow which maps second part of its input and passes the first part unchanged.
     * @param <C> left component type.
     * @return new arrow. Can't be null.
     */
    default @Nonnull <C> CoKleisli<Pair<C, A>, Pair<C, B>> second() {
      return ca -> ca.unzip(arg -> arg).biMap(Value::get, this::run);
    }

    /**
     * Returns an arrow which maps the first part of its input using this arrow and the second part using the given arrow.
     * @param coKleisli second <b>C -&gt; D</b> mapping. Can't be null.
     * @param <C> second argument type.
     * @param <D> second return type.
     * @return new arrow. Can't be null.
     * @throws NullPointerException if the argument is null.
     */
    default @Nonnull <C, D> CoKleisli<Pair<A, C>, Pair<B, D>> product(final CoKleisli<C, ? extends D> coKleisli) {
      nonNull(coKleisli);
      return ac -> ac.unzip(arg -> arg).biMap(this::run, coKleisli::run);
    }

    /**
     * Returns an arrow which maps its input using this arrow and the given arrow and returns two resulting values as a pair.
     * @param coKleisli second <b>A -&gt; C</b> mapping. Can't be null.
     * @param <C> second return type.
     * @return new arrow. Can't be null.
     * @throws NullPointerException if the argument is null.
     */
    default @Nonnull <C> CoKleisli<A, Pair<B, C>> fanOut(final CoKleisli<A, ? extends C> coKleisli) {
      nonNull(coKleisli);
      return a -> pair(run(a), coKleisli.run(a));
    }

    /**
     * Returns an arrow wrapping the given function.
     * @param function <b>A -&gt; B</b> function to wrap. Can't be null.
     * @param <A> argument type.
     * @param <B> return type parameter.
     * @return an arrow. Can't be null.
     * @throws NullPointerException if the argument is null.
     */
    static @Nonnull <A, B> CoKleisli<A, B> lift(final @Nonnull Function<? super A, ? extends B> function) {
      nonNull(function);
      return a -> function.$(a.get());
    }
  }
}