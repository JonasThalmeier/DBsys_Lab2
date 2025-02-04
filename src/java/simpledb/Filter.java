package simpledb;

import java.util.*;

/**
 * Filter is an operator that implements a relational select.
 */
public class Filter extends Operator {

    private static final long serialVersionUID = 1L;
    private Predicate p;
    private OpIterator child;

    /**
     * Constructor accepts a predicate to apply and a child operator to read
     * tuples to filter from.
     * 
     * @param p
     *              The predicate to filter tuples with
     * @param child
     *              The child operator
     */
    public Filter(Predicate p, OpIterator child) {
        // some code goes here
        this.p = p;
        this.child = child;
    }

    public Predicate getPredicate() {
        // some code goes here
        return this.p;
    }

    public TupleDesc getTupleDesc() {
        // some code goes here
        return this.child.getTupleDesc();

    }

    public void open() throws DbException, NoSuchElementException,
            TransactionAbortedException {
        // some code goes here
        super.open();
        this.child.open();

    }

    public void close() {
        // some code goes here
        super.close();
        this.child.close();
    }

    public void rewind() throws DbException, TransactionAbortedException {
        // some code goes here
        this.child.rewind();
    }

    /**
     * AbstractDbIterator.readNext implementation. Iterates over tuples from the
     * child operator, applying the predicate to them and returning those that
     * pass the predicate (i.e. for which the Predicate.filter() returns true.)
     * 
     * @return The next tuple that passes the filter, or null if there are no
     *         more tuples
     * @see Predicate#filter
     */
    protected Tuple fetchNext() throws NoSuchElementException,
            TransactionAbortedException, DbException {
        // some code goes here

        while (this.child.hasNext()) {
            // get the next tuple
            Tuple t = this.child.next();
            // check if the tuple satisfies the predicate
            if (this.p.filter(t)) {
                return t; // return the tuple, if not continue the loop until we find a tuple that
                          // satisfies the predicate
            }

        }
        // if we reach here, it means there are no more tuples that satisfy the
        // predicate
        return null;
    }

    @Override
    public OpIterator[] getChildren() {
        // some code goes here
        return new OpIterator[] { this.child }; // assume only one child?
    }

    @Override
    public void setChildren(OpIterator[] children) {
        // some code goes here

        // if more than one child, throw exception (we assume only one child)
        if (children.length != 1) {
            throw new IllegalArgumentException("Filter operator only accepts one child");
        }

        this.child = children[0];

    }

}
