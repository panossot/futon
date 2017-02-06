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

import static io.github.kurobako.futon.Pair.pair;
import static io.github.kurobako.futon.Util.nonNull;

/**
 * A version of {@link Value} which memoizes its result once it is retrieved using double-checked locking.
 * @param <A> result type.
 */
public final class Thunk<A> implements Value<A> {
  private volatile Value<A> computation;
  private A result;

  private Thunk(final Value<A> computation) {
    this.computation = computation;
  }

  @Override
  public @Nonnull A get() {
    boolean done = (computation == null);
    if (!done) {
      synchronized (this) {
        done = (computation == null);
        if (!done) {
          result = computation.get();
          computation = null;
        }
      }
    }
    return result;
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
    nonNull(computation);
    return new Thunk<>(computation);
  }
}
