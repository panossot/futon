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

import static io.github.kurobako.futon.Pair.pair;
import static io.github.kurobako.futon.Trampoline.delay;
import static io.github.kurobako.futon.Util.nonNull;

/**
 * <p>A {@link Value} wrapper which memoizes its result once it is retrieved using double-checked locking.</p>
 * <p>Thunk delays the evaluation of the value until {@link #get()} would be used to force the result.</p>
 * @param <A> result type.
 */
@Immutable
public final class Thunk<A> implements Value<A> {
  private volatile Trampoline<A> computation;
  private A result;

  private Thunk(final Trampoline<A> computation) {
    this.computation = computation;
  }

  @Override
  public @Nonnull A get() {
    boolean done = (computation == null);
    if (!done) {
      synchronized (this) {
        done = (computation == null);
        if (!done) {
          result = computation.run();
          computation = null;
        }
      }
    }
    return result;
  }

  @Override
  public @Nonnull <B> Thunk<B> map(final Function<? super A, ? extends B> function) {
    return new Thunk<>(computation.map(nonNull(function)));
  }

  @Override
  public @Nonnull <B> Thunk<B> apply(final Value<? extends Function<? super A, ? extends B>> value) {
    return new Thunk<>(computation.apply(delay(nonNull(value))));
  }

  @Override
  public @Nonnull <B> Thunk<B> bind(final Kleisli<? super A, B> kleisli) {
    nonNull(kleisli);
    return new Thunk<>(computation.bind(a -> delay(kleisli.run(a))));
  }

  @Override
  public @Nonnull <B> Thunk<B> extend(final CoKleisli<A, ? extends B> coKleisli) {
    nonNull(coKleisli);
    return new Thunk<>(delay(() -> coKleisli.run(computation::run)));
  }

  @Override
  public @Nonnull <B, C> Thunk<C> zip(final Value<B> value, final BiFunction<? super A, ? super B, ? extends C> function) {
    nonNull(value);
    nonNull(function);
    return new Thunk<>(computation.zip(delay(value), function));
  }

  @Override
  public @Nonnull <B, C> Pair<? extends Thunk<B>, ? extends Thunk<C>> unzip(final Function<? super A, Pair<B, C>> function) {
    nonNull(function);
    return pair(new Thunk<>(computation.map(a -> function.$(a).first)), new Thunk<>(computation.map(a -> function.$(a).second)));
  }

  @Override
  public @Nonnull String toString() {
    final boolean evaluated = (computation == null);
    return "Thunk#" + System.identityHashCode(this) + "[" + (evaluated ? String.valueOf(result) : "?") + "]";
  }

  /**
   * Creates a new Thunk which will run the given computation when {@link #get()} will be called for the first time.
   * @param computation a computation to run. Can't be null.
   * @param <A> result type.
   * @return new Thunk. Can't be null.
   * @throws NullPointerException if the argument was null.
   */
  public static @Nonnull <A> Thunk<A> thunk(final Value<A> computation) {
    return new Thunk<>(delay(nonNull(computation)));
  }
}
