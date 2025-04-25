package com.example.demo.pattarn;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;


public class InstrumentedHashSet<E> extends HashSet<E> {

    private int addCount = 0;

    public InstrumentedHashSet() {
    }

    @Override
    public boolean add(E e) {
        addCount++;
        return super.add(e);
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        addCount += c.size();
        return super.addAll(c);
    }

    public int getAddCount() {
        return addCount;
    }


    public static void main(String[] args) {
        InstrumentedHashSet<String> s = new InstrumentedHashSet<>();
        s.addAll(Arrays.asList("Snap", "Crackle", "Pop"));
        System.out.println(s.getAddCount());
    }

}

class InstrumentedHashSet1<E> extends InstrumentedHashSet<E> {
    public InstrumentedHashSet1(int i) {
        this();
//        super(12);
//        super();
    }

    public InstrumentedHashSet1() {
//      super(12);
//      super();
  }
}
