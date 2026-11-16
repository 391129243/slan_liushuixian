package com.gzlab.autoassemblyline_client.WintersUtils.queue;

import com.gzlab.autoassemblyline_client.WintersUtils.entity.procedureEntity.HexDebugEntity;

import java.util.LinkedList;

/**
 * Created by admin on 2018/7/29.
 */

public class HexDebugEntityActionListQueue {

    private LinkedList<HexDebugEntity> list = new LinkedList<HexDebugEntity>();

    public LinkedList<HexDebugEntity> getList() {
        return list;
    }

    public void setList(LinkedList<HexDebugEntity> list) {
        this.list = list;
    }

    public void clear()//销毁队列
    {
        list.clear();
    }
    public boolean QueueEmpty()//判断队列是否为空
    {
        return list.isEmpty();
    }
    public void enQueue(HexDebugEntity o)//进队
    {
        list.addLast(o);
    }

    public void enQueueHead(HexDebugEntity o)//进队
    {
        list.addFirst(o);
    }
    public HexDebugEntity deQueue()//出队
    {
        if(!list.isEmpty())
        {
            return list.removeFirst();
        }
        return null;
    }
    public int QueueLength()//获取队列长度
    {
        return list.size();
    }
    public HexDebugEntity QueuePeek()//查看队首元素
    {
        return list.getFirst();
    }

}
