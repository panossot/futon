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
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;

import static io.github.kurobako.futon.Function.id;
import static io.github.kurobako.futon.Pair.pair;
import static io.github.kurobako.futon.Util.nonNull;

public abstract class Option<A> implements Foldable<A>, Iterable<A> {
  Option() {}

  public abstract A asNullable();

  public abstract @Nonnull <B, C> Option<C> zip(@Nonnull Option<B> option, @Nonnull BiFunction<? super A, ? super B, ? extends C> biFunction);

  public abstract @Nonnull <B, C> Pair<? extends Option<B>, ? extends Option<C>> unzip(@Nonnull Function<? super A, Pair<B, C>> function);

  public abstract @Nonnull <B> Option<B> bind(@Nonnull Function<? super A, ? extends Option<B>> function);

  public abstract @Nonnull <B> Option<B> apply(@Nonnull Option<? extends Function<? super A, ? extends B>> option);

  public abstract @Nonnull <B> Option<B> map(@Nonnull Function<? super A, ? extends B> function);

  public abstract @Nonnull Option<A> filter(@Nonnull Predicate<A> predicate);

  public abstract @Nonnull Option<Some<A>> caseSome();

  public abstract @Nonnull Option<None<A>> caseNone();

  public abstract boolean isSome();

  public abstract boolean isNone();

  public static @Nonnull <A> Some<A> some(final @Nonnull A value) {
    nonNull(value);
    return new Some<>(value);
  }

  @SuppressWarnings("unchecked")
  public static @Nonnull <A> None<A> none() {
    return (None<A>) None.INSTANCE;
  }

  public static @Nonnull <A> Option<A> join(final @Nonnull Option<? extends Option<A>> option) {
    return option.bind(id());
  }

  public static @Nonnull <A> Some<A> unit(final @Nonnull A value) {
    return some(value);
  }

  public static @Nonnull <A> Option<A> fromNullable(final A value) {
    return value == null ? none() : some(value);
  }

  public static final class Some<A> extends Option<A> {
    public final @Nonnull A value;

    private Some(final A value) {
      this.value = value;
    }

    @Override
    public @Nonnull A asNullable() {
      return value;
    }

    @Override
    public @Nonnull <B, C> Option<C> zip(final @Nonnull Option<B> option, final @Nonnull BiFunction<? super A, ? super B, ? extends C> biFunction) {
      nonNull(option);
      nonNull(biFunction);
      return option.map(b -> biFunction.$(value, b));
    }

    @Override
    public @Nonnull <B, C> Pair<Some<B>, Some<C>> unzip(final @Nonnull Function<? super A, Pair<B, C>> function) {
      nonNull(function);
      Pair<? extends B, ? extends C> bc = function.$(value);
      return pair(some(bc.first), some(bc.second));
    }

    @Override
    public @Nonnull <B> Option<B> bind(final @Nonnull Function<? super A, ? extends Option<B>> function) {
      nonNull(function);
      return function.$(value);
    }

    @Override
    public @Nonnull <B> Option<B> apply(final @Nonnull Option<? extends Function<? super A, ? extends B>> option) {
      nonNull(option);
      return option.map(f -> f.$(value));
    }

    @Override
    public @Nonnull <B> Some<B> map(final @Nonnull Function<? super A, ? extends B> function) {
      nonNull(function);
      return some(function.$(value));
    }

    @Override
    public @Nonnull Option<A> filter(final @Nonnull Predicate<A> predicate) {
      nonNull(predicate);
      return predicate.$(value) ? this : none();
    }

    @Override
    public @Nonnull Some<Some<A>> caseSome() {
      return some(this);
    }

    @Override
    public @Nonnull None<None<A>> caseNone() {
      return none();
    }

    @Override
    public boolean isSome() {
      return true;
    }

    @Override
    public boolean isNone() {
      return false;
    }

    @Override
    public <B> B foldRight(final @Nonnull BiFunction<? super A, ? super B, ? extends B> biFunction, final B initial) {
      nonNull(biFunction);
      return biFunction.$(value, initial);
    }

    @Override
    public <B> B foldLeft(final @Nonnull BiFunction<? super B, ? super A, ? extends B> biFunction, final B initial) {
      nonNull(biFunction);
      return biFunction.$(initial, value);
    }

    @Override
    public @Nonnull Iterator<A> iterator() {
      return new Iterator<A>() {
        private boolean consumed;

        @Override
        public boolean hasNext() {
          return !consumed;
        }

        @Override
        public A next() {
          if (consumed) throw new NoSuchElementException();
          consumed = true;
          return value;
        }
      };
    }

    @Override
    public int hashCode() {
      return value.hashCode();
    }

    @Override
    public boolean equals(final Object o) {
      if (!(o instanceof Some)) return false;
      Some that = (Some) o;
      return this.value.equals(that.value);
    }

    @Override
    public @Nonnull String toString() {
      return "Just " + value.toString();
    }
  }

  public static final class None<A> extends Option<A> {
    private None() {}

    @Override
    public A asNullable() {
      return null;
    }

    @Override
    public @Nonnull <B, C> None<C> zip(final @Nonnull Option<B> option, final @Nonnull BiFunction<? super A, ? super B, ? extends C> biFunction) {
      nonNull(option);
      nonNull(biFunction);
      return none();
    }

    @Override
    public @Nonnull <B, C> Pair<None<B>, None<C>> unzip(final @Nonnull Function<? super A, Pair<B, C>> function) {
      nonNull(function);
      return pair(none(), none());
    }

    @Override
    public @Nonnull <B> None<B> bind(final @Nonnull Function<? super A, ? extends Option<B>> function) {
      nonNull(function);
      return none();
    }

    @Override
    public @Nonnull <B> None<B> apply(final @Nonnull Option<? extends Function<? super A, ? extends B>> option) {
      nonNull(option);
      return none();
    }

    @Override
    public @Nonnull <B> None<B> map(final @Nonnull Function<? super A, ? extends B> function) {
      nonNull(function);
      return none();
    }

    @Override
    public @Nonnull None<A> filter(final @Nonnull Predicate<A> predicate) {
      nonNull(predicate);
      return none();
    }

    @Override
    public @Nonnull None<Some<A>> caseSome() {
      return none();
    }

    @Override
    public @Nonnull Some<None<A>> caseNone() {
      return some(none());
    }

    @Override
    public boolean isSome() {
      return false;
    }

    @Override
    public boolean isNone() {
      return true;
    }

    @Override
    public <B> B foldRight(final @Nonnull BiFunction<? super A, ? super B, ? extends B> biFunction, final B initial) {
      nonNull(biFunction);
      return initial;
    }

    @Override
    public <B> B foldLeft(final @Nonnull BiFunction<? super B, ? super A, ? extends B> biFunction, final B initial) {
      nonNull(biFunction);
      return initial;
    }

    @Override
    public @Nonnull Iterator<A> iterator() {
      return Collections.emptyIterator();
    }

    @Override
    public @Nonnull String toString() {
      return "None";
    }

    private static final None<Object> INSTANCE = new None<>();
  }
}
