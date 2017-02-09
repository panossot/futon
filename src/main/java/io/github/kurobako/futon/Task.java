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

import io.github.kurobako.futon.annotation.Opaque;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

import java.io.Closeable;
import java.util.concurrent.ExecutionException;

import static io.github.kurobako.futon.Util.nonNull;

/**
 * <p>Task monad is a computation built out of individual impure, side-effecting actions to be executed all at once or
 * step by step later.</p>
 * <p>Thus, task allows for some reasoning about the side-effecting code. Task may use custom finalizer code for its
 * resources and catches any checked exceptions produced by {@link Effect}
 * instances it runs. Note that {@link RuntimeException} thrown on execution will be rethrown as is unless caught with
 * {@link #caught(Function)}.</p>
 * <p>{@link #map(Function)} makes Task a functor.</p>
 * <p>{@link #bind(Kleisli)} and {@link #task(A)} form a monad.</p>
 * @see <a href="https://github.com/tel/scala-tk/">https://github.com/tel/scala-tk/</a>
 * @param <A> result type.
 */
@ThreadSafe
public final class Task<A> {
  @Nonnull final Object resource;
  @Nonnull final Effect<Object, ? extends A> effect;
  @Nonnull final Procedure<Object> cleanup;

  Task(final Object initial, final Effect<Object, A> effect, final Procedure<Object> cleanup) {
    this.resource = initial;
    this.effect = effect;
    this.cleanup = cleanup;
  }

  @SuppressWarnings("unchecked")
  @Nonnull <E extends Exception, R> R perform(Function<? super A, ? extends R> onSuccess, ExceptionHandler<? extends E, ? extends R> onFailure) throws E {
    Exception failure = null;
    try {
      return onSuccess.$(effect.perform(resource));
    } catch (final Exception e) {
      failure = e;
      return onFailure.handle(failure);
    } finally {
      try {
        cleanup.run(resource);
      } catch (final Exception e) {
        if (failure != null) failure.addSuppressed(e);
      }
    }
  }

  /**
   * Executes this task, running all {@link Effect}s and transforming {@link Function}s it consists of one after another.
   * Checked exceptions will be caught and rethrown wrapped into {@link ExecutionException} which might be investigated
   * using {@link Exception#getCause()}.
   * Subsequent calls of this method might result in different values returned due to impure nature of effects wrapped
   * in a task.
   * @return execution result. Can't be null.
   * @throws ExecutionException if checked except
   */
  @Opaque
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
   * Subsequent calls of this method might result in different values returned due to impure nature of effects wrapped
   * in a task.
   * @return left failure or right result. Can't be null.
   */
  @Opaque
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
   * @param kleisli <b>A -&gt; Task&lt;B&gt;</b> transformation. Can't be null.
   * @param <B> new result type.
   * @return new layer of Task. Can't be null.
   * @throws NullPointerException if the argument was null.
   */
  public @Nonnull <B> Task<B> bind(final Kleisli<? super A, B> kleisli) {
    nonNull(kleisli);
    return task(this, tk -> tk.perform(kleisli::run, rethrow()).perform(arg -> arg, rethrow()));
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
   * Interleaves this Task with the given Task and combines their results using the given function.
   * @param task a Task to zip with.
   * @param function <b>A -&gt; B -&gt; C</b> transformation. Can't be null.
   * @param <B> result type of the Task to zip with.
   * @param <C> new result type.
   * @return a Task zipped with the given Task. Can't be null.
   * @throws NullPointerException if any argument was null.
   */
  public @Nonnull <B, C> Task<C> zip(final Task<B> task, final BiFunction<? super A, ? super B, ? extends C> function) {
    nonNull(task);
    nonNull(function);
    return task(task, tk -> function.$(perform(arg -> arg, rethrow()), tk.perform(arg -> arg, rethrow())));
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

  /**
   * <p>Kleisli arrow is a pure function from an argument of type <b>A</b> to <b>Task&lt;B&gt;</b>. </p>
   * <p>It can be combined with other arrows of the same type (but parameterized differently) in ways similar to how
   * {@link Function}s can be combined with other functions.</p>
   * @param <A> argument type.
   * @param <B> return type parameter.
   */
  @FunctionalInterface
  interface Kleisli<A, B> {
    /**
     * Run the computation, producing a monad.
     * @param arg computation argument. Can't be null.
     * @return new monad. Can't be null.
     */
    @Nonnull Task<B> run(A arg);

    /**
     * Returns an arrow combining this arrow with the given arrow: <b>Z -&gt; A -&gt; B</b>.
     * @param kleisli <b>Z -&gt; A</b> arrow. Can't be null.
     * @param <Z> argument type for the new arrow.
     * @return new <b>Z -&gt; A</b> arrow. Can't be null.
     * @throws NullPointerException if the argument was null.
     */
    default @Nonnull <Z> Kleisli<Z, B> precomposeKleisli(final Kleisli<? super Z, A> kleisli) {
      nonNull(kleisli);
      return z -> kleisli.run(z).bind(this);
    }

    /**
     * Returns an arrow combining this arrow with the given pure function: <b>Z -&gt; A -&gt; B</b>.
     * @param function <b>Z -&gt; A</b> function. Can't be null.
     * @param <Z> argument type for the new arrow.
     * @return new <b>Z -&gt; A</b> arrow. Can't be null.
     * @throws NullPointerException if the argument was null.
     */
    default @Nonnull <Z> Kleisli<Z, B> precomposeFunction(final Function<? super Z, ? extends A> function) {
      nonNull(function);
      return z -> run(function.$(z));
    }

    /**
     * Returns an arrow combining this arrow with the given arrow: <b>A -&gt; B -&gt; C</b>.
     * @param kleisli <b>B -&gt; C</b> arrow. Can't be null.
     * @param <C> return type for the new arrow.
     * @return new <b>A -&gt; C</b> arrow. Can't be null.
     * @throws NullPointerException if the argument was null.
     */
    default @Nonnull <C> Kleisli<A, C> postcomposeKleisli(final Kleisli<? super B, C> kleisli) {
      nonNull(kleisli);
      return a -> run(a).bind(kleisli);
    }

    /**
     * Returns an arrow combining this arrow with the given pure function: <b>A -&gt; B -&gt; C</b>.
     * @param function <b>B -&gt; C</b> function. Can't be null.
     * @param <C> return type for the new arrow.
     * @return new <b>A -&gt; C</b> arrow. Can't be null.
     * @throws NullPointerException if the argument was null.
     */
    default @Nonnull <C> Kleisli<A, C> postcomposeFunction(final Function<? super B, ? extends C> function) {
      nonNull(function);
      return a -> run(a).map(function);
    }

    /**
     * Returns an arrow which maps its input using this arrow of it is {@link Either.Left} and passes it
     * unchanged otherwise.
     * @param <C> right component type.
     * @return new arrow. Can't be null.
     */
    default @Nonnull <C> Kleisli<Either<A, C>, Either<B, C>> left() {
      return ac -> ac.either(a -> run(a).map(Either::left), c -> task(Either.right(c)));
    }

    /**
     * Returns an arrow which maps its input using this arrow of it is {@link Either.Right} and passes it
     * unchanged otherwise.
     * @param <C> left component type.
     * @return new arrow. Can't be null.
     */
    default @Nonnull <C> Kleisli<Either<C, A>, Either<C, B>> right() {
      return ca -> ca.either(c -> task(Either.left(c)), a -> run(a).map(Either::right));
    }

    /**
     * Returns an arrow which maps first part of its input and passes the second part unchanged.
     * @param <C> right component type.
     * @return new arrow. Can't be null.
     */
    default @Nonnull <C> Kleisli<Pair<A, C>, Pair<B, C>> first() {
      return ac -> run(ac.first).zip(task(ac.second), Pair::pair);
    }

    /**
     * Returns an arrow which maps second part of its input and passes the first part unchanged.
     * @param <C> left component type.
     * @return new arrow. Can't be null.
     */
    default @Nonnull <C> Kleisli<Pair<C, A>, Pair<C, B>> second() {
      return ca -> task(ca.first).zip(run(ca.second), Pair::pair);
    }

    /**
     * Returns an arrow which maps its input using this arrow if it is {@link Either.Left} and using the given arrow if
     * it is {@link Either.Right}.
     * @param kleisli right <b>C -&gt; D</b> mapping. Can't be null.
     * @param <C> right argument type.
     * @param <D> right return type.
     * @return new arrow. Can't be null.
     * @throws NullPointerException if the argument is null.
     */
    default @Nonnull <C, D> Kleisli<Either<A, C>, Either<B, D>> sum(final Kleisli<? super C, D> kleisli) {
      nonNull(kleisli);
      return ac -> ac.either(a -> run(a).map(Either::left), c -> kleisli.run(c).map(Either::right));
    }

    /**
     * Returns an arrow which maps the first part of its input using this arrow and the second part using the given arrow.
     * @param kleisli second <b>C -&gt; D</b> mapping. Can't be null.
     * @param <C> second argument type.
     * @param <D> second return type.
     * @return new arrow. Can't be null.
     * @throws NullPointerException if the argument is null.
     */
    default @Nonnull <C, D> Kleisli<Pair<A, C>, Pair<B, D>> product(final Kleisli<? super C, D> kleisli) {
      nonNull(kleisli);
      return ac -> run(ac.first).zip(kleisli.run(ac.second), Pair::pair);
    }

    /**
     * Returns an arrow which maps input using this arrow if it is {@link Either.Left} or the given arrow if it is {@link Either.Right}.
     * @param kleisli left <b>C -&gt; B</b> mapping. Can't be null.
     * @param <C> right argument type.
     * @return new arrow. Can't be null.
     * @throws NullPointerException if the argument is null.
     */
    default @Nonnull <C> Kleisli<Either<A, C>, B> fanIn(final Kleisli<? super C, B> kleisli) {
      nonNull(kleisli);
      return ac -> ac.either(this::run, kleisli::run);
    }

    /**
     * Returns an arrow which maps its input using this arrow and the given arrow and returns two resulting values as a pair.
     * @param kleisli second <b>A -&gt; C</b> mapping. Can't be null.
     * @param <C> second return type.
     * @return new arrow. Can't be null.
     * @throws NullPointerException if the argument is null.
     */
    default @Nonnull <C> Kleisli<A, Pair<B, C>> fanOut(final Kleisli<? super A, C> kleisli) {
      nonNull(kleisli);
      return a -> run(a).zip(kleisli.run(a), Pair::pair);
    }

    /**
     * Returns an arrow wrapping the given function.
     * @param function <b>A -&gt; B</b> function to wrap. Can't be null.
     * @param <A> argument type.
     * @param <B> return type parameter.
     * @return an arrow. Can't be null.
     * @throws NullPointerException if the argument is null.
     */
    static @Nonnull <A, B> Kleisli<A, B> lift(final Function<? super A, ? extends B> function) {
      nonNull(function);
      return a -> task(function.$(a));
    }

    /**
     * Returns an arrow <b>(A -&gt; B, B) -&gt; B</b> which applies its input arrow (<b>A -&gt; B</b>) to its input
     * value (<b>A</b>) and returns the result (<b>B</b>).
     * @param <A> argument type.
     * @param <B> return type.
     * @return an arrow. Can't be null.
     */
    static @Nonnull <A, B> Kleisli<Pair<Kleisli<A, B>, A>, B> apply() {
      return ka -> ka.first.run(ka.second);
    }
  }
}