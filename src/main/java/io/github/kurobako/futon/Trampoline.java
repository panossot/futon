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

import static io.github.kurobako.futon.Either.left;
import static io.github.kurobako.futon.Either.right;
import static io.github.kurobako.futon.Function.id;
import static io.github.kurobako.futon.Option.some;
import static io.github.kurobako.futon.Option.none;
import static java.util.Objects.requireNonNull;

public abstract class Trampoline<A> {
  Trampoline() {}

  public abstract @Nonnull <B> Trampoline<B> bind(@Nonnull Function<? super A, ? extends Trampoline<B>> bind);

  public @Nonnull <B> Trampoline<B> apply(final @Nonnull
                                          Trampoline<? extends Function<? super A, ? extends B>> trampoline) {
    requireNonNull(trampoline, "trampoline");
    return bind(a -> trampoline.bind(f -> done(f.$(a))));
  }

  public @Nonnull <B> Trampoline<B> map(final @Nonnull Function<? super A, ? extends B> map) {
    requireNonNull(map, "map");
    return bind(a -> done(map.$(a)));
  }

  public @Nonnull <B, C> Trampoline<C> zip(final @Nonnull Trampoline<B> another,
                                           final @Nonnull BiFunction<? super A, ? super B, ? extends C> zip) {
    requireNonNull(another, "another");
    requireNonNull(zip, "zip");
    Either<Lazy<Trampoline<A>>, A> thisResume = this.resume();
    Either<Lazy<Trampoline<B>>, B> thatResume = another.resume();
    for (Either.Left<Lazy<Trampoline<A>>, A> thisLeft: thisResume.caseLeft()) {
      //noinspection LoopStatementThatDoesntLoop
      for (Either.Left<Lazy<Trampoline<B>>, B> thatLeft: thatResume.caseLeft()) {
        return suspend(() -> thisLeft.left.extract().zip(thatLeft.left.extract(), zip));
      }
      //noinspection LoopStatementThatDoesntLoop
      for (Either.Right<Lazy<Trampoline<B>>, B> thatRight : thatResume.caseRight()) {
        return suspend(() -> thisLeft.left.extract().zip(done(thatRight.right), zip));
      }
    }
    for (Either.Right<Lazy<Trampoline<A>>, A> thisRight : thisResume.caseRight()) {
      //noinspection LoopStatementThatDoesntLoop
      for (Either.Left<Lazy<Trampoline<B>>, B> thatLeft: thatResume.caseLeft()) {
        return suspend(() -> done(thisRight.right).zip(thatLeft.left.extract(), zip));
      }
      //noinspection LoopStatementThatDoesntLoop
      for (Either.Right<Lazy<Trampoline<B>>, B> thatRight : thatResume.caseRight()) {
        return done(zip.$(thisRight.right, thatRight.right));
      }
    }
    assert false;
    throw new RuntimeException("assertion failed");
  }

  public abstract @Nonnull Either<Lazy<Trampoline<A>>, A> resume();

  public final A run() {
    Trampoline<A> current = this;
    while (true) {
      final Either<Lazy<Trampoline<A>>, A> step = current.resume();
      for (final Either.Left<Lazy<Trampoline<A>>, A> left : step.caseLeft()) {
        current = left.left.extract();
      }
      //noinspection LoopStatementThatDoesntLoop
      for (final Either.Right<Lazy<Trampoline<A>>, A> right: step.caseRight()) {
        return right.right;
      }
    }
  }

  public abstract @Nonnull Option<More<A>> caseMore();

  public abstract @Nonnull Option<Done<A>> caseDone();

  abstract <R> R dispatch(@Nonnull Function<AbstractTrampoline<A>, R> ifNormal, @Nonnull Function<Bind<A>, R> ifBind);

  public static @Nonnull <A> Trampoline<A> join(final @Nonnull Trampoline<? extends Trampoline<A>> trampoline) {
    requireNonNull(trampoline, "trampoline");
    return trampoline.bind(id());
  }

  public static @Nonnull <A> Trampoline.Done<A> done(final A result) {
    return new Done<>(result);
  }

  public static @Nonnull <A> Trampoline.More<A> suspend(final @Nonnull Lazy<Trampoline<A>> next) {
    requireNonNull(next, "next");
    return new More<>(next);
  }

  public static @Nonnull <A> Trampoline.More<A> lift(final @Nonnull Lazy<A> value) {
    requireNonNull(value, "value");
    return suspend(value.map(Trampoline::done));
  }

  public static final class Done<A> extends AbstractTrampoline<A> {
    public final A result;

    Done(final A result) {
      this.result = result;
    }

    @Override
    public @Nonnull Either<Lazy<Trampoline<A>>, A> resume() {
      return right(result);
    }

    @Override
    public @Nonnull Option.None<More<A>> caseMore() {
      return none();
    }

    @Override
    public @Nonnull Option.Some<Done<A>> caseDone() {
      return some(this);
    }

    @Override
    public @Nonnull String toString() {
      return "Done[" + result + "]@" + Integer.toHexString(hashCode());
    }
  }

  public static final class More<A> extends AbstractTrampoline<A> {
    public final @Nonnull Lazy<Trampoline<A>> next;

    More(final @Nonnull Lazy<Trampoline<A>> next) {
      //noinspection ConstantConditions
      assert next != null;
      this.next = next;
    }

    @Override
    public @Nonnull Either<Lazy<Trampoline<A>>, A> resume() {
      return left(next);
    }

    @Override
    public @Nonnull Option.Some<More<A>> caseMore() {
      return some(this);
    }

    @Override
    public @Nonnull Option.None<Done<A>> caseDone() {
      return none();
    }
  }

  private static abstract class AbstractTrampoline<A> extends Trampoline<A> {
    @Override
    @SuppressWarnings("unchecked")
    public @Nonnull <B> Trampoline<B> bind(final @Nonnull Function<? super A, ? extends Trampoline<B>> bind) {
      requireNonNull(bind, "bind");
      return new Bind<>((AbstractTrampoline<Object>)this, (Function<Object, Trampoline<B>>)bind);
    }

    @Override
    final <R> R dispatch(@Nonnull Function<AbstractTrampoline<A>, R> ifNormal, @Nonnull Function<Bind<A>, R> ifBind) {
      //noinspection ConstantConditions
      assert ifNormal != null;
      //noinspection ConstantConditions
      assert ifBind != null;
      return ifNormal.$(this);
    }
  }

  private static final class Bind<A> extends Trampoline<A> {
    final @Nonnull AbstractTrampoline<Object> trampoline;
    final @Nonnull Function<Object, Trampoline<A>> function;

    private Bind(final @Nonnull AbstractTrampoline<Object> trampoline,
                 final @Nonnull Function<Object, Trampoline<A>> function) {
      //noinspection ConstantConditions
      assert trampoline != null;
      //noinspection ConstantConditions
      assert function != null;
      this.trampoline = trampoline;
      this.function = function;
    }

    @Override
    public @Nonnull <B> Trampoline<B> bind(final @Nonnull Function<? super A, ? extends Trampoline<B>> bind) {
      requireNonNull(bind, "bind");
      return new Bind<>(this.trampoline, o -> suspend(() -> Bind.this.function.$(o).bind(bind)));
    }

    @Override
    public @Nonnull Either.Left<Lazy<Trampoline<A>>, A> resume() {
      return left(trampoline.resume().either(
       value -> value.map(trampoline -> trampoline.dispatch(
        at -> at.resume().either(v -> v.extract().bind(function), function::$),
        bind -> new Bind<>(bind.trampoline, o -> bind.function.$(o).bind(function)))),
      o -> () -> function.$(o)));
    }

    @Override
    public @Nonnull Option.None<Done<A>> caseDone() {
      return none();
    }

    @Override
    public @Nonnull Option.Some<More<A>> caseMore() {
      return some(suspend(resume().left));
    }

    @Override
    <R> R dispatch(final @Nonnull Function<AbstractTrampoline<A>, R> ifNormal,
                   final @Nonnull Function<Bind<A>, R> ifBind) {
      //noinspection ConstantConditions
      assert ifNormal != null;
      //noinspection ConstantConditions
      assert ifBind != null;
      return ifBind.$(this);
    }
  }
}
