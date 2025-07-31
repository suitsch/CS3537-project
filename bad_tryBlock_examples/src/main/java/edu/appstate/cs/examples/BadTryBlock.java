package edu.appstate.cs.examples;

public class BadTryBlock {
    public static void main(String[] args) {
        try {
            // Some code that might throw an exception
            throw new ArithmeticException("Division by zero");
        } catch (Throwable e) {
        } finally {
            System.out.println("Finally block executed.");
        }
    }
}