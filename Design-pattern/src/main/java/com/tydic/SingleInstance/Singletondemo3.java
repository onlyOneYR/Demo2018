package com.tydic.SingleInstance;

/*
静态内部类方式
 */
public class Singletondemo3 {

    private Singletondemo3() {
    }

    private static class SingletonInstance {
        private static final Singletondemo3 singletondemo3 = new Singletondemo3();
    }

    public static Singletondemo3 getInstance() {
        System.out.println("SingletonInstance.singletondemo3;  ");
        return SingletonInstance.singletondemo3;
    }

    public static void main(String[] args) {
        Singletondemo3 s1 = Singletondemo3.getInstance();
        Singletondemo3 s2 = Singletondemo3.getInstance();
        System.out.println(s1 == s2);
    }

}
