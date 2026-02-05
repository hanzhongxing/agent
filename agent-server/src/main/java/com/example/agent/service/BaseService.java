package com.example.agent.service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BaseService {

    protected final static ExecutorService executor= Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()*2);
}
