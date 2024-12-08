package simpledb;
import java.io.*;

public class test {
	
	public static void main(String[] argv) {
        // Define table schema
        Type[] types = new Type[]{ Type.INT_TYPE, Type.INT_TYPE, Type.INT_TYPE, Type.INT_TYPE };
        String[] names = new String[]{ "f0", "f1", "f2", "f3" };
        TupleDesc descriptor = new TupleDesc(types, names);

        // Create the table and add it to the catalog
        HeapFile table1 = new HeapFile(new File("some_data_file.dat"), descriptor);
        Database.getCatalog().addTable(table1, "test");

        // Construct a sequential scan query
        TransactionId tid = new TransactionId();
        SeqScan f = new SeqScan(tid, table1.getId());

        try {
            // Run the query
            f.open();
            while (f.hasNext()) {
                Tuple tup = f.next();
                System.out.println(tup);
            }
            f.close();
            Database.getBufferPool().transactionComplete(tid);
        } catch (Exception e) {
            System.out.println("Exception: " + e);
        }
    }

}
