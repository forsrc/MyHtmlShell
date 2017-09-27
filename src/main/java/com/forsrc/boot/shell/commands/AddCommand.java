package com.forsrc.boot.shell.commands;

import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

@ShellComponent
public class AddCommand {

	@ShellMethod("Add two integers together.")
    public int add(int a, int b) {
        return a + b;
    }
	
	@ShellMethod("Say hello.")
    public String hi(@ShellOption(defaultValue="World") String who) {
            return "Hello " + who;
    }
}
