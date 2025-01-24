package simpledb;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * BufferPool manages the reading and writing of pages into memory from
 * disk. Access methods call into it to retrieve pages, and it fetches
 * pages from the appropriate location.
 * <p>
 * The BufferPool is also responsible for locking;  when a transaction fetches
 * a page, BufferPool checks that the transaction has the appropriate
 * locks to read/write the page.
 * 
 * @Threadsafe, all fields are final
 */
public class BufferPool {
    /** Bytes per page, including header. */
    private static final int DEFAULT_PAGE_SIZE = 4096;

    private static int pageSize = DEFAULT_PAGE_SIZE;
    
    /** Default number of pages passed to the constructor. This is used by
    other classes. BufferPool should use the numPages argument to the
    constructor instead. */
    public static final int DEFAULT_PAGES = 50;

    private final int numPages;

    private final ConcurrentHashMap<PageId, Page> bufferpool;

    /**
     * Creates a BufferPool that caches up to numPages pages.
     *
     * @param numPages maximum number of pages in this buffer pool.
     */
    public BufferPool(int numPages) {
        // some code goes here
        // Initialize the buffer pool with the specified capacity.
        this.numPages = numPages;
        this.bufferpool = new ConcurrentHashMap<>(numPages);
    }
    
    public static int getPageSize() {
      return pageSize;
    }
    
    // THIS FUNCTION SHOULD ONLY BE USED FOR TESTING!!
    public static void setPageSize(int pageSize) {
    	BufferPool.pageSize = pageSize;
    }
    
    // THIS FUNCTION SHOULD ONLY BE USED FOR TESTING!!
    public static void resetPageSize() {
    	BufferPool.pageSize = DEFAULT_PAGE_SIZE;
    }

    /**
     * Retrieve the specified page with the associated permissions.
     * Will acquire a lock and may block if that lock is held by another
     * transaction.
     * <p>
     * The retrieved page should be looked up in the buffer pool.  If it
     * is present, it should be returned.  If it is not present, it should
     * be added to the buffer pool and returned.  If there is insufficient
     * space in the buffer pool, a page should be evicted and the new page
     * should be added in its place.
     *
     * @param tid the ID of the transaction requesting the page
     * @param pid the ID of the requested page
     * @param perm the requested permissions on the page
     */
    public  Page getPage(TransactionId tid, PageId pid, Permissions perm)
        throws TransactionAbortedException, DbException {
        // some code goes here

        // Debug: Log the method call
        System.out.println("BufferPool.getPage called with PageId: " + pid + ", Permissions: " + perm);

        // Check if the page is already in the buffer pool.
        if (bufferpool.containsKey(pid)) {
            System.out.println("Page found in BufferPool: " + pid);
            // Debug: Log page state after reading from disk
            Page pageBuffer = bufferpool.get(pid);
            if (pageBuffer instanceof HeapPage) {
                HeapPage heapPage = (HeapPage) pageBuffer;
                System.out.println("Page in Buffer. Empty slots: " + heapPage.getNumEmptySlots());
            }
            return bufferpool.get(pid);
        }

        // Evict a page if the buffer pool is full.
        if (bufferpool.size() >= numPages) {
            System.out.println("BufferPool is full. Size: " + bufferpool.size() + ", Capacity: " + numPages);
            //throw new DbException("Bufferpool is full");
            this.evictPage();
        }
        // Load the page from disk and add it to the buffer pool.
        System.out.println("Page not found in BufferPool. Loading from disk: " + pid);
        Page pageBuffer = Database.getCatalog().getDatabaseFile(pid.getTableId()).readPage(pid);

        // Debug: Log page state after reading from disk
        if (pageBuffer instanceof HeapPage) {
            HeapPage heapPage = (HeapPage) pageBuffer;
            System.out.println("Loaded page from disk. Empty slots: " + heapPage.getNumEmptySlots());
        }

        bufferpool.put(pid, pageBuffer);
        System.out.println("Page added to BufferPool: " + pid);

        return pageBuffer;
    }

    /**
     * Releases the lock on a page.
     * Calling this is very risky, and may result in wrong behavior. Think hard
     * about who needs to call this and why, and why they can run the risk of
     * calling it.
     *
     * @param tid the ID of the transaction requesting the unlock
     * @param pid the ID of the page to unlock
     */
    public  void releasePage(TransactionId tid, PageId pid) {
        // some code goes here
        // not necessary for lab1|lab2
    }

    /**
     * Release all locks associated with a given transaction.
     *
     * @param tid the ID of the transaction requesting the unlock
     */
    public void transactionComplete(TransactionId tid) throws IOException {
        // some code goes here
        // not necessary for lab1|lab2
    }

    /** Return true if the specified transaction has a lock on the specified page */
    public boolean holdsLock(TransactionId tid, PageId p) {
        // some code goes here
        // not necessary for lab1|lab2
        return false;
    }

    /**
     * Commit or abort a given transaction; release all locks associated to
     * the transaction.
     *
     * @param tid the ID of the transaction requesting the unlock
     * @param commit a flag indicating whether we should commit or abort
     */
    public void transactionComplete(TransactionId tid, boolean commit)
        throws IOException {
        // some code goes here
        // not necessary for lab1|lab2
    }

    /**
     * Add a tuple to the specified table on behalf of transaction tid.  Will
     * acquire a write lock on the page the tuple is added to and any other 
     * pages that are updated (Lock acquisition is not needed for lab2). 
     * May block if the lock(s) cannot be acquired.
     * 
     * Marks any pages that were dirtied by the operation as dirty by calling
     * their markDirty bit, and adds versions of any pages that have 
     * been dirtied to the cache (replacing any existing versions of those pages) so 
     * that future requests see up-to-date pages. 
     *
     * @param tid the transaction adding the tuple
     * @param tableId the table to add the tuple to
     * @param t the tuple to add
     */
    public void insertTuple(TransactionId tid, int tableId, Tuple t)
        throws DbException, IOException, TransactionAbortedException {
        // some code goes here
        // not necessary for lab1

        // Retrieve the table's HeapFile from the catalog using its tableId.
        HeapFile table = (HeapFile) Database.getCatalog().getDatabaseFile(tableId);
        // Call the HeapFile's insertTuple method to add the tuple.
        // This method returns a list of pages that were modified by the operation.
        ArrayList<Page> modifiedPages = table.insertTuple(tid, t);

        // DEBUGGING
        System.out.println("Modified pages count: " + modifiedPages.size());

        // Iterate through each modified page.
        for (Page page : modifiedPages) {
            page.markDirty(true, tid);
            bufferpool.put(page.getId(), page); // Replace any existing cached version

            // DEBUGGING
            System.out.println("Marking page dirty: " + page.getId());
        }
    }

    /**
     * Remove the specified tuple from the buffer pool.
     * Will acquire a write lock on the page the tuple is removed from and any
     * other pages that are updated. May block if the lock(s) cannot be acquired.
     *
     * Marks any pages that were dirtied by the operation as dirty by calling
     * their markDirty bit, and adds versions of any pages that have 
     * been dirtied to the cache (replacing any existing versions of those pages) so 
     * that future requests see up-to-date pages. 
     *
     * @param tid the transaction deleting the tuple.
     * @param t the tuple to delete
     */
    public  void deleteTuple(TransactionId tid, Tuple t)
        throws DbException, IOException, TransactionAbortedException {
        // some code goes here
        // not necessary for lab1

        // Retrieve the table's HeapFile from the catalog using the tableId from the tuple's RecordId.
        HeapFile table = (HeapFile) Database.getCatalog().getDatabaseFile(t.getRecordId().getPageId().getTableId());
        // Call the HeapFile's deleteTuple method to remove the tuple.
        // This method returns a list of pages that were modified by the operation.
        ArrayList<Page> modifiedPages = table.deleteTuple(tid, t);
        // Iterate through each modified page.
        for (Page page : modifiedPages) {
            page.markDirty(true, tid);
            bufferpool.put(page.getId(), page); // Replace any existing cached version
        }
    }

    /**
     * Flush all dirty pages to disk.
     * NB: Be careful using this routine -- it writes dirty data to disk so will
     *     break simpledb if running in NO STEAL mode.
     */
    public synchronized void flushAllPages() throws IOException {
        // some code goes here
        // not necessary for lab1
        // Iterate through all pages in the buffer pool
        for (PageId pid : bufferpool.keySet()) {
            // Flush each page
            flushPage(pid);
        }
        // Debugging: Log the flush operation
        System.out.println("Flushed all dirty pages to disk.");

    }

    /** Remove the specific page id from the buffer pool.
        Needed by the recovery manager to ensure that the
        buffer pool doesn't keep a rolled back page in its
        cache.
        
        Also used by B+ tree files to ensure that deleted pages
        are removed from the cache so they can be reused safely
    */
    public synchronized void discardPage(PageId pid) {
        // some code goes here
        // not necessary for lab1
        bufferpool.remove(pid);
    }

    /**
     * Flushes a certain page to disk
     * @param pid an ID indicating the page to flush
     */
    private synchronized  void flushPage(PageId pid) throws IOException {
        // some code goes here
        // not necessary for lab1
        // Check if the page is in the buffer pool
        Page page = bufferpool.get(pid);
        if (page == null) {
            throw new IOException("Page not found in the buffer pool: " + pid);
        }

        // Check if the page is dirty
        TransactionId dirtier = page.isDirty();
        if (dirtier == null) {
            // If the page is not dirty, no need to write it back to disk
            return;
        }

        // Write the page data to disk
        DbFile dbFile = Database.getCatalog().getDatabaseFile(pid.getTableId());
        dbFile.writePage(page);

        // Mark the page as clean
        page.markDirty(false, null);

        // Debugging: Log the flush operation
        System.out.println("Flushed page " + pid + " to disk.");
    }

    /** Write all pages of the specified transaction to disk.
     */
    public synchronized  void flushPages(TransactionId tid) throws IOException {
        // some code goes here
        // not necessary for lab1|lab2
        // Iterate through all pages in the buffer pool
        for (PageId pid : bufferpool.keySet()) {
            Page page = bufferpool.get(pid);

            // Check if the page is dirty and whether the given transaction dirtied it
            if (page != null && tid.equals(page.isDirty())) {
                // Flush the dirty page to disk
                flushPage(pid);
            }
        }

        // Debugging: Log the flush operation
        System.out.println("Flushed all pages for transaction: " + tid);
    }

    /**
     * Discards a page from the buffer pool.
     * Flushes the page to disk to ensure dirty pages are updated on disk.
     */
    private synchronized  void evictPage() throws DbException {
        // some code goes here
        // not necessary for lab1
        if (bufferpool.isEmpty()) {
            throw new DbException("BufferPool is empty; no page to evict.");
        }

        // Get a random page ID from the buffer pool
        List<PageId> pageIds = new ArrayList<>(bufferpool.keySet());
        PageId evictCandidate = pageIds.get(new Random().nextInt(pageIds.size()));

        try {
            // Flush the page if it is dirty
            flushPage(evictCandidate);
        } catch (IOException e) {
            throw new DbException("Error flushing page during eviction: " + e.getMessage());
        }

        // Remove the page from the buffer pool
        bufferpool.remove(evictCandidate);

        // Debugging: Log the eviction
        System.out.println("Evicted page: " + evictCandidate);
    }

}
