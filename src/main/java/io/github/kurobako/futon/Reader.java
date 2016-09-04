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
import static io.github.kurobako.futon.Util.nonNull;

public interface Reader<E, A> {
  A run(E environment);

  default @Nonnull <F> Reader<F, A> local(final @Nonnull Function<? super F, ? extends E> function) {
    nonNull(function);
    return function.postcompose(this::run)::$;
  }

  default @Nonnull Reader<E, A> scope(final E environment) {
    return ignoredArg -> run(environment);
  }

  default @Nonnull <B> Reader<E, B> bind(final @Nonnull Function<? super A, ? extends Reader<E, B>> function) {
    nonNull(function);
    return e -> function.$(run(e)).run(e);
  }

  default @Nonnull <B> Reader<E, B> apply(final @Nonnull Reader<E, ? extends Function<? super A, ? extends B>> reader) {
    nonNull(reader);
    return e -> reader.run(e).$(run(e));
  }

  default @Nonnull <B> Reader<E, B> map(final @Nonnull Function<? super A, ? extends B> function) {
    nonNull(function);
    Function<E, A> run = this::run;
    return run.postcompose(function)::$;
  }

  static @Nonnull <E> Reader<E, E> ask() {
    return e -> e;
  }

  static @Nonnull <E, A> Reader<E, A> reader(final @Nonnull Function<? super E, ? extends A> function) {
    nonNull(function);
    return function::$;
  }

  static @Nonnull <E, A> Reader<E, A> join(final @Nonnull Reader<E, ? extends Reader<E, A>> reader) {
    return nonNull(reader).bind(id());
  }

  static @Nonnull <E, A> Reader<E, A> unit(final A value) {
    return ignoredArg -> value;
  }
}