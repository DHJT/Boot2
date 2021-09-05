package com.example.demo.nio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Pipe;
import java.nio.channels.Pipe.SinkChannel;
import java.nio.channels.Pipe.SourceChannel;

import org.junit.Test;

public class NioTest {

    @Test
    public void testPipe() {
        try {
            String str = "====================";
            Pipe pipe = Pipe.open();
            SinkChannel sink = pipe.sink();
            ByteBuffer buf = ByteBuffer.allocate(1024);
            SourceChannel source = pipe.source();
            buf.clear();
            buf.put(str.getBytes());
            buf.flip();
//            sink.configureBlocking(false);

            while(buf.hasRemaining()) {
                sink.write(buf);
            }

            ByteBuffer readBuf = ByteBuffer.allocate(1024);
            source.read(readBuf);

//            Files.isDirectory();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Test
    public void testBite() {
        System.out.println(2 >>> 2);
        int hash = "0".hashCode();
        int n = 16;
//        int index = (n - 1) & hash;
        int index = hash & (n - 1);
        System.out.println(index);
        System.out.println(4 | 2);
    }

}
