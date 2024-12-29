package simpledb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Knows how to compute some aggregate over a set of StringFields.
 */
public class StringAggregator implements Aggregator {

    private static final long serialVersionUID = 1L;

    private int gbfield; // Group-by field index
    private Type gbfieldtype; // Type of the group-by field
    private int afield; // Aggregate field index
    private Op what; // Aggregation operator (only supports COUNT)
    private Map<Field, Integer> groupsInt; // For COUNT
    private Map<Field, String> groupsString;   // For MIN and MAX


    /**
     * Aggregate constructor
     * @param gbfield the 0-based index of the group-by field in the tuple, or NO_GROUPING if there is no grouping
     * @param gbfieldtype the type of the group by field (e.g., Type.INT_TYPE), or null if there is no grouping
     * @param afield the 0-based index of the aggregate field in the tuple
     * @param what aggregation operator to use -- only supports COUNT
     * @throws IllegalArgumentException if what != COUNT
     */

    public StringAggregator(int gbfield, Type gbfieldtype, int afield, Op what) {
        // some code goes here
        if (what != Op.COUNT && what != Op.MIN && what != Op.MAX) {
            throw new IllegalArgumentException("StringAggregator only supports COUNT, MIN, and MAX");
        }
        this.gbfield = gbfield;
        this.gbfieldtype = gbfieldtype;
        this.afield = afield;
        this.what = what;
        // Initialize maps based on the operation
        if (what == Op.COUNT) {
            groupsInt = new HashMap<>();
        } else{
            groupsString = new HashMap<>();
        }
    }

    /**
     * Merge a new tuple into the aggregate, grouping as indicated in the constructor
     * @param tup the Tuple containing an aggregate field and a group-by field
     */
    public void mergeTupleIntoGroup(Tuple tup) {
        // some code goes here
        Field groupKey = (gbfield == Aggregator.NO_GROUPING) ? null : tup.getField(gbfield);
        String value = ((StringField) tup.getField(afield)).getValue();

        if (what == Op.COUNT) {
            // Increment the count for this group key
            groupsInt.put(groupKey, groupsInt.getOrDefault(groupKey, 0) + 1);
        } else if (what == Op.MIN) {
            // Update the minimum for this group key
            groupsString.put(groupKey, groupsString.containsKey(groupKey)
                    ? (value.compareTo(groupsString.get(groupKey)) < 0 ? value : groupsString.get(groupKey))
                    : value);
        } else if (what == Op.MAX) {
            // Update the maximum for this group key
            groupsString.put(groupKey, groupsString.containsKey(groupKey)
                    ? (value.compareTo(groupsString.get(groupKey)) > 0 ? value : groupsString.get(groupKey))
                    : value);
        }
    }

    /**
     * Create a OpIterator over group aggregate results.
     *
     * @return a OpIterator whose tuples are the pair (groupVal,
     *   aggregateVal) if using group, or a single (aggregateVal) if no
     *   grouping. The aggregateVal is determined by the type of
     *   aggregate specified in the constructor.
     */
    public OpIterator iterator() {
        // some code goes here
        List<Tuple> results = new ArrayList<>();
        TupleDesc td;

        // Define the output schema based on the operation
        if (what == Op.COUNT) {
            // COUNT produces an integer result
            if (gbfield == Aggregator.NO_GROUPING) {
                td = new TupleDesc(new Type[]{Type.INT_TYPE}); // Single integer field for COUNT
            } else {
                td = new TupleDesc(new Type[]{gbfieldtype, Type.INT_TYPE}); // Grouping key + COUNT result
            }
        } else {
            // MIN and MAX produce string results
            if (gbfield == Aggregator.NO_GROUPING) {
                td = new TupleDesc(new Type[]{Type.STRING_TYPE}); // Single string field for MIN/MAX
            } else {
                td = new TupleDesc(new Type[]{gbfieldtype, Type.STRING_TYPE}); // Grouping key + MIN/MAX result
            }
        }

        // Populate results based on the operation
        if (what == Op.COUNT) {
            for (Map.Entry<Field, Integer> entry : groupsInt.entrySet()) {
                Tuple t = new Tuple(td);
                if (gbfield == Aggregator.NO_GROUPING) {
                    t.setField(0, new IntField(entry.getValue())); // COUNT result as IntField
                } else {
                    t.setField(0, entry.getKey()); // Grouping key
                    t.setField(1, new IntField(entry.getValue())); // COUNT result as IntField
                }
                results.add(t);
            }
        } else { // MIN or MAX
            for (Map.Entry<Field, String> entry : groupsString.entrySet()) {
                Tuple t = new Tuple(td);
                if (gbfield == Aggregator.NO_GROUPING) {
                    t.setField(0, new StringField(entry.getValue(), Type.STRING_TYPE.getLen())); // MIN/MAX result as StringField
                } else {
                    t.setField(0, entry.getKey()); // Grouping key
                    t.setField(1, new StringField(entry.getValue(), Type.STRING_TYPE.getLen())); // MIN/MAX result as StringField
                }
                results.add(t);
            }
        }

        return new TupleIterator(td, results);
    }
}

