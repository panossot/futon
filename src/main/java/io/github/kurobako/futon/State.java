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
public interface State<A, S> {
  @Nonnull Pair<A, S> run(S state);

  default @Nonnull <B> State<B, S> bind(final @Nonnull Function<? super A, ? extends State<B, S>> function) {
    requireNonNull(function);
    return s -> {
      final Pair<A, S> as = State.this.run(s);
      return function.$(as.first).run(as.second);
    };
  }

  default @Nonnull <B> State<B, S> apply(final @Nonnull State<? extends Function<? super A, ? extends B>, S> state) {
    requireNonNull(state);
    return bind(a -> state.bind(f -> unit(f.$(a))));
  }

  default @Nonnull <B> State<B, S> map(final @Nonnull Function<? super A, ? extends B> function) {
    requireNonNull(function);
    return bind(a -> unit(function.$(a)));
  }

  static @Nonnull <A, S> State<A, S> join(final @Nonnull State<? extends State<A, S>, S> state) {
    requireNonNull(state);
    return state.bind(id());
  }

  static @Nonnull <A, S> State<A, S> unit(final A value) {
    return s -> pair(value, s);
  }

  static @Nonnull <S> State<S, S> get() {
    return s -> pair(s, s);
  }

  static @Nonnull <A, S> State<A, S> state(final @Nonnull Function<? super S, ? extends A> function) {
    requireNonNull(function);
    return s -> pair(function.$(s), s);
  }

  static @Nonnull <S> State<Unit, S> put(final S state) {
    return modify(ignoredArg -> state);
  }

  static @Nonnull <S> State<Unit, S> modify(final Function<? super S, ? extends S> function) {
    requireNonNull(function);
    return s -> pair(Unit.INSTANCE, function.$(s));
  }

  @FunctionalInterface
  interface Kleisli<A, B, S> {
    @Nonnull State<B, S> run(A a);

    default @Nonnull <C> Kleisli<A, C, S> compose(final @Nonnull Kleisli<? super B, C, S> kleisli) {
      requireNonNull(kleisli);
      return a -> run(a).bind(kleisli::run);
    }

    default @Nonnull <C> Kleisli<A, C, S> compose(final @Nonnull Function<? super B, ? extends C> function) {
      requireNonNull(function);
      return a -> run(a).map(function);
    }

    default @Nonnull <C> Kleisli<Either<A, C>, Either<B, C>, S> left() {
      return ac -> ac.either(a -> run(a).map(Either::left), c -> unit(Either.right(c)));
    }

    default @Nonnull <C> Kleisli<Either<C, A>, Either<C, B>, S> right() {
      return ca -> ca.either(c -> unit(Either.left(c)), a -> run(a).map(Either::right));
    }

    default @Nonnull <C> Kleisli<Pair<A, C>, Pair<B, C>, S> first() {
      return ac -> run(ac.first).map(b -> pair(b, ac.second));
    }

    default @Nonnull <C> Kleisli<Pair<C, A>, Pair<C, B>, S> second() {
      return ca -> run(ca.second).map(b -> pair(ca.first, b));
    }

    default @Nonnull <C, D> Kleisli<Either<A, C>, Either<B, D>, S> sum(final @Nonnull Kleisli<? super C, ? extends D, S> kleisli) {
      requireNonNull(kleisli);
      return ac -> ac.either(a -> run(a).map(Either::left), c -> kleisli.run(c).map(Either::right));
    }

    default @Nonnull <C, D> Kleisli<Pair<A, C>, Pair<B, D>, S> product(final @Nonnull Kleisli<? super C, ? extends D, S> kleisli) {
      requireNonNull(kleisli);
      return ac -> run(ac.first).apply(kleisli.run(ac.second).map(d -> b -> pair(b, d)));
    }

    default @Nonnull <C> Kleisli<Either<A, C>, B, S> fanIn(final @Nonnull Kleisli<? super C, B, S> kleisli) {
      requireNonNull(kleisli);
      return ac -> ac.either(Kleisli.this::run, kleisli::run);
    }

    default @Nonnull <C> Kleisli<A, Pair<B, C>, S> fanOut(final @Nonnull Kleisli<? super A, ? extends C, S> kleisli) {
      requireNonNull(kleisli);
      return a -> run(a).apply(kleisli.run(a).map(c -> b -> pair(b, c)));
    }

    static @Nonnull <A, B, S> Kleisli<A, B, S> lift(final @Nonnull Function<? super A, ? extends B> function) {
      requireNonNull(function);
      return a -> unit(function.$(a));
    }

    static @Nonnull <A, S> Kleisli<A, A, S> id() {
      return State::unit;
    }

    static @Nonnull <A, B, S> Kleisli<Pair<Kleisli<A, B, S>, A>, B, S> apply() {
      return ka -> ka.first.run(ka.second);
    }
  }
}
