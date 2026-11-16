package com.gzlab.autoassemblyline_client.WintersUtils;

public class HandleMessageThread extends Thread {

    private CMDListQueue mRecvPool = new CMDListQueue();

    @Override
    public void run() {
        super.run();
        while(!isInterrupted()) {
            try {
                if(!mRecvPool.QueueEmpty()) {

                    String cmd = (String) mRecvPool.deQueue();

                    HandleData(cmd);

                }

            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }
    }

    private void HandleData(String cmd) {

        byte[] byteList = NumUtils.StringList16To16HexList(cmd);

        if(byteList == null ||  byteList.length < 10 ){


        }

        int length = byteList[2]*256 + byteList[3];

        if(byteList[0] == 0x5A && byteList[1] == 0xA5
                && byteList[byteList.length -1] == 0xFF
                && byteList[byteList.length -2] == 0xFC
                && byteList[byteList.length -3] == 0xFF){

               if(byteList.length == length + 10){

                   ///CRC
                   int CRC = byteList[byteList.length -5]*256 + byteList[byteList.length -4];

                   int sum = 0;

                   for(int i = 0;i < length;i++){

                       sum += byteList[4 + i ];

                   }

                   if((CRC & 0xFFFF) == (sum & 0xFFFF)){

                       byte[] cmdBytes = new byte[length];

                       System.arraycopy(byteList, 5, cmdBytes, 0, length);

                   }

               }

        }else{
            return;
        }

    }
}

