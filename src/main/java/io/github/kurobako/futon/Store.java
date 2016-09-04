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

public interface Store<I, A> {
  @Nonnull Pair<Function<? super I, ? extends A>, I> run();

  default I pos() {
    return run().second;
  }

  default @Nonnull Function<? super I, ? extends A> peek() {
    return run().first;
  }

  default A peeks(final @Nonnull Function<? super I, ? extends I> function) {
    nonNull(function);
    final Pair<Function<? super I, ? extends A>, I> fi = run();
    return fi.first.$(function.$(fi.second));
  }

  default @Nonnull Store<I, A> seek(final I index) {
    final Pair<Function<? super I, ? extends A>, I> fi = run();
    return store(fi.first, index);
  }

  default @Nonnull Store<I, A> seeks(final @Nonnull Function<? super I, ? extends I> function) {
    nonNull(function);
    final Pair<Function<? super I, ? extends A>, I> fi = run();
    return store(fi.first, function.$(fi.second));
  }

  default A extract() {
    final Pair<Function<? super I, ? extends A>, I> fi = run();
    return fi.first.$(fi.second);
  }

  default @Nonnull Store<I, Store<I, A>> duplicate() {
    return extend(id());
  }

  default @Nonnull <B> Store<I, B> extend(final @Nonnull Function<? super Store<I, A>, ? extends B> function) {
    nonNull(function);
    final Pair<Function<? super I, ? extends A>, I> fi = run();
    return store(index -> function.$(store(fi.first, index)), fi.second);
  }

  default @Nonnull <B> Store<I, B> map(final @Nonnull Function<? super A, ? extends B> function) {
    nonNull(function);
    final Pair<Function<? super I, ? extends A>, I> fi = run();
    return store(function.precompose(fi.first), fi.second);
  }

  static @Nonnull <I, A> Store<I, A> store(final @Nonnull Function<? super I, ? extends A> function, final I index) {
    nonNull(function);
    final Pair<Function<? super I, ? extends A>, I> fi = pair(function, index);
    return () -> fi;
  }
}