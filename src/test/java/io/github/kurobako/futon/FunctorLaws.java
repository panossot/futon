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

import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;


import static io.github.kurobako.futon.Function.id;
import static org.junit.Assert.assertEquals;

@RunWith(Theories.class)
public class FunctorLaws {

  // fmap id = id
  @Theory
  public void testStoreMapId(final Store<Character, Integer> original) {
    Store<Character, Integer> transformed = original.map(id());
    assertEquals(transformed.extract(), original.extract());
  }

  // fmap (p . q) = (fmap p) . (fmap q)
  @Theory
  public void testStoreMapOf(final Store<Character, Integer> data) {
    Function<Character, String> q = String::valueOf;
    Function<String, Character> p = s -> s.charAt(0);
    Store<Character, Integer> one = data.map(p.of(q));
    Store<Character, Integer> another = data.map(q).map(p);
    assertEquals(one.extract(), another.extract());
  }

  @DataPoint
  public static Store<Character, Integer> charAt0() {
    return StoreTest.charAt0();
  }
}
