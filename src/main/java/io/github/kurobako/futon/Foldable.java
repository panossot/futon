/*
 * Copyright (C) 2015 Fedor Gavrilov
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

import static java.util.Objects.requireNonNull;

public interface Foldable<A> {
  default  @Nonnull
  <M extends Semigroup<M>> M fold(final @Nonnull Function<? super A, ? extends M> function, final @Nonnull M empty) {
    requireNonNull(function, "function");
    requireNonNull(empty, "empty");
    return foldLeft((m, a) -> m.append(function.$(a)), empty);
  }

  <B> B foldRight(@Nonnull BiFunction<? super A, ? super B, ? extends B> function, B initial);

  <B> B foldLeft(@Nonnull BiFunction<? super B, ? super A, ? extends B> function, B initial);
}
