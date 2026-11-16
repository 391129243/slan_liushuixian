package com.gidi.bio_pcr.WintersUtils;

public class NumUtils {

    public static Byte String16ToByte(String hs)
    {
        if(hs == null|| hs.equals("")){
            return 0x00;
        }
        else
        {
            try
            {
                byte result = (byte) (Integer.parseInt(hs,16) & 0xFF);
                return result;
            }
            catch(Exception e)
            {
                return 0x00;
            }

        }
    }

    public static byte String10ToByte(String hs)
    {
        if (hs == null || hs.equals(""))
        {
            return 0x00;
        }
        else
        {
            try
            {
                byte result = (byte) (Integer.parseInt(hs,10) & 0xFF);
                return result;
            }
            catch (Exception e)
            {
                return 0x00;
            }

        }
    }

    public static double String10ToDouble(String hs)
    {

        if (hs == null || hs.equals(""))
        {
            return Double.NaN;
        }
        else
        {
            try
            {
                double result = Double.parseDouble(hs);
                return result;
            }
            catch (Exception e)
            {
                return Double.NaN;
            }

        }

    }

    public static int String10ToInt(String hs)
    {

        if (hs == null || hs.equals(""))
        {
            return 0;
        }
        else
        {
            try
            {
                int result = Integer.parseInt(hs);
                return result;
            }
            catch (Exception e)
            {
                return 0;
            }

        }

    }

    public static byte[] StringList16To16HexList(String hex)
    {
        String delims = "[ ]+";
        if (hex == null || hex.equals(""))
        {
            return null;
        }
        else
        {
            try
            {
                String[] list = hex.split(delims);

                if (list == null || list.length == 0)
                {
                    return null;
                }
                byte[] hexList = new byte[list.length];

                int i = 0;
                for (String num : list)
                {
                    hexList[i++] = (byte) (Integer.parseInt(num, 16) & 0xFF);
                }
                return hexList;
            }
            catch (Exception e)
            {
                return null;
            }

        }
    }

    public static String HexToString16(byte b1)
    {
        try
        {
            String hexString=Integer.toHexString(0xFF & b1);

            if(hexString.length()==1) {
                hexString = "0" + hexString;
            }

            hexString = hexString.toUpperCase();//将16进制字符串中的字母大写

            return hexString;

        }
        catch (Exception e)
        {
            return "";
        }
    }

    public static String HexToString10(byte b1)
    {
        try
        {
            String hexString=Integer.toString(0xFF & b1);

            return hexString;
        }
        catch (Exception e)
        {
            return "";
        }
    }


    public static String HexListToString16(byte[] list)
    {

        if (list == null || list.length == 0)
        {
            return "";
        }
        else
        {
            try
            {
                String result = "";
                for(byte num : list)
                {
                    String hexstring = HexToString16(num);
                    //string a = Convert.ToString(num, 16).ToUpper();
                    result = result + hexstring + " ";
                }
                return result;
            }
            catch (Exception e)
            {
                return "";
            }

        }

    }
}
