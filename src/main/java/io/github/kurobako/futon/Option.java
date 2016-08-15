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

public abstract class Option<A> implements Foldable<A>, Iterable<A> {
  Option() {}

  public abstract A asNullable();

  public abstract @Nonnull <B> Option<B> bind(@Nonnull Function<? super A, ? extends Option<B>> function);

  public abstract @Nonnull <B, C> Option<C> zip(@Nonnull Option<B> option, @Nonnull BiFunction<? super A, ? super B, ? extends C> biFunction);

  public abstract @Nonnull <B, C> Pair<? extends Option<B>, ? extends Option<C>> unzip(@Nonnull Function<? super A, Pair<B, C>> function);

  public abstract @Nonnull <B> Option<B> apply(@Nonnull Option<? extends Function<? super A, ? extends B>> option);

  public abstract @Nonnull <B> Option<B> map(@Nonnull Function<? super A, ? extends B> function);

  public abstract @Nonnull Option<A> filter(@Nonnull Predicate<A> predicate);

  public abstract @Nonnull Option<Some<A>> caseSome();

  public abstract @Nonnull Option<None<A>> caseNone();

  public abstract boolean isSome();

  public abstract boolean isNone();

  public static @Nonnull <A> Option<A> join(final @Nonnull Option<? extends Option<A>> option) {
    return option.bind(id());
  }

  public static @Nonnull <A> Some<A> some(final @Nonnull A value) {
    requireNonNull(value);
    return new Some<>(value);
  }

  @SuppressWarnings("unchecked")
  public static @Nonnull <A> None<A> none() {
    return (None<A>) None.INSTANCE;
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
    public @Nonnull <B> Option<B> bind(final @Nonnull Function<? super A, ? extends Option<B>> function) {
      requireNonNull(function);
      return function.$(value);
    }

    @Override
    public @Nonnull <B, C> Option<C> zip(final @Nonnull Option<B> option, final @Nonnull BiFunction<? super A, ? super B, ? extends C> biFunction) {
      requireNonNull(option);
      requireNonNull(biFunction);
      return option.map(b -> biFunction.$(value, b));
    }

    @Override
    public @Nonnull <B, C> Pair<Some<B>, Some<C>> unzip(final @Nonnull Function<? super A, Pair<B, C>> function) {
      requireNonNull(function);
      Pair<? extends B, ? extends C> bc = function.$(value);
      return pair(some(bc.first), some(bc.second));
    }

    @Override
    public @Nonnull <B> Option<B> apply(final @Nonnull Option<? extends Function<? super A, ? extends B>> option) {
      requireNonNull(option);
      return option.map(f -> f.$(value));
    }

    @Override
    public @Nonnull <B> Some<B> map(final @Nonnull Function<? super A, ? extends B> function) {
      requireNonNull(function);
      return some(function.$(value));
    }

    @Override
    public @Nonnull Option<A> filter(final @Nonnull Predicate<A> predicate) {
      requireNonNull(predicate);
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
      requireNonNull(biFunction);
      return biFunction.$(value, initial);
    }

    @Override
    public <B> B foldLeft(final @Nonnull BiFunction<? super B, ? super A, ? extends B> biFunction, final B initial) {
      requireNonNull(biFunction);
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
    public @Nonnull <B> None<B> bind(final @Nonnull Function<? super A, ? extends Option<B>> function) {
      requireNonNull(function);
      return none();
    }

    @Override
    public @Nonnull <B, C> None<C> zip(final @Nonnull Option<B> option, final @Nonnull BiFunction<? super A, ? super B, ? extends C> biFunction) {
      requireNonNull(option);
      requireNonNull(biFunction);
      return none();
    }

    @Override
    public @Nonnull <B, C> Pair<None<B>, None<C>> unzip(final @Nonnull Function<? super A, Pair<B, C>> function) {
      requireNonNull(function);
      return pair(none(), none());
    }

    @Override
    public @Nonnull <B> None<B> apply(final @Nonnull Option<? extends Function<? super A, ? extends B>> option) {
      requireNonNull(option);
      return none();
    }

    @Override
    public @Nonnull <B> None<B> map(final @Nonnull Function<? super A, ? extends B> function) {
      requireNonNull(function);
      return none();
    }

    @Override
    public @Nonnull None<A> filter(final @Nonnull Predicate<A> predicate) {
      requireNonNull(predicate);
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
      requireNonNull(biFunction);
      return initial;
    }

    @Override
    public <B> B foldLeft(final @Nonnull BiFunction<? super B, ? super A, ? extends B> biFunction, final B initial) {
      requireNonNull(biFunction);
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

  @FunctionalInterface
  interface Kleisli<A, B> {

    @Nonnull Option<B> run(A a);

    default @Nonnull <C> Kleisli<A, C> compose(final @Nonnull Kleisli<? super B, C> kleisli) {
      requireNonNull(kleisli);
      return a -> run(a).bind(kleisli::run);
    }

    default @Nonnull <C> Kleisli<A, C> compose(final @Nonnull Function<? super B, ? extends C> function) {
      requireNonNull(function);
      return a -> run(a).map(function);
    }

    default @Nonnull <C> Kleisli<Either<A, C>, Either<B, C>> left() {
      return ac -> ac.either(a -> run(a).map(Either::left), c -> some(Either.right(c)));
    }

    default @Nonnull <C> Kleisli<Either<C, A>, Either<C, B>> right() {
      return ca -> ca.either(c -> some(Either.left(c)), a -> run(a).map(Either::right));
    }

    default @Nonnull <C> Kleisli<Pair<A, C>, Pair<B, C>> first() {
      return ac -> run(ac.first).zip(some(ac.second), Pair::pair);
    }

    default @Nonnull <C> Kleisli<Pair<C, A>, Pair<C, B>> second() {
      return ca -> some(ca.first).zip(run(ca.second), Pair::pair);
    }

    default @Nonnull <C, D> Kleisli<Either<A, C>, Either<B, D>> sum(final @Nonnull Kleisli<? super C, ? extends D> kleisli) {
      requireNonNull(kleisli);
      return ac -> ac.either(a -> run(a).map(Either::left), c -> kleisli.run(c).map(Either::right));
    }

    default @Nonnull <C, D> Kleisli<Pair<A, C>, Pair<B, D>> product(final @Nonnull Kleisli<? super C, ? extends D> kleisli) {
      requireNonNull(kleisli);
      return ac -> run(ac.first).zip(kleisli.run(ac.second), Pair::pair);
    }

    default @Nonnull <C> Kleisli<Either<A, C>, B> fanIn(final @Nonnull Kleisli<? super C, B> kleisli) {
      requireNonNull(kleisli);
      return ac -> ac.either(Kleisli.this::run, kleisli::run);
    }

    default @Nonnull <C> Kleisli<A, Pair<B, C>> fanOut(final @Nonnull Kleisli<? super A, ? extends C> kleisli) {
      requireNonNull(kleisli);
      return a -> run(a).zip(kleisli.run(a), Pair::pair);
    }

    static @Nonnull <A, B> Kleisli<A, B> lift(final @Nonnull Function<? super A, ? extends B> function) {
      requireNonNull(function);
      return a -> some(function.$(a));
    }

    static @Nonnull <A> Kleisli<A, A> id() {
      return Option::some;
    }

    static @Nonnull <A, B> Kleisli<Pair<Kleisli<A, B>, A>, B> apply() {
      return ka -> ka.first.run(ka.second);
    }
  }
}
