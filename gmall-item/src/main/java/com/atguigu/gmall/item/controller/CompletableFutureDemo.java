package com.atguigu.gmall.item.controller;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

/**
 * 所有以Async结尾的方法都是异步方法，所有的异步方法都有重载的代线程池的方法
 *
 * 	4个构造方法/初始化方法：
 * 		1.runAsync(Runnable)
 * 		2.runAsync(Runnalbe, Executor)
 * 		3.supplyAsync(Supplier)
 * 		4.supplyAsync(Supplier, Executor)
 * 		run开头方法，子任务没有返回结果集
 * 		下面两个方法，子任务有返回结果集
 * 		一般使用带线程池的重载方法
 *
 * 	4个计算完成时方法：
 * 		1.whenComplete()
 * 		2.whenCompleteAsync()
 * 		3.whenCompleteAsync()
 * 		4.exceptionally()
 * 		whenComplete开头的方法可以处理正常或者异常任务
 * 		exceptionally可以处理异常任务
 *
 * 	9个串行化方法：
 * 		1.thenApply/thenApplyAsync：获取上一个任务的返回结果集，并返回自己的结果集
 * 		2.thenAccept/thenAcceptAsync：获取上一个任务返回结果集，没有自己的返回结果集
 * 		3.thenRun/thenRunAsync：上一个任务执行完成，就执行自己的任务，不获取返回结果集，也没有自己的返回结果集
 *
 * 	2个合并方法：
 * 		allOf：所有方法都执行完
 * 		anyOf：任何一个方法执行完成
 */
public class CompletableFutureDemo {

    public static void main(String[] args) throws ExecutionException, InterruptedException {
//        CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> {
//            System.out.println(Thread.currentThread().getName() + "\t completableFuture");
////            int i = 10 / 0;
//            return 1024;
//        }).whenComplete((object, throwable) -> {
//            System.out.println("-------object=" + object.toString()); // 上个方法的返回值
//            System.out.println("-------throwable=" + throwable); // 异常
//        }).exceptionally(throwable -> {
//            System.out.println("throwable=" + throwable);
//            return 6666;
//        });
//
//        System.out.println(future.get());

        List<CompletableFuture> futures = Arrays.asList(
                CompletableFuture.completedFuture("hello"),
                CompletableFuture.completedFuture(" world!"),
                CompletableFuture.completedFuture(" hello"),
                CompletableFuture.completedFuture("java!"));
        final CompletableFuture<Void> allCompleted = CompletableFuture.allOf(futures.toArray(new CompletableFuture[]{}));
        allCompleted.thenRun(() -> {
            futures.stream().forEach(future -> {
                try {
                    System.out.println("get future at:"+System.currentTimeMillis()+", result:"+future.get());
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            });
        });

    }
}
