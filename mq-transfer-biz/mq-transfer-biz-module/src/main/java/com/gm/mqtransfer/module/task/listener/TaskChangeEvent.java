package com.gm.mqtransfer.module.task.listener;

import com.gm.mqtransfer.module.support.EventType;
import com.gm.mqtransfer.module.task.model.TaskEntity;

public class TaskChangeEvent {

	private final EventType type;
    private final TaskEntity data;
    
    /**
     * @param type event type
     * @param data event data or null
     */
    public TaskChangeEvent(EventType type, TaskEntity data)
    {
        this.type = type;
        this.data = data;
    }

    /**
     * @return change type
     */
    public EventType getType()
    {
        return type;
    }

    /**
     * @return the node's data
     */
    public TaskEntity getData()
    {
        return data;
    }
    
    @Override
    public String toString()
    {
        return "TaskChangeEvent{" +
            "type=" + type +
            ", data=" + data +
            '}';
    }
}
