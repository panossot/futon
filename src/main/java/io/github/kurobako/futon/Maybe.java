/*
 * Copyright (C) 2015 Fedor Gavrilov
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

public interface Maybe<A> extends Functor<A>, Foldable<A> {
  default  @Nonnull <B> Maybe<B> bind(final @Nonnull Function<? super A, ? extends Maybe<B>> function) {
    requireNonNull(function, "function");
    return function.$(value());
  }

  default @Nonnull <B, C> Maybe<C> zip(final @Nonnull Maybe<B> another,
                                       final @Nonnull BiFunction<? super A, ? super B, ? extends Maybe<C>> function) {
    requireNonNull(another, "another");
    requireNonNull(function, "function");
    return function.$(this.value(), another.value());
  }

  default  @Nonnull Maybe<A> filter(final @Nonnull Predicate<? super A> predicate) {
    requireNonNull(predicate, "predicate");
    return predicate.$(value()) ? this : Maybe.nothing();
  }

  @Override
  default  <B> B foldRight(final @Nonnull BiFunction<A, B, B> function, B initial) {
    requireNonNull(function, "function");
    return function.$(value(), initial);
  }

  @Override
  default  <B> B foldLeft(final @Nonnull BiFunction<B, A, B> function, B initial) {
    requireNonNull(function, "function");
    return function.$(initial, value());
  }

  default @Nonnull <B> Maybe<Pair<A, B>> and(final @Nonnull Maybe<B> another) {
    requireNonNull(another, "another");
    if (this.isJust() && another.isJust()) return just(pair(this.value(), another.value()));
    return nothing();
  }

  default @Nonnull <B> Maybe<Pair<A, B>> or(final @Nonnull Maybe<B> another) {
    requireNonNull(another, "another");
    if (this.isJust() || another.isJust()) return just(pair(this.value(), another.value()));
    return nothing();
  }

  default @Nonnull <B> Maybe<Either<A, B>> xor(final @Nonnull Maybe<B> another) {
    requireNonNull(another, "another");
    if (this.isJust() && another.isNothing()) return just(Either.left(this.value()));
    if (this.isNothing() && another.isJust()) return just(Either.right(another.value()));
    return nothing();
  }

  boolean isJust();

  boolean isNothing();

  A value();

  @Override
  default  @Nonnull <B> Maybe<B> map(final @Nonnull Function<? super A, ? extends B> function) {
    requireNonNull(function, "function");
    return Maybe.just(function.$(value()));
  }

  static @Nonnull <A> Maybe<A> just(final @Nonnull A value) {
    requireNonNull(value, "value");
    return new Maybe$Just<>(value);
  }

  @SuppressWarnings("unchecked")
  static @Nonnull <A> Maybe<A> nothing() {
    return Maybe$Nothing.INSTANCE;
  }

  static @Nonnull <A> Maybe<A> join(final @Nonnull Maybe<? extends Maybe<A>> wrapper) {
    requireNonNull(wrapper, "wrapper");
    return wrapper.bind(id());
  }
}

final class Maybe$Just<A> implements Maybe<A> {
  private final @Nonnull A value;

  Maybe$Just(A value) {
    assert value != null;
    this.value = value;
  }

  @Override
  public boolean isJust() {
    return true;
  }

  @Override
  public boolean isNothing() {
    return false;
  }

  @Override
  public A value() {
    return value;
  }

  @Override
  public int hashCode() {
    return value.hashCode();
  }

  @Override
  public boolean equals(final Object o) {
    if (!(o instanceof Maybe$Just)) return false;
    Maybe$Just that = (Maybe$Just) o;
    return this.value.equals(that.value);
  }

  @Override
  public String toString() {
    return "Just " + value;
  }
}

enum Maybe$Nothing implements Maybe {
  INSTANCE;

  @Override
  public @Nonnull Maybe bind(final @Nonnull Function function) {
    requireNonNull(function, "function");
    return this;
  }

  @Override
  public @Nonnull Maybe map(final @Nonnull Function function) {
    requireNonNull(function, "function");
    return this;
  }

  @Override
  public @Nonnull  Maybe filter(final @Nonnull Predicate predicate) {
    requireNonNull(predicate, "predicate");
    return this;
  }

  @Override
  public boolean isJust() {
    return false;
  }

  @Override
  public boolean isNothing() {
    return true;
  }

  @Override
  public Object value() {
    return null;
  }

  @Override
  public Object foldRight(final @Nonnull BiFunction function, Object initial) {
    requireNonNull(function, "function");
    return initial;
  }

  @Override
  public Object foldLeft(final @Nonnull BiFunction function, Object initial) {
    requireNonNull(function, "function");
    return initial;
  }

  @Override
  public String toString() {
    return "Nothing";
  }
}