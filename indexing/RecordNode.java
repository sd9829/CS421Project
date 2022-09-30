package indexing;

import common.RecordPointer;

public class RecordNode {

    private RecordPointer rp;
    private Object searchKey;

    public RecordNode(RecordPointer rp, Object searchKey){
        this.rp = rp;
        this.searchKey = searchKey;
    }

    public RecordPointer getRecordPointer(){
        return rp;
    }

    public Object getSearchKey(){
        return searchKey;
    }
}
