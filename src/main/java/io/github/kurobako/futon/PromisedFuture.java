/*
 * Copyright (C) 2017 Fedor Gavrilov
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

import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import static io.github.kurobako.futon.Either.left;
import static io.github.kurobako.futon.Either.right;
import static io.github.kurobako.futon.Sequence.sequence;
import static io.github.kurobako.futon.Util.nonNull;
import static io.github.kurobako.futon.Future.successful;
import static io.github.kurobako.futon.Future.failed;
import static java.util.concurrent.atomic.AtomicReferenceFieldUpdater.newUpdater;

final class PromisedFuture<A> implements Promise<A>, Future<A> {
  private static final @Nonnull AtomicReferenceFieldUpdater<PromisedFuture, Either> updater = newUpdater(PromisedFuture.class, Either.class, "state");

  private volatile @Nonnull Either<Sequence<Runnable>, Either<Throwable, A>> state;

  PromisedFuture() {
    updater.lazySet(this, left(sequence()));
  }

  PromisedFuture(final A value) {
    updater.lazySet(this, right(right(value)));
  }

  PromisedFuture(final Throwable cause) {
    updater.lazySet(this, right(left(cause)));
  }

  @Override
  public @Nonnull Future<A> future() {
    return this;
  }

  @Override
  public @Nonnull Future<A> complete(final Either<? extends Throwable, ? extends A> completion) {
    nonNull(completion);
    final Either<Sequence<Runnable>, Either<? extends Throwable, ? extends A>> newState = right(completion);
    Either<Sequence<Runnable>, Either<Throwable, A>> snapshot;
    do {
      snapshot = state;
      if (snapshot.isRight()) throw new IllegalStateException();
    } while (!updater.compareAndSet(this, snapshot, newState));
    runCallbacks(snapshot);
    return this;
  }

  private void runCallbacks(final Either<Sequence<Runnable>, Either<Throwable, A>> state) {
    state.match(left -> left.value.foldLeft((u, runnable) -> {runnable.run(); return u; }, Unit.INSTANCE),
                right -> Unit.INSTANCE);
  }

  @Override
  public @Nonnull <B> Promise<B> contraMap(final Function<? super B, ? extends A> function) {
    nonNull(function);
    final PromisedFuture<B> result = new PromisedFuture<>();
    this.tryCompleteWith(result.map(function));
    return result;
  }

  @Override
  public @Nonnull Future<A> tryCompleteWith(final Future<? extends A> future) {
    nonNull(future);
    if (state.isRight()) throw new IllegalStateException();
    future.then(a -> {
      try {
        this.success(a);
      } catch (IllegalStateException e) {
        // yes, we want to swallow this one
      }
    });
    return this;
  }

  @Override
  public @Nonnull Future<A> success(final A value) {
    return complete(right(value));
  }

  @Override
  public @Nonnull Future<A> failure(final Throwable cause) {
    return complete(left(cause));
  }

  @Override
  public @Nonnull <B, C> Future<C> zip(final Future<B> future, final BiFunction<? super A, ? super B, ? extends C> function) {
    nonNull(future);
    nonNull(function);
    return bind(a -> future.map(b -> function.$(a, b)));
  }

  @Override
  public @Nonnull <B> Future<B> bind(final Function<? super A, ? extends Future<B>> function) {
    nonNull(function);
    final PromisedFuture<B> result = new PromisedFuture<>();
    final Runnable action = () -> state.match(incomplete -> result.future(),
                                              complete -> complete.value.either(result::failure,
                                                                                right -> result.tryCompleteWith(function.$(right))));
    return completeWith(result, action);
  }

  @Override
  public @Nonnull Future<A> then(final Procedure<? super A> procedure) {
    nonNull(procedure);
    final PromisedFuture<A> result = new PromisedFuture<>();
    final Runnable action = () -> {
      state.match(incomplete -> result.future(),
                  complete -> complete.value.match(left -> result.failure(left.value),
                                                   right -> {
                                                     try {
                                                       procedure.run(right.value);
                                                       return result.success(right.value);
                                                     } catch (Exception e) {
                                                       return result.failure(e);
                                                     }
                                                   }));
    };
    return completeWith(result, action);
  }

  private @Nonnull <B> Future<B> completeWith(final Future<B> result, final Runnable action) {
    Either<Sequence<Runnable>, Either<Throwable, A>> snapshot;
    Either<Sequence<Runnable>, Either<Throwable, A>> newState;
    do {
      snapshot = state;
      if (snapshot.isRight()) {
        action.run();
        return result;
      } else {
        newState = snapshot.match(left -> left(left.value.append(action)), right -> state);
        assert newState != snapshot;
      }
    } while (!updater.compareAndSet(this, snapshot, newState));
    runCallbacks(snapshot);
    return result;
  }

  @Override
  public @Nonnull <B> Future<B> apply(final Future<? extends Function<? super A, ? extends B>> future) {
    nonNull(future);
    return bind(a -> future.map(f -> f.$(a)));
  }

  @Override
  public @Nonnull <B> Future<B> map(final Function<? super A, ? extends B> function) {
    nonNull(function);
    final PromisedFuture<B> result = new PromisedFuture<>();
    final Runnable action = () -> {
      state.match(incomplete -> result.future(),
                  complete -> complete.value.match(left -> result.failure(left.value),
                                                   right -> result.success(function.$(right.value))));
    };
    return completeWith(result, action);
  }

  @Override
  public @Nonnull Future<A> filter(final Predicate<? super A> predicate) {
    nonNull(predicate);
    return bind(a -> predicate.$(a) ? successful(a) : failed(new NoSuchElementException()));
  }

  @Override
  public @Nonnull Future<A> recover(final Function<? super Throwable, A> function) {
    nonNull(function);
    final PromisedFuture<A> result = new PromisedFuture<>();
    final Runnable action = () -> {
      state.match(incomplete -> result.future(),
                  complete -> complete.value.match(left -> result.success(function.$(left.value)),
                                                   right -> result.success(right.value)));
    };
    return completeWith(result, action);
  }

  @Override
  public @Nonnull Future<A> fallback(final Function<? super Throwable, Future<A>> function) {
    nonNull(function);
    final PromisedFuture<A> result = new PromisedFuture<>();
    final Runnable action = () -> {
      state.match(incomplete -> result.future(),
                  complete -> complete.value.match(left -> result.tryCompleteWith(function.$(left.value)),
                                                   right -> result.success(right.value)));
    };
    return completeWith(result, action);
  }
}