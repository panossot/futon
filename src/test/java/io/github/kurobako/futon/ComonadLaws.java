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
public class ComonadLaws {

  // extend extract = id
  @Theory
  public void testStoreExtendExtract(final Store<Character, Integer> original) {
    Store<Character, Integer> transformed = original.extend(Store::extract);
    assertEquals(transformed.extract(), original.extract());
  }

  // extract . extend f  = f
  @Theory
  public void testStoreExtractExtend(final Store<Character, Integer> data) {
    Function<Store<Character, Integer>, Integer> f = store -> store.peeks(i -> i++).hashCode();
    Integer one = data.extend(f).extract();
    Integer another = f.$(data);
    assertEquals(one, another);
  }

  // extend f . extend g = extend (f . extend g)
  @Theory
  public void testStoreExtendExtend(final Store<Character, Integer> data) {
    Function<Store<String, Integer>, Character> f = store -> store.extract().charAt(0);
    Function<Store<Character, Integer>, String> g = store -> store.extract().toString();
    Store<Character, Integer> one = data.extend(g).extend(f);
    Store<Character, Integer> another = data.extend(f.of(store -> store.extend(g)));
    assertEquals(one.extract(), another.extract());
  }

  // extract . duplicate = id
  @Theory
  public void testStoreExtractDuplicate(final Store<Character, Integer> original) {
    Store<Character, Integer> transformed = original.duplicate().extract();
    assertEquals(transformed.extract(), original.extract());
  }

  // fmap extract . duplicate = id
  @Theory
  public void testStoreMapExtractDuplicate(final Store<Character, Integer> data) {
    Store<Character, Integer> transformed = data.duplicate().map(Store::extract);
    assertEquals(transformed.extract(), data.extract());
  }

  // duplicate . duplicate = fmap duplicate . duplicate
  @Theory
  public void testStoreDuplicateDuplicate(final Store<Character, Integer> data) {
    Store<Store<Store<Character, Integer>, Integer>, Integer> one = data.duplicate().duplicate();
    Store<Store<Store<Character, Integer>, Integer>, Integer> another = data.duplicate().map(Store::duplicate);
    assertEquals(one.extract().extract().extract(), another.extract().extract().extract());
  }

  // extend f  = fmap f . duplicate
  @Theory
  public void testStoreExtend(final Store<Character, Integer> data) {
    Function<Store<Character, Integer>, String> f = store -> store.extract().toString();
    Store<String, Integer> one = data.extend(f);
    Store<String, Integer> another = data.duplicate().map(f);
    assertEquals(one.extract(), another.extract());
  }

  // duplicate = extend id
  @Theory
  public void testStoreDuplicate(final Store<Character, Integer> data) {
    Store<Store<Character, Integer>, Integer> one = data.duplicate();
    Store<Store<Character, Integer>, Integer> another = data.extend(id());
    assertEquals(one.extract().extract(), another.extract().extract());
  }

  // fmap f = extend (f . extract)
  @Theory
  public void testStoreMap(final Store<Character, Integer> data) {
    Function<Character, String> f = String::valueOf;
    Store<String, Integer> one = data.map(f);
    Store<String, Integer> another = data.extend(f.of(Store::extract));
    assertEquals(one.extract(), another.extract());
  }

  @DataPoint
  public static Store<Character, Integer> charAt0() {
    return StoreTest.charAt0();
  }
}
