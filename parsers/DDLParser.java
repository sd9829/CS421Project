package parsers;

import catalog.ACatalog;
import common.Attribute;
import common.ForeignKey;
import common.ITable;
import common.Table;
import storagemanager.AStorageManager;

import java.awt.desktop.SystemEventListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/*
  Class for DDL parser

  This class is responsible for parsing DDL statements

  You will implement the parseDDLStatement function.
  You can add helper functions as needed, but the must be private and static.

  @author Scott C Johnson (sxjcs@rit.edu)

 */
public class DDLParser {

    private static final ACatalog catalog = ACatalog.getCatalog();
    private static final AStorageManager sm = AStorageManager.getStorageManager();

    static List<String> keywords = Arrays.asList("create", "integer", "double", "float","drop","alter","table","drop","boolean","varchar","char");

    public static boolean parseDropClause(String tableName) {
        if (!catalog.containsTable(tableName)) {
            System.err.println("Table " + tableName + " does not exist in catalog");
            return false;
        }
        boolean didDrop = catalog.dropTable(tableName);
        if (didDrop) {
            System.out.println("Table " + tableName + " dropped successfully");
        } else {
            System.err.println("Table " + tableName + " could not be dropped");
        }
        return didDrop;
    }

    /**
     * This function will parse and execute DDL statements (create table, create index, etc)
     *
     * @param stmt the statement to parse
     * @return true if successfully parsed/executed; false otherwise
     */
    public static boolean parseDDLStatement(String stmt) {
        if(stmt.endsWith(";")){
            stmt = stmt.replace(";","");
        }else{
            return false;
        }
        stmt = stmt.toLowerCase().strip();
        if (stmt.toLowerCase().startsWith("create table")) { // Create statement
            return parseCreateClause(stmt);
        } else if (stmt.toLowerCase().startsWith("drop table")) {
            return parseDropClause(stmt.split(" ")[2].replace(";", "").strip());
        }
        //to do alter table <instruction> stuff
        else if (stmt.toLowerCase().startsWith("alter table")){
            return parseAlterClause(stmt);
        }
        //to do create index statements
        else if(stmt.toLowerCase().startsWith("create index")){
            System.out.println("here");
            return parseIndexClause(stmt);
        }
        System.err.println("DDL Statement not properly structured, could not parse");
        return false;
    }


    /**
     * the method which the alter table part calls in the DMLParser method
     * @param stmt
     * @return
     */
    private static boolean parseAlterClause(String stmt){
        String tableName = stmt.toLowerCase().split("table")[1].split(" ")[1];
        if(keywords.contains(tableName.toLowerCase())){
            System.err.println("tablename is a keyword");
            return false;
        }
        Table table = (Table) catalog.getTable(tableName);

        if (!catalog.containsTable(tableName)) {
            System.err.println("Table " + tableName + " does not exist in catalog. " + stmt);
            return false;
        }

        String instruction = stmt.toLowerCase().split(tableName)[1].split(" ")[1];
        String attributeName = stmt.split(tableName)[1].split(" ")[2];

        if(instruction.equals("add")){
            //eg. alter table foo add name varchar(20);
            String attributeType = stmt.split(tableName)[1].split(" ")[3];
            boolean success;
            if(stmt.contains("default")) {
                Object value = stmt.split("default")[1].strip().replace("\"","");
                success = catalog.alterTable(tableName,new Attribute(attributeName,attributeType),false,value);
            }else{
                success = catalog.alterTable(tableName,new Attribute(attributeName,attributeType),false,null);
            }
            return success;
        }
        else if(instruction.equals("drop")){
            int columnIndex = table.getColumnIndex(attributeName);
            if(columnIndex == -1){
                System.err.println("Column '"+attributeName+"'does not exist in table '"+tableName+"'");
                return false;
            }
            if(table.isKey(columnIndex)){
                System.err.println("Cannot drop a key column");
                return false;
            }
            ArrayList<ArrayList<Object>> oldRecords = sm.getRecords(table);
            ArrayList<Attribute> newAttributes = table.getAttributes();
            newAttributes.remove(columnIndex);
            if(!catalog.dropTable(table.getTableName())){
                return false;
            }
            Table newTable = (Table) catalog.addTable(table.getTableName(),newAttributes,table.getPrimaryKey());
            boolean success =  true;
                for (ArrayList<Object> record : oldRecords) {
                    ArrayList<Object> newRecord = (ArrayList<Object>) record.clone();
                    newRecord.remove(columnIndex);
                    success = success && sm.insertRecord(newTable, newRecord);
                }
            return success;

            //eg. alter table foo drop name;
            // this will go through the buffer manager which will reset the record size after
            //deleting a record.
        }
        return false;
    }


    private static boolean parseCreateClause(String stmt) {
        String[] ddlDetails = stmt.split(" ");
        String parendParams = stmt.substring(stmt.strip().indexOf('(') + 1); // Grab string within parenthesis
        if (ddlDetails.length < 3) {
            System.err.println("Not enough values entered for table creation");
            return false;
        }
        String tableName = ddlDetails[2].toLowerCase().split("\\(")[0]; // Grab the table name

        //checking if the table name is null - feel free to remove if this is redundant
        if(tableName==null){
            System.err.println("The table name is null.");
            return false;
        }
        if(keywords.contains(tableName.toLowerCase())){
            System.err.println("tablename is a keyword");
        }

        //checking if table name starts with alpha char
        char c = tableName.charAt(0);
        if (!(c >= 'A' && c <= 'Z') && !(c >= 'a' && c <= 'z')){
            System.err.println("The table name does not start with an alphabetic character.");
        }

        // New table attributes
        Attribute primaryKey = null;
        ForeignKey foreignKey = null;
        ArrayList<Attribute> tableAttributes = new ArrayList<>();
        ArrayList<Attribute> nonNullAttributes = new ArrayList<>();

        // Grab details within parenthesis, ex: (attr1 Integer, attr2 double);
        parendParams = parendParams.substring(0, parendParams.length() - 1);

        // Split param string on comma and iterate through each attribute line
        for (String params : parendParams.split(",")) {
            params = params.toLowerCase().strip(); // Clear whitespace chars;
            if (params.startsWith("primarykey")) { // If line is for primary key
                if (primaryKey != null) { // If a primary key already exists, throw error
                    System.err.println("More than one primary key specified for table");
                    return false;
                }
                // Grab primary key between parends
                String pKey = params.substring(params.indexOf('(') + 1, params.indexOf(')')).strip();
                // Key should already be defined, find it and set primarykey attribute equal to attribute from attr list
                for (Attribute attr : tableAttributes) {
                    if (attr.attributeName().equals(pKey)) {
                        primaryKey = attr;
                        break;
                    }
                }
                // If we got here and no primary key is found, throw error
                if (primaryKey == null) {
                    System.err.println("Primary key not correctly defined in table creation: ");
                    return false;
                }
            } else if (params.startsWith("foreignkey")) {
                // Grab value inside first set of ()
                String fKey = params.substring(params.indexOf('(') + 1, params.indexOf(')')).strip();

                // Split the string on references and grab table name/primary column name
                String refParams = params.toLowerCase().split("references")[1];
                String refKey = refParams.substring(refParams.indexOf('(') + 1, refParams.indexOf(')')).strip();
                String refTable = refParams.substring(0, refParams.indexOf('(')).strip();

                // Create new foreign key object
                foreignKey = new ForeignKey(refTable, refKey, fKey);
                //we have key and table
                //we can add an if statement in the catalog that if the table exists,
                boolean canAddForeignKey = false;
                Table foreignTable = (Table) catalog.getTable(refTable.toLowerCase());
                if (foreignTable != null){
                    //if table exists, the key is in its attributes
                    //if both of those are good, also check if the type of attributes is same
                    for (Attribute attribute: catalog.getTable(refTable.toLowerCase()).getAttributes()){
                            if(attribute.getAttributeType().equalsIgnoreCase(foreignTable.getAttrByName(foreignKey.getRefAttribute()).getAttributeType())){
                                canAddForeignKey = true;
                            }
                        }
                }
                if(!canAddForeignKey){
                    System.err.println("Could not find foreignkey attribute in referenced table, table not created");
                    return false;
                }
            } else {
                String[] columnParams = params.split(" ");
                if (columnParams.length >= 2) {
                    String attributeName = columnParams[0].strip();
                    String attributeType = columnParams[1].strip();
                    if(keywords.contains(attributeName.toLowerCase())){
                        System.err.println("Attribute name "+attributeName+ " contains a keyword");
                        return false;
                    }
                    Attribute newAttr = new Attribute(attributeName,attributeType);
                    tableAttributes.add(newAttr);
                    if (columnParams.length > 2) {
                        if (columnParams[2].strip().equalsIgnoreCase("primarykey")) {
                            primaryKey = newAttr;
                        }
                        if (columnParams[2].strip().equalsIgnoreCase("notnull")) {
                            nonNullAttributes.add(newAttr);
                        }
                    }
                } else {
                    System.err.println("Not all parameters specified for " + params);
                    return false;
                }
            }
        }
        boolean success = true;
        if (primaryKey == null) {
            System.err.println("Primary key attribute not specified in table declaration");
            return false;
        }
        Table newTable = (Table) catalog.addTable(tableName, tableAttributes, primaryKey);
        if (newTable == null) {
            System.err.println("Table already exists");
            return false;
        } else {
            System.out.println("Added table " + tableName + " successfully");
        }
        if (foreignKey != null) {
            success = catalog.getTable(tableName).addForeignKey(foreignKey);
        }
        if(nonNullAttributes.size() > 0){
            newTable.setNonNullAttribute(nonNullAttributes);
        }
        System.out.println("Table creation: " + success);
        return success;
    }

    private static boolean parseIndexClause(String stmt){
        //"create index myFooIndex on foo( bar );";
        String index = stmt.split(" ")[2];
        String indexString = stmt.split("on")[1].strip();
        String tableName = indexString.split("\\(")[0];
        String attrName = indexString.split("\\( ")[1].split("\\)")[0];
        return catalog.addIndex(tableName, index, attrName);
    }

}
