package indexing;

import common.RecordPointer;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;

public class BPTreeNode {

    public int numKeys = 1;
    public Object[] keys;
    private BPTreeNode[] children;
    private BPTreeNode parent = null;
    private BPTreeNode next = null;
    private boolean isLeaf = true;
    private int pageIndex;

    public BPTreeNode(int pageIndex, int keySize){
        // TODO need to write out to hardware and update each time anything is changed.
        // Function will need to be outside of constructor and called for every set operation
        this.pageIndex = pageIndex;
        keys = new Object[keySize+1];
        children = new BPTreeNode[keySize+1];
    }

    public boolean isLeaf() {
        return isLeaf;
    }

    public int getPageIndex(){
        return pageIndex;
    }

    public Object[] getKeys() {
        return keys;
    }

    public BPTreeNode[] getChildren() {
        return children;
    }

    public BPTreeNode getParent() {
        return parent;
    }

    public void setParent(BPTreeNode parent) {
        this.parent = parent;
    }

    public BPTreeNode getNext() {
        return next;
    }

    public void setNext(BPTreeNode next){
        this.next = next;
    }

    public void setLeaf(boolean leaf) {
        this.isLeaf = leaf;
    }
}
