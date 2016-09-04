/*
 * Copyright (C) 2016 Fedor Gavrilov
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

public final class Thunk<A> implements Value<A> {
  private volatile Value<A> computation;
  private A result;

  public Thunk(final @Nonnull Value<A> computation) {
    nonNull(computation);
    this.computation = computation;
  }

  @Override
  public A extract() {
    boolean done = (computation == null);
    if (!done) {
      synchronized (this) {
        done = (computation == null);
        if (!done) {
          result = computation.extract();
          computation = null;
        }
      }
    }
    return result;
  }

  @Override
  public @Nonnull <B> Thunk<B> extend(final @Nonnull Function<? super Value<A>, B> function) {
    nonNull(function);
    return new Thunk<>(() -> function.$(this));
  }

  @Override
  public @Nonnull Thunk<Thunk<A>> duplicate() {
    return new Thunk<>(() -> this);
  }

  @Override
  public @Nonnull <B, C> Thunk<C> zip(final @Nonnull Value<B> value, final @Nonnull BiFunction<? super A, ? super B, ? extends C> biFunction) {
    nonNull(value);
    nonNull(biFunction);
    return new Thunk<>(() -> biFunction.$(extract(), value.extract()));
  }

  @Override
  public @Nonnull <B, C> Pair<Thunk<B>, Thunk<C>> unzip(final @Nonnull Function<? super A, Pair<B, C>> function) {
    nonNull(function);
    return pair(new Thunk<>(() -> function.$(extract()).first), new Thunk<>(() -> function.$(extract()).second));
  }

  @Override
  public @Nonnull <B> Thunk<B> bind(final @Nonnull Function<? super A, ? extends Value<B>> function) {
    nonNull(function);
    return new Thunk<>(() -> function.$(extract()).extract());
  }

  @Override
  public @Nonnull <B> Thunk<B> apply(final @Nonnull Value<? extends Function<? super A, ? extends B>> value) {
    nonNull(value);
    return new Thunk<>(() -> value.extract().$(extract()));
  }

  @Override
  public @Nonnull <B> Thunk<B> map(final @Nonnull Function<? super A, ? extends B> function) {
    nonNull(function);
    return new Thunk<>(() -> function.$(extract()));
  }

  @Override
  public @Nonnull String toString() {
    final boolean evaluated = (computation == null);
    return "Thunk#" + System.identityHashCode(this) + "[" + (evaluated ? String.valueOf(result) : "?") + "]";
  }
}
