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
 * <p>{@link #bind(Function)} and {@link #done(A)} form a monad.</p>
 * @see <a href="http://days2012.scala-lang.org/sites/days2012/files/bjarnason_trampolines.pdf">http://days2012.scala-lang.org/sites/days2012/files/bjarnason_trampolines.pdf</a>
 * @param <A> result type.
 */
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
   * @param function <b>A -&gt; Trampoline&lt;B&gt;</b> transformation. Can't be null.
   * @param <B> new result type.
   * @return transformed Trampoline. Can't be null.
   * @throws NullPointerException if the argument was null.
   */
  public final @Nonnull <B> Trampoline<B> bind(final Function<? super A, Trampoline<B>> function) {
    nonNull(function);
    return new More<>(this, function);
  }

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
   * @param value computation which yields the Trampoline to run internally. Can't be null.
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
    private final @Nonnull Function<? super O, Trampoline<A>> transform;

    More(final Trampoline<O> previous, final Function<? super O, Trampoline<A>> transform) {
      this.previous = previous;
      this.transform = transform;
    }

    @SuppressWarnings("unchecked")
    @Override
    public @Nonnull A run() {
      Trampoline<?> current = this;
      Sequence<Function<Object, Trampoline<?>>> stack = sequence();
      A result = null;
      while (result == null) {
        if (current.isDone()) {
          final A value = (A) current.run();
          if (stack.isEmpty()) {
            result = value;
          } else {
            current = stack.head().$(current.run());
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
      return previous.match(done -> left(transform.$(done.value)), more -> left(more.bind(transform)));
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
}