package com.arloor.sogo.common;

public class ByteArrayUtils {

    private ByteArrayUtils(){}

    public static boolean startWith(byte[] array,byte[] target){
        if(array.length<target.length){
            return false;
        }
        for (int i = 0; i < target.length; i++) {
            if(array[i]!=target[i]){
                return false;
            }
        }
        return true;
    }

    public static boolean endWith(byte[] array,int lastIndex,byte[] target){
        if(array.length<target.length||lastIndex<target.length){
            return false;
        }
        for (int i = 0; i < target.length; i++) {
            if(array[lastIndex-i]!=target[target.length-i-1]){
                return false;
            }
        }
        return true;
    }
}
