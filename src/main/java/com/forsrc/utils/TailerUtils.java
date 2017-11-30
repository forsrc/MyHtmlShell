package com.forsrc.utils;

import java.io.File;

import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListener;
import org.apache.commons.io.input.TailerListenerAdapter;

public class TailerUtils {

    public static void main(String[] args) {
        TailerListener listener = new TailerListenerAdapter() {
            @Override
            public void handle(String line) {
                super.handle(line);
                System.out.println(line);
            }
        };
        Tailer tailer = new Tailer(new File("/tmp/test.txt"), listener, 500, true);
        tailer.run();
    }
}
