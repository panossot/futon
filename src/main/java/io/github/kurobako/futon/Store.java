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

import static io.github.kurobako.futon.Function.id;
import static io.github.kurobako.futon.Pair.pair;
import static java.util.Objects.requireNonNull;

@FunctionalInterface
public interface Store<A, I> {
  @Nonnull Pair<Function<? super I, ? extends A>, I> run();

  default I pos() {
    return run().right;
  }

  default @Nonnull Function<? super I, ? extends A> peek() {
    return run().left;
  }

  default A peeks(final @Nonnull Function<? super I, ? extends I> f) {
    requireNonNull(f, "f");
    return seeks(f).extract();
  }

  default @Nonnull Store<A, I> seek(final I index) {
    return store(peek(), index);
  }

  default @Nonnull Store<A, I> seeks(final @Nonnull Function<? super I, ? extends I> f) {
    requireNonNull(f, "f");
    return store(peek(), f.$(pos()));
  }

  default A extract() {
    return peek().$(pos());
  }

  default @Nonnull Store<Store<A, I>, I> duplicate() {
    return extend(id());
  }

  default @Nonnull <B> Store<B, I> extend(final @Nonnull Function<? super Store<A, I>, ? extends B> extend) {
    requireNonNull(extend, "extend");
    return store(index -> extend.$(Store.this.seek(index)), pos());
  }

  default @Nonnull <B> Store<B, I> map(final @Nonnull Function<? super A, ? extends B> map) {
    requireNonNull(map, "map");
    return store(peek().map(map), pos());
  }

  static @Nonnull <A, I> Store<A, I> store(final @Nonnull Function<? super I, ? extends A> peek, final I pos) {
    requireNonNull(peek, "peek");
    return new Store<A, I>() {
      private final Pair<Function<? super I, ? extends A>, I> run = pair(peek, pos);

      @Override
      public @Nonnull Pair<Function<? super I, ? extends A>, I> run() {
        return run;
      }
    };
  }
}