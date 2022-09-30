package conditionals;

import storagemanager.BufferManager;
import storagemanager.RecordHelper;
import storagemanager.StorageManager;
import common.Attribute;

import java.nio.Buffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class OperatorNode extends Node {

    private final String operator;

    public OperatorNode(Node left, Node right, String operator) {
        this.operator = operator;
        this.left = left;
        this.right = right;
    }

    public ArrayList<ArrayList<Object>> evaluate() {
        ArrayList<ArrayList<Object>> results = new ArrayList<>();
        ArrayList<ArrayList<Object>> leftResults = left.evaluate();
        if (right instanceof ValueNode) {
            for (ArrayList<Object> leftRecord : leftResults) {
                int columnIndex = ((ColumnNode) left).getColumnIndex();

                Object leftValue = leftRecord.get(columnIndex);
                Object rightValue = ((ValueNode) right).getValue();

                switch (operator) {
                    case ">":
                        if (RecordHelper.greaterThan(leftValue, rightValue)) {
                            results.add(leftRecord);
                        }
                        break;
                    case "<":
                        if (RecordHelper.lessThan(leftValue, rightValue)) {
                            results.add(leftRecord);
                        }
                        break;
                    case "=":
                        if (RecordHelper.equals(leftValue, rightValue)) {
                            results.add(leftRecord);
                        }
                        break;
                    case ">=":
                        if (RecordHelper.greaterThanEquals(leftValue, rightValue)) {
                            results.add(leftRecord);
                        }
                        break;
                    case "<=":
                        if (RecordHelper.lessThanEquals(leftValue, rightValue)) {
                            results.add(leftRecord);
                        }
                        break;
                    case "!=":
                        if (RecordHelper.notEquals(leftValue, rightValue)) {
                            results.add(leftRecord);
                        }
                        break;
                    default:
                        System.err.println("Invalid operation in 'where' clause: " + operator);
                        return null;
                }
            }
        } else {
            ArrayList<ArrayList<Object>> rightResults = right.evaluate();
            for (ArrayList<Object> leftRecord : leftResults) {
                for (ArrayList<Object> rightRecord : rightResults) {
                    int leftColumnIndex = ((ColumnNode) left).getColumnIndex();
                    Object leftValue = leftRecord.get(leftColumnIndex);

                    int rightColumnIndex = ((ColumnNode) right).getColumnIndex();
                    Object rightValue = rightRecord.get(rightColumnIndex);

                    switch (operator) {
                        case ">":
                            if (RecordHelper.greaterThan(leftValue, rightValue)) {
                                results.add(leftRecord);
                            }
                            break;
                        case "<":
                            if (RecordHelper.lessThan(leftValue, rightValue)) {
                                results.add(leftRecord);
                            }
                            break;
                        case "=":
                            if (RecordHelper.equals(leftValue, rightValue)) {
                                results.add(leftRecord);
                            }
                            break;
                        case ">=":
                            if (RecordHelper.greaterThanEquals(leftValue, rightValue)) {
                                results.add(leftRecord);
                            }
                            break;
                        case "<=":
                            if (RecordHelper.lessThanEquals(leftValue, rightValue)) {
                                results.add(leftRecord);
                            }
                            break;
                        case "!=":
                            if (RecordHelper.notEquals(leftValue, rightValue)) {
                                results.add(leftRecord);
                            }
                            break;
                        default:
                            System.err.println("Invalid operation in 'where' clause: " + operator);
                            return null;
                    }
                }
            }
        }
        return results;
    }
    public String toString(){
        return "(" +left.toString() + ") " + operator + " (" + right.toString() + ")";
    }
}
