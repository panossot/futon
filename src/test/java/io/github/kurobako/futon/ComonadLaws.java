package io.github.kurobako.futon;

import org.junit.Test;

import javax.annotation.Nonnull;

import static io.github.kurobako.futon.Function.id;
import static io.github.kurobako.futon.Pair.pair;
import static org.junit.Assert.assertEquals;

public class ComonadLaws {

  // extend extract = id
  @Test
  public void testStoreExtendExtract() {
    Store<Character, Integer> one = charAt().extend(Store::extract);
    Store<Character, Integer> another = charAt();
    assertEquals(one.extract(), another.extract());
  }

  // extract . extend f  = f
  @Test
  public void testStoreExtractExtend() {
    Function<Store<Character, Integer>, Integer> f = store -> store.peeks(i -> i++).hashCode();
    Integer one = charAt().extend(f).extract();
    Integer another = f.$(charAt());
    assertEquals(one, another);
  }

  // extend f . extend g = extend (f . extend g)
  @Test
  public void testStoreExtendExtend() {
    Function<Store<String, Integer>, Character> f = store -> store.extract().charAt(0);
    Function<Store<Character, Integer>, String> g = store -> store.extract().toString();
    Store<Character, Integer> one = charAt().extend(g).extend(f);
    Store<Character, Integer> another = charAt().extend(f.of(store -> store.extend(g)));
    assertEquals(one.extract(), another.extract());
  }

  // extract . duplicate = id
  @Test
  public void testStoreExtractDuplicate() {
    Store<Character, Integer> one = charAt().duplicate().extract();
    Store<Character, Integer> another = charAt();
    assertEquals(one.extract(), another.extract());
  }

  // fmap extract . duplicate = id
  @Test
  public void testStoreMapExtractDuplicate() {
    Store<Character, Integer> one = charAt().duplicate().map(Store::extract);
    Store<Character, Integer> another = charAt();
    assertEquals(one.extract(), another.extract());
  }

  // duplicate . duplicate = fmap duplicate . duplicate
  @Test
  public void testStoreDuplicateDuplicate() {
    Store<Store<Store<Character, Integer>, Integer>, Integer> one = charAt().duplicate().duplicate();
    Store<Store<Store<Character, Integer>, Integer>, Integer> another = charAt().duplicate().map(Store::duplicate);
    assertEquals(one.extract().extract().extract(), another.extract().extract().extract());
  }

  // extend f  = fmap f . duplicate
  @Test
  public void testStoreExtend() {
    Function<Store<Character, Integer>, String> f = store -> store.extract().toString();
    Store<String, Integer> one = charAt().extend(f);
    Store<String, Integer> another = charAt().duplicate().map(f);
    assertEquals(one.extract(), another.extract());
  }

  // duplicate = extend id
  @Test
  public void testStoreDuplicate() {
    Store<Store<Character, Integer>, Integer> one = charAt().duplicate();
    Store<Store<Character, Integer>, Integer> another = charAt().extend(id());
    assertEquals(one.extract().extract(), another.extract().extract());
  }

  // fmap f = extend (f . extract)
  @Test
  public void testStoreMap() {
    Function<Character, String> f = String::valueOf;
    Store<String, Integer> one = charAt().map(f);
    Store<String, Integer> another = charAt().extend(f.of(Store::extract));
    assertEquals(one.extract(), another.extract());
  }

  private static Store<Character, Integer> charAt() {
    return new Store<Character, Integer>() {
      private final String s = "The quick brown fox jumps over the lazy dog";

      @Override
      public @Nonnull Pair<Function<? super Integer, ? extends Character>, Integer> run() {
        return pair(s::charAt, 0);
      }
    };
  }
}
