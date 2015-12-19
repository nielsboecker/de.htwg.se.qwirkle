/*
Main class to start Quirkle.
Based on blueprint from https://moodle.htwg-konstanz.de/moodle/course/view.php?id=713
 */

package de.htwg.qwirkle;

import de.htwg.qwirkle.aview.tui.TextUI;
import de.htwg.qwirkle.aview.gui.QFrame;
import de.htwg.qwirkle.controller.impl.QController;
import de.htwg.qwirkle.model.Grid;

import java.util.Scanner;

public final class Qwirkle {

    private static Scanner scanner;

    private Qwirkle() {};

    public static void main(String[] args) {

        // Build up the application, start TUI and GUI
        QController controller =  new QController(new Grid(35,35));
        new QFrame(controller);
        TextUI tui = new TextUI(controller);

        // continue to read user input on the tui until the user decides to quit
        tui.printTUI();

        boolean loop = true;
        scanner = new Scanner(System.in);
        while (loop) {
            loop = tui.processInputLine(scanner.next());
        }
    }

}
