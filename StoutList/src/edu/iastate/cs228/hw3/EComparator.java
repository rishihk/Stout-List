package edu.iastate.cs228.hw3;

import java.util.Comparator;

public class EComparator<E extends Comparable<? super E>> implements Comparator<E> {

	@Override
	public int compare(E o1, E o2) {
		return o1.compareTo(o2);
	}

}
