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

import static io.github.kurobako.futon.Either.left;
import static io.github.kurobako.futon.Either.right;
import static io.github.kurobako.futon.Sequence.sequence;
import static io.github.kurobako.futon.Util.nonNull;

/**
 * <p>Trampoline is a type-safe builder of possibly recursive computations which can later be forced and won't overflow the
 * stack.</p>
 * <p>Currently around {@value Sequence#MAX_MEASURE} on-heap stack "frames" are possible which should be quite enough
 * even for a deep recursive cases.</p>
 * <p>{@link #map(Function)} makes Value a functor.</p>
 * <p>{@link #apply(Trampoline)} and {@link #done(A)} form an applicative functor.</p>
 * <p>{@link #bind(Kleisli)} and {@link #done(A)} form a monad.</p>
 * @see <a href="http://days2012.scala-lang.org/sites/days2012/files/bjarnason_trampolines.pdf">http://days2012.scala-lang.org/sites/days2012/files/bjarnason_trampolines.pdf</a>
 * @param <A> result type.
 */
@Immutable
public abstract class Trampoline<A> {
  Trampoline() {}

  /**
   * Run the computation up to its final result.
   * @return computation result. Can't be null.
   */
  public abstract @Nonnull A run();

  /**
   * Run one layer of the computation, returning {@link Either} a next layer of the final result.
   * @return next computation layer or the final result. Can't be null.
   */
  public abstract @Nonnull Either<Trampoline<A>, A> resume();

  /**
   * Returns a Trampoline whose result is the product of applying the given function to the result of this Trampoline.
   * @param function <b>A -&gt; B</b> transformation. Can't be null.
   * @param <B> new result type.
   * @return transformed Trampoline. Can't be null.
   * @throws NullPointerException if the argument was null.
   */
  public @Nonnull <B> Trampoline<B> map(final Function<? super A, ? extends B> function) {
    nonNull(function);
    return bind(a -> new More<>(More.UNIT, u -> new Done<B>(function.$(a))));
  }

  /**
   * Returns a Trampoline whose result is the product of applying the function stored as the given Trampoline to this result.
   * @param trampoline <b>Trampoline&lt;A -&gt; B&gt;</b>: transformation inside the Trampoline. Can't be null.
   * @param <B> new result type.
   * @return transformed Trampoline. Can't be null.
   * @throws NullPointerException if the argument was null.
   */
  public @Nonnull <B> Trampoline<B> apply(final Trampoline<? extends Function<? super A, ? extends B>> trampoline) {
    nonNull(trampoline);
    return bind(a -> trampoline.bind(f -> new Done<B>(f.$(a))));
  }

  /**
   * Returns a Trampoline which is the product of applying the given function to the result of this Trampoline.
   * @param kleisli <b>A -&gt; Trampoline&lt;B&gt;</b> transformation. Can't be null.
   * @param <B> new result type.
   * @return transformed Trampoline. Can't be null.
   * @throws NullPointerException if the argument was null.
   */
  public final @Nonnull <B> Trampoline<B> bind(final Kleisli<? super A, B> kleisli) {
    nonNull(kleisli);
    return new More<>(this, kleisli);
  }

  /**
   * Interleaves this computation with the given computation and combines their results using the given function.
   * @param trampoline a Trampoline to zip with.
   * @param function <b>A -&gt; B -&gt; C</b> transformation. Can't be null.
   * @param <B> result type of the Trampoline to zip with.
   * @param <C> new result type.
   * @return a Trampoline zipped with the given Trampoline. Can't be null.
   * @throws NullPointerException if any argument was null.
   */
  public abstract @Nonnull <B, C> Trampoline<C> zip(final Trampoline<B> trampoline, final BiFunction<? super A, ? super B, ? extends C> function);

  abstract @Nonnull <X> X match(Function<? super Done<A>, ? extends X> done, Function<? super More<?, A>, ? extends X> right);

  abstract boolean isDone();

  /**
   * Creates a Trampoline which contains the given value.
   * @param value result value of the returned Trampoline. Can't be null.
   * @param <A> result type.
   * @return new Trampoline. Can't be null.
   * @throws NullPointerException if the argument was null.
   */
  public static @Nonnull <A> Trampoline<A> done(final A value) {
    nonNull(value);
    return new Done<>(value);
  }

  /**
   * Creates a Trampoline which contains the given delayed computation.
   * @param value computation which yields the result value of the returned Trampoline. Can't be null.
   * @param <A> result type.
   * @return new Trampoline. Can't be null.
   * @throws NullPointerException if the argument was null.
   */
  public static @Nonnull <A> Trampoline<A> delay(final Value<A> value) {
    nonNull(value);
    return new More<>(More.UNIT, unit -> new Done<>(value.get()));
  }

  /**
   * Creates a Trampoline which contains the given computation producing another Trampoline. The value of that Trampoline
   * will become new Trampoline's result.
   * @param value computation which produces the Trampoline to run internally. Can't be null.
   * @param <A> result type.
   * @return new Trampoline. Can't be null.
   * @throws NullPointerException if the argument was null.
   */
  public static @Nonnull <A> Trampoline<A> suspend(final Value<Trampoline<A>> value) {
    nonNull(value);
    return new More<>(More.UNIT, unit -> value.get());
  }

  private static final class Done<A> extends Trampoline<A> {
    private final @Nonnull A value;

    Done(final A value) {
      this.value = value;
    }

    @Override
    public @Nonnull A run() {
      return value;
    }

    @Override
    public @Nonnull Either<Trampoline<A>, A> resume() {
      return right(value);
    }

    @Override
    public @Nonnull <B, C> Trampoline<C> zip(final Trampoline<B> trampoline, final BiFunction<? super A, ? super B, ? extends C> function) {
      nonNull(trampoline);
      nonNull(function);
      return new More<>(trampoline, b -> done(function.$(run(), b)));
    }

    @Override
    @Nonnull <X> X match(Function<? super Done<A>, ? extends X> done, Function<? super More<?, A>, ? extends X> right) {
      return done.$(this);
    }

    @Override
    public boolean isDone() {
      return true;
    }
  }

  private static final class More<O, A> extends Trampoline<A> {
    private static final @Nonnull Done<Unit> UNIT = new Done<>(Unit.INSTANCE);

    private final @Nonnull Trampoline<O> previous;
    private final @Nonnull Kleisli<? super O, A> transform;

    More(final Trampoline<O> previous, final Kleisli<? super O, A> transform) {
      this.previous = previous;
      this.transform = transform;
    }

    @SuppressWarnings("unchecked")
    @Override
    public @Nonnull A run() {
      Trampoline<?> current = this;
      Sequence<Kleisli<Object, Object>> stack = sequence();
      A result = null;
      while (result == null) {
        if (current.isDone()) {
          final A value = (A) current.run();
          if (stack.isEmpty()) {
            result = value;
          } else {
            current = stack.head().run(current.run());
            stack = stack.tail();
          }
        } else {
          final More more = (More<?, ?>) current;
          current = more.previous;
          stack = stack.prepend(more.transform);
        }
      }
      return result;
    }

    @Override
    public @Nonnull Either<Trampoline<A>, A> resume() {
      return previous.match(done -> left(transform.run(done.value)), more -> left(more.bind(transform)));
    }

    @Override
    public @Nonnull <B, C> Trampoline<C> zip(final Trampoline<B> trampoline, final BiFunction<? super A, ? super B, ? extends C> function) {
      nonNull(trampoline);
      nonNull(function);
      return new More<>(trampoline, b -> this.map(a -> function.$(a, b)));
    }

    @Override
    @Nonnull <X> X match(Function<? super Done<A>, ? extends X> done, Function<? super More<?, A>, ? extends X> right) {
      return right.$(this);
    }

    @Override
    public boolean isDone() {
      return false;
    }
  }

  /**
   * <p>Kleisli arrow is a pure function from an argument of type <b>A</b> to <b>Trampoline&lt;B&gt;</b>. </p>
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
    @Nonnull Trampoline<B> run(A arg);

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
      return ac -> ac.either(a -> run(a).map(Either::left), c -> done(Either.right(c)));
    }

    /**
     * Returns an arrow which maps its input using this arrow of it is {@link Either.Right} and passes it
     * unchanged otherwise.
     * @param <C> left component type.
     * @return new arrow. Can't be null.
     */
    default @Nonnull <C> Kleisli<Either<C, A>, Either<C, B>> right() {
      return ca -> ca.either(c -> done(Either.left(c)), a -> run(a).map(Either::right));
    }

    /**
     * Returns an arrow which maps first part of its input and passes the second part unchanged.
     * @param <C> right component type.
     * @return new arrow. Can't be null.
     */
    default @Nonnull <C> Kleisli<Pair<A, C>, Pair<B, C>> first() {
      return ac -> run(ac.first).zip(done(ac.second), Pair::pair);
    }

    /**
     * Returns an arrow which maps second part of its input and passes the first part unchanged.
     * @param <C> left component type.
     * @return new arrow. Can't be null.
     */
    default @Nonnull <C> Kleisli<Pair<C, A>, Pair<C, B>> second() {
      return ca -> done(ca.first).zip(run(ca.second), Pair::pair);
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
      return a -> done(function.$(a));
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