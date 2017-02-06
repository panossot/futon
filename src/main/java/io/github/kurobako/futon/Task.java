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

import java.io.Closeable;
import java.util.concurrent.ExecutionException;

import static io.github.kurobako.futon.Either.left;
import static io.github.kurobako.futon.Either.right;
import static io.github.kurobako.futon.Util.nonNull;

/**
 * <p>Task monad is a computation built out of individual impure, side-effecting actions wrapped inside monad to be executed
 * all at once later</p>
 * <p>Task memoizes its result after {@link #execute()} is called for the first time, so consecutive calls will return
 * the same value without any additional external actions. A new instance of Task needs to be built if new portion of
 * side effects needs to be executed. Thus, task allows for some reasoning about the side-effecting code and may be used
 * in a pure code.</p>
 * <p>Task may use custom finalizer code for its resources and catches any checked exceptions produced by {@link Effect}
 * instances it runs. Note that {@link RuntimeException} thrown on execution will be rethrown as is unless caugh with
 * {@link #caught(Function)}.</p>
 * <p>{@link #map(Function)} makes Task a functor.</p>
 * <p>{@link #bind(Function)} and {@link #task(A)} form a monad.</p>
 * @see <a href="https://github.com/tel/scala-tk/">https://github.com/tel/scala-tk/</a>
 * @param <A> result type.
 */
public final class Task<A> {
  volatile Object resource;
  Effect<Object, ? extends A> effect;
  Procedure<Object> cleanup;
  Either<Exception, A> result;

  Task(final Object initial, final Effect<Object, A> effect, final Procedure<Object> cleanup) {
    this.resource = initial;
    this.effect = effect;
    this.cleanup = cleanup;
  }

  @SuppressWarnings("unchecked")
  @Nonnull <E extends Exception, R> R perform(Function<? super A, ? extends R> onSuccess, ExceptionHandler<? extends E, ? extends R> onFailure) throws E {
    boolean performed = (resource == null);
    if (!performed) {
      synchronized (this) {
        performed = (resource == null);
        if (!performed) {
          try {
            Exception failure = null;
            A success = null;
            try {
              success = effect.perform(resource);
              result = right(success);
              return onSuccess.$(effect.perform(resource));
            } catch (final Exception e) {
              failure = e;
              result = left(failure);
              return onFailure.handle(failure);
            } finally {
              try {
                cleanup.run(resource);
              } catch (final Exception e) {
                if (failure != null) failure.addSuppressed(e);
              }
            }
          } finally {
            effect = null;
            cleanup = null;
            resource = null;
          }
        }
      }
    }
    if (result instanceof Either.Left) {
      return onFailure.handle(((Either.Left<Exception, A>)result).value);
    } else {
      return onSuccess.$(((Either.Right<Exception, A>)result).value);
    }
  }

  /**
   * Executes this task, running all {@link Effect}s and transforming {@link Function}s it consists of one after another.
   * Checked exceptions will be caught and rethrown wrapped into {@link ExecutionException} which might be investigated
   * using {@link Exception#getCause()}.
   * @return execution result. Can't be null.
   * @throws ExecutionException if checked except
   */
  public @Nonnull A execute() throws ExecutionException {
    try {
      return perform(arg -> arg, rethrow());
    } catch (Exception e) {
      if (e instanceof RuntimeException) throw (RuntimeException) e;
      throw new ExecutionException(e);
    }
  }

  private static @Nonnull <R> ExceptionHandler<Exception, R> rethrow() {
    return e -> { throw e; };
  }

  /**
   * Executes this task like {@link #execute()} does, but checked exceptions are returned unwrapped as {@link Either.Left}
   *  value. If the execution is succesful, {@link Either.Right} is returned containing the result.
   * @return left failure or right result. Can't be null.
   */
  public @Nonnull Either<Exception, A> executeChecked() {
    return perform(Either::right, Either::left);
  }

  /**
   * Wraps this Task in another layer which adds the given exception handler. The function provided may be used to recover
   * from the exceptional condition or rewrap the exception (see {@link #except(Exception)}) depending on what kind of
   * exception was thrown.
   * @param function exception handler. Can't be null.
   * @return new layer of Task. Can't be null.
   * @throws NullPointerException if the argument was null.
   */
  public @Nonnull Task<A> caught(Function<? super Exception, Task<A>> function) {
    nonNull(function);
    return task(this, tk -> tk.perform(Task::task, function::$).perform(arg -> arg, rethrow()));
  }

  /**
   * Wraps this Task in another layer which transforms this task's result using the given function.
   * If this Task fails, no transformation happens and returned Task fails with the same exception.
   * @param function <b>A -&gt; B</b> transformation. Can't be null.
   * @param <B> new result type.
   * @return new layer of Task. Can't be null.
   * @throws NullPointerException if the argument was null.
   */
  public @Nonnull <B> Task<B> map(final Function<? super A, ? extends B> function) {
    nonNull(function);
    return task(this, tk -> tk.perform(function, rethrow()));
  }

  /**
   * Wraps this Task in another layer which applies the given function to this task's result. If this task contains
   * an exception, returned task fails with the same exception. If this task is successful and the task returned
   * by the function fails, returned task fails with its exception.
   * @param function <b>A -&gt; Task&lt;B&gt;</b> transformation. Can't be null.
   * @param <B> new result type.
   * @return new layer of Task. Can't be null.
   * @throws NullPointerException if the argument was null.
   */
  public @Nonnull <B> Task<B> bind(final Function<? super A, Task<B>> function) {
    nonNull(function);
    return task(this, tk -> tk.perform(function, rethrow()).perform(arg -> arg, rethrow()));
  }

  /**
   * Wraps this Task in another layer which discards the result of this task and executes the given task afterwards. Is
   * useful when you have a task resulting in {@link Unit} value and thus are not interested in the result but want to
   * preserve te order of operations.
   * @param task task to execute once this is done. Can't be null.
   * @param <B> new result type.
   * @return new layer of Task. Can't be null.
   * @throws NullPointerException if the argument was null.
   */
  public @Nonnull <B> Task<B> then(final Task<B> task) {
    nonNull(task);
    return task(this, prev -> { prev.perform(arg -> arg, rethrow()); return task.perform(arg -> arg, rethrow()); });
  }

  /**
   * Creates a new Task which contains the given value.
   * @param initial result value. Can't be null.
   * @param <A> result type.
   * @return new Task. Can't be null.
   * @throws NullPointerException if the argument was null.
   */
  public static @Nonnull <A> Task<A> task(final A initial) {
    return task(nonNull(initial), a -> a);
  }

  /**
   * Creates a new Task which fails with the given exception.
   * @param e exception to fail with. Can't be null.
   * @return new Task. Can't be null.
   * @throws NullPointerException if the argument was null.
   */
  public static @Nonnull <A> Task<A> except(final Exception e) {
    nonNull(e);
    return task(Unit.INSTANCE, nothing -> { throw e; });
  }

  /**
   * Creates a new Task which applies the given side-{@link Effect} to the given resource.
   * @param resource argument for the side-effecting action. Can't be null.
   * @param effect side-effecting action. Can't be null.
   * @param <A> resource type.
   * @return new Task. Can't be null.
   * @throws NullPointerException if the argument was null.
   */
  public static @Nonnull <A, B> Task<B> task(final A resource, final Effect<? super A, ? extends B> effect) {
    return task(resource, effect, arg -> {});
  }

  /**
   * Creates a new Task which applies the given side-{@link Effect} to the given resource and executes the given cleanup
   * {@link Procedure} when this task is done or fails. it might be used to close {@link Closeable}s, unlock {@link java.util.concurrent.locks.Lock}s
   * etc.
   * @param resource argument for the side-effecting action. Can't be null.
   * @param effect side-effecting action. Can't be null.
   * @param cleanup cleanup to execute.
   * @param <A> resource type.
   * @return new Task. Can't be null.
   * @throws NullPointerException if the argument was null.
   */
  @SuppressWarnings("unchecked")
  public static @Nonnull <A, B> Task<B> task(final A resource, final Effect<? super A, ? extends B> effect, final Procedure<? super A> cleanup) {
    return new Task<>(nonNull(resource), (Effect<Object, B>) nonNull(effect), (Procedure<Object>) nonNull(cleanup));
  }

  /**
   * Flattens nested Tasks.
   * @param task Task wrapped in another Task.
   * @param <A> result type of the returned Task.
   * @return flattened Task. Can't be null.
   * @throws NullPointerException if the argument was null.
   */
  static @Nonnull <A> Task<A> unwrap(final Task<? extends Task<A>> task) {
    return nonNull(task).bind(arg -> arg);
  }

  private interface ExceptionHandler<E extends Exception, R> {
    @Nonnull R handle(Exception exception) throws E;
  }
}