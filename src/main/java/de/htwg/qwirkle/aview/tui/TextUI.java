package de.htwg.qwirkle.aview.tui;

import de.htwg.qwirkle.controller.IQController.*;
import de.htwg.qwirkle.controller.impl.QController;
import de.htwg.qwirkle.model.Player;
import de.htwg.qwirkle.model.Tile;

import util.Constants;
import util.observer.QEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.PatternSyntaxException;

import util.observer.IObserver;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * Created by niboecke on 30.10.2015.
 */
public class TextUI implements IObserver {

    private Scanner scanner;
    private QController controller;
    private static final Logger LOG = LogManager.getLogger(TextUI.class);

    /**
     * @param controller Qwirkle game controller
     */
    public TextUI(QController controller) {
        this.scanner = new Scanner(System.in);
        this.controller = controller;
        controller.addObserver(this);

        // only read names in TUI if they haven't been read in GUI already
        if (controller.getState() == State.UNINIZIALIZED) {
            this.readPlayerNames();
        } else {
            printTUI();
        }

        // continue to read user input on the tui until the user quits
        boolean loop = true;
        scanner = new Scanner(System.in);
        while (loop) {
            loop = processInputLine(scanner.next());
        }
    }

    /**
     * Prints the minor information of the grid and current player
     */
    public void printTUI() {
        // print name of player, content of player's hand, number of round
        LOG.info(controller.getGridString());
        LOG.info(controller.getStatusMessage());
        LOG.info("Hand: " + controller.getCurrentPlayer().printHand());
        LOG.info("Score: " + controller.getCurrentPlayer().getScore());
        LOG.info("Please enter a command (press h for help)");
    }

    /**
     * Reads number of players from console and starts the initialisation
     */
    public void readPlayerNames() {
        int noP = 1;
        String name, tmp;
        List<Player> players;

        players = new ArrayList<Player>();

        while(noP <= 4) {
            if (noP >= 3) {

                LOG.info("Enter another Player? (y/n)");
                tmp = this.scanner.next();
                if ("n".equals(tmp)) {
                    break;
                }
            }

            LOG.info("Enter name of Player" + noP + ": ");
            name = this.scanner.next();
            players.add(new Player(name));
            noP++;
        }

        this.controller.init(players);
    }

    /**
     * Prints the controllers status message
     */
    public void printMessage() {
        LOG.info(this.controller.getStatusMessage());
    }

    /**
     * @param line the user input to process. Possible inputs are:
     *             a - add tile(s) to grid
     *             t - trade in tile(s)
     *             h - show help
     *             q - quit
     */
    public boolean processInputLine(String line) {

        if ("a".equalsIgnoreCase(line)) {
            controller.setState(State.ADDTILES);
            addTileRoutine();
        }

        if ("t".equalsIgnoreCase(line)) {
            controller.setState(State.TRADETILES);
            tradeTileRoutine();
        }

        if ("h".equalsIgnoreCase(line)) {
            LOG.info(Constants.INSTRUCTIONS);
        }

        if ("q".equalsIgnoreCase(line)) {
            // ToDo: evaluate player's score
            LOG.info(Constants.SEEYOU);
            controller.exit();
        }

        return true; // keep looping
    }

    private void tradeTileRoutine() {
        int size = this.controller.getCurrentPlayer().getHand().size();
        LOG.info("Which tiles do you want to trade? (1-" + size + ", separated by " +
                "space, 0 to quit:");
        String trading = scanner.next();

        if ("0".equals(trading)) {
            // no tiles added, so same player's turn
            printTUI();
            return;
        }

        List<Integer> integerList;
        try {
            String[] stringArray = trading.split("\\s+");
            integerList = new ArrayList<Integer>();
            for(String string : stringArray) {
                int i = Integer.parseInt(string);
                assert i <= this.controller.getCurrentPlayer().getHand().size();
                integerList.add(i);

                Tile t = controller.peekTileFromHand(i);
                controller.select(t, true);
            }
        } catch (PatternSyntaxException ex) {
            LOG.info("Invalid input");
            return;
        }

        List<Tile> oldTiles = new ArrayList<>();
        for(int i : integerList) {
            oldTiles.add(controller.getCurrentPlayer().getTileFromHand(i));
        }
        List<Tile> newTiles = controller.tradeTiles(oldTiles);
        controller.getCurrentPlayer().addTilesToHand(newTiles);

        controller.nextPlayer();
        controller.setState(State.PLAYING);
        printTUI();
    }

    private void addTileRoutine() {
        boolean tileSet = false;

        while(true) {
            int size = this.controller.getCurrentPlayer().getHand().size();
            LOG.info("Select tile to add to grid(1-" + size + ", 0 to quit):");
            int iTile = scanner.nextInt();
            if(iTile == 0){
                break;
            }

            Tile selectedTile = controller.getCurrentPlayer().getTileFromHand(iTile);
            if (selectedTile == null) {
                LOG.info(Constants.INVALID);
                continue;
            }

            LOG.info("Select position on grid(row column):");
            int row = scanner.nextInt();
            int col = scanner.nextInt();

            controller.addTileToGrid(selectedTile, row, col);
            tileSet = true;
        }

        if(tileSet) {
            controller.refillPlayer();
            controller.nextPlayer();
            controller.setState(State.PLAYING);
            printTUI();

        } else {
            // no tiles added, so same player's turn
            printTUI();
        }
    }

    /**
     * Reacts on a given event.
     * @param e Event for update
     */
    @Override
    public void update(QEvent e) {
        switch(e.getEvent()) {
            case GET_PLAYER:
                readPlayerNames();
                break;
            case MESSAGE:
                printMessage();
                break;
            case IGNORE:
                break;
            default:
                printTUI();
                break;
        }
    }
}
