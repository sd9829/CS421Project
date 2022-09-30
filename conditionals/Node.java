package conditionals;

import java.util.ArrayList;

public abstract class Node {

    public Node left;
    public Node right;

    public abstract ArrayList<ArrayList<Object>> evaluate();

    public String toString(){
        return left.toString() + " " + right.toString();
    }

}
