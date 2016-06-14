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

import static io.github.kurobako.futon.Function.constant;
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
    return bind(a -> state.bind(f -> state(f.$(a))));
  }

  default @Nonnull <B> State<B, S> map(final @Nonnull Function<? super A, ? extends B> function) {
    requireNonNull(function);
    return bind(a -> state(function.$(a)));
  }

  static @Nonnull <A, S> State<A, S> join(final @Nonnull State<? extends State<A, S>, S> state) {
    requireNonNull(state);
    return state.bind(id());
  }

  static @Nonnull <A, S> State<A, S> state(final A value) {
    return s -> pair(value, s);
  }

  static @Nonnull <S> State<S, S> get() {
    return s -> pair(s, s);
  }

  static @Nonnull <A, S> State<A, S> getState(final @Nonnull Function<? super S, ? extends A> function) {
    requireNonNull(function);
    return s -> pair(function.$(s), s);
  }

  static @Nonnull <S> State<Unit, S> put(final S state) {
    return modify(constant(state));
  }

  static @Nonnull <S> State<Unit, S> modify(final Function<? super S, ? extends S> function) {
    requireNonNull(function);
    return s -> pair(Unit.INSTANCE, function.$(s));
  }
}
