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
import static io.github.kurobako.futon.Sequence.empty;
import static io.github.kurobako.futon.Sequence.singleton;
import static io.github.kurobako.futon.Util.nonNull;

public interface Writer<O, A> {
  @Nonnull Pair<Sequence<O>, A> run();

  default Sequence<O> exec() {
    return run().first;
  }

  default @Nonnull Writer<O, Pair<Sequence<O>, A>> listen() {
    final Pair<Sequence<O>, A> initial = run();
    final Pair<Sequence<O>, Pair<Sequence<O>, A>> result = pair(initial.first, initial);
    return () -> result;
  }

  default @Nonnull <P> Writer<O, Pair<Sequence<P>, A>> listens(final @Nonnull Function<? super O, ? extends P> function) {
    nonNull(function);
    final Pair<Sequence<O>, A> initial = run();
    final Pair<Sequence<O>, Pair<Sequence<P>, A>> result = pair(initial.first, pair(initial.first.map(function), initial.second));
    return () -> result;
  }

  default @Nonnull Writer<O, A> censor(final @Nonnull Function<? super O, ? extends O> function) {
    nonNull(function);
    final Pair<Sequence<O>, A> initial = run();
    final Pair<Sequence<O>, A> result = pair(initial.first.map(function), initial.second);
    return () -> result;
  }

  default @Nonnull <B> Writer<O, B> bind(final @Nonnull Function<? super A, ? extends Writer<O, B>> function) {
    nonNull(function);
    final Pair<Sequence<O>, A> initial = run();
    final Pair<Sequence<O>, B> returned = function.$(initial.second).run();
    final Pair<Sequence<O>, B> result = pair(initial.first.append(returned.first), returned.second);
    return () -> result;
  }

  default @Nonnull <B> Writer<O, B> apply(final @Nonnull Writer<O, ? extends Function<? super A, ? extends B>> writer) {
    nonNull(writer);
    final Pair<Sequence<O>, A> initial = run();
    final Pair<Sequence<O>, ? extends Function<? super A, ? extends B>> application = writer.run();
    final Pair<Sequence<O>, B> result = pair(application.first.append(initial.first), application.second.$(initial.second));
    return () -> result;
  }

  default @Nonnull <B> Writer<O, B> map(final @Nonnull Function<? super A, ? extends B> function) {
    nonNull(function);
    final Pair<Sequence<O>, A> initial = run();
    final Pair<Sequence<O>, B> result = pair(initial.first, function.$(initial.second));
    return () -> result;
  }

  static @Nonnull <O, A> Writer<O, A> pass(final @Nonnull Writer<O, Pair<A, ? extends Function<? super O, ? extends O>>> writer) {
    nonNull(writer);
    final Pair<Sequence<O>, Pair<A, ? extends Function<? super O, ? extends O>>> initial = writer.run();
    final Pair<Sequence<O>, A> result = pair(initial.first.map(initial.second.second), initial.second.first);
    return () -> result;
  }

  static @Nonnull <O> Writer<O, Unit> tell(final @Nonnull Sequence<O> output) {
    return writer(output, Unit.INSTANCE);
  }

  static @Nonnull <O> Writer<O, Unit> tell(final O output) {
    return writer(output, Unit.INSTANCE);
  }

  static @Nonnull <O, A> Writer<O, A> writer(final @Nonnull Sequence<O> output, final A value) {
    final Pair<Sequence<O>, A> result = pair(nonNull(output), value);
    return () -> result;
  }

  static @Nonnull <O, A> Writer<O, A> writer(final @Nonnull O output, final A value) {
    return writer(singleton(output), value);
  }

  static @Nonnull <O, A> Writer<O, A> join(final @Nonnull Writer<O, ? extends Writer<O, A>> writer) {
    return nonNull(writer).bind(id());
  }

  static @Nonnull <O, A> Writer<O, A> unit(final A value) {
    return writer(empty(), value);
  }
}
