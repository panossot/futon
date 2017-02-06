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
 * <p>Semigroup members can be combined using the associative binary operation.</p>
 * <p>Note that {@link #append(E)}</p> might not have commutative property which might be useful in some use cases.
 * Semigroup with a neutral element (neutral.append(this) equal to this.append(neutral) equal to this) is a monoid.</p>
 * @see <a href="https://en.wikipedia.org/wiki/Semigroup">https://en.wikipedia.org/wiki/Semigroup</a>
 * @param <E> member type.
 */
public interface Semigroup<E extends Semigroup<E>> {
  /**
   * Associative operation combining this member of the semigroup with the given member.
   * @param element semigroup member to combine with. Can't be null.
   * @return associative operation result. Can't be null.
   */
  @Nonnull E append(E element);
}
