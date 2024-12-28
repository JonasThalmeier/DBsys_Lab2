package simpledb;

import java.util.*;

/**
 * The Join operator implements the relational join operation.
 */
public class Join extends Operator {

    private static final long serialVersionUID = 1L;
    private JoinPredicate p;
    private OpIterator child1;
    private OpIterator child2;

    /**
     * Constructor. Accepts two children to join and the predicate to join them
     * on
     * 
     * @param p
     *               The predicate to use to join the children
     * @param child1
     *               Iterator for the left(outer) relation to join
     * @param child2
     *               Iterator for the right(inner) relation to join
     */
    public Join(JoinPredicate p, OpIterator child1, OpIterator child2) {
        // some code goes here
        this.p = p;
        this.child1 = child1;
        this.child2 = child2;

    }

    public JoinPredicate getJoinPredicate() {
        // some code goes here
        return this.p;
    }

    /**
     * @return
     *         the field name of join field1. Should be quantified by
     *         alias or table name.
     */
    public String getJoinField1Name() {
        // some code goes here
        return "t1." + this.child1.getTupleDesc().getFieldName(p.getField1());
    }

    /**
     * @return
     *         the field name of join field2. Should be quantified by
     *         alias or table name.
     */
    public String getJoinField2Name() {
        // some code goes here
        return "t2." + this.child2.getTupleDesc().getFieldName(p.getField2());
    }

    /**
     * @see simpledb.TupleDesc#merge(TupleDesc, TupleDesc) for possible
     *      implementation logic.
     */
    public TupleDesc getTupleDesc() {
        // some code goes here
        TupleDesc td1 = this.child1.getTupleDesc();
        TupleDesc td2 = this.child2.getTupleDesc();
        int totalFields = td1.numFields() + td2.numFields();

        Type[] types = new Type[totalFields];
        String[] fieldNames = new String[totalFields];

        for (int i = 0; i < td1.numFields(); i++) {
            types[i] = td1.getFieldType(i);

            // use the getJoinField1Name() method to get the field name of the join field
            if (p.getField1() == i) {
                fieldNames[i] = this.getJoinField1Name();
            }

            fieldNames[i] = td1.getFieldName(i);
        }

        for (int i = 0; i < td2.numFields(); i++) {
            types[i + td1.numFields()] = td2.getFieldType(i);

            // use the getJoinField2Name() method to get the field name of the join field
            if (p.getField2() == i) {
                fieldNames[i] = this.getJoinField2Name();
            }

            fieldNames[i + td1.numFields()] = td2.getFieldName(i);
        }

        return new TupleDesc(types, fieldNames);
    }

    public void open() throws DbException, NoSuchElementException,
            TransactionAbortedException {
        // some code goes here
        this.child1.open();
        this.child2.open();
    }

    public void close() {
        // some code goes here
        this.child1.close();
        this.child2.close();
    }

    public void rewind() throws DbException, TransactionAbortedException {
        // some code goes here
        this.child1.rewind();
        this.child2.rewind();
    }

    /**
     * Returns the next tuple generated by the join, or null if there are no
     * more tuples. Logically, this is the next tuple in r1 cross r2 that
     * satisfies the join predicate. There are many possible implementations;
     * the simplest is a nested loops join.
     * <p>
     * Note that the tuples returned from this particular implementation of Join
     * are simply the concatenation of joining tuples from the left and right
     * relation. Therefore, if an equality predicate is used there will be two
     * copies of the join attribute in the results. (Removing such duplicate
     * columns can be done with an additional projection operator if needed.)
     * <p>
     * For example, if one tuple is {1,2,3} and the other tuple is {1,5,6},
     * joined on equality of the first column, then this returns {1,2,3,1,5,6}.
     * 
     * @return The next matching tuple.
     * @see JoinPredicate#filter
     */
    protected Tuple fetchNext() throws TransactionAbortedException, DbException {
        // some code goes here
        while (this.child1.hasNext()) {
            Tuple t1 = this.child1.next();
            while (this.child2.hasNext()) {
                Tuple t2 = this.child2.next();

                // check if the two tuples satisfy the join predicate
                if (this.p.filter(t1, t2)) {
                    // get a new tuple desc, as defined in the getTupleDesc() method
                    TupleDesc td = this.getTupleDesc();

                    // create a new tuple
                    Tuple t = new Tuple(td);

                    // fill the new tuple with the fields of the two tuples

                    // iterate over the fields of the first tuple, add them to the new tuple
                    for (int i = 0; i < t1.getTupleDesc().numFields(); i++) {
                        t.setField(i, t1.getField(i));
                    }
                    // iterate over the fields of the second tuple, add them to the new tuple
                    for (int i = 0; i < t2.getTupleDesc().numFields(); i++) {
                        t.setField(i + t1.getTupleDesc().numFields(), t2.getField(i));
                    }

                    return t;
                }

            }

        }
        // if we reach here, it means there are no more tuples that satisfy the join
        // predicate
        return null;
    }

    @Override
    public OpIterator[] getChildren() {
        // some code goes here
        return new OpIterator[] { this.child1, this.child2 };

    }

    @Override
    public void setChildren(OpIterator[] children) {
        // some code goes here
        if (children.length != 2) {
            throw new IllegalArgumentException("Join operator requires exactly two children.");
        }

        // Set the children to the corresponding child operators

        this.child1 = children[0];
        this.child2 = children[1];
    }

}
