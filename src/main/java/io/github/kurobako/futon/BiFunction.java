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

@FunctionalInterface
public interface BiFunction<A, B, C> extends Function<Pair<A, B>, C> {
  C $(A a, B b);

  @Override
  default C $(final @Nonnull Pair<A, B> p) {
    requireNonNull(p, "p");
    return $(p.left, p.right);
  }

  default @Nonnull BiFunction<B, A, C> flip() {
    return (b, a) -> BiFunction.this.$(a, b);
  }

  default @Nonnull Function<A, Function<B, C>> curry() {
    return a -> b -> BiFunction.this.$(a, b);
  }

  static @Nonnull <A, B, C> BiFunction<A, B, C> uncurry(final @Nonnull
                                                        Function<? super A, ? extends Function<? super B, ? extends C>>
                                                        f) {
    requireNonNull(f, "f");
    return (a, b) -> f.$(a).$(b);
  }
}
