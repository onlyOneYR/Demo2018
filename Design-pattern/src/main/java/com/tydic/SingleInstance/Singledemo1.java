package com.tydic.SingleInstance;

public class Singledemo1 {

    private static Singledemo1 singledemo1 = new Singledemo1();

    private Singledemo1() {

    }

    public static Singledemo1 getInstance() {
        System.out.println("new instance:");
        return singledemo1;
    }

    public static void main(String[] args) {
        Singledemo1 s = Singledemo1.getInstance();
        Singledemo1 s1 = Singledemo1.getInstance();
        System.out.println(s == s1);//饿汉，天生线程安全。true
    }


}
