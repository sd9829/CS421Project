package conditionals;

import catalog.ACatalog;
import common.Attribute;
import common.Table;
import storagemanager.RecordHelper;
import storagemanager.StorageManager;

import java.util.ArrayList;
import java.util.List;

public class ColumnNode extends Node{

    private final Table table;
    private int columnIndex = -1;
    private String columnName;

    public ColumnNode(String columnName, Table table){
        this.table = table;
        columnName = RecordHelper.checkTableColumns(table.getAttributes(),columnName);
        columnIndex = table.getColumnIndex(columnName);
    }


    public int getColumnIndex(){
        return columnIndex;
    }

    @Override
    public ArrayList<ArrayList<Object>> evaluate() {
        return StorageManager.getStorageManager().getRecords(table);
    }

    public String toString(){
        return "Column " + columnName;
    }
}
