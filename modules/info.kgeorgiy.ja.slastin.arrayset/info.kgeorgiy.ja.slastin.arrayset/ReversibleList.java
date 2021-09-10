package info.kgeorgiy.ja.slastin.arrayset;

import java.util.AbstractList;
import java.util.List;

public class ReversibleList<E> extends AbstractList<E> {
    private final List<E> storage;
    private boolean reversed;

    public ReversibleList(final List<E> list) {
        if (list instanceof ReversibleList) {
            var reversibleList = (ReversibleList<E>) list;
            storage = reversibleList.storage;
            reversed = reversibleList.reversed;
        } else {
            storage = list;
            reversed = false;
        }
    }

    public ReversibleList<E> reverse() {
        reversed = !reversed;
        return this;
    }

    @Override
    public int size() {
        return storage.size();
    }

    @Override
    public E get(final int index) {
        return storage.get(reversed ? size() - 1 - index : index);
    }
}
