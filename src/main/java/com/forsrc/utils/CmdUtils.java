package com.forsrc.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Date;

public class CmdUtils {

	public static void main(String[] args) throws IOException, InterruptedException {
		CmdUtils.cmd(new String[] { "java", "-version" });
		CmdUtils.cmd(new String[] { "cmd", "/c java -version" });
	}

	public static int cmd(String[] cmd, ProcessHandler handler) throws IOException, InterruptedException {
		System.out.println(String.format("[CmdUtils] START: %s On %s", Arrays.asList(cmd), new Date()));
		System.out.println("----------");
		Process process = null;
		int rt = 0;
		try {
			process = Runtime.getRuntime().exec(cmd);
			handler.Handle(process);
			rt = process.waitFor();
		} catch (IOException e) {
			throw new IOException(e);
		} finally {
			if (process != null) {
				process.destroy();
			}
		}
		System.out.println("----------");
		System.out.println(String.format("[CmdUtils] END exit: %s", rt));
		return rt;
	}

	public static int cmd(String[] cmd) throws IOException, InterruptedException {

		cmd(cmd, new ProcessHandler() {

			@Override
			public void Handle(Process process) throws IOException {
				print(process.getInputStream(), false);
				print(process.getErrorStream(), true);
			}

		});
		return 0;
	}

	public static void print(InputStream in, boolean isErrorStream) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(in));

		String line = null;
		try {
			while ((line = br.readLine()) != null) {
				if (isErrorStream) {
					System.err.println(line);
				} else {
					System.out.println(line);
				}
			}
		} catch (IOException e) {
			throw new IOException(e);
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					throw new IOException(e);
				}
			}
		}
	}

	public static interface ProcessHandler {
		public void Handle(Process process) throws IOException;
	}
}
