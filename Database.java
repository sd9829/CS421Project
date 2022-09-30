import catalog.ACatalog;
import common.Attribute;
import parsers.DDLParser;
import parsers.DMLParser;
import parsers.ResultSet;
import storagemanager.AStorageManager;
import storagemanager.StorageManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Scanner;

/*
    This is the main driver class for the database.

    It is responsible for starting and running the database.

    Other than in the provided testers this will be the only class to contain a main.

    You will add functionality to this class during the different phases.

    More details in the writeup.
 */
public class Database {

    private static StorageManager sm;
    private static ACatalog catalog;

    public static void main(String[] args) {
        catalog = ACatalog.createCatalog(args[0],Integer.parseInt(args[1]),Integer.parseInt(args[2]));
        sm = (StorageManager) AStorageManager.createStorageManager();
        System.out.println("READY. Please enter a statement:");
        Scanner in = new Scanner(System.in);
        StringBuilder input = new StringBuilder(in.next());

        while(!input.toString().equalsIgnoreCase("quit;")){
            if(!input.toString().endsWith(";")) {
                if(input.toString().equalsIgnoreCase("quit;")){
                    break;
                }
                input.append(" ").append(in.next());
                continue;
            }
            String[] inputStrings = input.toString().split(";");
            for(String inputString : inputStrings) {
                inputString += ";";
                // Ugly but will be useful when we implement executeQuery
                if (inputString.toLowerCase().startsWith("create table")
                        || inputString.toLowerCase().startsWith("drop table")
                        || inputString.toLowerCase().startsWith("create index")
                        || inputString.toLowerCase().startsWith("insert")
                        || inputString.toLowerCase().startsWith("update")
                        || inputString.toLowerCase().startsWith("delete")
                        || inputString.toLowerCase().startsWith("alter table")) {
                    if(executeStatement(inputString)){
                        System.out.println("SUCCESS");
                    }else{
                        System.err.println("ERROR: " + inputString);
                    }
                }else if (inputString.toLowerCase().startsWith("select")) {
                   printTable(executeQuery(inputString));
                }
            }
            input = new StringBuilder(in.next().toLowerCase());
        }
        if(terminateDatabase()){
            System.out.println("Saved and closed database successfully");
            in.close();
            System.exit(0);
        }else{
            System.err.println("Could not save and shutdown database");
            in.close();
            System.exit(-1);
        }
    }

    public static boolean executeStatement(String stmt){
        if(stmt.toLowerCase().startsWith("insert")
                || stmt.toLowerCase().startsWith("update")
                || stmt.toLowerCase().startsWith("delete")){
            return DMLParser.parseDMLStatement(stmt);
        }else{
            return DDLParser.parseDDLStatement(stmt);
        }
    }

    public static ResultSet executeQuery(String query){
        return DMLParser.parseDMLQuery(query);
    }

    public static boolean terminateDatabase(){
        sm.purgePageBuffer();
        return catalog.saveToDisk();
    }

    private static String centerString(int width, String s) {
        return String.format("|%-" + width  + "s", String.format("%" + (s.length() + (width - s.length()) / 2) + "s", s));
    }
    public static void printTable(ResultSet tableData){
        if(tableData == null){
            return;
        }
        ArrayList<ArrayList<Object>> rows = tableData.results();
        ArrayList<Object> attrs = new ArrayList<>();
        for(Attribute attr: tableData.attrs()){
            attrs.add(attr.attributeName());
        }
        rows.add(0,attrs);
        int[] maxLengths = new int[rows.get(0).size()];
        for (ArrayList<Object> row : rows)
        {
            for (int i = 0; i < row.size(); i++)
            {
                maxLengths[i] = Math.max(maxLengths[i], row.get(i).toString().length()+2);
            }
        }

        StringBuilder result = new StringBuilder();
        addLines(result, maxLengths);
        int rowIndex = 0;
        for (ArrayList<Object> row : rows)
        {
            for(int i = 0; i < maxLengths.length;i++) {
                result.append(centerString(maxLengths[i], row.get(i).toString()));
            }
            result.append("|\n");
            if(rowIndex == 0){
                addLines(result, maxLengths);
            }
            rowIndex+=1;
        }
        if(rowIndex>1) {
            addLines(result, maxLengths);
        }
        System.out.println(result);
    }

    private static void addLines(StringBuilder result, int[] maxLengths){
        result.append("|-");
        for(int i = 0; i < maxLengths.length;i++){
            int maxLength = maxLengths[i];
            if(i == 0 || i == maxLengths.length-1){
                maxLength -= 1;
            }
            result.append(String.join("", Collections.nCopies(maxLength+1, "-")));
        }
        result.append("|\n");
    }
}
