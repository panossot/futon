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
import static io.github.kurobako.futon.Util.nonNull;

/**
 * <p>State monad represents a computation taking a seed <b>S</b> and returning a value <b>A</b> along with a new <b>S</b>.</p>
 * <p>It might be used to track the chain of modifications and observe the difference (see {@link #delta(InverseSemigroup)})
 * or to separate a computation from the mutable state it operates on. Consider pseudo-random number generator
 * accepting passed seed and producing a random number along with a new seed. It is a pure function as long as the seed value
 * and the algorithm are kept separate.</p>
 * <p>{@link #map(Function)} makes State a functor.</p>
 * <p>{@link #apply(State)} and {@link #state(A)} form an applicative functor.</p>
 * <p>{@link #bind(Function)} and {@link #state(A)} form a monad.</p>
 * @see <a href="http://web.cecs.pdx.edu/~mpj/pubs/springschool95.pdf">http://web.cecs.pdx.edu/~mpj/pubs/springschool95.pdf</a>
 * @param <S> seed type.
 * @param <A> produced value type.
 */
public interface State<S, A> {
  /**
   * Runs the computation, producing new seed <b>S</b> and a value <b>A</b> based on passed <b>S</b>.
   * @param initial seed. Can't be null.
   * @return a {@link Pair} of <b>S</b> and <b>A</b> produced by the computation. Can't be null.
   */
  @Nonnull Pair<S, A> run(S initial);

  /**
   * <i>Executes</i> the transition: {@link #run(S)} the computation and return new <b>S</b>.
   * @param initial seed. Can't be null.
   * @return new seed. Can't be null.
   */
  default @Nonnull S exec(final S initial) {
    return run(initial).first;
  }

  /**
   * <i>Evaluates</i> the result: {@link #run(S)} the computation and return produced <b>A</b>.
   * @param initial seed. Can't be null.
   * @return produced value. Can't be null.
   */
  default @Nonnull A eval(final S initial) {
    return run(initial).second;
  }

  /**
   * Returns a new State which returns seed transformed by applying the given function to the seed returned by this State.
   * @param function <b>S -&gt; S</b> transformation. Can't be null.
   * @return new State. Can't be null.
   * @throws NullPointerException if the argument was null.
   */
  default @Nonnull State<S, A> with(final @Nonnull Function<? super S, ? extends S> function) {
    nonNull(function);
    return s -> {
      final Pair<S, A> sa = run(s);
      return pair(function.$(sa.first), sa.second);
    };
  }

  /**
   * Returns a new State whose produced value is the product of applying the given function to this State's produced value.
   * @param function <b>A -&gt; B</b> transformation. Can't be null.
   * @param <B> new value type.
   * @return new State. Can't be null.
   * @throws NullPointerException if the argument was null.
   */
  default @Nonnull <B> State<S, B> map(final @Nonnull Function<? super A, ? extends B> function) {
    nonNull(function);
    return s -> {
      final Pair<S, A> sa = run(s);
      return pair(sa.first, function.$(sa.second));
    };
  }

  /**
   * Returns a new State whose produced value is the product of applying the function produced by the given State to the value produced by this.
   * @param state <b>State&lt;S, A -&gt; B&gt:</b> transformation inside the State. Can't be null.
   * @param <B> new value type.
   * @return new State. Can't be null.
   * @throws NullPointerException if the argument was null.
   */
  default @Nonnull <B> State<S, B> apply(final @Nonnull State<S, ? extends Function<? super A, ? extends B>> state) {
    nonNull(state);
    return bind(a -> state.bind(f -> state(f.$(a))));
  }

  /**
   * Returns a new State which is the product of applying the given function to this State's produced value.
   * @param function <b>A -&gt; State&lt;S, B&gt;</b> transformation. Can't be null.
   * @param <B> new value type.
   * @return new State. Can't be null.
   * @throws NullPointerException if the argument was null.
   */
  default @Nonnull <B> State<S, B> bind(final @Nonnull Function<? super A, ? extends State<S, B>> function) {
    nonNull(function);
    return s -> {
      final Pair<S, A> sa = State.this.run(s);
      return function.$(sa.second).run(sa.first);
    };
  }

  /**
   * Creates a new State which just returns a pair containing the seed it was given in both left and right positions.
   * @param <S> seed and value type.
   * @return a {@link Pair}, both components of which are the seed. Can't be null.
   */
  static @Nonnull <S> State<S, S> get() {
    return s -> pair(s, s);
  }

  /**
   * Creates a new State which ignores the seed it was given and always returns the given seed.
   * @param s seed to return. Can't be null.
   * @param <S> seed type.
   * @return new State. Can't be null.
   * @throws NullPointerException if the argument was null.
   */
  static @Nonnull <S> State<S, Unit> put(final S s) {
    final Pair<S, Unit> su = pair(s, Unit.INSTANCE);
    return ignored -> su;
  }

  /**
   * Creates a new State which returns the seed it was given transformed by the supplied function.
   * @param function seed transformation. Can't be null.
   * @param <S> seed type.
   * @return new State. Can't be null.
   * @throws NullPointerException if the argument was null.
   */
  static @Nonnull <S> State<S, Unit> modify(final @Nonnull Function<? super S, ? extends S> function) {
    nonNull(function);
    return s -> pair(function.$(s), Unit.INSTANCE);
  }

  /**
   * Creates a new State which returns the seed it was given as a new seed and the seed transformed by the supplied function as its produced value.
   * @param function <b>S -&gt; A</b> transformation. Can't be null.
   * @param <S> seed type.
   * @param <A> produced value type.
   * @return new State. Can't be null.
   */
  static @Nonnull <S, A> State<S, A> gets(final @Nonnull Function<? super S, ? extends A> function) {
    nonNull(function);
    return s -> pair(s, function.$(s));
  }

  /**
   * Creates a new State which calculates a diff between the given seed and the seed that was passed to its run method.
   * @param current the seed to comapre with. Can't be null.
   * @param <S> seed type. Has to implement {@link InverseSemigroup} interface.
   * @return new State. Can't be null.
   * @throws NullPointerException if the argument was null.
   */
  static @Nonnull <S extends InverseSemigroup<S>> State<S, S> delta(final @Nonnull S current) {
    nonNull(current);
    return s -> pair(current.inverse().append(s), current);
  }

  /**
   * Flattens nested States, returning a new State containing the innermost computation.
   * @param state State containing another State as its produced value.
   * @param <S> seed type.
   * @param <A> inner produced value type.
   * @return new State. Can't be null.
   * @throws NullPointerException if the argument was null.
   */
  static @Nonnull <S, A> State<S, A> unwrap(final @Nonnull State<S, ? extends State<S, A>> state) {
    return nonNull(state).bind(arg -> arg);
  }

  /**
   * Creates a new State which always returns the given value as its produced value and unchanged seed that was passed to its run method.
   * @param value produced value to return.
   * @param <S> seed type.
   * @param <A> produced value type.
   * @return new State. Can't be null.
   * @throws NullPointerException if the argument was null.
   */
  static @Nonnull <S, A> State<S, A> state(final A value) {
    return s -> pair(s, value);
  }
}
