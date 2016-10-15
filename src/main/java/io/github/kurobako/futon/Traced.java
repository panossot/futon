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

import static io.github.kurobako.futon.Sequence.empty;
import static io.github.kurobako.futon.Util.nonNull;

public interface Traced<O, A> {
  A run(Sequence<O> output);

  default A traces(final @Nonnull Function<? super A, ? extends Sequence<O>> function) {
    nonNull(function);
    return run(function.$(run(empty())));
  }

  default A extract() {
    return run(empty());
  }

  default @Nonnull Traced<O, Traced<O, A>> duplicate() {
    return o1 -> o2 -> run(o1.append(o2));
  }

  default @Nonnull <B> Traced<O, B> extend(final @Nonnull Function<? super Traced<O, A>, ? extends B> function) {
    nonNull(function);
    return duplicate().map(function);
  }

  default @Nonnull <B> Traced<O, B> map(final @Nonnull Function<? super A, ? extends B> function) {
    nonNull(function);
    return function.precompose(this::run)::$;
  }
}
