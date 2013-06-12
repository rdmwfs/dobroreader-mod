package org.anonymous.dobrochan;

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class Reversed<T> implements Iterable<T> {
	private final List<T> original;

	public Reversed(List<T> original) {
		this.original = original;
	}

	@Override
	public Iterator<T> iterator() {
		final ListIterator<T> i = original.listIterator(original.size());

		return new Iterator<T>() {
			@Override
			public boolean hasNext() {
				return i.hasPrevious();
			}

			@Override
			public T next() {
				return i.previous();
			}

			@Override
			public void remove() {
				i.remove();
			}
		};
	}

	public static Reversed<String[]> reversed(List<String[]> original) {
		return new Reversed<String[]>(original);
	}
}