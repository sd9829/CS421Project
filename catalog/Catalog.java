package catalog;

import common.Attribute;
import common.ITable;
import common.Table;
import indexing.BPTreeNode;
import indexing.BPlusTree;
import storagemanager.AStorageManager;
import storagemanager.FileManager;
import storagemanager.StorageManager;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

public class Catalog extends ACatalog {

    private String location;
    private int pageSize;
    private int pageBufferSize;
    private File catalogFile;
    HashMap<String,Table> tables = new HashMap<String,Table>();
    HashMap<String, BPlusTree> indexes = new HashMap<>();

    public Catalog(String location, int pageSize, int pageBufferSize) {
        this.location = location;
        File[] listOfFiles = new File(location).listFiles();

        if(listOfFiles != null && listOfFiles.length > 0) {
            for (File file : listOfFiles) {
                if (file.isFile()) {
                    if (file.getName().equals("catalog")) {
                        catalogFile = file;
                        loadCatalogFromDisk();
                    }
                }
            }
        }
        this.pageSize = pageSize;
        this.pageBufferSize = pageBufferSize;
    }

    @Override
    public String getDbLocation() {
        return this.location;
    }

    @Override
    public int getPageSize() {
        return this.pageSize;
    }

    @Override
    public int getPageBufferSize() {
        return this.pageBufferSize;
    }

    @Override
    public boolean containsTable(String tableName) {
        return tables.containsKey(tableName.toLowerCase());
    }

    @Override
    public ITable addTable(String tableName, ArrayList<Attribute> attributes, Attribute primaryKey) {
        tableName = tableName.toLowerCase();
        Table newTable = new Table(tableName,attributes,primaryKey);
        if(!tables.containsKey(tableName)){
            tables.put(tableName,newTable);
            return newTable;
        }
        return null;
    }

    @Override
    public ITable getTable(String tableName) {
        if(containsTable(tableName.toLowerCase())){
            return tables.get(tableName.toLowerCase());
        }
        return null;
    }

    @Override
    public boolean dropTable(String tableName) {
        if(containsTable(tableName)){
            tables.remove(tableName);
              return true;
        }
        return false;
    }

    /**
     * FOR LATER PHASES
     */
    @Override
    public boolean alterTable(String tableName, Attribute attr, boolean drop, Object defaultValue) {
        Table table = (Table) getTable(tableName);
        if(drop) {
            return StorageManager.getStorageManager().dropAttributeValue(table,table.getColumnIndex(attr.getAttributeName()))
                    && table.addAttribute(attr.getAttributeName(),attr.getAttributeType());
        }else {
            return StorageManager.getStorageManager().addAttributeValue(table,defaultValue) && table.addAttribute(attr.getAttributeName(),attr.getAttributeType());
        }
    }

    /**
     * Will clear all data stored in the table with the provided name. This includes clearing all indices related
     * to this table (later phase when index becomes a part). This does not touch any metadata related to the table.
     * @param tableName the name of the table to clear.
     * @return true if successful; false otherwise
     */
    @Override
    public boolean clearTable(String tableName) {
        if(containsTable(tableName)){
            Table table = (Table) getTable(tableName);
            table.getAttributes().clear();
            table.clearPages();
            table.getPageList().clear();
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * FOR LATER PHASES
     */
    @Override
    public boolean addIndex(String tableName, String indexName, String attrName) {
        if(containsTable(tableName)){
            Table table = (Table) getTable(tableName);
            Attribute attr = table.getAttrByName(attrName);
            if(attr == null){
                System.err.println("Error finding attribute to index: " + indexName);
                return false;
            }
            StorageManager sm = (StorageManager) AStorageManager.getStorageManager();
            return table.addIndex(attr.getAttributeName()) && sm.populateIndex(table,attrName);
        }
        else {
            return false;
        }
    }

    /**
     * FOR LATER PHASES
     */
    @Override
    public boolean dropIndex(String tableName, String indexName) {
        return false;
    }

    @Override
    public boolean saveToDisk() {
        try {
            catalogFile = new File(location + "/catalog");
            if(!catalogFile.exists()){
                new File(location).mkdirs();
                try {
                    catalogFile.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            FileOutputStream fout = new FileOutputStream(catalogFile);
            DataOutputStream dos = new DataOutputStream(fout);
            // Write out global vars
            dos.writeInt(pageBufferSize);
            dos.writeInt(pageSize);
            // Write out the number of tables so load loop can correctly read in
            dos.writeInt(tables.size());
            // For all the tables
            for(String t : tables.keySet()){
                Table table = tables.get(t);
                // Write out the length of the table name and the table name
                FileManager.writeChars(table.getTableName().toLowerCase(),dos);
                // Write out the length of the attribute name and the name
                FileManager.writeChars(table.getPrimaryKey().attributeName(),dos);

                // Write out the length of the attribute type and the type
                FileManager.writeChars(table.getPrimaryKey().attributeType(),dos);

                // Write out the number of attributes before the attribute section
                dos.writeInt(table.getAttributes().size());

                // Iterate through attributes and write out the len/value for each
                for(Attribute attribute : table.getAttributes()){
                    // Write the length of the attribute name and value
                    FileManager.writeChars(attribute.getAttributeName(),dos);

                    // Write the length of the attribute type and value
                    FileManager.writeChars(attribute.getAttributeType(),dos);
                }

                // Write out length of page ID list
                dos.writeInt(table.getPageList().size());
                for(Integer pageID : table.getPageList()) {
                    // Write out pageIDs to file
                    dos.writeInt(pageID);
                }
            }
            return true;
        } catch (IOException e) {
            System.err.println("Error writing catalog to disk");
            e.printStackTrace();
        }
        return false;
    }

    private void loadCatalogFromDisk(){
        try {
            FileInputStream fin  = new FileInputStream(catalogFile);

            DataInputStream dis = new DataInputStream(fin);
            // First two ints are pageBuf size and pageSize
            pageBufferSize = dis.readInt();
            pageSize = dis.readInt();

            // Third int is the num of tables
            int numTables = dis.readInt();
            for(int i = 0; i < numTables;i++) {
                // Read in table name based on # of chars to parse
                String tableName = FileManager.readChars(dis).toLowerCase();

                // Read in primary key name based on len
                String primaryKeyName = FileManager.readChars(dis);

                // Read in primary key type based on len
                String primaryKeyType = FileManager.readChars(dis);

                // Parse num of attributes to read in
                int numAttributes = dis.readInt();
                ArrayList<Attribute> tableAttributes = new ArrayList<>();
                for(int attrib = 0; attrib < numAttributes;attrib++){
                    // Read in attrib name based on len
                    String attribName = FileManager.readChars(dis);

                    // Read in attrib type with given len
                    String attribType = FileManager.readChars(dis);

                    // Add new attribute to list for new table
                    tableAttributes.add(new Attribute(attribName,attribType));
                }

                // Parse how many page ID values to read in
                int numPageIDs = dis.readInt();
                // Create the new table to add the pages to
                Table newTable = new Table(tableName,tableAttributes,new Attribute(primaryKeyName,primaryKeyType));
                for(int pageIndex = 0; pageIndex < numPageIDs; pageIndex++){
                    // Read in page ID and add it to the table
                    newTable.addPage(dis.readInt());
                }
                tables.put(tableName,newTable);

            }

        } catch (IOException e) {
            System.err.println("Error reading in catalog file. Delete the catalog from the database directory and try again");
            e.printStackTrace();
        }
    }

}
