package de.htwg.qwirkle.controller.impl;

import de.htwg.qwirkle.aview.gui.QFrame;
import de.htwg.qwirkle.controller.IQController;
import de.htwg.qwirkle.controller.IQControllerGui;
import de.htwg.qwirkle.model.Grid;
import de.htwg.qwirkle.model.Player;
import de.htwg.qwirkle.model.Supply;
import de.htwg.qwirkle.model.Tile;
import util.Constants;
import util.GridPosition;
import util.observer.QEvent;
import util.observer.Observable;

import java.util.ArrayList;
import java.util.List;

public class QController extends Observable implements IQController, IQControllerGui {

    private State state;
    private List<Player> players;
    private Player currentPlayer;
    private Supply supply;
    private Grid grid;
    private GridPosition targetPositionOnGrid;
    private String statusMessage;
    private QFrame guiMainFrame;

    public QController() {
        this.state = State.UNINIZIALIZED;
        create();
    }

    @Override
    public void init(List<Player> players) {
        assert (!players.isEmpty()) && (players.size() < 5);

        //print welcome
        setStatusMessage(Constants.WELCOME);
        notifyObservers(new QEvent(QEvent.Event.MESSAGE));

        this.players = (ArrayList) players;
        initPlayers();
        state = State.INITIALIZED;
        notifyObservers();
    }

    /**
     * Initializes the players. Adds tile and chooses the first player.
     */
    private void initPlayers() {
        // deal tiles
        for (Player player : this.players) {
            refillPlayer(player);
        }

        // first player starts; could be replaced by more complex algorithm
        this.currentPlayer = this.players.get(0);

        setStatusMessage(this.currentPlayer.getName() + " starts.");

        setState(State.CHOOSE_ACTION);
        notifyObservers();
    }

    @Override
    public boolean gridFieldIsEmpty(int row, int col) {
        return grid.fieldIsEmpty(row, col);
    }

    @Override
    public void addTileToGrid(Tile tile, GridPosition position) {
        int points = 42;

        tile.setPosition(position);
        grid.setTile(tile, position);
        // compute score points...
        getCurrentPlayer().addScore(points);

        setStatusMessage("Still " + this.currentPlayer.getName() +"s turn");
        notifyObservers();
    }

    @Override
    public void addTileToGrid(Tile tile, int row, int col) {
        addTileToGrid(tile, new GridPosition(row, col));
    }

    @Override
    public void addSelectedTileToTargetPosition() {
        addTileToGrid(getSingleSelectedTile(), getTargetPositionOnGrid());
    }

    @Override
    public Tile tradeTile(Tile oldTile) {
        assert !supply.isEmpty();
        Tile newTile = supply.getTile();
        supply.insertTile(oldTile);

        notifyObservers();
        return newTile;
    }

    @Override
    public List<Tile> tradeTiles(List<Tile> oldTiles) {
        List<Tile> newTiles = new ArrayList<>();
        for (Tile oldTile : oldTiles) {
            newTiles.add(tradeTile(oldTile));
        }
        return newTiles;
        // no notifyObservers(); ?
    }

    @Override
    public Player getCurrentPlayer() {
        return this.currentPlayer;
    }

    @Override
    public void refillCurrentAndGoToNextPlayer(){
        refillCurrentPlayer();

        int index = (players.indexOf(currentPlayer) + 1) % players.size();
        currentPlayer = players.get(index);

        setStatusMessage(currentPlayer.getName() + ", it's your turn!");
        setState(State.CHOOSE_ACTION);

        notifyObservers(new QEvent(QEvent.Event.NEXTPLAYER));
    }

    @Override
    public void refillPlayer(Player player) {
        for (int i = player.getHandSize(); i < 6; i++) {
            addTileToPlayer(supply.getTile(), player);
        }
        notifyObservers();
    }

    @Override
    public void refillCurrentPlayer() {
        refillPlayer(getCurrentPlayer());
    }

    @Override
    public Tile getTileFromGrid(GridPosition position) {
        return this.grid.getTile(position);
    }

    @Override
    public Tile getTileFromGrid(int row, int col) {
        return getTileFromGrid(new GridPosition(row, col));
    }

    @Override
    public Tile getTileFromPlayer(int index) {
        if (index > getCurrentPlayer().getHandSize()) {
            return null;
        }
        return getCurrentPlayer().getTile(index);
    }

    @Override
    public void removeTileFromCurrentPlayer(int index) {
        if (index >= getCurrentPlayer().getHandSize()) {
            throw new IllegalArgumentException();
        }
        getCurrentPlayer().removeTile(index);
        notifyObservers();
    }

    @Override
    public void removeTilesFromCurrentPlayer(List<Tile> tiles) {
        getCurrentPlayer().removeTiles(tiles);
        notifyObservers();
    }

    @Override
    public void removeSelectedTilesFromCurrentPlayer() {
        List<Tile> selectedTiles = getSelectedTiles();
        unselectAllTilesAtHand();
        removeTilesFromCurrentPlayer(selectedTiles);
    }

    @Override
    public void addTileToCurrentPlayer(Tile tile) {
        addTileToPlayer(tile, getCurrentPlayer());
    }

    @Override
    public void addTilesToCurrentPlayer(List<Tile> tiles) {
        getCurrentHand().addAll(tiles);
    }

    @Override
    public void addTileToPlayer(Tile tile, Player player) {
        player.addTileToHand(tile);
        notifyObservers();
    }

    @Override
    public String getGridString() {
        return grid.toString();
    }

    @Override
    public String getStatusMessage() {
        return statusMessage;
    }

    @Override
    public void setStatusMessage(String message) {
        statusMessage = message;
        notifyObservers();
    }


    @Override
    public void create() {
        this.grid = new Grid();
        this.supply = new Supply();
        notifyObservers();
    }

    @Override
    public int getNumRows() {
        return grid.getNumRows();
    }

    @Override
    public int getNumCols() {
        return grid.getNumCols();
    }

    @Override
    public void selectTile(Tile tile, boolean isSelected) {
        if (isSelected && getState() == State.ADDTILES) {
            unselectAllTilesAtHand();
        }
        tile.setSelectedAtHand(isSelected);

        notifyObservers();
    }

    @Override
    public void selectTileToggle(Tile tile) {
        selectTile(tile, !tile.isSelectedAtHand());
    }

    @Override
    public void unselectAllTilesAtHand() {
        for (Tile tile : getCurrentHand()) {
            tile.setSelectedAtHand(false);
        }
        notifyObservers();
    }

    @Override
    public Tile getSingleSelectedTile() {
        if (getNumberOfSelectedTiles() != 1) {
            throw new IllegalArgumentException();
        }
        Tile selectedTile = null;
        for (Tile tile : getCurrentHand()) {
            if (tile.isSelectedAtHand()) {
                selectedTile = tile;
                break;
            }
        }
        return selectedTile;
    }

    @Override
    public List<Tile> getSelectedTiles() {
        List<Tile> selectedTiles = new ArrayList<>();
        for (Tile tile : getCurrentHand()) {
            if (tile.isSelectedAtHand()) {
                selectedTiles.add(tile);
            }
        }
        return selectedTiles;
    }

    @Override
    public int getNumberOfSelectedTiles() {
        return getSelectedTiles().size();
    }

    @Override
    public void tradeSelectedTiles() {
        List<Tile> oldTiles = getSelectedTiles();
        removeSelectedTilesFromCurrentPlayer();
        unselectAllTilesAtHand();
        List<Tile> newTiles = tradeTiles(oldTiles);
        addTilesToCurrentPlayer(newTiles);
        notifyObservers();
    }

    @Override
    public GridPosition getTargetPositionOnGrid() {
        return targetPositionOnGrid;
    }

    @Override
    public void setTargetPositionOnGrid(GridPosition position) {
        unselectTargetPositionOnGrid();

        Tile newTarget = getTileFromGrid(position);
        newTarget.setIsTargetedOnGrid(true);

        this.targetPositionOnGrid = position;
        notifyObservers();
    }

    @Override
    public void unselectTargetPositionOnGrid() {
        if (targetPositionOnGridIsSet()) {
            Tile oldTarget = getTileFromGrid(getTargetPositionOnGrid());
            oldTarget.setIsTargetedOnGrid(false);
            targetPositionOnGrid = null;
            notifyObservers();
        }
    }

    @Override
    public boolean targetPositionOnGridIsSet() {
        return targetPositionOnGrid != null;
    }

    @Override
    public List<Tile> getCurrentHand() {
        return getCurrentPlayer().getHand();
    }

    @Override
    public int getCurrentHandSize() {
        return getCurrentHand().size();
    }

    @Override
    public void exit() {
        System.exit(0);
    }

    @Override
    public State getState() {
        return state;
    }

    @Override
    public void setState(State state) {
        this.state = state;
        notifyObservers();
    }

    @Override
    public List<Player> getPlayers() {
        return players;
    }

    @Override
    public QFrame getGuiMainFrame() {
        return guiMainFrame;
    }

    @Override
    public void setGuiMainFrame(QFrame qFrame) {
        this.guiMainFrame = qFrame;
    }
}
