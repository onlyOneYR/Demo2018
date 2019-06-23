package com.tydic.SingleInstance;

public class Singledemo2 {

    private static Singledemo2 singledemo2;

    private Singledemo2() {
        System.out.println("init ..");
    }

    public static Singledemo2 getInstance() {
        if (singledemo2 == null) {
            return new Singledemo2();
        }
        return singledemo2;
    }

    public static void main(String[] args) {
        Singledemo2 s = Singledemo2.getInstance();
        Singledemo2 s2 = Singledemo2.getInstance();
        System.out.println(s == s2);//懒汉 线程不安全。false
    }

}
