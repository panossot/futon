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
import static java.util.Objects.requireNonNull;

@FunctionalInterface
public interface Store<A, I> {
  @Nonnull Pair<Function<? super I, ? extends A>, I> run();

  default I pos() {
    return run().second;
  }

  default @Nonnull Function<? super I, ? extends A> peek() {
    return run().first;
  }

  default A peeks(final @Nonnull Function<? super I, ? extends I> function) {
    requireNonNull(function);
    final Pair<Function<? super I, ? extends A>, I> fi = run();
    return fi.first.$(function.$(fi.second));
  }

  default @Nonnull Store<A, I> seek(final I index) {
    final Pair<Function<? super I, ? extends A>, I> fi = run();
    return store(fi.first, index);
  }

  default @Nonnull Store<A, I> seeks(final @Nonnull Function<? super I, ? extends I> function) {
    requireNonNull(function);
    final Pair<Function<? super I, ? extends A>, I> fi = run();
    return store(fi.first, function.$(fi.second));
  }

  default A extract() {
    final Pair<Function<? super I, ? extends A>, I> fi = run();
    return fi.first.$(fi.second);
  }

  default @Nonnull Store<Store<A, I>, I> duplicate() {
    return extend(id());
  }

  default @Nonnull <B> Store<B, I> extend(final @Nonnull Function<? super Store<A, I>, ? extends B> function) {
    requireNonNull(function);
    final Pair<Function<? super I, ? extends A>, I> fi = run();
    return store(index -> function.$(seek(index)), pos());
  }

  default @Nonnull <B> Store<B, I> map(final @Nonnull Function<? super A, ? extends B> function) {
    requireNonNull(function);
    final Pair<Function<? super I, ? extends A>, I> fi = run();
    return store(fi.first.compose(function), fi.second);
  }

  static @Nonnull <A, I> Store<A, I> store(final @Nonnull Function<? super I, ? extends A> function, final I index) {
    requireNonNull(function);
    final Pair<Function<? super I, ? extends A>, I> run = pair(function, index);
    return () -> run;
  }

  static @Nonnull <A, I> Store<A, I> lazy(final @Nonnull Store<A, I> store) {
    requireNonNull(store);
    return new Store<A, I>() {
      final Thunk<Pair<Function<? super I, ? extends A>, I>> thunk = new Thunk<>(store::run);

      @Override
      public @Nonnull Pair<Function<? super I, ? extends A>, I> run() {
        return thunk.extract();
      }

      @Override
      public @Nonnull Store<A, I> seek(final I index) {
        return lazy(() -> pair(thunk.extract().first, index));
      }

      @Override
      public @Nonnull Store<A, I> seeks(final @Nonnull Function<? super I, ? extends I> function) {
        return lazy(() -> {
          final Pair<Function<? super I, ? extends A>, I> fi = thunk.extract();
          return pair(fi.first, function.$(fi.second));
        });
      }

      @Override
      public @Nonnull <B> Store<B, I> extend(final @Nonnull Function<? super Store<A, I>, ? extends B> function) {
        requireNonNull(function);
        return lazy(() -> pair(index -> function.$(seek(index)), pos()));
      }

      @Override
      public @Nonnull <B> Store<B, I> map(final @Nonnull Function<? super A, ? extends B> function) {
        requireNonNull(function);
        return lazy(() -> {
          final Pair<Function<? super I, ? extends A>, I> fi = thunk.extract();
          return pair(fi.first.compose(function), fi.second);
        });
      }
    };
  }

  @FunctionalInterface
  interface CoKleisli<A, B, I> {
    B run(@Nonnull Store<A, I> store);

    default @Nonnull <C> CoKleisli<A, C, I> compose(final @Nonnull CoKleisli<B, ? extends C, I> coKleisli) {
      requireNonNull(coKleisli);
      return a -> coKleisli.run(a.extend(this::run));
    }

    default @Nonnull <C> CoKleisli<A, C, I> compose(final @Nonnull Function<? super B, ? extends C> function) {
      requireNonNull(function);
      return a -> function.$(run(a));
    }

    default @Nonnull <C> CoKleisli<Pair<A, C>, Pair<B, C>, I> first() {
      return ac -> pair(ac.map(p -> p.first), ac.map(p -> p.second)).biMap(this::run, Store::extract);
    }

    default @Nonnull <C> CoKleisli<Pair<C, A>, Pair<C, B>, I> second() {
      return ca -> pair(ca.map(p -> p.first), ca.map(p -> p.second)).biMap(Store::extract, this::run);
    }

    default @Nonnull <C, D> CoKleisli<Pair<A, C>, Pair<B, D>, I> product(final @Nonnull CoKleisli<C, ? extends D, I> coKleisli) {
      requireNonNull(coKleisli);
      return ac -> pair(ac.map(p -> p.first), ac.map(p -> p.second)).biMap(this::run, coKleisli::run);
    }

    default @Nonnull <C> CoKleisli<A, Pair<B, C>, I> fanOut(final @Nonnull CoKleisli<A, ? extends C, I> coKleisli) {
      requireNonNull(coKleisli);
      return a -> pair(run(a), coKleisli.run(a));
    }

    static @Nonnull <A, B, I> CoKleisli<A, B, I> lift(final @Nonnull Function<? super A, ? extends B> function) {
      requireNonNull(function);
      return a -> function.$(a.extract());
    }

    static @Nonnull <A, I> CoKleisli<A, A, I> id() {
      return Store::extract;
    }
  }
}