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
import static java.util.Objects.requireNonNull;

public abstract class Trampoline<A> {

  public abstract @Nonnull <B> Trampoline<B> bind(@Nonnull Function<? super A, Trampoline<B>> function);

  public @Nonnull <B> Trampoline<B> apply(final @Nonnull
                                          Trampoline<? extends Function<? super A, ? extends B>> trampoline) {
    requireNonNull(trampoline, "trampoline");
    return bind(a -> trampoline.bind(f -> done(f.$(a))));
  }

  public @Nonnull <B> Trampoline<B> map(final @Nonnull Function<? super A, ? extends B> function) {
    requireNonNull(function, "function");
    return bind(a -> done(function.$(a)));
  }

  public final A run() {
    Trampoline<A> current = this;
    while (true) {
      Either<Value<Trampoline<A>>, A> either = current.resume();
      for (Value<Trampoline<A>> value : either.caseLeft()) {
        current = value.$();
      }
      //noinspection LoopStatementThatDoesntLoop
      for (final A result : either.caseRight()) {
        return result;
      }
    }
  }

  public abstract @Nonnull Either<Value<Trampoline<A>>, A> resume();

  public @Nonnull Optional<Value<Trampoline<A>>> caseMore() {
    return resume().caseLeft();
  }

  public @Nonnull Optional<A> caseDone() {
    return resume().caseRight();
  }

  abstract <R> R dispatch(@Nonnull Function<AbstractTrampoline<A>, R> ifNormal, @Nonnull Function<Bind<A>, R> ifBind);

  public static @Nonnull <A> Trampoline<A> join(final @Nonnull Trampoline<Trampoline<A>> trampoline) {
    requireNonNull(trampoline, "trampoline");
    return trampoline.bind(id());
  }

  public static @Nonnull <A> Trampoline<A> done(final A value) {
    return new Done<>(new Either.Right<>(new Optional.Some<>(value)));
  }

  public static @Nonnull <A> Trampoline<A> suspend(final @Nonnull Value<Trampoline<A>> value) {
    requireNonNull(value, "value");
    return new More<>(new Either.Left<>(new Optional.Some<>(value)));
  }

  public static @Nonnull <A> Trampoline<A> lift(final @Nonnull Value<A> value) {
    requireNonNull(value, "value");
    return suspend(value.map(Trampoline::done));
  }

  abstract void NOT_FOR_EXTENSION();

  abstract static class AbstractTrampoline<A> extends Trampoline<A> {
    @Override
    @SuppressWarnings("unchecked")
    public @Nonnull <B> Trampoline<B> bind(final @Nonnull Function<? super A, Trampoline<B>> function) {
      requireNonNull(function, "function");
      return new Bind<>((AbstractTrampoline<Object>)this, (Function<Object, Trampoline<B>>)function);
    }

    @Override
    <R> R dispatch(final @Nonnull Function<AbstractTrampoline<A>, R> ifNormal,
                   final @Nonnull Function<Bind<A>, R> ifBind) {
      assert ifNormal != null;
      assert ifBind != null;
      return ifNormal.$(this);
    }

    @Override
    void NOT_FOR_EXTENSION() {}
  }

  final static class Done<A> extends AbstractTrampoline<A> {
    final Either.Right<Value<Trampoline<A>>, A> rightA;

    Done(final @Nonnull Either.Right<Value<Trampoline<A>>, A> rightA) {
      assert rightA != null;
      this.rightA = rightA;
    }

    @Override
    public @Nonnull Either<Value<Trampoline<A>>, A> resume() {
      return rightA;
    }

    @Override
    public String toString() {
      return "Done " + rightA.someR.value;
    }
  }

  final static class More<A> extends AbstractTrampoline<A> {
    final Either.Left<Value<Trampoline<A>>, A> leftV;

    More(final @Nonnull Either.Left<Value<Trampoline<A>>, A> leftV) {
      assert leftV != null;
      this.leftV = leftV;
    }

    @Override
    public @Nonnull Either<Value<Trampoline<A>>, A> resume() {
      return leftV;
    }

    @Override
    public String toString() {
      return "More#" + System.identityHashCode(this);
    }
  }

  final static class Bind<A> extends Trampoline<A> {
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
    public @Nonnull <B> Trampoline<B> bind(final @Nonnull Function<? super A, Trampoline<B>> function) {
      requireNonNull(function, "function");
      return new Bind<>(this.trampoline, o -> suspend(() -> Bind.this.function.$(o).bind(function)));
    }

    @Override
    public @Nonnull Either<Value<Trampoline<A>>, A> resume() {
      return new Either.Left<>(new Optional.Some<>(trampoline.resume().either(value -> {
        return value.map(trampoline -> {
          return trampoline.dispatch(at -> at.resume().either(v -> v.$().bind(function), function::$),
          bind -> new Bind<>(bind.trampoline, o -> bind.function.$(o).bind(function)));
        });
      }, o -> () -> function.$(o))));
    }

    @Override
    <R> R dispatch(final @Nonnull Function<AbstractTrampoline<A>, R> ifNormal, final @Nonnull Function<Bind<A>, R> ifBind) {
      assert ifNormal != null;
      assert ifBind != null;
      return ifBind.$(this);
    }

    @Override
    public String toString() {
      return "More#" + System.identityHashCode(this);
    }

    @Override
    void NOT_FOR_EXTENSION() {}
  }
}
