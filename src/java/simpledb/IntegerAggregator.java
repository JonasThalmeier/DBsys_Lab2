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
        this.gbfield = gbfield;
        this.gbfieldtype = gbfieldtype;
        this.afield = afield;
        this.what = what;
        this.groups = new HashMap<>();
        this.counts = new HashMap<>();
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
        Field groupKey = (gbfield == NO_GROUPING) ? null : tup.getField(gbfield);
        int value = ((IntField) tup.getField(afield)).getValue();

        if (!groups.containsKey(groupKey)) {
            groups.put(groupKey, value);
            counts.put(groupKey, 1);
        } else {
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
        TupleDesc td = (gbfield == NO_GROUPING) ?
                new TupleDesc(new Type[]{Type.INT_TYPE}) :
                new TupleDesc(new Type[]{gbfieldtype, Type.INT_TYPE});

        for (Map.Entry<Field, Integer> entry : groups.entrySet()) {
            Tuple t = new Tuple(td);
            if (gbfield == NO_GROUPING) {
                t.setField(0, new IntField(entry.getValue()));
            } else {
                t.setField(0, entry.getKey());
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
