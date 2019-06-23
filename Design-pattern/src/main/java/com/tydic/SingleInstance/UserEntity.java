package com.tydic.SingleInstance;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class UserEntity {

    private String username;

    public UserEntity() {
        System.out.println("init  successfully ~");
    }

    public UserEntity(String username) {
        this.username = username;
        System.out.println("have argument constructor...");
    }

    public String getUsername() {
        return "name";
    }

    public static void main(String[] args) throws ClassNotFoundException, IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {

//        UserEntity ue = new UserEntity();
//        System.out.println(ue.username);
        Class<?> forName = Class.forName("com.tydic.SingleInstance.UserEntity");
//        UserEntity ue  = (UserEntity) forName.newInstance();
//        System.out.println(ue.username);

//        Field[] fields  = forName.getDeclaredFields();
//        for (Field field:fields) {
//            System.out.println(field.getName());
//        }

        Constructor<?> constructor = forName.getConstructor();
        constructor.setAccessible(true);
        UserEntity asdf = (UserEntity) constructor.newInstance("as");
        System.out.println(asdf.username);
//        Method[] method  = forName.getMethods();
//        for (Method method1: method) {
//            System.out.println(method1.getName());
//        }

    }
}
