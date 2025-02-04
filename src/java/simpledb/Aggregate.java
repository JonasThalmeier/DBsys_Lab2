package simpledb;

import java.util.*;

//tes

/**
 * The Aggregation operator that computes an aggregate (e.g., sum, avg, max,
 * min). Note that we only support aggregates over a single column, grouped by a
 * single column.
 */
public class Aggregate extends Operator {

    private static final long serialVersionUID = 1L;

    private OpIterator child;          // The child operator providing input tuples.
    private int afield;                // The index of the field to compute the aggregate over.
    private int gfield;                // The index of the group-by field (-1 for no grouping).
    private Aggregator.Op aop;         // The aggregation operator (SUM, AVG, etc.).
    private Aggregator aggregator;     // The specific IntegerAggregator or StringAggregator to compute results.
    private OpIterator resultIterator; // Iterator over the aggregated results.


    /**
     * Constructor.
     * 
     * Implementation hint: depending on the type of afield, you will want to
     * construct an {@link IntegerAggregator} or {@link StringAggregator} to help
     * you with your implementation of readNext().
     * 
     * 
     * @param child
     *            The OpIterator that is feeding us tuples.
     * @param afield
     *            The column over which we are computing an aggregate.
     * @param gfield
     *            The column over which we are grouping the result, or -1 if
     *            there is no grouping
     * @param aop
     *            The aggregation operator to use
     */
    public Aggregate(OpIterator child, int afield, int gfield, Aggregator.Op aop) {
	// some code goes here
        this.child = child;
        this.afield = afield;
        this.gfield = gfield;
        this.aop = aop;
        Type gtype = (gfield == Aggregator.NO_GROUPING) ? null : child.getTupleDesc().getFieldType(gfield);

        // Choose the correct aggregator based on the type of afield
        if (child.getTupleDesc().getFieldType(afield) == Type.INT_TYPE) {
            this.aggregator = new IntegerAggregator(gfield, gtype, afield, aop);
        } else {
            this.aggregator = new StringAggregator(gfield, gtype, afield, aop);
        }
    }

    /**
     * @return If this aggregate is accompanied by a groupby, return the groupby
     *         field index in the <b>INPUT</b> tuples. If not, return
     *         {@link simpledb.Aggregator#NO_GROUPING}
     * */
    public int groupField() {
	// some code goes here
	return gfield;
    }

    /**
     * @return If this aggregate is accompanied by a group by, return the name
     *         of the groupby field in the <b>OUTPUT</b> tuples. If not, return
     *         null;
     * */
    public String groupFieldName() {
	// some code goes here
        // If there is no grouping, return null.
        if (gfield == Aggregator.NO_GROUPING) {
            return null;
        }
        // Otherwise, return the name of the group-by field from the child tuple's TupleDesc.
        return child.getTupleDesc().getFieldName(gfield);
    }

    /**
     * @return the aggregate field
     * */
    public int aggregateField() {
	// some code goes here
	return afield;
    }

    /**
     * @return return the name of the aggregate field in the <b>OUTPUT</b>
     *         tuples
     * */
    public String aggregateFieldName() {
	// some code goes here
	return child.getTupleDesc().getFieldName(afield);
    }

    /**
     * @return return the aggregate operator
     * */
    public Aggregator.Op aggregateOp() {
	// some code goes here
	return aop;
    }

    public static String nameOfAggregatorOp(Aggregator.Op aop) {
	return aop.toString();
    }

    public void open() throws NoSuchElementException, DbException,
	    TransactionAbortedException {
	    // some code goes here
        // Open the child operator and aggregate tuples using the aggregator.
        super.open();
        child.open();

        // Aggregate all tuples from the child operator into the aggregator.
        while (child.hasNext()) {
            aggregator.mergeTupleIntoGroup(child.next());
        }

        // Prepare the result iterator from the aggregator.
        resultIterator = aggregator.iterator();
        resultIterator.open();
    }

    /**
     * Returns the next tuple. If there is a group by field, then the first
     * field is the field by which we are grouping, and the second field is the
     * result of computing the aggregate. If there is no group by field, then
     * the result tuple should contain one field representing the result of the
     * aggregate. Should return null if there are no more tuples.
     */
    protected Tuple fetchNext() throws TransactionAbortedException, DbException {
	// some code goes here
        // Use the result iterator to fetch the next aggregated result tuple.
        if (resultIterator != null && resultIterator.hasNext()) {
            return resultIterator.next();
        }
        // Return null if there are no more tuples.
	    return null;
    }

    public void rewind() throws DbException, TransactionAbortedException {
	// some code goes here
        // Rewind the child operator and the result iterator.
        child.rewind();
        resultIterator.rewind();
    }

    /**
     * Returns the TupleDesc of this Aggregate. If there is no group by field,
     * this will have one field - the aggregate column. If there is a group by
     * field, the first field will be the group by field, and the second will be
     * the aggregate value column.
     * 
     * The name of an aggregate column should be informative. For example:
     * "aggName(aop) (child_td.getFieldName(afield))" where aop and afield are
     * given in the constructor, and child_td is the TupleDesc of the child
     * iterator.
     */
    public TupleDesc getTupleDesc() {
	// some code goes here
        // Get the TupleDesc of the child operator
        TupleDesc childDesc = child.getTupleDesc();

        // If there is no grouping, return a TupleDesc with one field for the aggregate result
        if (gfield == Aggregator.NO_GROUPING) {
            return new TupleDesc(new Type[]{Type.INT_TYPE}, new String[]{nameOfAggregatorOp(aop) + "(" + childDesc.getFieldName(afield) + ")"});
        }

        // If there is grouping, return a TupleDesc with two fields: the group-by field and the aggregate result
        return new TupleDesc(
                new Type[]{childDesc.getFieldType(gfield), Type.INT_TYPE},
                new String[]{childDesc.getFieldName(gfield), nameOfAggregatorOp(aop) + "(" + childDesc.getFieldName(afield) + ")"}
        );
    }

    public void close() {
	// some code goes here
        super.close();
        child.close();
        resultIterator.close();
    }

    @Override
    public OpIterator[] getChildren() {
	// some code goes here
        return new OpIterator[]{child};
    }

    @Override
    public void setChildren(OpIterator[] children) {
	// some code goes here
        // Ensure only one child is provided
        if (children.length != 1) {
            throw new IllegalArgumentException("Aggregate only supports a single child.");
        }
        this.child = children[0];
    }
    
}
