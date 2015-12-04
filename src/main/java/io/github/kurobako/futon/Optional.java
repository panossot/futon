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

public interface Optional<A> extends Functor<A>, Foldable<A> {
  @Nonnull <B> Optional<B> bind(@Nonnull Function<? super A, ? extends Optional<B>> function);

  @Nonnull <B> Optional<B> apply(@Nonnull Optional<? extends Function<? super A, ? extends B>> transformation);

  @Nonnull
  Optional<A> filter(@Nonnull Predicate<? super A> predicate);

  default @Nonnull <B> Optional<Pair<A, B>> and(final @Nonnull Optional<B> another) {
    requireNonNull(another, "another");
    if (this.isSome() && another.isSome()) return some(pair(this.value(), another.value()));
    return none();
  }

  default @Nonnull <B> Optional<Pair<A, B>> or(final @Nonnull Optional<B> another) {
    requireNonNull(another, "another");
    if (this.isSome() || another.isSome()) return some(pair(this.value(), another.value()));
    return none();
  }

  default @Nonnull <B> Optional<Either<A, B>> xor(final @Nonnull Optional<B> another) {
    requireNonNull(another, "another");
    if (this.isSome() && another.isNone()) return some(Either.left(this.value()));
    if (this.isNone() && another.isSome()) return some(Either.right(another.value()));
    return none();
  }

  boolean isSome();

  boolean isNone();

  A value();

  @Override
  @Nonnull <B> Optional<B> map(@Nonnull Function<? super A, ? extends B> function);

  @Override
  int hashCode();

  @Override
  boolean equals(Object o);

  static @Nonnull <A> Optional<A> some(final @Nonnull A value) {
    requireNonNull(value, "value");
    return new Optional$Some<>(value);
  }

  @SuppressWarnings("unchecked")
  static @Nonnull <A> Optional<A> none() {
    return Optional$None.INSTANCE;
  }

  static @Nonnull <A> Optional<A> join(final @Nonnull Optional<? extends Optional<A>> wrapper) {
    requireNonNull(wrapper, "wrapper");
    return wrapper.bind(id());
  }
}

final class Optional$Some<A> implements Optional<A> {
  private final @Nonnull A value;

  Optional$Some(A value) {
    assert value != null;
    this.value = value;
  }

  @Override
  public @Nonnull <B> Optional<B> bind(final @Nonnull Function<? super A, ? extends Optional<B>> function) {
    requireNonNull(function, "function");
    return function.$(value());
  }

  @Override
  public @Nonnull <B> Optional<B> apply(final @Nonnull Optional<? extends Function<? super A, ? extends B>> transformation) {
    requireNonNull(transformation, "transformation");
    if (transformation.isNone()) return Optional.none();
    else return Optional.some(transformation.value().$(value()));
  }

  @Override
  public @Nonnull
  Optional<A> filter(final @Nonnull Predicate<? super A> predicate) {
    requireNonNull(predicate, "predicate");
    return predicate.$(value()) ? this : Optional.none();
  }

  @Override
  public @Nonnull <B> Optional<B> map(final @Nonnull Function<? super A, ? extends B> function) {
    requireNonNull(function, "function");
    return Optional.some(function.$(value()));
  }

  @Override
  public <B> B foldRight(final @Nonnull BiFunction<A, B, B> function, B initial) {
    requireNonNull(function, "function");
    return function.$(value(), initial);
  }

  @Override
  public <B> B foldLeft(final @Nonnull BiFunction<B, A, B> function, B initial) {
    requireNonNull(function, "function");
    return function.$(initial, value());
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
  public @Nonnull A value() {
    return value;
  }

  @Override
  public int hashCode() {
    return value.hashCode();
  }

  @Override
  public boolean equals(final Object o) {
    if (!(o instanceof Optional)) return false;
    Optional that = (Optional) o;
    return value.equals(that.value());
  }

  @Override
  public String toString() {
    return "Some " + value;
  }
}

final class Optional$None implements Optional {
  private Optional$None() {}

  @Override
  public @Nonnull
  Optional bind(final @Nonnull Function function) {
    requireNonNull(function, "function");
    return this;
  }

  @Override
  public @Nonnull
  Optional apply(final @Nonnull Optional transformation) {
    requireNonNull(transformation, "transformation");
    return this;
  }

  @Override
  public @Nonnull
  Optional filter(final @Nonnull Predicate predicate) {
    requireNonNull(predicate, "predicate");
    return this;
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
  public Object value() {
    return null;
  }

  @Override
  public @Nonnull
  Optional map(final @Nonnull Function function) {
    requireNonNull(function, "function");
    return this;
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
  public int hashCode() {
    return 0;
  }

  @Override
  public boolean equals(final Object o) {
    if (!(o instanceof Optional)) return false;
    Optional that = (Optional) o;
    return that.value() == null;
  }

  @Override
  public String toString() {
    return "None";
  }

  static final Optional$None INSTANCE = new Optional$None();
}