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
 * preserved, consider using something like {@link Either}<b>&lt;Exception, A&gt;</b>.</p>
 * <p>This class is not supposed to be extended by users.</p>
 * @see <a href="http://hackage.haskell.org/packages/archive/base/latest/doc/html/Data-Maybe.html">http://hackage.haskell.org/packages/archive/base/latest/doc/html/Data-Maybe.html</a>
 * @param <A> value type.
 */
public abstract class Option<A> implements Foldable<A>, Iterable<A> {
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
   * @param function <b>A -&gt; B?</b> transformation. Can't be null.
   * @param <B> new value type.
   * @return transformed Option. Can't be null.
   * @throws NullPointerException if the argument was null.
   */
  public abstract @Nonnull <B> Option<B> bind(Function<? super A, ? extends Option<B>> function);


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
  public static final class Some<A> extends Option<A> {
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
    public @Nonnull <B> Option<B> bind(final @Nonnull Function<? super A, ? extends Option<B>> function) {
      nonNull(function);
      return function.$(value);
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
  public static final class None<A> extends Option<A> {
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
    public @Nonnull <B> None<B> bind(final @Nonnull Function<? super A, ? extends Option<B>> function) {
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
}
