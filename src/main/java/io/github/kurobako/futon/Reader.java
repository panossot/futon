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
public interface Reader<E, A> {
  A run(E environment);

  default @Nonnull Reader<E, A> local(final @Nonnull Function<? super E, ? extends E> function) {
    requireNonNull(function);
    return function.compose(this::run)::$;
  }

  default @Nonnull Reader<E, A> scope(final E environment) {
    return ignored -> run(environment);
  }

  default @Nonnull <B> Reader<E, B> bind(final @Nonnull Function<? super A, ? extends Reader<E, B>> function) {
    requireNonNull(function);
    return e -> function.$(run(e)).run(e);
  }

  default @Nonnull <B> Reader<E, B> apply(final @Nonnull Reader<E, ? extends Function<? super A, ? extends B>> reader) {
    requireNonNull(reader);
    return e -> reader.run(e).$(run(e));
  }

  default @Nonnull <B> Reader<E, B> map(final @Nonnull Function<? super A, ? extends B> function) {
    requireNonNull(function);
    Function<E, A> run = this::run;
    return run.compose(function)::$;
  }

  static @Nonnull <E, A> Reader<E, A> join(final @Nonnull Reader<E, ? extends Reader<E, A>> reader) {
    requireNonNull(reader);
    return reader.bind(id());
  }

  static @Nonnull <E, A> Reader<E, A> unit(final A value) {
    return ignored -> value;
  }

  static @Nonnull <E> Reader<E, E> ask() {
    return e -> e;
  }

  static @Nonnull <E, A> Reader<E, A> reader(final @Nonnull Function<? super E, ? extends A> function) {
    requireNonNull(function);
    return function::$;
  }

  @FunctionalInterface
  interface Kleisli<E, A, B> {
    @Nonnull Reader<E, B> run(A a);

    default @Nonnull <C> Kleisli<E, A, C> compose(final @Nonnull Kleisli<E, ? super B, C> kleisli) {
      requireNonNull(kleisli);
      return a -> run(a).bind(kleisli::run);
    }

    default @Nonnull <C> Kleisli<E, A, C> compose(final @Nonnull Function<? super B, ? extends C> function) {
      requireNonNull(function);
      return a -> run(a).map(function);
    }

    default @Nonnull <C> Kleisli<E, Either<A, C>, Either<B, C>> left() {
      return ac -> ac.either(a -> run(a).map(Either::left), c -> unit(Either.right(c)));
    }

    default @Nonnull <C> Kleisli<E, Either<C, A>, Either<C, B>> right() {
      return ca -> ca.either(c -> unit(Either.left(c)), a -> run(a).map(Either::right));
    }

    default @Nonnull <C> Kleisli<E, Pair<A, C>, Pair<B, C>> first() {
      return ac -> run(ac.first).map(b -> pair(b, ac.second));
    }

    default @Nonnull <C> Kleisli<E, Pair<C, A>, Pair<C, B>> second() {
      return ca -> run(ca.second).map(b -> pair(ca.first, b));
    }

    default @Nonnull <C, D> Kleisli<E, Either<A, C>, Either<B, D>> sum(final @Nonnull Kleisli<E, ? super C, ? extends D> kleisli) {
      requireNonNull(kleisli);
      return ac -> ac.either(a -> run(a).map(Either::left), c -> kleisli.run(c).map(Either::right));
    }

    default @Nonnull <C, D> Kleisli<E, Pair<A, C>, Pair<B, D>> product(final @Nonnull Kleisli<E, ? super C, ? extends D> kleisli) {
      requireNonNull(kleisli);
      return ac -> run(ac.first).apply(kleisli.run(ac.second).map(d -> b -> pair(b, d)));
    }

    default @Nonnull <C> Kleisli<E, Either<A, C>, B> fanIn(final @Nonnull Kleisli<E, ? super C, B> kleisli) {
      requireNonNull(kleisli);
      return ac -> ac.either(Kleisli.this::run, kleisli::run);
    }

    default @Nonnull <C> Kleisli<E, A, Pair<B, C>> fanOut(final @Nonnull Kleisli<E, ? super A, ? extends C> kleisli) {
      requireNonNull(kleisli);
      return a -> run(a).apply(kleisli.run(a).map(c -> b -> pair(b, c)));
    }

    static @Nonnull <E, A, B> Kleisli<E, A, B> lift(final @Nonnull Function<? super A, ? extends B> function) {
      requireNonNull(function);
      return a -> unit(function.$(a));
    }

    static @Nonnull <E, A> Kleisli<E, A, A> id() {
      return Reader::unit;
    }

    static @Nonnull <E, A, B> Kleisli<E, Pair<Kleisli<E, A, B>, A>, B> apply() {
      return ka -> ka.first.run(ka.second);
    }
  }
}
