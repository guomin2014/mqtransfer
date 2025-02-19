package com.gm.mqtransfer.module.support.storage.listener;

public class StorageChangeEvent {

	private final Type type;
    private final String data;
    
	/**
     * Type of change
     */
    public enum Type
    {
		/**
	     * A child was added to the path
	     */
	    CHILD_ADDED,
	
	    /**
	     * A child's data was changed
	     */
	    CHILD_UPDATED,
	
	    /**
	     * A child was removed from the path
	     */
	    CHILD_REMOVED,
	    ;
    }
    
    /**
     * @param type event type
     * @param data event data or null
     */
    public StorageChangeEvent(Type type, String data)
    {
        this.type = type;
        this.data = data;
    }

    /**
     * @return change type
     */
    public Type getType()
    {
        return type;
    }

    /**
     * @return the node's data
     */
    public String getData()
    {
        return data;
    }
    
    @Override
    public String toString()
    {
        return "StorageChangeEvent{" +
            "type=" + type +
            ", data=" + data +
            '}';
    }
}
