package com.starlingbank;

import com.starlingbank.model.HelloResponse;

public class HelloServiceImpl implements HelloService {
    @Override
    public HelloResponse greet(String name) {
        return new HelloResponse("Hello, " + name + "!", System.currentTimeMillis());
    }
}