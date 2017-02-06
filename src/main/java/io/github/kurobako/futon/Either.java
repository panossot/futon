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

import static io.github.kurobako.futon.BiFunction.curry;
import static io.github.kurobako.futon.Pair.pair;
import static io.github.kurobako.futon.Sequence.sequence;
import static io.github.kurobako.futon.Util.nonNull;

/**
 * <p>Either is a container which can hold a value of type <b>L</b> or a value of type <b>R</b> but never both.</p>
 * <p>It can be used to represent a result of computation which can end in two possible ways. If failure is involved,
 * left value can be used for it since {@link #map(Function)}, {@link #apply(Either)} and {@link #bind(Function)}
 * methods are parameterized by <b>R</b> value (which is probably what you need).</p>
 * <p>{@link #map(Function)} makes Either a functor.</p>
 * <p>{@link #apply(Either)} and {@link #right(R)} form an applicative functor.</p>
 * <p>{@link #bind(Function)} and {@link #right(R)} form a monad.</p>
 * <p>This class is not supposed to be extended by users.</p>
 * @see <a href="http://hackage.haskell.org/packages/archive/base/latest/doc/html/Data-Either.html">http://hackage.haskell.org/packages/archive/base/latest/doc/html/Data-Either.html</a>
 * @param <L> left value type.
 * @param <R> right value type.
 */
public abstract class Either<L, R> implements Foldable<R> {
  Either() {}

  /**
   * Case analysis: apply left function to the value stored if this Either is {@link Left} or apply right function is
   * this is {@link Right}.
   * @param ifLeft function to apply if left. Can't be null.
   * @param ifRight function to apply if right. Can't be null.
   * @param <X> result type. Can't be null.
   * @return case analysis result. Can't be null.
   * @throws NullPointerException if any argument was null.
   */
  public abstract @Nonnull <X> X either(Function<? super L, ? extends X> ifLeft, final @Nonnull Function<? super R, ? extends X> ifRight);

  /**
   * Returns a new Either which holds this left value as its right value or this right value as its left value.
   * @return new Either with <b>L</b> and <b>R</b> types swapped. Can't be null.
   */
  public abstract @Nonnull Either<R, L> swap();

  /**
   * Returns an Either whose right value is the product of applying the given function to this right value.
   * If this is {@link Left} no transformation happens.
   * @param function <b>R -&gt; X</b> transformation. Can't be null.
   * @param <X> new right value type.
   * @return an Either transformed in right value. Can't be null.
   * @throws NullPointerException if the argument was null.
   */
  public abstract @Nonnull <X> Either<L, X> map(Function<? super R, ? extends X> function);

  /**
   * Returns an Either whose right value is the product of applying the function stored as the given Either's right
   * value to this right value. If this or the argument Either is {@link Left} no transformation happens.
   * @param either <b>L|(R -&gt; X)</b>: transformation inside the Either. Can't be null.
   * @param <X> new right value type.
   * @return an Either transformed in right value. Can't be null.
   * @throws NullPointerException if the argument was null.
   */
  public abstract @Nonnull <X> Either<L, X> apply(Either<L, ? extends Function<? super R, ? extends X>> either);

  /**
   * Returns an Either which is the product of applying the given function to this Either's right value. If this
   * or the Either returned by the argument function is {@link Left} no transformation happens.
   * @param function <b>R -&gt; L|X</b> transformation. Can't be null.
   * @param <X> new right value type.
   * @return an Either transformed in right value. Can't be null.
   * @throws NullPointerException if the argument was null.
   */
  public abstract @Nonnull <X> Either<L, X> bind(Function<? super R, ? extends Either<L, X>> function);

  /**
   * Zips this with the given Either using the given function. Transformation happens only if both this and the argument
   * Either are {@link Right}.
   * @param either an Either to zip with.
   * @param function <b>R -&gt; X -&gt; Y</b> transformation. Can't be null.
   * @param <X> right value type of the Either to zip with.
   * @param <Y> new right value type.
   * @return an Either zipped with the given Either in its right value. Can't be null.
   * @throws NullPointerException if any argument was null.
   */
  public abstract @Nonnull <X, Y> Either<L, Y> zip(Either<L, X> either, BiFunction<? super R, ? super X, ? extends Y> function);

  /**
   * Unzips this into a {@link Pair} of Eithers using the given function. If this is {@link Left}, a pair of duplicate
   * untransformed Eithers is returned.
   * @param function <b>R -&gt; (X, Y)</b> transformation. Can't be null.
   * @param <X> right value type of the first result.
   * @param <Y> right value type of the second result.
   * @return a Pair of unzipped Eithers.
   * @throws NullPointerException if the argument was null.
   */
  public abstract @Nonnull <X, Y> Pair<? extends Either<L, X>, ? extends Either<L, Y>> unzip(Function<? super R, Pair<X, Y>> function);

  /**
   * Transforms this Either using left function if it is {@link Left} and using right function if it is {@link Right}.
   * @param leftFunction <b>L -&gt; X</b> transformation. Can't be null.
   * @param rightFunction <b>R -&gt: Y</b> transformation. Can't be null.
   * @param <X> new left value type.
   * @param <Y> new right value type.
   * @return an Either transformed in both left and right values. Can't be null.
   * @throws NullPointerException if any argument was null.
   */
  public abstract @Nonnull <X, Y> Either<X, Y> biMap(Function<? super L, ? extends X> leftFunction, Function<? super R, ? extends Y> rightFunction);

  @Override
  public abstract @Nonnull <B> B foldRight(BiFunction<? super R, ? super B, ? extends B> function, B initial);

  @Override
  public abstract @Nonnull <B> B foldLeft(BiFunction<? super B, ? super R, ? extends B> function, B initial);

  /**
   * Transforms this Either's right value using the given function, wrapping returned {@link Sequence} of values into
   * a sequence of Eithers. If this Either is {@link Left}, a sequence of size 1 containing this sole element is returned.
   * @param function traversal function: <b>R -&gt; [X]</b>. Can't be null.
   * @param <X> new right value type.
   * @return a sequence of values wrapped in Either. Can't be null.
   * @throws NullPointerException if the argument was null.
   */
  public abstract @Nonnull <X> Sequence<Either<L, X>> traverse(Function<? super R, Sequence<X>> function);

  /**
   * Poor man's pattern matching: reduce this Either to <b>X</b> using left function if it is {@link Left} or reduce
   * it using right function if it is {@link Right}.
   * @param left a function to apply if this is Left. Can't be null.
   * @param right a function to apply if this is Right. Can't be null.
   * @param <X> reduced value type.
   * @return reduced value. Can't be null.
   * @throws NullPointerException if any argument was null.
   */
  public abstract @Nonnull <X> X match(Function<? super Left<L, R>, ? extends X> left, Function<? super Right<L, R>, ? extends X> right);

  /**
   * Checks if this is {@link Left}.
   * @return true if Left, false is Right.
   */
  public abstract boolean isLeft();

  /**
   * Checks if this is {@link Right}.
   * @return true if Right, false if Left.
   */
  public abstract boolean isRight();

  /**
   * Creates a new {@link Left} containing the given value.
   * @param value left value. Can't be null.
   * @param <L> left value type.
   * @return new Left. Can't be null.
   * @throws NullPointerException if the argument was null;
   */
  public static @Nonnull <L, R> Left<L, R> left(final L value) {
    return new Left<>(nonNull(value));
  }

  /**
   * Creates a new {@link Right} Either containing the given value.
   * @param value right value. Can't be null.
   * @param <R> right value type.
   * @return new Right. Can't be null.
   * @throws NullPointerException if the argument was null.
   */
  public static @Nonnull <L, R> Right<L, R> right(final R value) {
    return new Right<>(nonNull(value));
  }

  /**
   * Flattens nested Eithers by their right values. The value of the returned Either depends on the structure of the
   * argument:
   * <ul>
   *   <li>outer was {@link Right}, inner was Right: inner right value</li>
   *   <li>outer was Right, inner was {@link Left}: inner left value</li>
   *   <li>outer was Left: outer left value</li>
   * </ul>
   * @param either Either whose right value might be another Either.
   * @param <L> left value type.
   * @param <R> right value type.
   * @return new Either. Can't be null.
   * @throws NullPointerException if the argument was null.
   */
  public static @Nonnull <L, R> Either<L, R> unwrap(final @Nonnull Either<L, ? extends Either<L, R>> either) {
    return nonNull(either).bind(arg -> arg);
  }

  /**
   * One of two possible {@link Either} cases: Left contains a value of type <b>L</b> and is inert to most transformations.
   * @param <L> left value type.
   */
  public static final class Left<L, R> extends Either<L, R> {
    public final @Nonnull L value;

    private Left(final L left) {
      this.value = left;
    }

    @Override
    public @Nonnull <X> X either(final @Nonnull Function<? super L, ? extends X> ifLeft, final @Nonnull Function<? super R, ? extends X> ifRight) {
      nonNull(ifLeft);
      nonNull(ifRight);
      return ifLeft.$(value);
    }

    @Override
    public @Nonnull Right<R, L> swap() {
      return right(value);
    }

    @Override
    public @Nonnull <X> Left<L, X> bind(final @Nonnull Function<? super R, ? extends Either<L, X>> function) {
      nonNull(function);
      return self();
    }

    @Override
    public @Nonnull <X, Y> Left<L, Y> zip(final @Nonnull Either<L, X> either, final @Nonnull BiFunction<? super R, ? super X, ? extends Y> biFunction) {
      nonNull(either);
      nonNull(biFunction);
      return self();
    }

    @Override
    public @Nonnull <X, Y> Pair<Left<L, X>, Left<L, Y>> unzip(final @Nonnull Function<? super R, Pair<X, Y>> function) {
      nonNull(function);
      return pair(self(), self());
    }

    @Override
    public @Nonnull <X> Left<L, X> apply(final @Nonnull Either<L, ? extends Function<? super R, ? extends X>> either) {
      nonNull(either);
      return self();
    }

    @Override
    public @Nonnull <X> Left<L, X> map(final @Nonnull Function<? super R, ? extends X> function) {
      nonNull(function);
      return self();
    }

    @SuppressWarnings("unchecked")
    private <X> Left<L, X> self() {
      return (Left<L, X>) this;
    }

    @Override
    public @Nonnull <X, Y> Left<X, Y> biMap(final @Nonnull Function<? super L, ? extends X> leftFunction, @Nonnull Function<? super R, ? extends Y> rightFunction) {
      nonNull(leftFunction);
      nonNull(rightFunction);
      return left(leftFunction.$(value));
    }

    @Override
    public @Nonnull <X> X match(Function<? super Left<L, R>, ? extends X> left, Function<? super Right<L, R>, ? extends X> right) {
      nonNull(left);
      nonNull(right);
      return left.$(this);
    }

    @Override
    public boolean isLeft() {
      return true;
    }

    public boolean isRight() {
      return false;
    }

    @Override
    public @Nonnull <B> B foldRight(final @Nonnull BiFunction<? super R, ? super B, ? extends B> biFunction, final B initial) {
      nonNull(biFunction);
      return initial;
    }

    @Override
    public @Nonnull <B> B foldLeft(final @Nonnull BiFunction<? super B, ? super R, ? extends B> biFunction, final B initial) {
      nonNull(biFunction);
      return initial;
    }

    @SuppressWarnings("unchecked")
    @Override
    public @Nonnull <X> Sequence<Either<L, X>> traverse(final Function<? super R, Sequence<X>> function) {
      nonNull(function);
      return sequence((Either<L, X>) this);
    }

    @Override
    public int hashCode() {
      return value.hashCode();
    }

    @Override
    public boolean equals(final Object o) {
      if (!(o instanceof Left)) return false;
      final Left that = (Left) o;
      return this.value.equals(that.value);
    }

    @Override
    public @Nonnull String toString() {
      return "Left " + String.valueOf(value);
    }
  }

  /**
   * One of two possible {@link Either} cases: Right contains a value of type <b>R</b> and is participant in all transformations.
   * @param <R> right value type.
   */
  public static final class Right<L, R> extends Either<L, R> {
    public final @Nonnull R value;

    private Right(final R right) {
      this.value = right;
    }

    @Override
    public @Nonnull <X> X either(final @Nonnull Function<? super L, ? extends X> ifLeft, final @Nonnull Function<? super R, ? extends X> ifRight) {
      nonNull(ifLeft);
      nonNull(ifRight);
      return ifRight.$(value);
    }

    @Override
    public @Nonnull Left<R, L> swap() {
      return left(value);
    }

    @Override
    public @Nonnull <X> Either<L, X> bind(final @Nonnull Function<? super R, ? extends Either<L, X>> function) {
      nonNull(function);
      return function.$(value);
    }

    @Override
    public @Nonnull <X, Y> Either<L, Y> zip(final @Nonnull Either<L, X> either, final @Nonnull BiFunction<? super R, ? super X, ? extends Y> biFunction) {
      nonNull(either);
      nonNull(biFunction);
      return either.map(curry(biFunction).$(value));
    }

    @Override
    public @Nonnull <X, Y> Pair<Right<L, X>, Right<L, Y>> unzip(final @Nonnull Function<? super R, Pair<X, Y>> function) {
      final Pair<? extends X, ? extends Y> xy = nonNull(function).$(value);
      return pair(right(xy.first), right(xy.second));
    }

    @Override
    public @Nonnull <X> Either<L, X> apply(final @Nonnull Either<L, ? extends Function<? super R, ? extends X>> either) {
      return nonNull(either).map(f -> f.$(value));
    }

    @Override
    public @Nonnull <X> Right<L, X> map(final @Nonnull Function<? super R, ? extends X> function) {
      return right(nonNull(function).$(value));
    }

    @Override
    public @Nonnull <X, Y> Either<X, Y> biMap(final @Nonnull Function<? super L, ? extends X> leftFn, final @Nonnull Function<? super R, ? extends Y> rightFn) {
      nonNull(leftFn);
      nonNull(rightFn);
      return right(rightFn.$(value));
    }

    @Override
    public @Nonnull <X> X match(Function<? super Left<L, R>, ? extends X> left, Function<? super Right<L, R>, ? extends X> right) {
      nonNull(left);
      nonNull(right);
      return right.$(this);
    }

    @Override
    public boolean isLeft() {
      return false;
    }

    public boolean isRight() {
      return true;
    }

    @Override
    public @Nonnull <B> B foldRight(final @Nonnull BiFunction<? super R, ? super B, ? extends B> biFunction, final B initial) {
      return nonNull(biFunction).$(value, initial);
    }

    @Override
    public @Nonnull <B> B foldLeft(final @Nonnull BiFunction<? super B, ? super R, ? extends B> biFunction, final B initial) {
      return nonNull(biFunction).$(initial, value);
    }

    @Override
    public @Nonnull <X> Sequence<Either<L, X>> traverse(final Function<? super R, Sequence<X>> function) {
      return nonNull(function).$(value).map(Either::right);
    }

    @Override
    public int hashCode() {
      return value.hashCode();
    }

    @Override
    public boolean equals(final Object o) {
      if (!(o instanceof Right)) return false;
      final Right that = (Right) o;
      return this.value.equals(that.value);
    }

    @Override
    public @Nonnull String toString() {
      return "Right " + String.valueOf(value);
    }
  }
}
