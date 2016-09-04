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

import static io.github.kurobako.futon.Function.id;
import static io.github.kurobako.futon.Pair.pair;
import static io.github.kurobako.futon.Util.nonNull;

public interface Environment<E, A> {
  @Nonnull Pair<E, A> run();

  default E ask() {
    return run().first;
  }

  default <F> F asks(final @Nonnull Function<? super E, ? extends F> function) {
    return nonNull(function).$(run().first);
  }

  default <F> Environment<F, A> local(final @Nonnull Function<? super E, ? extends F> function) {
    nonNull(function);
    final Pair<F, A> fa = run().biMap(function, id());
    return () -> fa;
  }

  default A extract() {
    return run().second;
  }

  default @Nonnull Environment<E, Environment<E, A>> duplicate() {
    return extend(id());
  }

  default @Nonnull <B> Environment<E, B> extend(final @Nonnull Function<? super Environment<E, A>, ? extends B> function) {
    nonNull(function);
    return environment(run().first, function.$(this));
  }

  default @Nonnull <B> Environment<E, B> map(final @Nonnull Function<? super A, ? extends B> function) {
    nonNull(function);
    final Pair<E, A> ea = run();
    return environment(ea.first, function.$(run().second));
  }

  static @Nonnull <E, A> Environment<E, A> environment(final E environment, final A value) {
    final Pair<E, A> ea = pair(environment, value);
    return () -> ea;
  }
}