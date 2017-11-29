package com.forsrc.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.StreamGobbler;

public class Ssh2GanymedUtils {

    public static void main(String[] args) throws IOException {
        String hostname = args[0];
        String username = args[1];
        String password = args[2];

        Connection conn = new Connection(hostname);

        /* Now connect */

        conn.connect();

        /* Authenticate.
         * If you get an IOException saying something like
         * "Authentication method password not supported by the server at this stage."
         * then please check the FAQ.
         */
        boolean isAuthenticated = conn.authenticateWithPassword(username, password);

        if (isAuthenticated == false)
            throw new IOException("Authentication failed.");

        /* Create a session */

        Session sess = conn.openSession();

        sess.execCommand("uname -a && date && uptime && who");

        System.out.println("Here is some information about the remote host:");

        /* 
         * This basic example does not handle stderr, which is sometimes dangerous
         * (please read the FAQ).
         */

        InputStream stdout = new StreamGobbler(sess.getStdout());

        BufferedReader br = new BufferedReader(new InputStreamReader(stdout));

        while (true)
        {
            String line = br.readLine();
            if (line == null)
                break;
            System.out.println(line);
        }

        /* Show exit status, if available (otherwise "null") */

        System.out.println("ExitCode: " + sess.getExitStatus());

        /* Close this session */

        sess.close();

        /* Close the connection */

        conn.close();

    }
}
