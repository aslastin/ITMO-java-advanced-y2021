package info.kgeorgiy.ja.slastin.arrayset;

import java.util.*;

public class ArraySet<E> extends AbstractSet<E> implements NavigableSet<E> {
    private final List<E> storage;
    private final Comparator<? super E> comparator;

    public ArraySet() {
        storage = Collections.emptyList();
        comparator = null;
    }

    public ArraySet(final Collection<? extends E> collection) {
        this(collection, null);
    }

    public ArraySet(final Collection<? extends E> collection, final Comparator<? super E> comparator) {
        final Set<E> treeSet = new TreeSet<>(comparator);
        treeSet.addAll(collection);
        storage = new ArrayList<>(treeSet);
        this.comparator = comparator;
    }

    private ArraySet(final List<E> list, final Comparator<? super E> comparator) {
        storage = list;
        this.comparator = comparator;
    }

    private void checkNonEmpty() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
    }

    private boolean checkIndex(final int index) {
        return 0 <= index && index < size();
    }

    private int getIndex(final E e, final int shiftFound, final int shiftNotFound) {
        int index = Collections.binarySearch(storage, e, comparator);
        if (index < 0) {
            index = -index - 1;
            return index + shiftNotFound;
        }
        return index + shiftFound;
    }

    private int getLowerIndex(final E e) {
        return getIndex(e, -1, -1);
    }

    private int getFloorIndex(final E e) {
        return getIndex(e, 0, -1);
    }

    private int getHigherIndex(final E e) {
        return getIndex(e, 1, 0);
    }

    private int getCeilingIndex(final E e) {
        return getIndex(e, 0, 0);
    }

    private E getElement(final int index) {
        return checkIndex(index) ? storage.get(index) : null;
    }

    private NavigableSet<E> unsafeSubSet(final E fromElement, final boolean fromInclusive,
                                         final E toElement, final boolean toInclusive) {
        final int fromIndex = fromInclusive ? getCeilingIndex(fromElement) : getHigherIndex(fromElement);
        final int toIndex = toInclusive ? getFloorIndex(toElement) : getLowerIndex(toElement);
        return new ArraySet<>(
                fromIndex > toIndex || !checkIndex(fromIndex) || !checkIndex(toIndex) ?
                        Collections.emptyList() :
                        storage.subList(fromIndex, toIndex + 1),
                comparator
        );
    }

    private ArraySet<E> getEmpty() {
        return new ArraySet<>(Collections.emptyList(), comparator);
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean contains(final Object o) {
        return Collections.binarySearch(storage, (E) o, comparator) >= 0;
    }

    @Override
    public Iterator<E> iterator() {
        return Collections.unmodifiableList(storage).iterator();
    }

    @Override
    public int size() {
        return storage.size();
    }

    @Override
    public E lower(final E e) {
        return getElement(getLowerIndex(e));
    }

    @Override
    public E floor(final E e) {
        return getElement(getFloorIndex(e));
    }

    @Override
    public E ceiling(final E e) {
        return getElement(getCeilingIndex(e));
    }

    @Override
    public E higher(final E e) {
        return getElement(getHigherIndex(e));
    }

    @Override
    public E pollFirst() {
        throw new UnsupportedOperationException();
    }

    @Override
    public E pollLast() {
        throw new UnsupportedOperationException();
    }

    @Override
    public NavigableSet<E> descendingSet() {
        return new ArraySet<>(new ReversibleList<>(storage).reverse(), Collections.reverseOrder(comparator));
    }

    @Override
    public Iterator<E> descendingIterator() {
        return descendingSet().iterator();
    }

    @Override
    @SuppressWarnings("unchecked")
    public NavigableSet<E> subSet(final E fromElement, final boolean fromInclusive,
                                  final E toElement, final boolean toInclusive) {
        if ((comparator != null && comparator.compare(fromElement, toElement) > 0) ||
                (comparator == null && ((Comparable<? super E>) fromElement).compareTo(toElement) > 0)) {
            throw new IllegalArgumentException("fromElement > toElement");
        }
        return unsafeSubSet(fromElement, fromInclusive, toElement, toInclusive);
    }

    @Override
    public NavigableSet<E> headSet(final E toElement, final boolean inclusive) {
        return isEmpty() ? getEmpty() : unsafeSubSet(first(), true, toElement, inclusive);
    }

    @Override
    public NavigableSet<E> tailSet(final E fromElement, final boolean inclusive) {
        return isEmpty() ? getEmpty() : unsafeSubSet(fromElement, inclusive, last(), true);
    }

    @Override
    public SortedSet<E> subSet(final E fromElement, final E toElement) {
        return subSet(fromElement, true, toElement, false);
    }

    @Override
    public SortedSet<E> headSet(final E toElement) {
        return headSet(toElement, false);
    }

    @Override
    public SortedSet<E> tailSet(final E fromElement) {
        return tailSet(fromElement, true);
    }

    @Override
    public Comparator<? super E> comparator() {
        return comparator;
    }

    @Override
    public E first() {
        checkNonEmpty();
        return storage.get(0);
    }

    @Override
    public E last() {
        checkNonEmpty();
        return storage.get(size() - 1);
    }
}
