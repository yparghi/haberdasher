package com.haberdashervcs.server;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
        System.out.println( "Hello Haberdasher!" );
        spark.Spark.get("/hello", (request, response) -> "Hello World!");
    }
}
