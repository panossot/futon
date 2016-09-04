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

import static io.github.kurobako.futon.Pair.pair;
import static io.github.kurobako.futon.Util.nonNull;
import static io.github.kurobako.futon.Function.id;

public interface State<S, A> {
  @Nonnull Pair<S, A> run(S initial);

  default S exec(final S initial) {
    return run(initial).first;
  }

  default A eval(final S initial) {
    return run(initial).second;
  }

  default @Nonnull <B> State<S, B> bind(final @Nonnull Function<? super A, ? extends State<S, B>> function) {
    nonNull(function);
    return s -> {
      final Pair<S, A> sa = State.this.run(s);
      return function.$(sa.second).run(sa.first);
    };
  }

  default @Nonnull <B> State<S, B> apply(final @Nonnull State<S, ? extends Function<? super A, ? extends B>> state) {
    nonNull(state);
    return bind(a -> state.bind(f -> unit(f.$(a))));
  }

  default @Nonnull <B> State<S, B> map(final @Nonnull Function<? super A, ? extends B> function) {
    nonNull(function);
    return s -> {
      final Pair<S, A> sa = run(s);
      return pair(sa.first, function.$(sa.second));
    };
  }

  default @Nonnull State<S, A> with(final @Nonnull Function<? super S, ? extends S> function) {
    nonNull(function);
    return s -> {
      final Pair<S, A> sa = run(s);
      return pair(function.$(sa.first), sa.second);
    };
  }

  static @Nonnull <S> State<S, S> get() {
    return s -> pair(s, s);
  }

  static @Nonnull <S> State<S, Unit> put(final S s) {
    final Pair<S, Unit> su = pair(s, Unit.INSTANCE);
    return ignoredArg -> su;
  }

  static @Nonnull <S> State<S, Unit> modify(final @Nonnull Function<? super S, ? extends S> function) {
    nonNull(function);
    return s -> pair(function.$(s), Unit.INSTANCE);
  }

  static @Nonnull <S, T> State<S, T> gets(final @Nonnull Function<? super S, ? extends T> function) {
    nonNull(function);
    return s -> pair(s, function.$(s));
  }

  static @Nonnull <S extends InverseSemigroup<S>> State<S, S> delta(final @Nonnull S current) {
    nonNull(current);
    return s -> pair(current.inverse().append(s), current);
  }

  static @Nonnull <S, A> State<S, A> join(final @Nonnull State<S, ? extends State<S, A>> state) {
    return nonNull(state).bind(id());
  }

  static @Nonnull <S, A> State<S, A> unit(final A value) {
    return s -> pair(s, value);
  }
}
