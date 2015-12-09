package io.github.kurobako.futon;

import javax.annotation.Nonnull;

public interface Semigroup<Self extends Semigroup<Self>> {
  Self append(@Nonnull Self another);
}
