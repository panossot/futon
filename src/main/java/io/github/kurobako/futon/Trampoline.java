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

public abstract class Trampoline<A> implements Functor<A> {

  public A run() {
    Either<Value<Trampoline<A>>, A> step = step();
    while (!step.isRight()) {
      //noinspection ConstantConditions
      step = step.left().asNullable().$().step();
    }
    return step.right().asNullable();
  }

  public abstract Either<Value<Trampoline<A>>, A> step();

  public @Nonnull <B> Trampoline<B> bind(final @Nonnull Function<? super A, Trampoline<B>> function) {
    requireNonNull(function, "function");
    return new Bind<>(this, function);
  }

  public @Nonnull <B> Trampoline<B> apply(final @Nonnull
                                   Trampoline<? extends Function<? super A, ? extends B>> transformation) {
    requireNonNull(transformation, "transformation");
    return bind(a -> transformation.bind(ab -> done(ab.$(a))));
  }

  public @Nonnull <B, C> Trampoline<C> zip(final @Nonnull Trampoline<B> another,
                                       final @Nonnull BiFunction<? super A, ? super B, Trampoline<C>> function) {
    requireNonNull(another, "another");
    requireNonNull(function, "function");
    Either<Value<Trampoline<A>>, A> fst = this.step();
    Either<Value<Trampoline<B>>, B> snd = another.step();
    if (fst.isRight()) {
      A fstVal = fst.right().asNullable();
      if (snd.isRight()) {
        B sndVal = snd.right().asNullable();
        return function.$(fstVal, sndVal);
      } else {
        Value<Trampoline<B>> sndVal = snd.left().asNullable();
        return more(() -> done(fstVal).zip(sndVal.$(), function));
      }
    } else {
      Value<Trampoline<A>> fstVal = fst.left().asNullable();
      if (snd.isRight()) {
        B sndVal = snd.right().asNullable();
        return more(() -> fstVal.$().zip(done(sndVal), function));
      } else {
        Value<Trampoline<B>> sndVal = snd.left().asNullable();
        return more(() -> fstVal.$().zip(sndVal.$(), function));
      }
    }
  };

  public  @Nonnull <B> Trampoline<Pair<A, B>> zip(final @Nonnull Trampoline<B> another) {
    requireNonNull(another, "another");
    return zip(another, (a, b) -> done(pair(a, b)));
  }

  @Override
  public @Nonnull <B> Trampoline<B> map(final @Nonnull Function<? super A, ? extends B> function) {
    requireNonNull(function, "function");
    return bind(a -> done(function.$(a)));
  }

  abstract <B> Either<Value<Trampoline<B>>, B> stepAndTransform(final @Nonnull
                                                                Function<? super A, Trampoline<B>> function);

  public static <A> Trampoline<A> done(final A value) {
    return new Done<>(value);
  }

  public static <A> Trampoline<A> more(final @Nonnull Value<Trampoline<A>> trampoline) {
    requireNonNull(trampoline, "trampoline");
    return new More<>(trampoline);
  }


  public static @Nonnull <A> Trampoline<A> join(final @Nonnull Trampoline<Trampoline<A>> wrapper) {
    requireNonNull(wrapper, "wrapper");
    return wrapper.bind(id());
  }

  final static class Done<A> extends Trampoline<A> {
    private final Either.Right<Value<Trampoline<A>>, A> result;

    Done(final A result) {
      this.result = new Either.Right<>(result);
    }

    @Override
    public Either<Value<Trampoline<A>>, A> step() {
      return result;
    }

    @Override
    <B> Either<Value<Trampoline<B>>, B> stepAndTransform(final @Nonnull
                                                         Function<? super A, Trampoline<B>> function) {
      assert function != null;
      return function.$(result.val()).step();
    }
  }

  final static class More<A> extends Trampoline<A> {
    private final @Nonnull Either.Left<Value<Trampoline<A>>, A> next;

    More(final Value<Trampoline<A>> next) {
      assert next != null;
      this.next = new Either.Left<>(next);
    }

    @Override
    public Either<Value<Trampoline<A>>, A> step() {
      return next;
    }

    @Override
    <B> Either<Value<Trampoline<B>>, B> stepAndTransform(final @Nonnull
                                                         Function<? super A, Trampoline<B>> function) {
      assert function != null;
      return Either.left(() -> next.val().$().bind(function));
    }
  }

  final static class Bind<Z, A> extends Trampoline<A> {
    private final @Nonnull Trampoline<Z> sub;
    private final @Nonnull Function<? super Z, Trampoline<A>> transform;

    Bind(final Trampoline<Z> sub, final Function<? super Z, Trampoline<A>> transform) {
      assert sub != null;
      assert transform != null;
      this.sub = sub;
      this.transform = transform;
    }

    @Override
    public @Nonnull <B> Trampoline<B> bind(final @Nonnull Function<? super A, Trampoline<B>> function) {
      requireNonNull(function, "function");
      return new Bind<>(this.sub, z -> transform.$(z).bind(function));
    }

    @Override
    public Either<Value<Trampoline<A>>, A> step() {
      return sub.stepAndTransform(transform);
    }

    @Override
    <B> Either<Value<Trampoline<B>>, B> stepAndTransform(final @Nonnull
                                                         Function<? super A, Trampoline<B>> function) {
      assert function != null;
      return sub.bind(z -> transform.$(z).bind(function)).step();
    }
  }
}