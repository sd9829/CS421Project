package storagemanager;

import catalog.ACatalog;
import catalog.Catalog;
import common.Attribute;
import common.ITable;
import common.RecordPointer;
import common.Table;
import indexing.BPTreeNode;
import indexing.BPlusTree;

import java.io.*;
import java.util.*;

public class BufferManager {

    private final ArrayList<Page> buffer = new ArrayList<>();
    private final int pageLimit;
    private final String pageFolder;
    private final File pageDir;
    private List<Integer> pageList = new ArrayList<Integer>();

    public BufferManager() {
        this.pageLimit = ACatalog.getCatalog().getPageBufferSize();
        String location = ACatalog.getCatalog().getDbLocation();
        this.pageFolder = location + "/pages";
        this.pageDir = new File(pageFolder);
        if (!pageDir.exists()) {
            pageDir.mkdir();
        }else{
            String[] pathnames = pageDir.list();

            // For each pathname in the pathnames array
            for (String pathname : pathnames) {
                // Print the names of files and directories
                pageList.add(Integer.parseInt(pathname));
            }
        }
    }

    public ArrayList<Page> loadAllPages(Table table) {
        ArrayList<Page> pages = new ArrayList<>();
        for (Integer fileName : table.getPageList()) {
            Page page = findPageInBuffer(fileName);
            if (page == null) {
                page = new Page(table, pageDir.getPath() + "/" + fileName, fileName);
                addPageToBuffer(page);
                table.addPage(fileName);
            }
            pages.add(page);
        }
        return pages;
    }

    public Page loadPage(Table table, Integer pageID) {
            Page page = findPageInBuffer(pageID);
            if (page == null) {
                page = new Page(table, pageDir.getPath() + "/" + pageID, pageID);
                addPageToBuffer(page);
                table.addPage(pageID);
            }
        return page;
    }

    public Page findPageInBuffer(Integer searchPage) {
        for (Page page : buffer) {
            if (Objects.equals(searchPage, page.getPageId())) {
                return page;
            }
        }
        return null;
    }

    public void clearPageBuffer() {
        boolean success = true;
        for (Page page : buffer) {
            success = success && writeToDisk(page);
        }
        if (!success) {
            System.err.println("Error purging page buffer");
        }
        buffer.clear();
    }

    public void updateBuffer() {
        while (buffer.size() > pageLimit) {
            Page p = buffer.get(0);
            if (!p.hasSpace()) {
                cutRecords(p.getTable(), p, p.getRecords().size() / 2);
                continue;
            }
            buffer.remove(0);
            writeToDisk(p);
        }
    }

    private boolean writeToDisk(Page p) {
        if (!pageDir.exists()) {
            pageDir.mkdirs();
        }
        File finalPageFile = new File(pageFolder + "/" + p.getPageId());
        try {
            if (!finalPageFile.exists()) {
                finalPageFile.createNewFile();
            }
            FileOutputStream fileOutputStream = new FileOutputStream(finalPageFile);
            DataOutputStream outputStream = new DataOutputStream(fileOutputStream);
            outputStream.writeInt(p.getPageId());
            outputStream.writeInt(p.getRecords().size());
            for (int i = 0; i < p.getRecords().size(); i++) {
                ArrayList<Object> records = p.getRecords().get(i);
                RecordHelper.formatRecord(p.getTable(),records);
                for (int j = 0; j < records.size(); j++) {
                    Object record = records.get(j);
                    String type = p.getTable().getAttributes().get(j).getAttributeType();
                    if (type.equalsIgnoreCase("Integer")) {
                        outputStream.writeInt((Integer) record);
                    } else if (type.equalsIgnoreCase("Double")) {
                        outputStream.writeDouble((Double) record);
                    } else if (type.equalsIgnoreCase("Boolean")) {
                        outputStream.writeBoolean((boolean) record);
                    } else if (type.toLowerCase().startsWith("varchar")) {
                        String outputString = (String) record;
                        FileManager.writeChars(outputString, outputStream);
                    } else if (type.toLowerCase().startsWith("char")) {
                        int charLen = Integer.parseInt(type.substring(type.indexOf("(") + 1, type.indexOf(")")));
                        String outputString = (String) record;
                        outputStream.writeInt(charLen);
                        for (int readIndex = 0; readIndex < charLen; readIndex++) {
                            if (readIndex > outputString.length() - 1) {
                                outputStream.writeChar('\t');
                            } else {
                                outputStream.writeChar(outputString.charAt(readIndex));
                            }
                        }
                    }
                }

            }
            outputStream.close();
            fileOutputStream.close();
            return true;
        } catch (IOException e) {
            System.err.println("Error writing page file to disk: " + p);
            e.printStackTrace();
        }
        return false;
    }

    private void addPageToBuffer(Page page) {
        buffer.remove(page);
        buffer.add(page);
        pageList.add(page.getPageId());
    }

    private Page addNewPage(Table table) {
        Page page = new Page(table, getAvailablePageID());
        table.addPage(page.getPageId());
        addPageToBuffer(page);
        updateBuffer();
        return page;
    }

    public ArrayList<Object> getRecord(ITable itable, Object pkValue) {
        Table table = (Table) itable;
        BPlusTree tree = table.getIndex(table.getPrimaryKey());
        if(tree != null) {
            ArrayList<RecordPointer> rpList = tree.search(pkValue);
            if(rpList.size() > 0) {
                RecordPointer rp = rpList.get(0);
                Page p = loadPage(table,rp.page());
                return p.getRecords().get(rp.index());
            }else{
                System.err.println("Error could not find page");
                return null;
            }
        }
        int primaryKeyIndex = table.getAttributes().indexOf(table.getPrimaryKey());
        Page searchPage = searchForPage(itable, pkValue);
        if (searchPage != null) {
            for (int i = 0; i < searchPage.getRecords().size(); i++) {
                ArrayList<Object> record = searchPage.getRecords().get(i);
                Object primaryKeyValue = record.get(primaryKeyIndex);
                if (primaryKeyValue != null && primaryKeyValue.equals(pkValue)) {
                    updateBuffer();
                    return record;
                }
            }
        }
        updateBuffer();
        return null;
    }

    private Page searchForPage(ITable itable, Object pkValue) {
        Table table = (Table) itable;
        BPlusTree tree = table.getIndex(table.getPrimaryKey());
        if(tree != null){
            ArrayList<RecordPointer> rpList = tree.search(pkValue);
            RecordPointer rp = rpList.get(0);
            return loadPage(table,rp.page());
        }
        int primaryKeyIndex = table.getPrimaryKeyIndex();
        for (Page page : loadAllPages(table)) {
            if (page.getRecords().size() > 0) {
                for (int i = 0; i < page.getRecords().size(); i++) {
                    ArrayList<Object> record = page.getRecords().get(i);
                    Object primaryKeyValue = record.get(primaryKeyIndex);
                    if (primaryKeyValue != null && primaryKeyValue.equals(pkValue)) {
                        return page;
                    }
                }
            }
        }
        return null;
    }

    public ArrayList<ArrayList<Object>> getAllRecords(ITable table) {
        Table castedTable = (Table) table;
        ArrayList<Page> tablePages = loadAllPages(castedTable);
        ArrayList<ArrayList<Object>> allRecords = new ArrayList<>();
        for (Page page : tablePages) {
            allRecords.addAll(page.getRecords());
        }
        return allRecords;
    }

    public boolean populateIndex(ITable itable, String indexName){
        Table table = (Table) itable;
        Attribute indexCol = table.getAttrByName(indexName);
        boolean success = true;
        if(indexCol != null && table.hasIndex(indexCol)){
            BPlusTree tree = table.getIndex(table.getPrimaryKey());
            for(Integer page : table.getPageList()){
                Page p = loadPage(table, page);
                ArrayList<ArrayList<Object>> pageRecords = p.getRecords();
                for(int i = 0; i < pageRecords.size(); i++) {
                    ArrayList<Object> row = pageRecords.get(i);
                    Object value = row.get(table.getColumnIndex(indexName));
                    RecordPointer rp = new RecordPointer(page,i);
                    success = success && tree.insertRecordPointer(rp,value);
                }
            }
        }
        return success;
    }

    public boolean insertRecord(ITable itable, ArrayList<Object> record) {
        Table table = (Table) itable;
        BPlusTree tree = table.getIndex(table.getPrimaryKey());
        if (RecordHelper.formatRecord(table, record) == null) {
            System.err.println("Record improperly formatted: " + record + " for attributes: " + table.getAttributes());
            return false;
        }
        if(tree != null){
            if(tree.search(record.get(table.getPrimaryKeyIndex())) != null){
                System.err.println("Error: Primary key already exists in column");
                return false;
            }
        }
        ArrayList<Page> tablePages = loadAllPages(table);
        if (record == null) {
            System.err.println("Record cannot be null.");
            return false;
        }

        if (tablePages.size() == 0) {
            Page p = addNewPage(table);
            return p.addRecord(table, record, 0);
        }

        if (!table.checkNonNullAttributes(record)) {
            System.err.println("Record contains null values in a non-null column.");
            return false;
        }
        int canAdd = -2;
        for (Page page : tablePages) {
            canAdd = canAddRecord(table, page, record);
            if (canAdd > -1) {
                if (!page.hasSpace()) {
                    page = cutRecords(table, page, canAdd);
                }
                return page.addRecord(table, record, canAdd) &&
                        (tree == null || tree.insertRecordPointer(
                                new RecordPointer(page.getPageId(),canAdd),
                                record.get(table.getPrimaryKeyIndex()))
                        );
            }
        }
        if (canAdd == -1) {
            Page page = tablePages.get(tablePages.size() - 1);
            updateBuffer();
            return page.addRecord(table, record, page.getRecords().size()) &&
                    (tree == null || tree.insertRecordPointer(
                            new RecordPointer(page.getRecords().size(), canAdd),
                            record.get(table.getPrimaryKeyIndex())));
        } else {
            System.err.println("error");
            return false;
        }
    }

    private int canAddRecord(Table table, Page page, ArrayList<Object> record) {
        Object recordVal = record.get(table.getPrimaryKeyIndex());
        if(page.getRecords().size() == 0){
            return 0;
        }
        if (page.getRecords().size() == 1) {
            Object compareVal = page.getRecords().get(0).get(table.getPrimaryKeyIndex());
            if (RecordHelper.equals(compareVal, recordVal)) {
                return -2;
            }
            if (RecordHelper.compareObjects(recordVal, compareVal)) {
                return 0;
            } else {
                return 1;
            }
        }
        for (int i = 1; i < page.getRecords().size(); i++) {
            Object previousVal = page.getRecords().get(i - 1).get(table.getPrimaryKeyIndex());
            Object compareVal = page.getRecords().get(i).get(table.getPrimaryKeyIndex());
            if (RecordHelper.equals(compareVal, recordVal)
                    || RecordHelper.equals(previousVal, recordVal)) {
                return -2;
            }
            if (RecordHelper.compareObjects(previousVal, recordVal)
                    && RecordHelper.compareObjects(recordVal, compareVal)) {
                return i;
            }
            if (i == 1 && RecordHelper.compareObjects(recordVal, previousVal)) {
                return 0;
            }
        }
        return -1;
    }


    public boolean updateRecord(ITable table, ArrayList<Object> oldRecord, ArrayList<Object> newRecord) {
        Object primaryKey = oldRecord.get(((Table) table).getPrimaryKeyIndex());
        Page updatePage = searchForPage(table, primaryKey);
        if (updatePage != null) {
            updateBuffer();
            return updatePage.updateRecord(table, primaryKey, newRecord);
        }
        System.err.println("Could not find page for update");
        updateBuffer();
        return false;
    }

    public boolean deleteRecord(ITable itable, Object pkValue) {
        Table table = (Table) itable;
        Page updatePage = searchForPage(table, pkValue);

        if (updatePage != null) {
            updatePage.deleteRecord(table, pkValue);
            updateBuffer();
            return true;
        }
        return false;
    }

    public Page cutRecords(ITable itable, Page page, int cutIndex) {
        Table table = (Table) itable;
        ArrayList<ArrayList<Object>> firstHalfRecords = new ArrayList<>(page.getRecords().subList(0, cutIndex));
        ArrayList<ArrayList<Object>> secondHalfRecords = new ArrayList<>(page.getRecords().subList(cutIndex, page.getRecords().size()));
        removePageFromBuffer(page);
        Page firstPage = new Page(table, getAvailablePageID(), firstHalfRecords);
        addPageToBuffer(firstPage);
        Page secondPage = new Page(table, getAvailablePageID(), secondHalfRecords);
        table.insertPage(page.getPageId(), firstPage.getPageId(), secondPage.getPageId());
        addPageToBuffer(secondPage);
        return firstPage;
    }

    private int getAvailablePageID(){
        File[] files = pageDir.listFiles();
        int fileMax = 0;
        int bufferMax = 0;
        if(files.length > 0) {
            String lastPage = files[files.length - 1].getName();
            Integer pageID = Integer.parseInt(lastPage);
            fileMax = pageID;
        }
        if(pageList.size() > 0){
            bufferMax = pageList.get(pageList.size() - 1);
        }
        int max = 0;
        if(fileMax > max){
            max = fileMax;
        }
        if(bufferMax > max){
            max = bufferMax;
        }
        max = max + 1;
        return max ;
    }

    private void removePageFromBuffer(Page page) {
        buffer.remove(page);
        if(pageList.contains(page.getPageId())){
            pageList.remove(page.getPageId());
        }
    }


    public boolean addAttributeValue(ITable table1, Object defaultValue) {
        Table table = (Table) table1;
        for(ArrayList<Object> record : getAllRecords(table)){
            record.add(defaultValue);
        }
        return true;
    }

    public boolean clearTableData(ITable table) {
        Table t = (Table) table;
        for(Page pageID : loadAllPages(t)){
            File pageFile = new File(pageFolder + "/" +pageID.getPageId());
            if(pageFile.exists()) {
                pageFile.delete();
            }
            removePageFromBuffer(pageID);
        }
        t.clear();
        return true;
    }
}
