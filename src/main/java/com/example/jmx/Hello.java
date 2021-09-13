package com.example.jmx;

public class Hello implements HelloMBean {
    @Override
    public void sayHello() {
        System.out.println("Hello World");
    }
}
