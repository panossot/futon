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
public interface Environment<A, E> {
  @Nonnull Pair<A, E> run();

  default E ask() {
    return run().second;
  }

  default <F> F local(final @Nonnull Function<? super E, ? extends F> function) {
    requireNonNull(function);
    return function.$(run().second);
  }

  default A extract() {
    return run().first;
  }

  default @Nonnull Environment<Environment<A, E>, E> duplicate() {
    return extend(id());
  }

  default @Nonnull <B> Environment<B, E> extend(final @Nonnull Function<? super Environment<A, E>, ? extends B> function) {
    requireNonNull(function);
    return environment(function.$(this), run().second);
  }

  default @Nonnull <B> Environment<B, E> map(final @Nonnull Function<? super A, ? extends B> function) {
    requireNonNull(function);
    final Pair<A, E> ae = run();
    return environment(function.$(ae.first), ae.second);
  }

  static @Nonnull <A, E> Environment<A, E> environment(final A value, final E environment) {
    final Pair<A, E> ae = pair(value, environment);
    return () -> ae;
  }
  
  static @Nonnull <A, E> Environment<A, E> lazy(final Environment<A, E> environment) {
    requireNonNull(environment);
    return new Environment<A, E>() {
      final Thunk<Pair<A, E>> thunk = new Thunk<>(environment::run);
      
      @Override
      public @Nonnull Pair<A, E> run() {
        return thunk.extract();
      }
      
      @Override
      public @Nonnull<B> Environment<B, E> extend(@Nonnull Function<? super Environment<A, E>, ? extends B> function) {
        requireNonNull(function);
        return lazy(() -> pair(function.$(this), run().second));
      }
      
      @Override
      public @Nonnull <B> Environment<B, E> map(@Nonnull Function<? super A, ? extends B> function) {
        requireNonNull(function);
        return lazy(() -> pair(function.$(run().first), run().second));
      }
    };
  }

  @FunctionalInterface
  interface CoKleisli<A, B, E> {
    B run(@Nonnull Environment<A, E> environment);

    default @Nonnull <C> CoKleisli<A, C, E> compose(final @Nonnull CoKleisli<B, ? extends C, E> coKleisli) {
      requireNonNull(coKleisli);
      return a -> coKleisli.run(a.extend(this::run));
    }

    default @Nonnull <C> CoKleisli<A, C, E> compose(final @Nonnull Function<? super B, ? extends C> function) {
      requireNonNull(function);
      return a -> function.$(run(a));
    }

    default @Nonnull <C> CoKleisli<Pair<A, C>, Pair<B, C>, E> first() {
      return ac -> pair(ac.map(p -> p.first), ac.map(p -> p.second)).biMap(this::run, Environment::extract);
    }

    default @Nonnull <C> CoKleisli<Pair<C, A>, Pair<C, B>, E> second() {
      return ca -> pair(ca.map(p -> p.first), ca.map(p -> p.second)).biMap(Environment::extract, this::run);
    }

    default @Nonnull <C, D> CoKleisli<Pair<A, C>, Pair<B, D>, E> product(final @Nonnull CoKleisli<C, ? extends D, E> coKleisli) {
      requireNonNull(coKleisli);
      return ac -> pair(ac.map(p -> p.first), ac.map(p -> p.second)).biMap(this::run, coKleisli::run);
    }

    default @Nonnull <C> CoKleisli<A, Pair<B, C>, E> fanOut(final @Nonnull CoKleisli<A, ? extends C, E> coKleisli) {
      requireNonNull(coKleisli);
      return a -> pair(run(a), coKleisli.run(a));
    }

    static @Nonnull <A, B, E> CoKleisli<A, B, E> lift(final @Nonnull Function<? super A, ? extends B> function) {
      requireNonNull(function);
      return a -> function.$(a.extract());
    }

    static @Nonnull <A, E> CoKleisli<A, A, E> id() {
      return Environment::extract;
    }
  }
}
