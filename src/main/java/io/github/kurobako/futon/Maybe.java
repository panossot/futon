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
import static java.util.Objects.requireNonNull;

public interface Maybe<A> extends Foldable<A>, Iterable<A> {

  A asNullable();

  @Nonnull Maybe<? extends Maybe<A>> duplicate();

  @Nonnull <B> Maybe<B> extend(final @Nonnull Function<? super Maybe<A>, ? extends B> function);

  @Nonnull <B> Maybe<B> bind(@Nonnull Function<? super A, ? extends Maybe<B>> function);

  @Nonnull <B, C> Maybe<C> zip(@Nonnull Maybe<B> maybe, @Nonnull BiFunction<? super A, ? super B, ? extends C> function);

  @Nonnull <B, C> Pair<? extends Maybe<B>, ? extends Maybe<C>> unzip(@Nonnull Function<? super A, Pair<? extends B, ? extends C>> function);

  @Nonnull <B> Maybe<B> apply(@Nonnull Maybe<? extends Function<? super A, ? extends B>> maybe);

  @Nonnull <B> Maybe<B> map(@Nonnull Function<? super A, ? extends B> function);

  @Nonnull Maybe<A> filter(@Nonnull Predicate<? super A> predicate);

  static @Nonnull <A> Maybe<A> join(final @Nonnull Maybe<? extends Maybe<A>> maybe) {
    requireNonNull(maybe);
    return maybe.bind(id());
  }

  static @Nonnull <A> Just<A> just(final @Nonnull A value) {
    requireNonNull(value);
    return new Just<A>() {
      @Override
      public @Nonnull Just<Just<A>> duplicate() {
        return just(this);
      }

      public @Nonnull <B> Just<B> extend(final @Nonnull Function<? super Maybe<A>, ? extends B> function) {
        requireNonNull(function);
        return just(function.$(this));
      }

      @Override
      public @Nonnull <B> Maybe<B> bind(final @Nonnull Function<? super A, ? extends Maybe<B>> function) {
        requireNonNull(function);
        return function.$(value);
      }

      @Override
      public @Nonnull <B, C> Maybe<C> zip(final @Nonnull Maybe<B> maybe, final @Nonnull BiFunction<? super A, ? super B, ? extends C> biFunction) {
        requireNonNull(maybe);
        requireNonNull(biFunction);
        return maybe.map(b -> biFunction.$(value, b));
      }

      @Override
      public @Nonnull <B> Maybe<B> apply(final @Nonnull Maybe<? extends Function<? super A, ? extends B>> maybe) {
        requireNonNull(maybe);
        return maybe.map(f -> f.$(value));
      }

      @Override
      public @Nonnull <B, C> Pair<? extends Just<B>, ? extends Just<C>> unzip(final @Nonnull Function<? super A, Pair<? extends B, ? extends C>> function) {
        requireNonNull(function);
        Pair<? extends B, ? extends C> bc = function.$(value);
        return pair(just(bc.first), just(bc.second));
      }

      @Override
      public @Nonnull <B> Just<B> map(final @Nonnull Function<? super A, ? extends B> function) {
        requireNonNull(function);
        return just(function.$(value));
      }

      @Override
      public @Nonnull Maybe<A> filter(final @Nonnull Predicate<? super A> predicate) {
        requireNonNull(predicate);
        return predicate.$(value) ? this : nothing();
      }

      @Override
      public @Nonnull A asNullable() {
        return value;
      }

      @Override
      public <B> B foldRight(final @Nonnull BiFunction<? super A, ? super B, ? extends B> biFunction, final B initial) {
        requireNonNull(biFunction);
        return biFunction.$(value, initial);
      }

      @Override
      public <B> B foldLeft(@Nonnull BiFunction<? super B, ? super A, ? extends B> biFunction, final B initial) {
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
      public boolean equals(Object obj) {
        if (!(obj instanceof Maybe)) return false;
        final Maybe that = (Maybe) obj;
        return value.equals(that.asNullable());
      }

      @Override
      public @Nonnull String toString() {
        return "Just " + value.toString();
      }
    };
  }

  @SuppressWarnings("unchecked")
  static @Nonnull <A> Nothing<A> nothing() {
    return (Nothing<A>) Nothing.INSTANCE;
  }

  static @Nonnull <A> Maybe<A> fromNullable(final A value) {
    return value == null ? nothing() : just(value);
  }

  interface Just<A> extends Maybe<A> {
    @Nonnull Just<Just<A>> duplicate();

    @Nonnull <B> Just<B> extend(final @Nonnull Function<? super Maybe<A>, ? extends B> function);

    @Override
    @Nonnull <B, C> Pair<? extends Just<B>, ? extends Just<C>> unzip(@Nonnull Function<? super A, Pair<? extends B, ? extends C>> function);

    @Nonnull <B> Just<B> map(final @Nonnull Function<? super A, ? extends B> function);

    @Nonnull A asNullable();
  }

  interface Nothing<A> extends Maybe<A> {
    Nothing<Object> INSTANCE = new Nothing<Object>() {
      @Override
      public @Nonnull String toString() {
        return "Nothing";
      }
    };

    @Override
    default @Nonnull Nothing<Nothing<A>> duplicate() {
      return nothing();
    }

    @Override
    default @Nonnull <B> Maybe<B> extend(@Nonnull final Function<? super Maybe<A>, ? extends B> function) {
      requireNonNull(function);
      return nothing();
    }

    @Override
    default @Nonnull <B> Nothing<B> bind(final @Nonnull Function<? super A, ? extends Maybe<B>> function) {
      requireNonNull(function);
      return nothing();
    }

    @Override
    default @Nonnull <B, C> Nothing<C> zip(final @Nonnull Maybe<B> maybe, final @Nonnull BiFunction<? super A, ? super B, ? extends C> biFunction) {
      requireNonNull(maybe);
      requireNonNull(biFunction);
      return nothing();
    }

    @Override
    default @Nonnull <B, C> Pair<? extends Nothing<B>, ? extends Nothing<C>> unzip(final @Nonnull Function<? super A, Pair<? extends B, ? extends C>> function) {
      requireNonNull(function);
      return pair(nothing(), nothing());
    }

    @Override
    default @Nonnull <B> Nothing<B> apply(final @Nonnull Maybe<? extends Function<? super A, ? extends B>> maybe) {
      requireNonNull(maybe);
      return nothing();
    }

    @Override
    default @Nonnull <B> Nothing<B> map(final @Nonnull Function<? super A, ? extends B> function) {
      requireNonNull(function);
      return nothing();
    }

    @Override
    default @Nonnull Nothing<A> filter(final @Nonnull Predicate<? super A> predicate) {
      requireNonNull(predicate);
      return nothing();
    }

    @Override
    default A asNullable() {
      return null;
    }

    @Override
    default <B> B foldRight(final @Nonnull BiFunction<? super A, ? super B, ? extends B> biFunction, final B initial) {
      requireNonNull(biFunction);
      return initial;
    }

    @Override
    default <B> B foldLeft(final @Nonnull BiFunction<? super B, ? super A, ? extends B> biFunction, final B initial) {
      requireNonNull(biFunction);
      return initial;
    }

    @Override
    default @Nonnull Iterator<A> iterator() {
      return Collections.emptyIterator();
    }
  }
}