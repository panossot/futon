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
import java.util.Objects;
import java.util.StringJoiner;

import static io.github.kurobako.futon.Function.id;
import static io.github.kurobako.futon.Maybe.*;
import static io.github.kurobako.futon.Pair.pair;
import static java.util.Objects.requireNonNull;

public interface List<A> extends Foldable<A>, Iterable<A> {

  default @Nonnull Cons<A> cons(final A value) {
    return new ConsList<>(this, value);
  }

  @Nonnull List<A> append(@Nonnull List<A> list);

  @Nonnull List<A> take(int position);

  @Nonnull List<A> drop(int position);

  @Nonnull Pair<? extends List<A>, ? extends List<A>> splitAt(int position);

  @Nonnull List<A> takeWhile(@Nonnull Predicate<? super A> predicate);

  @Nonnull List<A> dropWhile(@Nonnull Predicate<? super A> predicate);

  @Nonnull Pair<? extends List<A>, ? extends List<A>> span(@Nonnull Predicate<? super A> predicate);

  @Nonnull List<A> reverse();

  int lookup(A value);

  int length();

  @Nonnull <B> List<B> bind(@Nonnull Function<? super A, List<B>> function);

  @Nonnull <B, C> List<C> zip(@Nonnull List<B> list, @Nonnull BiFunction<? super A, ? super B, ? extends C> function);

  @Nonnull <B, C> Pair<? extends List<B>, ? extends List<C>> unzip(@Nonnull Function<? super A, Pair<? extends B, ? extends C>> function);

  @Nonnull <B> List<B> apply(@Nonnull List<? extends Function<? super A, ? extends B>> list);

  @Nonnull <B> List<B> map(@Nonnull Function<? super A, ? extends B> function);

  @Nonnull List<A> filter(@Nonnull Predicate<? super A> predicate);

  @Nonnull <B> Cons<B> scanRight(@Nonnull BiFunction<? super A, ? super B, ? extends B> biFunction, B initial);

  @Nonnull <B> Cons<B> scanLeft(@Nonnull BiFunction<? super B, ? super A, ? extends B> biFunction, B initial);

  @Nonnull Maybe<Cons<A>> caseCons();

  @Nonnull Maybe<Nil<A>> caseNil();

  static @Nonnull <A> List<A> join(final @Nonnull List<? extends List<A>> list) {
    requireNonNull(list);
    return list.bind(id());
  }

  @SuppressWarnings("unchecked")
  static @Nonnull <A> Nil<A> nil() {
    return (Nil<A>) Nil.INSTANCE;
  }

  static @Nonnull <A> Cons<A> list(final A value) {
    return new ConsList<>(nil(), value);
  }

  static @Nonnull <A> List<A> list(final A... values) {
    List<A> result = nil();
    for (int i = values.length-1; i >= 0; i--) {
      result = result.cons(values[i]);
    }
    return result;
  }

  interface Cons<A> extends List<A> {
    A head();

    @Nonnull List<A> tail();

    @Override
    @Nonnull Cons<A> append(@Nonnull List<A> list);

    @Override
    @Nonnull Cons<A> reverse();

    @Override
    @Nonnull <B, C> Pair<? extends Cons<B>, ? extends Cons<C>> unzip(@Nonnull Function<? super A, Pair<? extends B, ? extends C>> function);

    @Override
    @Nonnull <B> Cons<B> map(@Nonnull Function<? super A, ? extends B> function);

    @Override
    @Nonnull Just<Cons<A>> caseCons();

    @Override
    @Nonnull Nothing<Nil<A>> caseNil();
  }

  interface Nil<A> extends List<A> {
    Nil<Object> INSTANCE = new Nil<Object>() {
      private final Just<Nil<Object>> JUST_NIL = just(this);
      private final Pair<Nil<Object>, Nil<Object>> NILS = pair(this, this);

      @Override
      public @Nonnull Nil<Object> take(final int position) {
        if (position < 0) throw new IllegalArgumentException();
        return this;
      }

      @Override
      public @Nonnull  Nil<Object> drop(final int position) {
        if (position < 0) throw new IllegalArgumentException();
        return this;
      }

      @Override
      public @Nonnull Pair<? extends Nil<Object>, ? extends Nil<Object>> splitAt(final int position) {
        if (position < 0) throw new IllegalArgumentException();
        return NILS;
      }

      @Override
      public @Nonnull Nil<Object> takeWhile(final @Nonnull Predicate<? super Object> predicate) {
        requireNonNull(predicate);
        return this;
      }

      @Override
      public @Nonnull Nil<Object> dropWhile(final @Nonnull Predicate<? super Object> predicate) {
        requireNonNull(predicate);
        return this;
      }

      @Override
      public @Nonnull Pair<? extends Nil<Object>, ? extends Nil<Object>> span(final @Nonnull Predicate<? super Object> predicate) {
        requireNonNull(predicate);
        return NILS;
      }

      @Override
      public @Nonnull Nil<Object> reverse() {
        return this;
      }

      @Override
      public int lookup(final Object value) {
        return -1;
      }

      @Override
      public int length() {
        return 0;
      }

      @Override
      public @Nonnull <B> Nil<B> bind(final @Nonnull Function<? super Object, List<B>> function) {
        requireNonNull(function);
        return nil();
      }

      @Override
      public @Nonnull <B, C> Nil<C> zip(final @Nonnull List<B> list, final @Nonnull BiFunction<? super Object, ? super B, ? extends C> function) {
        requireNonNull(list);
        requireNonNull(function);
        return nil();
      }

      @Override
      @SuppressWarnings("unchecked")
      public @Nonnull <B, C> Pair<? extends Nil<B>, ? extends Nil<C>> unzip(final @Nonnull Function<? super Object, Pair<? extends B, ? extends C>> function) {
        requireNonNull(function);
        return (Pair<? extends Nil<B>, ? extends Nil<C>>) NILS;
      }

      @Override
      public @Nonnull <B> Nil<B> apply(final @Nonnull List<? extends Function<? super Object, ? extends B>> list) {
        requireNonNull(list);
        return nil();
      }

      @Override
      public @Nonnull <B> Nil<B> map(final @Nonnull Function<? super Object, ? extends B> function) {
        requireNonNull(function);
        return nil();
      }

      @Override
      public @Nonnull Nil<Object> filter(final @Nonnull Predicate<? super Object> predicate) {
        requireNonNull(predicate);
        return this;
      }

      @Override
      public @Nonnull Nothing<Cons<Object>> caseCons() {
        return nothing();
      }

      @Override
      public @Nonnull Just<Nil<Object>> caseNil() {
        return JUST_NIL;
      }

      @Override
      public @Nonnull List<Object> append(final @Nonnull List<Object> list) {
        requireNonNull(list);
        return list;
      }

      @Override
      public @Nonnull <B> Cons<B> scanRight(final @Nonnull BiFunction<? super Object, ? super B, ? extends B> biFunction, final B initial) {
        requireNonNull(biFunction);
        return List.list(initial);
      }

      @Override
      public @Nonnull <B> Cons<B> scanLeft(final @Nonnull BiFunction<? super B, ? super Object, ? extends B> biFunction, final B initial) {
        requireNonNull(biFunction);
        return List.list(initial);
      }

      @Override
      public <B> B foldRight(final @Nonnull BiFunction<? super Object, ? super B, ? extends B> biFunction, final B initial) {
        requireNonNull(biFunction);
        return initial;
      }

      @Override
      public <B> B foldLeft(final @Nonnull BiFunction<? super B, ? super Object, ? extends B> biFunction, final B initial) {
        requireNonNull(biFunction);
        return initial;
      }

      @Override
      public @Nonnull Iterator<Object> iterator() {
        return Collections.emptyIterator();
      }

      @Override
      public @Nonnull String toString() {
        return "Nil";
      }
    };

    @Nonnull Nil<A> take(int position);

    @Nonnull Nil<A> drop(int position);

    @Nonnull Pair<? extends Nil<A>, ? extends Nil<A>> splitAt(int position);

    @Nonnull Nil<A> takeWhile(@Nonnull Predicate<? super A> predicate);

    @Nonnull Nil<A> dropWhile(@Nonnull Predicate<? super A> predicate);

    @Nonnull Pair<? extends Nil<A>, ? extends Nil<A>> span(@Nonnull Predicate<? super A> predicate);

    @Nonnull Nil<A> reverse();

    int lookup(A value);

    int length();

    @Nonnull <B> Nil<B> bind(@Nonnull Function<? super A, List<B>> function);

    @Nonnull <B, C> Nil<C> zip(@Nonnull List<B> list, @Nonnull BiFunction<? super A, ? super B, ? extends C> function);

    @Nonnull <B, C> Pair<? extends Nil<B>, ? extends Nil<C>> unzip(@Nonnull Function<? super A, Pair<? extends B, ? extends C>> function);

    @Nonnull <B> Nil<B> apply(@Nonnull List<? extends Function<? super A, ? extends B>> list);

    @Nonnull <B> Nil<B> map(@Nonnull Function<? super A, ? extends B> function);

    @Nonnull Nil<A> filter(@Nonnull Predicate<? super A> predicate);

    @Nonnull Nothing<Cons<A>> caseCons();

    @Nonnull Just<Nil<A>> caseNil();
  }
}

final class ConsList<A> implements List.Cons<A> {
  private static final int MAX_SIZE = Integer.MAX_VALUE - 8;

  private final A head;
  private final List<A> tail;
  private final int length;

  ConsList(List<A> tail, A head) {
    assert tail != null;
    this.head = head;
    this.tail = tail;
    this.length = tail.length() + 1;
    if (length > MAX_SIZE) throw new IllegalArgumentException();
  }

  @Override
  public A head() {
    return head;
  }

  @Override
  public @Nonnull List<A> tail() {
    return tail;
  }

  @Override
  public @Nonnull Cons<A> append(final @Nonnull List<A> list) {
    requireNonNull(list);
    return null; // TODO
  }

  @Override
  public @Nonnull List<A> take(final int position) {
    if (position < 0) throw new IllegalArgumentException();
    return null; // TODO
  }

  @Override
  public @Nonnull List<A> drop(final int position) {
    if (position < 0) throw new IllegalArgumentException();
    return null; // TODO
  }

  @Override
  public @Nonnull Pair<? extends List<A>, ? extends List<A>> splitAt(final int position) {
    if (position < 0) throw new IllegalArgumentException();
    return null; // TODO
  }

  @Override
  public @Nonnull List<A> takeWhile(final @Nonnull Predicate<? super A> predicate) {
    requireNonNull(predicate);
    return null; // TODO
  }

  @Override
  public @Nonnull List<A> dropWhile(final @Nonnull Predicate<? super A> predicate) {
    requireNonNull(predicate);
    return null; // TODO
  }

  @Override
  public @Nonnull Pair<? extends List<A>, ? extends List<A>> span(final @Nonnull Predicate<? super A> predicate) {
    requireNonNull(predicate);
    return null; // TODO
  }

  @Override
  public @Nonnull Cons<A> reverse() {
    List<A> result = foldLeft((BiFunction<List<A>, A, List<A>>) List::cons, List.nil());
    assert result instanceof Cons;
    return (Cons<A>) result;
  }

  @Override
  public int lookup(final A value) {
    return 0; // TODO
  }

  @Override
  public int length() {
    return length;
  }

  @Override
  public @Nonnull <B> List<B> bind(final @Nonnull Function<? super A, List<B>> function) {
    requireNonNull(function);
    return null; // TODO
  }

  @Override
  public @Nonnull <B, C> List<C> zip(final @Nonnull List<B> list, final @Nonnull BiFunction<? super A, ? super B, ? extends C> function) {
    requireNonNull(list);
    requireNonNull(function);
    return null; // TODO
  }

  @Override
  public @Nonnull <B, C> Pair<? extends Cons<B>, ? extends Cons<C>> unzip(final @Nonnull Function<? super A, Pair<? extends B, ? extends C>> function) {
    requireNonNull(function);
    return null; // TODO
  }

  @Override
  public @Nonnull <B> List<B> apply(final @Nonnull List<? extends Function<? super A, ? extends B>> list) {
    requireNonNull(list);
    return null; // TODO
  }

  @Override
  public @Nonnull <B> Cons<B> map(final @Nonnull Function<? super A, ? extends B> function) {
    requireNonNull(function);
    return null; // TODO
  }

  @Override
  public @Nonnull List<A> filter(final @Nonnull Predicate<? super A> predicate) {
    requireNonNull(predicate);
    return foldRight((BiFunction<A, List<A>, List<A>>) (a, as) -> predicate.$(a) ? as.cons(a) : as, List.nil());
  }

  @Override
  public @Nonnull <B> Cons<B> scanRight(final @Nonnull BiFunction<? super A, ? super B, ? extends B> biFunction, final B initial) {
    requireNonNull(biFunction);
    return reverse().scanLeft(biFunction.flip(), initial);
  }

  @Override
  public @Nonnull <B> Cons<B> scanLeft(final @Nonnull BiFunction<? super B, ? super A, ? extends B> biFunction, final B initial) {
    requireNonNull(biFunction);
    Cons<B> result = List.list(initial);
    B current = initial;
    List<A> tail = this;
    for (int i = 0; i < length; i++) {
      assert tail instanceof Cons;
      Cons<A> cons = (Cons<A>) tail;
      current = biFunction.$(current, cons.head());
      result = result.cons(current);
      tail = cons.tail();
    }
    return result;
  }

  @Override
  public @Nonnull Just<Cons<A>> caseCons() {
    return just(this);
  }

  @Override
  public @Nonnull Nothing<Nil<A>> caseNil() {
    return nothing();
  }

  @Override
  public <B> B foldRight(final @Nonnull BiFunction<? super A, ? super B, ? extends B> biFunction, final B initial) {
    requireNonNull(biFunction);
    return reverse().foldLeft(biFunction.flip(), initial);
  }

  @Override
  public <B> B foldLeft(final @Nonnull BiFunction<? super B, ? super A, ? extends B> biFunction, final B initial) {
    requireNonNull(biFunction);
    B result = initial;
    List<A> tail = this;
    for (int i = 0; i < length; i++) {
      assert tail instanceof Cons;
      Cons<A> cons = (Cons<A>) tail;
      result = biFunction.$(result, cons.head());
      tail = cons.tail();
    }
    return initial;
  }

  @Override
  public @Nonnull Iterator<A> iterator() {
    return new Iterator<A>() {
      List<A> next = ConsList.this;

      @Override
      public boolean hasNext() {
        return next instanceof Cons;
      }

      @Override
      public A next() {
        if (next instanceof Cons) {
          Cons<A> cons = (Cons<A>) next;
          next = cons.tail();
          return cons.head();
        }
        throw new NullPointerException();
      }
    };
  }

  @Override
  public int hashCode() {
    return foldLeft((integer, a) -> integer + Objects.hashCode(a), 31);
  }

  @Override
  public boolean equals(final Object obj) {
    if (!(obj instanceof Cons)) return false;
    final Cons that = (Cons)obj;
    final Iterator thisI = this.iterator();
    final Iterator thatI = that.iterator();
    if (this.length != that.length()) return false;
    for (int i = 0; i < length; i++) {
      assert thisI.hasNext();
      assert thatI.hasNext();
      if (!Objects.equals(thisI.next(), thatI.next())) return false;
    }
    return true;
  }

  @Override
  public @Nonnull String toString() {
    return foldLeft((sj, a) -> sj.add(String.valueOf(a)), new StringJoiner(", ", "[", "]")).toString();
  }
}
