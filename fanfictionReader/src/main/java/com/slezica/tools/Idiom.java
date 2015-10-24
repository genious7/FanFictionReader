/*
 * The MIT License
 * Copyright (c) 2011 Santiago Lezica (slezica89@gmail.com)
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 */

package com.slezica.tools;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("unchecked")
public class Idiom {

    /* This class is kind of an experiment.
     * The methods provided here are meant to be statically
     * imported to add some syntactic sugar to common operations.
     * 
     * God knows Java needs some.
     * 
     * Statically importing the functions declared below, you can
     * have some of the syntactic shortcuts readily available in more
     * expressive languages. For example:
     * 
     * List<Integer> someNumbers = list(1, 2, 3, 4);
     * 
     * Set<String> aSet = set("hey", "what's", "up");
     * 
     * for (int i: range(0, 10))
     *     System.out.println(i);
     * 
     *  for (int i : set(1, 5, 23, 6, 5)) {
     *      // This has poor performance, for obvious reasons
     *      System.out.println(range(1, 6).contains(i));
     *  }
     *  
     *  String[] strings = array("some", "strings");
     *  
     *  final Map<Integer, String> = map(
     *      t(22, "myAge"),
     *      t(53, "myDadsAge")
     *  );
     *  
     *  return t(124, anObject.getSomething()) // tuple return 
     * 
     * Java does provide initialization techniques (such as double-brace
     * initialization) that allow for similar syntax, albeit much more
     * verbose. The methods below take advantage of the compiler's type
     * inference capabilities (which are rarely an advantage) to save
     * some typing.
     * 
     * Again, this is an experiment. It's far from standard Java practice,
     * so use can quickly turn into abuse.
     */
    
    public static <T> T[] array(T... elements) {
        return elements;
    }
    
    public static <T> List<T> list(T... elements) {
        return Collections.unmodifiableList(Arrays.asList(elements));
    }
    
    public static <T> Set<T> set(T... elements) {
        return Collections.unmodifiableSet(new HashSet<T>(Arrays.asList(elements)));
    }
    
    public static <K, V> Map<K, V> map(Tuple2<K, V>... elements) {
        Map<K, V> map = new HashMap<K, V>();
        
        for (Tuple2<K, V> t : elements)
            map.put(t.first(), t.second());
        
        return Collections.unmodifiableMap(map);
    }
    
    public static Range range(int start, int endNonInclusive) {
        return new Range(start, endNonInclusive);
    }
    
    public static <T0> Tuple1<T0> t(T0 i0) {
        return new Tuple1<T0>(i0);
    }
    
    public static <T0, T1> Tuple2<T0, T1> t(T0 i0, T1 i1) {
        return new Tuple2<T0, T1>(i0, i1);
    }
    
    
    public static class Range implements Iterable<Integer> {
        
        private static class RangeIterator implements Iterator<Integer> {
            private int current;
            private final Range owner;
            
            private RangeIterator(Range range) {
                owner   = range;
                current = range.start;
            }
            
            @Override
            public boolean hasNext() {
                return ((current + 1) < owner.end);
            }

            @Override
            public Integer next() {
                return (current++);
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        }
        /* Note: end is non-inclusive, meaning the range covers all
         * items t such that start <= t < end
        */
        private final int start, end;

        private Range(int start, int end) {
            this.start = start;
            this.end   = end;
        }
        
        @Override
        public Iterator<Integer> iterator() {
            return new RangeIterator(this);
        }
        
        public boolean contains(int x) {
            return (start <= x && x < end);
        }
    }
      
    public static class Tuple1<T0> {
        protected final Object[] items = new Object[size()];
        
        public Tuple1(T0 i0) {
            items[0] = i0;
        }
        
        public Object at(int index) { return items[index]; }
        
        public int size() { return 1; }
        
		public T0  first()  { return (T0) items[0]; }        
    }
    
    public static class Tuple2<T0, T1> extends Tuple1<T0> {
        public Tuple2(T0 i0, T1 i1) {
            super(i0);
            items[1] = i1;
        }
        
        public int size() { return 2; }
        public T1  second()  { return (T1) items[1]; }
    }

    public static class Tuple3<T0, T1, T2> extends Tuple2<T0, T1> {
        public Tuple3(T0 i0, T1 i1, T2 i2) {
            super(i0, i1);
            items[2] = i1;
        }
        
        public int size() { return 3; }
        public T2  third()  { return (T2) items[2]; }
    }
}
