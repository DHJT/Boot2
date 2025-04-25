package com.example.demo;

import java.net.URL;
import java.sql.SQLException;

import org.junit.Test;

import com.mysql.cj.jdbc.Driver;

import sun.misc.Launcher;

@SuppressWarnings("restriction")
public class TestJDKClassLoader {

    public static void main(String[] args) {
        System.out.println();
        System.out.println("bootstrap Loader加载以下文件:");
        URL[] urls = Launcher.getBootstrapClassPath().getURLs();
        for (int i = 0; i < urls.length; i++) {
            System.out.println(urls[i]);
        }

        System.out.println();
        System.out.println("extClassLoader加载以下文件");
        System.out.println(System.getProperty("java.ext.dirs"));

        System.out.println();
        System.out.println("appClassLoader加载以下文件");
        System.out.println(System.getProperty("java.class.path"));
    }

    @Test
    public void testClassLoader() throws SQLException {
        Driver driver = new Driver();
        System.out.println(driver.getClass().isLocalClass());
        System.out.println(driver);
        System.out.println(driver.getClass().getClassLoader());
        System.out.println(ClassLoader.getSystemClassLoader());
    }

}
