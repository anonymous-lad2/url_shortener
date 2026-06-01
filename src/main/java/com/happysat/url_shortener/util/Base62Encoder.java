package com.happysat.url_shortener.util;

public class Base62Encoder {

    private static final String BASE62 = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    public static String encode(long id) {
        if(id == 0) return "0";
        StringBuilder res = new StringBuilder();

        while(id > 0) {
            res.append(BASE62.charAt((int) (id % 62)));
            id /= 62;
        }

        return res.reverse().toString();
    }

    public static long decode(String code) {
        if(code.isEmpty()) return 0;
        long num = 0;
        for(char ch : code.toCharArray()) {
            int val = BASE62.indexOf(ch);
            num = num * 62 + val;
        }
        return num;
    }
}
