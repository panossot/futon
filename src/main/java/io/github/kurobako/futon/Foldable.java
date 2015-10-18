package io.github.kurobako.futon;

import javax.annotation.Nonnull;

public interface Foldable<A> {
  <B> B foldRight(@Nonnull BiFunction<A, B, B> function, B initial);

  <B> B foldLeft(@Nonnull BiFunction<B, A, B> function, B initial);
}
