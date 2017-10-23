package App;

import org.apache.commons.lang3.tuple.Pair;

/**
 * Created by Алексей on 21.10.2017.
 */
public class MyPair<T> extends Pair {
    public T left;
    public T right;

    @Override
    public T getLeft() {
        return left;
    }

    @Override
    public T getRight() {
        return right;
    }

    public MyPair(T left, T right) {
        this.left = left;
        this.right = right;
    }

    public int compareTo(Object o) {
        return 0;
    }

    public Object setValue(Object value) {
        return null;
    }
}