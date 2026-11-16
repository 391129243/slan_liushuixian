package com.winters.boweilibrary.BoWei.utils;

import com.gidi.bio_pcr.WintersUtils.NumUtils;

public class BoWeiUtils {

    public static void main(String[] args){
        String text = "5E 86 00 06 00 E5";

        byte[] lust =NumUtils.StringList16To16HexList(text);

        boolean test = checkCRC(lust);

        int total = 0;
    }


    public static Boolean checkCRC(byte[] hexlist)
    {
        if(hexlist == null || hexlist.length <= 4)
        {
            return false;
        }
        else
        {
            int total = 0;

            for(int i = 0; i < hexlist.length - 1; i++)
            {
                int temp = hexlist[i] & 0xFF;

                total = total + temp;
            }

            if((total&0xFF) == (hexlist[hexlist.length - 1]&0xFF))
            {
                return true;
            }
            else
            {
                return false;
            }
        }
    }
}
