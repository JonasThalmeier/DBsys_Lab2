package simpledb;

import java.io.IOException;

/**
 * Inserts tuples read from the child operator into the tableId specified in the
 * constructor
 */
public class Insert extends Operator {

    private static final long serialVersionUID = 1L;

    private TransactionId transactionId;
    private OpIterator child;
    private int tableId;
    private boolean inserted;

    /**
     * Constructor.
     *
     * @param t
     *                The transaction running the insert.
     * @param child
     *                The child operator from which to read tuples to be inserted.
     * @param tableId
     *                The table in which to insert tuples.
     * @throws DbException
     *                     if TupleDesc of child differs from table into which we
     *                     are to
     *                     insert.
     */
    public Insert(TransactionId t, OpIterator child, int tableId)
            throws DbException {
        // some code goes here
        this.transactionId = t;
        this.child = child;
        this.tableId = tableId;
        this.inserted = false;

        if (!child.getTupleDesc().equals(Database.getCatalog().getTupleDesc(tableId))) {
            throw new DbException("tupledesc is not correct");
        }
    }

    public TupleDesc getTupleDesc() {
        // some code goes here
        return new TupleDesc(new Type[] { Type.INT_TYPE });
    }

    public void open() throws DbException, TransactionAbortedException {
        // some code goes here
        super.open();
        child.open();
    }

    public void close() {
        // some code goes here
        super.close();
        child.close();
    }

    public void rewind() throws DbException, TransactionAbortedException {
        // some code goes here
        child.rewind();
    }

    /**
     * Inserts tuples read from child into the tableId specified by the
     * constructor. It returns a one field tuple containing the number of
     * inserted records. Inserts should be passed through BufferPool. An
     * instances of BufferPool is available via Database.getBufferPool(). Note
     * that insert DOES NOT need check to see if a particular tuple is a
     * duplicate before inserting it.
     *
     * @return A 1-field tuple containing the number of inserted records, or
     *         null if called more than once.
     * @see Database#getBufferPool
     * @see BufferPool#insertTuple
     */
    protected Tuple fetchNext() throws TransactionAbortedException, DbException {
        // some code goes here
        int count = 0;
        BufferPool bufferpool = Database.getBufferPool();

        // if already inserted, do nothing
        if (inserted == true) {
            return null;
        }

        inserted = true;

        // iterate over all the elements to be inserted
        while (child.hasNext()) {
            Tuple tuple = child.next();
            try {
                bufferpool.insertTuple(transactionId, tableId, tuple);
                count++;
            } catch (IOException e) {
                throw new DbException("error inserting tuple");
            }
        }

        // store and return the number of inserted tuple
        Tuple insertcount = new Tuple(getTupleDesc());
        insertcount.setField(0, new IntField(count));

        return insertcount;
    }

    @Override
    public OpIterator[] getChildren() {
        // some code goes here
        return new OpIterator[] { child };
    }

    @Override
    public void setChildren(OpIterator[] children) {
        // some code goes here

        this.child = children[0];
    }
}
