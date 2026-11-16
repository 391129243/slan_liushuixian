package com.winters.boweilibrary.BoWei.queue;


import com.winters.boweilibrary.BoWei.tempdata.BoWeiPCRStepEntity;

import java.util.LinkedList;

public class BoWeiPCRStepEntityActionListQueue {private LinkedList<BoWeiPCRStepEntity> list = new LinkedList<BoWeiPCRStepEntity>();

    public LinkedList<BoWeiPCRStepEntity> getList() {
        return list;
    }

    public void setList(LinkedList<BoWeiPCRStepEntity> list) {
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
    public void enQueue(BoWeiPCRStepEntity o)//进队
    {
        list.addLast(o);
    }

    public void enQueueHead(BoWeiPCRStepEntity o)//进队
    {
        list.addFirst(o);
    }
    public BoWeiPCRStepEntity deQueue()//出队
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
    public BoWeiPCRStepEntity QueuePeek()//查看队首元素
    {
        return list.getFirst();
    }

}
