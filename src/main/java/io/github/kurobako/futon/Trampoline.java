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
import static io.github.kurobako.futon.Optional.none;
import static io.github.kurobako.futon.Optional.some;
import static java.util.Objects.requireNonNull;

public abstract class Trampoline<A> {
  Trampoline() {}

  public abstract @Nonnull <B> Trampoline<B> bind(@Nonnull Function<? super A, ? extends Trampoline<B>> function);

  public @Nonnull <B> Trampoline<B> apply(final @Nonnull
                                          Trampoline<? extends Function<? super A, ? extends B>> trampoline) {
    requireNonNull(trampoline, "trampoline");
    return bind(a -> trampoline.bind(f -> done(f.$(a))));
  }

  public @Nonnull <B> Trampoline<B> map(final @Nonnull Function<? super A, ? extends B> function) {
    requireNonNull(function, "function");
    return bind(a -> done(function.$(a)));
  }

  public abstract @Nonnull Either<Value<Trampoline<A>>, A> resume();

  public final A run() {
    Trampoline<A> current = this;
    while (true) {
      final Either<Value<Trampoline<A>>, A> step = current.resume();
      for (final Either.Left<Value<Trampoline<A>>, A> left : step.caseLeft()) {
        current = left.value.$();
      }
      //noinspection LoopStatementThatDoesntLoop
      for (final Either.Right<Value<Trampoline<A>>, A> right: step.caseRight()) {
        return right.value;
      }
    }
  }

  public abstract @Nonnull Optional<More<A>> caseMore();

  public abstract @Nonnull Optional<Done<A>> caseDone();

  abstract <R> R dispatch(@Nonnull Function<AbstractTrampoline<A>, R> ifNormal, @Nonnull Function<Bind<A>, R> ifBind);

  public static @Nonnull <A> Trampoline<A> join(final @Nonnull Trampoline<? extends Trampoline<A>> trampoline) {
    requireNonNull(trampoline, "trampoline");
    return trampoline.bind(id());
  }

  public static @Nonnull <A> Trampoline.Done<A> done(final A result) {
    return new Done<>(result);
  }

  public static @Nonnull <A> Trampoline.More<A> suspend(final @Nonnull Value<Trampoline<A>> next) {
    requireNonNull(next, "next");
    return new More<>(next);
  }

  public static @Nonnull <A> Trampoline.More<A> lift(final @Nonnull Value<A> value) {
    requireNonNull(value, "value");
    return suspend(value.map(Trampoline::done));
  }

  public static final class Done<A> extends AbstractTrampoline<A> {
    public final A result;

    Done(final A result) {
      this.result = result;
    }

    @Override
    public @Nonnull Either<Value<Trampoline<A>>, A> resume() {
      return right(result);
    }


    @Override
    public @Nonnull Optional.None<More<A>> caseMore() {
      return none();
    }

    @Override
    public @Nonnull Optional.Some<Done<A>> caseDone() {
      return some(this);
    }

    @Override
    public String toString() {
      return "Done[" + result + "]@" + Integer.toHexString(hashCode());
    }
  }

  public static final class More<A> extends AbstractTrampoline<A> {
    public final @Nonnull Value<Trampoline<A>> next;

    More(final @Nonnull Value<Trampoline<A>> next) {
      assert next != null;
      this.next = next;
    }

    @Override
    public @Nonnull Either<Value<Trampoline<A>>, A> resume() {
      return left(next);
    }


    @Override
    public @Nonnull Optional.Some<More<A>> caseMore() {
      return some(this);
    }

    @Override
    public @Nonnull Optional.None<Done<A>> caseDone() {
      return none();
    }
  }

  private static abstract class AbstractTrampoline<A> extends Trampoline<A> {
    @Override
    @SuppressWarnings("unchecked")
    public @Nonnull <B> Trampoline<B> bind(@Nonnull Function<? super A, ? extends Trampoline<B>> function) {
      requireNonNull(function, "function");
      return new Bind<>((AbstractTrampoline<Object>)this, (Function<Object, Trampoline<B>>)function);
    }

    @Override
    final <R> R dispatch(@Nonnull Function<AbstractTrampoline<A>, R> ifNormal, @Nonnull Function<Bind<A>, R> ifBind) {
      assert ifNormal != null;
      assert ifBind != null;
      return ifNormal.$(this);
    }
  }

  private static final class Bind<A> extends Trampoline<A> {
    final AbstractTrampoline<Object> trampoline;
    final Function<Object, Trampoline<A>> function;

    private Bind(final @Nonnull AbstractTrampoline<Object> trampoline,
                 final @Nonnull Function<Object, Trampoline<A>> function) {
      assert trampoline != null;
      assert function != null;
      this.trampoline = trampoline;
      this.function = function;
    }

    @Override
    public @Nonnull <B> Trampoline<B> bind(final @Nonnull Function<? super A, ? extends Trampoline<B>> function) {
      requireNonNull(function, "function");
      return new Bind<>(this.trampoline, o -> suspend(() -> Bind.this.function.$(o).bind(function)));
    }

    @Override
    public @Nonnull Either.Left<Value<Trampoline<A>>, A> resume() {
      return left(trampoline.resume().either(value -> {
        return value.map(trampoline -> {
          return trampoline.dispatch(at -> at.resume().either(v -> v.$().bind(function), function::$),
          bind -> new Bind<>(bind.trampoline, o -> bind.function.$(o).bind(function)));
        });
      }, o -> () -> function.$(o)));
    }

    @Override
    public @Nonnull Optional.None<Done<A>> caseDone() {
      return none();
    }

    @Override
    public @Nonnull Optional.Some<More<A>> caseMore() {
      return some(suspend(resume().value));
    }

    @Override
    <R> R dispatch(final @Nonnull Function<AbstractTrampoline<A>, R> ifNormal, final @Nonnull Function<Bind<A>, R> ifBind) {
      assert ifNormal != null;
      assert ifBind != null;
      return ifBind.$(this);
    }
  }
}
