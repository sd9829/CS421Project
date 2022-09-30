package common;

import catalog.ACatalog;
import indexing.BPlusTree;
import storagemanager.AStorageManager;
import storagemanager.StorageManager;

import java.util.ArrayList;
import java.util.HashMap;

public class Table implements ITable {

    private String tableName;
    private int tableId;

    private ArrayList<Attribute> attributes = new ArrayList<Attribute>();
    private final ArrayList<ForeignKey> foreignKeys = new ArrayList<ForeignKey>();
    private ArrayList<Attribute> nonNullAttributes = new ArrayList<>();
    private Attribute primaryKey;
    private ForeignKey foreignKey;
    private String index;

    private HashMap<String,BPlusTree> indexes = new HashMap<>();
    private ArrayList<Integer> pageList = new ArrayList<>();

    public Table(String tableName, ArrayList<Attribute> attributes, Attribute primaryKey) {
        this.tableName = tableName;
        this.attributes = attributes;
        this.primaryKey = primaryKey;
        nonNullAttributes.add(primaryKey);
    }

    public int getPrimaryKeyIndex(){
        return attributes.indexOf(primaryKey);
    }

    @Override
    public String getTableName() {
        return tableName;
    }

    @Override
    public void setTableName(String name) {
        this.tableName = name;
    }

    @Override
    public int getTableId() {
        return tableId;
    }

    @Override
    public ArrayList<Attribute> getAttributes() {
        return attributes;
    }

    @Override
    public Attribute getAttrByName(String name) {
        // Iterate through attribute list to try and find a matching name
        // If not found, return null
        for (Attribute attr : attributes) {
            if (attr.getAttributeName().equals(name)) {
                return attr;
            }
        }
        return null;
    }

    @Override
    public Attribute getPrimaryKey() {
        return primaryKey;
    }

    @Override
    public ArrayList<ForeignKey> getForeignKeys() {
        return foreignKeys;
    }

    public void setNonNullAttribute(ArrayList<Attribute> nonnull){
        this.nonNullAttributes = nonnull;
    }

    @Override
    public boolean addAttribute(String name, String type) {
        // Attributes of the same name are not allowed, check and return false if match is found
        if (getAttrByName(name) != null) {
            return false;
        }
        attributes.add(new Attribute(name, type));
        return true;
    }

    @Override
    public boolean dropAttribute(String name) {
        // Iterate through the list to try and find an attrib with the same name
        // If found, drop it and return true. If no match, fall back on returning false
        Attribute removalAttribute = null;
        for (Attribute attr : attributes) {
            if (attr.getAttributeName().equalsIgnoreCase(name)) {
                removalAttribute = attr;
                attributes.remove(attr);
                nonNullAttributes.remove(attr);
                break;
            }
        }
        if(removalAttribute == null){
            System.err.println("Could not drop column, " + name + " not found in table schema");
        }else{
            return true;
        }
        return false;
    }

    public boolean containsColumn(String columnName){
        return getColumnIndex(columnName) != -1;
    }


    public int getColumnIndex(String columnName){
        for(int i = 0; i <attributes.size();i++){
            if(attributes.get(i).getAttributeName().equalsIgnoreCase(columnName.strip())){
                return i;
            }
        }
        return -1;
    }


    @Override
    public boolean addForeignKey(ForeignKey fk) {
        // TODO VERIFY THAT FOREIGN KEY TABLE EXISTS AND COLUMN EXISTS
        // Check that the foreign key object is not already set, if so return false
        if (foreignKey != null) {
            return false;
        }
        this.foreignKey = fk;
        return true;
    }

    @Override
    public boolean addIndex(String attributeName) {
        if(indexes.containsKey(attributeName)){
            return false;
        }
        if(containsColumn(attributeName)) {
            BPlusTree tree = new BPlusTree(this,attributeName, ACatalog.getCatalog().getPageSize());
            indexes.put(attributeName, tree);
            return true;
        }
        return false;
    }

    public boolean addPage(int pageID) {
        if (pageList.contains(pageID)) {
            return false;
        }
        pageList.add(pageID);
        return true;
    }

    public ArrayList<Integer> getPageList() {
        return pageList;
    }

    public void insertPage(Integer pageId, Integer pageId1, Integer pageId2) {
        int index = pageList.indexOf(pageId);
        pageList.remove(pageId);
        pageList.add(index,pageId1);
        pageList.add(index+1,pageId2);
    }

    public boolean checkNonNullAttributes(ArrayList<Object> record) {
        for(int i = 0; i < attributes.size();i++){
            // If attribute is non null but record object is null, error
            if(!isNullable(i) && record.get(i)==null){
                return false;
            }
        }
        return true;
    }

    public boolean isNullable(int i) {
        return !nonNullAttributes.contains(attributes.get(i));
    }

    public boolean isKey(int columnIndex) {
        return attributes.get(columnIndex).equals(primaryKey) || foreignKeys.contains(attributes.get(columnIndex));
    }

    public void clearPages() {
        StorageManager.getStorageManager().clearTableData(this);
    }

    public void clear() {
        this.primaryKey = null;
        this.pageList.clear();
        this.attributes.clear();
        this.foreignKey = null;
        this.foreignKeys.clear();
        this.nonNullAttributes.clear();
        this.index = null;

    }

    public boolean hasIndex(Attribute primaryKey) {
        return indexes.containsKey(primaryKey.getAttributeName());
    }

    public BPlusTree getIndex(Attribute primaryKey) {
        if(containsColumn(primaryKey.getAttributeName())) {
            return indexes.get(primaryKey.getAttributeName());
        }
        System.err.println("Index name not found");
        return null;
    }

}