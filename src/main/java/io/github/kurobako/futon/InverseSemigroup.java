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

/**
 * <p>Inverse semigroup is a {@link Semigroup} with an inverse operation added.</p>
 * <p>Inverse Semigroup with a neutral element (neutral.append(this) equal to this.append(neutral) equal to this) is a
 * group.</p>
 * @see <a href="https://en.wikipedia.org/wiki/Inverse_semigroup">https://en.wikipedia.org/wiki/Inverse_semigroup</a>
 * @param <E>
 */
public interface InverseSemigroup<E extends InverseSemigroup<E>> extends Semigroup<E> {
  /**
   * Returns an inverse element for this member so that this.append(this.inverse()).append(this) is equal to this and
   * this.inverse().append(this).append(this.inverse()) is equal to this.inverse().
   * @return an inverse. Can't be null.
   */
  @Nonnull E inverse();
}
