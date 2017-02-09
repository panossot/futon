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
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.io.Serializable;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;

import static io.github.kurobako.futon.Pair.pair;
import static io.github.kurobako.futon.Sequence.sequence;
import static io.github.kurobako.futon.Util.nonNull;

/**
 * <p>Option is a container for a value which might be not present and can be used as a safe replacement for nullable
 * values.</p>
 * <p>Unlike with Java 8 {@link java.util.Optional}, users are equipped with tools to go with this result for as long as they
 * want: the value inside the Option may be filtered, transformed in several ways inclusing the power of monadic bind,
 * zipped with other values etc. In case the information about failure causing the value to be absent needs to be
 * preserved, consider using {@link Either}<b>&lt;Exception, A&gt;</b>.</p>
 * <p>{@link #map(Function)} makes Value a functor.</p>
 * <p>{@link #apply(Option)} and {@link #some(A)} form an applicative functor.</p>
 * <p>{@link #bind(Kleisli)} and {@link #some(A)} form a monad.</p>
 * <p>This class is not supposed to be extended by users.</p>
 * @see <a href="http://hackage.haskell.org/packages/archive/base/latest/doc/html/Data-Maybe.html">http://hackage.haskell.org/packages/archive/base/latest/doc/html/Data-Maybe.html</a>
 * @param <A> value type.
 */
@Immutable
public abstract class Option<A> implements Foldable<A>, Iterable<A>, Serializable {
  private static final long serialVersionUID = 1L;

  Option() {}

  /**
   * Compatibility method: returns this Option's value as a nullable.
   * @return value if this Option is not absent, null otherwise.
   */
  public abstract @Nullable A asNullable();

  /**
   * Case analysis: apply first function to the value is this Option is {@link Some} or return {@link Value} provided if
   * this Option is {@link None}.
   * @param ifSome function to apply if some. Can't be null.
   * @param ifNone value to return if none. Can't be null.
   * @param <X> result type.
   * @return case analysis result. Can't be null.
   * @throws NullPointerException if any argument was null.
   */
  public abstract @Nonnull <X> X option(Function<? super A, ? extends X> ifSome, Value<X> ifNone);

  /**
   * Returns an Option whose value is the product of applying the given function to this option value.
   * If this is {@link None} no transformation happens.
   * @param function <b>A -&gt; B</b> transformation. Can't be null.
   * @param <B> new value type.
   * @return transformed Option. Can't be null.
   * @throws NullPointerException if the argument was null.
   */
  public abstract @Nonnull <B> Option<B> map(Function<? super A, ? extends B> function);

  /**
   * Returns an Option whose value is the product of applying the function stored in the given Option to this value.
   * If this or the argument Option is {@link None} no transformation happens.
   * @param option <b>(A -&gt; B)?</b>: transformation inside the Option. Can't be null.
   * @param <B> new value type.
   * @return transformed Option. Can't be null.
   * @throws NullPointerException if the argument was null.
   */
  public abstract @Nonnull <B> Option<B> apply(Option<? extends Function<? super A, ? extends B>> option);

  /**
   * Returns an Option which is the product of applying the given function to this Option's value. If this or the Option
   * returned by the argument function is {@link None} no transformation happens.
   * @param kleisli <b>A -&gt; B?</b> transformation. Can't be null.
   * @param <B> new value type.
   * @return transformed Option. Can't be null.
   * @throws NullPointerException if the argument was null.
   */
  public abstract @Nonnull <B> Option<B> bind(Kleisli<? super A, B> kleisli);


  /**
   * Zips this with the given Option using the given function. Transformation happens only if both this and the argument
   * Option are {@link Some}.
   * @param option an Option to zip with.
   * @param function <b>A -&gt; B -&gt; C</b> transformation. Can't be null.
   * @param <B> value type of the Option to zip with.
   * @param <C> new value type.
   * @return an Option zipped with the given Option. Can't be null.
   * @throws NullPointerException if any argument was null.
   */
  public abstract @Nonnull <B, C> Option<C> zip(Option<B> option, BiFunction<? super A, ? super B, ? extends C> function);

  /**
   * Unzips this into a {@link Pair} of Options using the given function. If this is {@link None}, a pair of nones is returned.
   * @param function <b>A -&gt; (B, C)</b> transformation. Can't be null.
   * @param <B> value type of the first result.
   * @param <C> value type of the second result.
   * @return a Pair of unzipped Options.
   * @throws NullPointerException if the argument was null.
   */
  public abstract @Nonnull <B, C> Pair<? extends Option<B>, ? extends Option<C>> unzip(Function<? super A, Pair<B, C>> function);

  /**
   * Filters this Option's result: if the predicate holds, the Option returned by this method remains the same,
   * if it does not, it becomes {@link None}. If this Option was None, predicate is not applied and None is returned.
   * @param predicate filtering function. Can't be null.
   * @return filtered Option. Can't be null.
   * @throws NullPointerException if the argument was null.
   */
  public abstract @Nonnull Option<A> filter(Predicate<? super A> predicate);

  /**
   * Transforms this Option's value using the given function, wrapping returned {@link Sequence} of values into
   * a sequence of Options. If this Option is {@link None}, a sequence of size 1 containing this sole element is returned.
   * @param function traversal function: <b>A -&gt; [B]</b>. Can't be null.
   * @param <B> new value type.
   * @return a sequence of values wrapped in Option. Can't be null.
   * @throws NullPointerException if the argument was null.
   */
  public abstract @Nonnull <B> Sequence<Option<B>> traverse(final Function<? super A, Sequence<B>> function);

  /**
   * Poor man's pattern matching: reduce this Option to <b>X</b> using first function if it is {@link Some} or reduce
   * it using second function if it is {@link None}.
   * @param some a function to apply is this is Some. Can't be null.
   * @param none a function to apply if this is None. Can't be null.
   * @param <X> reduced value type.
   * @return reduced value. Can't be null.
   * @throws NullPointerException if the argument was null.
   */
  public abstract @Nonnull <X> X match(Function<? super Some<A>, ? extends X> some, Function<? super None<A>, ? extends X> none);

  /**
   * Checks is this is {@link Some}.
   * @return true if Some, false is None.
   */
  public abstract boolean isSome();

  /**
   * Checks is this is {@link None}.
   * @return true is None, false otherwise.
   */
  public abstract boolean isNone();

  /**
   * Creates a new {@link Some} containing the given value.
   * @param value value to contain. Can't be null.
   * @param <A> value type.
   * @return new Some. Can't be null.
   * @throws NullPointerException if the argument was null.
   */
  public static @Nonnull <A> Some<A> some(final A value) {
    return new Some<>(nonNull(value));
  }

  /**
   * Returns a {@link None} instance.
   * @return a None. Can't be null.
   */
  @SuppressWarnings("unchecked")
  public static @Nonnull <A> None<A> none() {
    return (None<A>) None.INSTANCE;
  }

  /**
   * Flattens nested Options. Some&lt;A&gt; is returned if both outer and inner options are some, None otherwise.
   * @param option wrapped Option.
   * @param <A> returned value type.
   * @return new Option. Can't be null.
   * @throws NullPointerException if the argument was null.
   */
  public static @Nonnull <A> Option<A> unwrap(final Option<? extends Option<A>> option) {
    return option.bind(arg -> arg);
  }

  /**
   * Compatibility method: creates a new Option from the nullable value: Some is returned is the value is not null, None otherwise.
   * @param value nullable value to wrap.
   * @param <A> value type.
   * @return flattened Option. Can't be null.
   */
  public static @Nonnull <A> Option<A> fromNullable(final @Nullable A value) {
    return value == null ? none() : some(value);
  }

  /**
   * One of two possible {@link Option} cases: Some contains a value of type <b>A</b> and participates in all transformations.
   * @param <A> value type.
   */
  @Immutable
  public static final class Some<A> extends Option<A> {
    private static final long serialVersionUID = 100L;

    public final @Nonnull A value;

    private Some(final A value) {
      this.value = value;
    }

    @Override
    public @Nonnull A asNullable() {
      return value;
    }

    @Override
    public @Nonnull <X> X option(final Function<? super A, ? extends X> ifSome, final Value<X> ifNone) {
      nonNull(ifNone);
      return nonNull(ifSome).$(value);
    }

    @Override
    public @Nonnull <B, C> Option<C> zip(final @Nonnull Option<B> option, final @Nonnull BiFunction<? super A, ? super B, ? extends C> biFunction) {
      nonNull(option);
      nonNull(biFunction);
      return option.map(b -> biFunction.$(value, b));
    }

    @Override
    public @Nonnull <B, C> Pair<Some<B>, Some<C>> unzip(final @Nonnull Function<? super A, Pair<B, C>> function) {
      nonNull(function);
      Pair<? extends B, ? extends C> bc = function.$(value);
      return pair(some(bc.first), some(bc.second));
    }

    @Override
    public @Nonnull <B> Option<B> bind(final @Nonnull Kleisli<? super A, B> function) {
      return nonNull(function).run(value);
    }

    @Override
    public @Nonnull <B> Option<B> apply(final @Nonnull Option<? extends Function<? super A, ? extends B>> option) {
      nonNull(option);
      return option.map(f -> f.$(value));
    }

    @Override
    public @Nonnull <B> Some<B> map(final @Nonnull Function<? super A, ? extends B> function) {
      nonNull(function);
      return some(function.$(value));
    }

    @Override
    public @Nonnull Option<A> filter(final @Nonnull Predicate<? super A> predicate) {
      nonNull(predicate);
      return predicate.$(value) ? this : none();
    }

    @Override
    public @Nonnull <B> Sequence<Option<B>> traverse(final Function<? super A, Sequence<B>> function) {
      nonNull(function);
      return function.$(value).map(Option::some);
    }

    @Override
    public @Nonnull <X> X match(Function<? super Some<A>, ? extends X> some, Function<? super None<A>, ? extends X> none) {
      nonNull(some);
      nonNull(none);
      return some.$(this);
    }

    @Override
    public boolean isSome() {
      return true;
    }

    @Override
    public boolean isNone() {
      return false;
    }

    @Override
    public @Nonnull <B> B foldRight(final @Nonnull BiFunction<? super A, ? super B, ? extends B> biFunction, final B initial) {
      nonNull(biFunction);
      return biFunction.$(value, initial);
    }

    @Override
    public @Nonnull <B> B foldLeft(final @Nonnull BiFunction<? super B, ? super A, ? extends B> biFunction, final B initial) {
      nonNull(biFunction);
      return biFunction.$(initial, value);
    }

    @Override
    public @Nonnull Iterator<A> iterator() {
      return new Iterator<A>() {
        private boolean consumed;

        @Override
        public boolean hasNext() {
          return !consumed;
        }

        @Override
        public A next() {
          if (consumed) throw new NoSuchElementException();
          consumed = true;
          return value;
        }
      };
    }

    @Override
    public int hashCode() {
      return value.hashCode();
    }

    @Override
    public boolean equals(final Object o) {
      if (!(o instanceof Some)) return false;
      Some that = (Some) o;
      return this.value.equals(that.value);
    }

    @Override
    public @Nonnull String toString() {
      return "Just " + value.toString();
    }
  }

  /**
   * One of two possible {@link Option} cases: None contains no values and is inert to all transformations.
   */
  @Immutable
  public static final class None<A> extends Option<A> {
    private static final long serialVersionUID = 100L;

    private None() {}

    @Override
    public A asNullable() {
      return null;
    }

    @Override
    public @Nonnull <X> X option(final Function<? super A, ? extends X> ifSome, final Value<X> ifNone) {
      nonNull(ifSome);
      return nonNull(ifNone).get();
    }

    @Override
    public @Nonnull <B, C> None<C> zip(final @Nonnull Option<B> option, final @Nonnull BiFunction<? super A, ? super B, ? extends C> biFunction) {
      nonNull(option);
      nonNull(biFunction);
      return none();
    }

    @Override
    public @Nonnull <B, C> Pair<None<B>, None<C>> unzip(final @Nonnull Function<? super A, Pair<B, C>> function) {
      nonNull(function);
      return pair(none(), none());
    }

    @Override
    public @Nonnull <B> None<B> bind(final @Nonnull Kleisli<? super A, B> function) {
      nonNull(function);
      return none();
    }

    @Override
    public @Nonnull <B> None<B> apply(final @Nonnull Option<? extends Function<? super A, ? extends B>> option) {
      nonNull(option);
      return none();
    }

    @Override
    public @Nonnull <B> None<B> map(final @Nonnull Function<? super A, ? extends B> function) {
      nonNull(function);
      return none();
    }

    @Override
    public @Nonnull None<A> filter(final @Nonnull Predicate<? super A> predicate) {
      nonNull(predicate);
      return none();
    }

    @Override
    public @Nonnull <B> Sequence<Option<B>> traverse(final Function<? super A, Sequence<B>> function) {
      nonNull(function);
      return sequence(none());
    }

    @Override
    public @Nonnull <X> X match(Function<? super Some<A>, ? extends X> some, Function<? super None<A>, ? extends X> none) {
      nonNull(some);
      nonNull(none);
      return none.$(this);
    }

    @Override
    public boolean isSome() {
      return false;
    }

    @Override
    public boolean isNone() {
      return true;
    }

    @Override
    public @Nonnull <B> B foldRight(final @Nonnull BiFunction<? super A, ? super B, ? extends B> biFunction, final B initial) {
      nonNull(biFunction);
      return initial;
    }

    @Override
    public @Nonnull <B> B foldLeft(final @Nonnull BiFunction<? super B, ? super A, ? extends B> biFunction, final B initial) {
      nonNull(biFunction);
      return initial;
    }

    @Override
    public @Nonnull Iterator<A> iterator() {
      return Collections.emptyIterator();
    }

    @Override
    public @Nonnull String toString() {
      return "None";
    }

    private static final None<Object> INSTANCE = new None<>();
  }

  /**
   * <p>Kleisli arrow is a pure function from an argument of type <b>A</b> to <b>Option&lt;B&gt;</b>. </p>
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
    @Nonnull Option<B> run(A arg);

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
      return ac -> ac.either(a -> run(a).map(Either::left), c -> some(Either.right(c)));
    }

    /**
     * Returns an arrow which maps its input using this arrow of it is {@link Either.Right} and passes it
     * unchanged otherwise.
     * @param <C> left component type.
     * @return new arrow. Can't be null.
     */
    default @Nonnull <C> Kleisli<Either<C, A>, Either<C, B>> right() {
      return ca -> ca.either(c -> some(Either.left(c)), a -> run(a).map(Either::right));
    }

    /**
     * Returns an arrow which maps first part of its input and passes the second part unchanged.
     * @param <C> right component type.
     * @return new arrow. Can't be null.
     */
    default @Nonnull <C> Kleisli<Pair<A, C>, Pair<B, C>> first() {
      return ac -> run(ac.first).zip(some(ac.second), Pair::pair);
    }

    /**
     * Returns an arrow which maps second part of its input and passes the first part unchanged.
     * @param <C> left component type.
     * @return new arrow. Can't be null.
     */
    default @Nonnull <C> Kleisli<Pair<C, A>, Pair<C, B>> second() {
      return ca -> some(ca.first).zip(run(ca.second), Pair::pair);
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
      return a -> some(function.$(a));
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
