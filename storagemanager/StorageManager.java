package storagemanager;

import common.Attribute;
import common.ITable;
import common.Table;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class StorageManager extends AStorageManager{


    // Page buffer will be an arraylist of hashmaps containing records by table name
    // To get a specific page, we need to iterate through all pages and find if the value is present
    // Or in the case of inserting a record, find a page where it makes sense to store
    BufferManager pageBuffer = new BufferManager();

    @Override
    public boolean clearTableData(ITable table) {
        return pageBuffer.clearTableData(table);

    }

    @Override
    public ArrayList<Object> getRecord(ITable table, Object pkValue) {
        return pageBuffer.getRecord(table,pkValue);
    }

    @Override
    public ArrayList<ArrayList<Object>> getRecords(ITable table) {
        return pageBuffer.getAllRecords(table);
    }

    @Override
    public boolean insertRecord(ITable table, ArrayList<Object> record) {
        return pageBuffer.insertRecord(table,record);
    }

    @Override
    public boolean deleteRecord(ITable table, Object primaryKey) {
        return pageBuffer.deleteRecord(table,primaryKey);
    }

    @Override
    public boolean updateRecord(ITable table, ArrayList<Object> oldRecord, ArrayList<Object> newRecord) {
        return pageBuffer.updateRecord(table,oldRecord,newRecord);
    }

    @Override
    public void purgePageBuffer() {
        // TODO PAGE BUFFER PURGE
        pageBuffer.clearPageBuffer();
    }

    /**
     * Adds the provided default value to the end of all record in the provided table.
     * @param table the table to add the attribute value to
     * @param defaultValue the default value to add
     * @return true if successful; false otherwise
     */
    @Override
    public boolean addAttributeValue(ITable table, Object defaultValue) {
        return pageBuffer.addAttributeValue(table,defaultValue);
    }

    /**
     * drops the attribute value at the provided index in the provided table.
     *
     * All other attribute values will be shifted down
     *
     * @param table the table to drop the attribute value from
     * @param attrIndex the index of the attrbute value to drop.
     * @return true if successful; false otherwise
     */
    @Override
    public boolean dropAttributeValue(ITable table, int attrIndex) {
        ArrayList attributes = table.getAttributes();
        boolean success = true;
        if(attributes.size() != 0) {
            for (ArrayList<Object> record : pageBuffer.getAllRecords(table)) {
                ArrayList<Object> originalRecord = (ArrayList<Object>) record.clone();
                record.remove(attrIndex);
                success = success && updateRecord(table, originalRecord, record);
            }
        }
        return success;
    }


    public boolean populateIndex(ITable itable, String indexName){
        return pageBuffer.populateIndex(itable,indexName);
    }
}
