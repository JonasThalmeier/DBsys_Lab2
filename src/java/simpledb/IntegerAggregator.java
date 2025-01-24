package simpledb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Knows how to compute some aggregate over a set of IntFields.
 */
public class IntegerAggregator implements Aggregator {

    private static final long serialVersionUID = 1L;

    private int gbfield;
    private Type gbfieldtype;
    private int afield;
    private Op what;
    private Map<Field, Integer> groups;
    private Map<Field, Integer> counts;

    /**
     * Aggregate constructor
     * 
     * @param gbfield
     *            the 0-based index of the group-by field in the tuple, or
     *            NO_GROUPING if there is no grouping
     * @param gbfieldtype
     *            the type of the group by field (e.g., Type.INT_TYPE), or null
     *            if there is no grouping
     * @param afield
     *            the 0-based index of the aggregate field in the tuple
     * @param what
     *            the aggregation operator
     */

    public IntegerAggregator(int gbfield, Type gbfieldtype, int afield, Op what) {
        // some code goes here
        // Initialize instance variables with the provided parameters.
        this.gbfield = gbfield;
        this.gbfieldtype = gbfieldtype;
        this.afield = afield;
        this.what = what;
        this.groups = new HashMap<>(); // Map: group key -> aggregated value.
        this.counts = new HashMap<>(); // Map: group key -> count of tuples in the group.
    }

    /**
     * Merge a new tuple into the aggregate, grouping as indicated in the
     * constructor
     * 
     * @param tup
     *            the Tuple containing an aggregate field and a group-by field
     */
    public void mergeTupleIntoGroup(Tuple tup) {
        // some code goes here

        // Determine the group key based on whether grouping is enabled or not.
        // If no grouping, the group key is set to null.
        Field groupKey = (gbfield == NO_GROUPING) ? null : tup.getField(gbfield);
        // Extract the value of the aggregate field from the tuple.
        int value = ((IntField) tup.getField(afield)).getValue();
        // Check if the group key already exists in the groups map.
        if (!groups.containsKey(groupKey)) {
            // If not, initialize the group with the current value and, if needed, the count.
            groups.put(groupKey, value);
            counts.put(groupKey, 1); // Used for AVG.
        } else {
            // Perform the specified aggregation operation based on the 'what' field.
            switch (what) {
                case COUNT:
                    groups.put(groupKey, groups.get(groupKey) + 1);
                    break;
                case SUM:
                    groups.put(groupKey, groups.get(groupKey) + value);
                    break;
                case AVG:
                    groups.put(groupKey, groups.get(groupKey) + value);
                    counts.put(groupKey, counts.get(groupKey) + 1);
                    break;
                case MIN:
                    groups.put(groupKey, Math.min(groups.get(groupKey), value));
                    break;
                case MAX:
                    groups.put(groupKey, Math.max(groups.get(groupKey), value));
                    break;
            }
        }
    }

    /**
     * Create a OpIterator over group aggregate results.
     * 
     * @return a OpIterator whose tuples are the pair (groupVal, aggregateVal)
     *         if using group, or a single (aggregateVal) if no grouping. The
     *         aggregateVal is determined by the type of aggregate specified in
     *         the constructor.
     */
    public OpIterator iterator() {
        // some code goes here
        List<Tuple> results = new ArrayList<>();

        // Define the schema (TupleDesc) for the result tuples.
        // If there is no grouping, the schema consists of a single integer field for the aggregate value.
        // Otherwise, it includes the group-by field type and the aggregate value type.
        TupleDesc td = (gbfield == NO_GROUPING) ?
                new TupleDesc(new Type[]{Type.INT_TYPE}) :
                new TupleDesc(new Type[]{gbfieldtype, Type.INT_TYPE});

        // Iterate through each group and its associated aggregate value.
        for (Map.Entry<Field, Integer> entry : groups.entrySet()) {
            Tuple t = new Tuple(td);
            // If no grouping is applied, the result only contains the aggregate value.
            if (gbfield == NO_GROUPING) {
                t.setField(0, new IntField(entry.getValue()));
            } else {
                // Set the group-by field value in the first position of the tuple.
                t.setField(0, entry.getKey());
                // For AVG, compute the average using the sum and count.
                // Otherwise, use the aggregate value directly.
                if (what == Op.AVG) {
                    t.setField(1, new IntField(entry.getValue() / counts.get(entry.getKey())));
                } else {
                    t.setField(1, new IntField(entry.getValue()));
                }
            }
            results.add(t);
        }
        return new TupleIterator(td, results);
    }

}
