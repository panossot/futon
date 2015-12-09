package io.github.kurobako.futon;

import javax.annotation.Nonnull;

import static java.util.Objects.requireNonNull;

public interface Foldable<A> {
  default  @Nonnull
  <M extends Semigroup<M>> M fold(final @Nonnull Function<? super A, ? extends M> function, final @Nonnull M empty) {
    requireNonNull(function, "function");
    requireNonNull(empty, "empty");
    return foldRight((a, m) -> m.append(function.$(a)), empty);
  }

  <B> B foldRight(@Nonnull BiFunction<? super A, ? super B, ? extends B> function, B initial);

  <B> B foldLeft(@Nonnull BiFunction<? super B, ? super A, ? extends B> function, B initial);
}
