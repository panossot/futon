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

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FunctionTest {

  @Test
  public void testOf() {
    final Function<Integer, Integer> f = i -> i++;
    final Function<Integer, Integer> g = i -> i*i;
    assertEquals(f.of(g).$(3), f.$(g.$(3)));
  }

  @Test(expected = NullPointerException.class)
  public void testOfNPE() {
    final Function<?, ?> f = a -> a;
    //noinspection ConstantConditions
    f.of(null);
  }

  @Test
  public void testStaticId() {
    final Object arg = new Object();
    assertEquals(Function.id().$(arg), arg);
  }
}
