package simpledb;

import java.io.*;
import java.util.*;

/**
 * HeapFile is an implementation of a DbFile that stores a collection of tuples
 * in no particular order. Tuples are stored on pages, each of which is a fixed
 * size, and the file is simply a collection of those pages. HeapFile works
 * closely with HeapPage. The format of HeapPages is described in the HeapPage
 * constructor.
 * 
 * @see simpledb.HeapPage#HeapPage
 * @author Sam Madden
 */
public class HeapFile implements DbFile {

    private File f;
    private TupleDesc td;
    private int numPages;

    // define a page size that is assumed to be constant, will be used in
    // readPageMethod.
    // the page file has been taken according to BufferPool.
    // need to define for avoiding using bufferMethods while reading from disk
    private final int DEFAULT_PAGE_SIZE = 4096;

    /**
     * Constructs a heap file backed by the specified file.
     * 
     * @param f
     *          the file that stores the on-disk backing store for this heap
     *          file.
     */
    public HeapFile(File f, TupleDesc td) {
        // some code goes here
        this.f = f;
        this.td = td;
        this.numPages = 0;
        /* this.numPages = (int) (this.f.length() / DEFAULT_PAGE_SIZE);
        if (this.f.length() % DEFAULT_PAGE_SIZE != 0) {
            this.numPages++;
        }*/

    }

    /**
     * Returns the File backing this HeapFile on disk.
     * 
     * @return the File backing this HeapFile on disk.
     */
    public File getFile() {
        // some code goes here
        return this.f;
    }

    /**
     * Returns an ID uniquely identifying this HeapFile. Implementation note:
     * you will need to generate this tableid somewhere to ensure that each
     * HeapFile has a "unique id," and that you always return the same value for
     * a particular HeapFile. We suggest hashing the absolute file name of the
     * file underlying the heapfile, i.e. f.getAbsoluteFile().hashCode().
     * 
     * @return an ID uniquely identifying this HeapFile.
     */
    public int getId() {
        // some code goes here
        return this.f.getAbsoluteFile().hashCode();

    }

    /**
     * Returns the TupleDesc of the table stored in this DbFile.
     * 
     * @return TupleDesc of this DbFile.
     */
    public TupleDesc getTupleDesc() {
        // some code goes here
        return this.td;
    }

    // see DbFile.java for javadocs
    // read a page from disk and s a HeapPage out of the read data.
    public Page readPage(PageId pid) {
        // some code goes here

        // check if the page is contained in the HeapFIle
        if (pid.getTableId() != this.getId()) {
            throw new IllegalArgumentException("PageId does not match HeapFile ID.");
        }

        int pageNumber = pid.getPageNumber();
        int offset = pageNumber * this.DEFAULT_PAGE_SIZE;

        try (RandomAccessFile raf = new RandomAccessFile(f, "r")) {
            byte[] data = new byte[this.DEFAULT_PAGE_SIZE];

            // find the desired page
            raf.seek(offset);
            // read data of this page
            raf.read(data);

            // create HeapPage according to retrieved data
            return new HeapPage((HeapPageId) pid, data);
        } catch (IOException e) {
            throw new IllegalArgumentException("Error reading page from disk", e);
        }
    }

    // see DbFile.java for javadocs
    public void writePage(Page page) throws IOException {
        // some code goes here
        // not necessary for lab1
    }

    /**
     * Returns the number of pages in this HeapFile.
     */
    public int numPages() {
        // some code goes here
        long fileSize = this.f.length();
        int calculatedPages = (int) (fileSize / DEFAULT_PAGE_SIZE);
        if (fileSize % DEFAULT_PAGE_SIZE != 0) {
            calculatedPages++;
        }
        return Math.max(this.numPages, calculatedPages);
    }

    // see DbFile.java for javadocs
    public ArrayList<Page> insertTuple(TransactionId tid, Tuple t)
            throws DbException, IOException, TransactionAbortedException {
        // some code goes here
        // not necessary for lab1

        ArrayList<Page> modifiedPages = new ArrayList<>();

        // Debug: Log the number of pages
        System.out.println("Number of pages in file: " + this.numPages);

        for (int i = 0; i < this.numPages; i++) {
            HeapPageId pid = new HeapPageId(this.getId(), i);

            // Debug: Log the page being checked
            System.out.println("Checking page: " + pid);

            // access the page from the buffer pool
            HeapPage page = (HeapPage) Database.getBufferPool().getPage(tid, pid, Permissions.READ_WRITE);

            // check if there is space in the page, if so insert the tuple
            if (page.getNumEmptySlots() > 0) {
                page.insertTuple(t);

                // Debug: Log tuple insertion and updated empty slots
                System.out.println("Inserting tuple into page: " + pid);
                System.out.println("Page " + pid + " empty slots after insertion: " + page.getNumEmptySlots());

                modifiedPages.add(page);
                return modifiedPages;
            }
        }

        // if there is no space in the pages, create a new page and insert the tuple
        HeapPageId pid = new HeapPageId(this.getId(), this.numPages);
        // Debug: Log new page creation
        System.out.println("Creating a new page with ID: " + pid);
        HeapPage newPage = new HeapPage(pid, HeapPage.createEmptyPageData());

        // insert the tuple into the new page
        newPage.insertTuple(t);
        // Debug: Log tuple insertion in the new page
        System.out.println("Inserting tuple into new page: " + pid);
        System.out.println("New page empty slots after insertion: " + newPage.getNumEmptySlots());

        modifiedPages.add(newPage);

        // Increment numPages to reflect the new page
        this.numPages++;

        // Mark the new page as dirty and make it available in the buffer pool
        Database.getBufferPool().insertTuple(tid, this.getId(), t);

        // Debug: Log the modified pages before returning
        System.out.println("Modified pages: ");
        for (Page modifiedPage : modifiedPages) {
            System.out.println(modifiedPage.getId());
        }
        return modifiedPages;
    }

    // see DbFile.java for javadocs
    public ArrayList<Page> deleteTuple(TransactionId tid, Tuple t) throws DbException,
            TransactionAbortedException {
        // some code goes here
        // not necessary for lab1
        ArrayList<Page> modifiedPages = new ArrayList<>();

        RecordId rid = t.getRecordId();
        if (rid == null) {
            throw new DbException("The record Id is not valid");
        }

        // retrieve the page id of the tuple
        HeapPageId pid = (HeapPageId) rid.getPageId();

        if (pid != null && pid.getTableId() != this.getId()) {
            throw new DbException("The record Id is not valid");
        }

        // retrieve the page from the buffer pool
        HeapPage page = (HeapPage) Database.getBufferPool().getPage(tid, pid, Permissions.READ_WRITE);

        // remove the tuple from the page and add the modified page to the list
        page.deleteTuple(t);
        modifiedPages.add(page);

        return modifiedPages;

    }

    // see DbFile.java for javadocs
    public DbFileIterator iterator(TransactionId tid) {
        // some code goes here

        // override the class DbFileIterator for the specific HeapFile
        return new DbFileIterator() {

            // initialize variables
            private int currentPageIndex = 0;
            private Iterator<Tuple> currentPageIterator = null;
            private int numPages = numPages();

            // initialize iterator
            @Override
            public void open() throws DbException, TransactionAbortedException {
                currentPageIndex = 0; // Reset to the first page
                currentPageIterator = getPageIterator(currentPageIndex); // Load the first page
            }

            // define a method for iterating over the page
            private Iterator<Tuple> getPageIterator(int pageIndex) throws DbException, TransactionAbortedException {
                // create pageId for the current page
                PageId pid = new HeapPageId(getId(), pageIndex);

                // load the page in the BufferPool
                Page page = Database.getBufferPool().getPage(tid, pid, Permissions.READ_ONLY);

                // return iterator defined in HeapPage.
                return ((HeapPage) page).iterator();

            }

            @Override
            public boolean hasNext() throws DbException, TransactionAbortedException {

                // check if iterator is open
                if (currentPageIterator == null) {
                    return false;
                }

                // check if there are more tuples to iterate over.
                if (currentPageIterator.hasNext()) {
                    return true;
                }

                // check if there is something next in a new page(currentPageIterator has no
                // next)
                // while loop needed if the current page is empty, move to the next one until we
                // find a tuple to read
                while (currentPageIndex < numPages - 1) {
                    currentPageIndex++;
                    currentPageIterator = getPageIterator(currentPageIndex);

                    if (currentPageIterator.hasNext()) {
                        return true; // Found a page with tuples
                    }
                }

                // if we are here no tuples to read have been found.
                return false;
            }

            // get the next tuple
            @Override
            public Tuple next() throws DbException, TransactionAbortedException, NoSuchElementException {
                // check if there are more pages
                if (!this.hasNext()) {
                    throw new NoSuchElementException("No more tuples in the iterator");
                }
                Tuple nextTuple = null;
                // check if we are at the end of the current page
                nextTuple = this.currentPageIterator.next();
                return nextTuple;
            }

            // re-open the iterator to reset it
            @Override
            public void rewind() throws DbException, TransactionAbortedException {
                open();
            }

            // clean up resources
            @Override
            public void close() {
                currentPageIterator = null;
            }
        };

    }

}
