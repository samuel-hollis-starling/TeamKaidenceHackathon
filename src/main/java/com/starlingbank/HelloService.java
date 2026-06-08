package com.starlingbank;

import com.starlingbank.model.HelloResponse;

public interface HelloService {
    HelloResponse greet(String name);
}