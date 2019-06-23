package com.tydic.SingleInstance;

public class User {

    public static User getInstance() {
        return SingletonInstance.INSTANCE.getInstance();
    }

    private static enum SingletonInstance {
        INSTANCE;

        private User user;

        SingletonInstance() {
            System.out.println("内部枚举类 的  构造方法");
            user = new User();
        }

        public User getInstance() {
            return user;
        }
    }


    public static void main(String[] args) {
        System.out.println(UserEnum.HTTP_200.getCode());
        User u1 = User.getInstance();
        User u2 = User.getInstance();

        System.out.println(u1 == u2);
    }
}
