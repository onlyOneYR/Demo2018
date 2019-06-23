package com.tydic.SingleInstance;

/*
双层检测锁：比懒汉多了一个嵌套同步锁,非线程安全。
 */
public class Singletondemo4 {
    private static Singletondemo4 singletondemo4;

    //单例防止反射漏洞攻击
    private static boolean flag = false;

    private Singletondemo4() {

        if (flag == false) {
            flag = !flag;
        } else {
            throw new RuntimeException("单例模式被侵犯！");
        }
    }

    public Singletondemo4 getInstance() {
        if (singletondemo4 == null) {

            synchronized (this) {

                if (singletondemo4 == null) {

                    return new Singletondemo4();
                }
            }
        }
        return singletondemo4;
    }

    public static void main(String[] args) {
        Singletondemo4 a1 = new Singletondemo4().getInstance();
        Singletondemo4 a2 = new Singletondemo4().getInstance();
        System.out.println(a1 == a2);

    }


}
