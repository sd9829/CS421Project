package conditionals;

import storagemanager.RecordHelper;

import java.util.ArrayList;

public class ValueNode extends Node{

    private final Object value;
    public ValueNode(String value){
        Object isNumeric = RecordHelper.returnNumeric(value);
        if(!(isNumeric instanceof Boolean && !(boolean)isNumeric)){
            this.value = isNumeric;
        }else if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
            this.value = Boolean.parseBoolean(value);
        }else{
            System.out.println(value);
            if(value.contains("\"")) {
                this.value = value.replace("\"","");
            }else{
                this.value =null;
                System.err.println("String not defined with quotes in where clause");
            }
        }
    }

    public Object getValue(){
        return value;
    }

    @Override
    public ArrayList<ArrayList<Object>> evaluate() {
        //not used for value nodes, use getValue()
        return null;
    }

    public String toString(){
        return "Value " + value;
    }
}
