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

import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;

import static io.github.kurobako.futon.Function.id;
import static io.github.kurobako.futon.Optional.none;
import static io.github.kurobako.futon.Optional.some;
import static java.util.Objects.requireNonNull;

public abstract class List<A> implements Foldable<A>, Iterable<A> {
  public abstract @Nonnull <B> List<B> bind(@Nonnull Function<? super A, List<B>> function);

  public abstract @Nonnull <B> List<B> apply(@Nonnull List<? extends Function<? super A, ? extends B>> list);

  public abstract @Nonnull <B> List<B> map(@Nonnull Function<? super A, ? extends B> function);

  public abstract @Nonnull List<A> filter(@Nonnull Predicate<A> predicate);

  public @Nonnull List<A> append(final @Nonnull List<A> list) {
    requireNonNull(list, "list");
    return list.foldLeft(List::cons, this);
  }

  public @Nonnull Cons<A> cons(final A value) {
    return new Cons<>(value, this);
  }

  public abstract @Nonnull List<A> reverse();

  public abstract int length();

  public abstract @Nonnull Optional<Cons<A>> caseCons();

  public abstract @Nonnull Optional<Nil> caseNil();

  public static @Nonnull <A> List<A> join(final @Nonnull List<List<A>> list) {
    requireNonNull(list, "value");
    return list.bind(id());
  }

  public static @Nonnull <A> Cons<A> list(final A value) {
    return new Cons<>(value, nil());
  }

  @SafeVarargs
  public static @Nonnull <A> Cons<A> list(final A first, final A... last) {
    List<A> list = nil();
    for (int i = last.length - 1; i >= 0; i--) list = list.cons(last[i]);
    return list.cons(first);
  }

  @SuppressWarnings("unchecked")
  public static @Nonnull <A> Nil<A> nil() {
    return Nil.INSTANCE;
  }

  public static final class Cons<A> extends List<A> {
    private static final int MAX_LENGTH = Integer.MAX_VALUE - 8;

    public final A head;
    public final @Nonnull List<A> tail;

    private final int length;

    Cons(final A head, final @Nonnull List<A> tail) {
      assert tail != null;
      assert tail.length() > 0;
      this.head = head;
      this.tail = tail;
      this.length = tail.length() + 1;
      if (this.length > MAX_LENGTH) throw new IllegalArgumentException("Can't have lists longer than " + MAX_LENGTH);
    }

    @Override
    public @Nonnull <B> List<B> bind(final @Nonnull Function<? super A, List<B>> function) {
      requireNonNull(function, "function");
      return foldRight((BiFunction<? super A, List<B>, List<B>>) (a, bs) -> bs.append(function.$(a)), nil());
    }

    @Override
    public @Nonnull <B> List<B> apply(final @Nonnull List<? extends Function<? super A, ? extends B>> list) {
      requireNonNull(list, "list");
      return foldRight((BiFunction<? super A, List<B>, List<B>>) (a, bs) -> bs.append(
        list.foldRight((BiFunction<Function<? super A, ? extends B>, List<B>, List<B>>) (f, bs2) -> bs2.cons(f.$(a)),
        nil())),
      nil());
    }

    @Override
    public @Nonnull <B> List<B> map(final @Nonnull Function<? super A, ? extends B> function) {
      requireNonNull(function, "function");
      return foldLeft((BiFunction<List<B>, A, List<B>>) (bs, a) -> bs.cons(function.$(a)), nil());
    }

    @Override
    public @Nonnull List<A> filter(final @Nonnull Predicate<A> predicate) {
      requireNonNull(predicate, "predicate");
      return foldLeft((BiFunction<List<A>, A, List<A>>) (as, a) -> predicate.$(a) ? as.cons(a) : as, nil());
    }

    @Override
    public @Nonnull List<A> reverse() {
      return foldLeft((BiFunction<List<A>, A, List<A>>) List::cons, nil());
    }

    @Override
    public int length() {
      return length;
    }

    @Override
    public @Nonnull Optional.Some<Cons<A>> caseCons() {
      return some(this);
    }

    @Override
    public @Nonnull Optional.None<Nil> caseNil() {
      return none();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <B> B foldRight(final @Nonnull BiFunction<? super A, ? super B, ? extends B> function, final B initial) {
      requireNonNull(function, "function");

      final A[] elements = (A[]) new Object[length];
      B result = initial;
      List<A> tail = this;
      for (int i = elements.length - 1; i >= 0; i--) {
        assert tail instanceof Cons;
        Cons<A> cons = (Cons<A>) tail;
        elements[i] = cons.head;
        tail = cons.tail;
      }
      for (A element : elements) {
        result = function.$(element, result);
      }
      return result;
    }

    @Override
    public <B> B foldLeft(final @Nonnull BiFunction<? super B, ? super A, ? extends B> function, final B initial) {
      requireNonNull(function, "function");
      B result = initial;
      List<A> tail = this;
      for (int i = 0; i < length; i++) {
        assert tail instanceof Cons;
        Cons<A> cons = (Cons<A>) tail;
        result = function.$(result, cons.head);
        tail = cons.tail;
      }
      return result;
    }

    @Override
    public Iterator<A> iterator() {
      return new Iterator<A>() {
        List<A> next = Cons.this;

        @Override
        public boolean hasNext() {
          //noinspection LoopStatementThatDoesntLoop
          for (Nil nil : next.caseNil()) return false;
          return true;
        }

        @Override
        public A next() {
          //noinspection LoopStatementThatDoesntLoop
          for (Cons<A> cons : next.caseCons()) {
            A result = cons.head;
            next = cons.tail;
            return result;
          }
          throw new NoSuchElementException();
        };
      };
    }

    @Override
    public int hashCode() {
      return super.hashCode();
    }

    @Override
    public boolean equals(final Object o) {
      return super.equals(o);
    }

    @Override
    public String toString() {
      return foldLeft(StringBuilder::append, new StringBuilder()).toString();
    }
  }

  @SuppressWarnings("unchecked")
  public static final class Nil<A> extends List<A> {
    private static final Nil INSTANCE = new Nil();
    private static final Optional.Some<Nil> SOME_NIL = some(INSTANCE);

    private Nil() {}

    @Override
    public @Nonnull <B> List<B> bind(final @Nonnull Function<? super A, List<B>> function) {
      requireNonNull(function, "function");
      return (List<B>) this;
    }

    @Override
    public @Nonnull <B> List<B> apply(final @Nonnull List<? extends Function<? super A, ? extends B>> list) {
      requireNonNull(list, "list");
      return (List<B>) this;
    }

    @Override
    public @Nonnull <B> List<B> map(final @Nonnull Function<? super A, ? extends B> function) {
      requireNonNull(function, "function");
      return (List<B>) this;
    }

    @Override
    public @Nonnull List<A> filter(final @Nonnull Predicate<A> predicate) {
      requireNonNull(predicate, "predicate");
      return this;
    }

    @Override
    public @Nonnull List<A> reverse() {
      return this;
    }

    @Override
    public int length() {
      return 0;
    }

    @Override
    public @Nonnull Optional.None<Cons<A>> caseCons() {
      return none();
    }

    @Override
    public @Nonnull Optional.Some<Nil> caseNil() {
      return SOME_NIL;
    }

    @Override
    public <B> B foldRight(final @Nonnull BiFunction<? super A, ? super B, ? extends B> function, final B initial) {
      requireNonNull(function, "function");
      return initial;
    }

    @Override
    public <B> B foldLeft(final @Nonnull BiFunction<? super B, ? super A, ? extends B> function, final B initial) {
      requireNonNull(function, "function");
      return initial;
    }

    @Override
    public Iterator<A> iterator() {
      return Collections.emptyIterator();
    }
  }
}


