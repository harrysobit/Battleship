package battleship;

import java.util.HashMap;
import java.util.List;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Scanner;

class Ship {
    public final String name;
    public final int size;
    private final HashMap<Integer, Integer> coordinates = new HashMap<>();

    public Ship(String name, int size) {
        this.name = name;
        this.size = size;
    }

    public void addCoordinate(int i, int j) {
        if (coordinates.size() < size) {
            coordinates.put(i, j);
        } else throw new RuntimeException("Ship size exceeded.");
    }

    public void hitShip(int i, int j, boolean vertical) {
        if (coordinates.size() != 0) {
            coordinates.remove(vertical ? i : j, vertical ? j : i);
        } else throw new RuntimeException("Ship is already sunk.");
    }

    public boolean isSunk() {
        return coordinates.isEmpty();
    }

    public static Ship findShip(int i, int j, List<Ship> ships, boolean vertical) {
        for (Ship ship : ships) {
            if (ship.coordinates.containsKey(vertical ? i : j)) {
                if (ship.coordinates.get(vertical ? i : j).equals(vertical ? j : i)) {
                    return ship;
                }
            }
        }
        throw new RuntimeException("There was no ship.");
    }
}
class Battlefield {
    private final static Scanner SCAN = new Scanner(System.in);
    private final String playerName;
    private final char[][] ocean = new char[10][10];
    private final LinkedList<Ship> ships = new LinkedList<>(Arrays.asList(
            new Ship("Aircraft Carrier", 5), new Ship("Battleship", 4),
            new Ship("Submarine", 3), new Ship("Cruiser", 3), new Ship("Destroyer", 2)));

    private Battlefield(String playerName) {
        this.playerName = playerName;
        initializeOcean();
    }

    public static void startGame(String player1Name, String player2Name) {
        Battlefield player1 = new Battlefield(player1Name);
        Battlefield player2 = new Battlefield(player2Name);
        boolean player1Turn = true;

        player1.placeShips();
        clearScreen();
        player2.placeShips();
        while (!player1.ships.isEmpty() && !player2.ships.isEmpty()) {
            String message;
            int[] position;
            Battlefield game1 = player1Turn ? player1 : player2;
            Battlefield game2 = player1Turn ? player2 : player1;

            clearScreen();
            game2.printOceanField(true);
            System.out.println("---------------------");
            game1.printOceanField(false);
            position = testPosition(getString(String.format("%n%s, it's your turn:\n\n", game1.playerName)).toUpperCase());
            if (position.length == 0 || notRange(position)) {
                message = "\nError! You entered the wrong coordinates!";
            } else {
                char spot = game2.ocean[position[0]][position[1]];

                if (spot != '~' && !"VO".contains("" + spot)) {
                    message = "\nCoordinate has been entered before!";
                } else {
                    boolean miss = (spot == '~');

                    game2.ocean[position[0]][position[1]] = miss ? 'M' : 'X';
                    message = miss ? "\nYou missed!" : game2.hitShip(position[0], position[1], spot == 'V');
                }
            }
            System.out.print(message);
            player1Turn = !player1Turn;
        }
        System.out.println();
    }

    private void initializeOcean() {
        for (int i = 0; i < 10; i++) {
            ocean[i] = "~".repeat(10).toCharArray();
        }
    }

    private void placeShips() {
        System.out.printf("%s, place your ships on the game field%n%n", playerName);
        printOceanField(false);
        for (Ship ship : ships) {
            setCoordinate(ship);
            System.out.println();
            printOceanField(false);
        }
    }

    private void printOceanField(boolean fog) {
        System.out.println("  1 2 3 4 5 6 7 8 9 10");
        for (int i = 0; i < 10; i++) {
            System.out.print((char) (i + 65) + " ");
            for (int j = 0; j < 10; j++) {
                char spot = (ocean[i][j] == 'V') ? 'O' : ocean[i][j];

                spot = (fog && spot == 'O') ? '~' : spot;
                System.out.print(j == 9 ? spot + "\n" : spot + " ");
            }
        }
    }

    private void setCoordinate(Ship ship) {
        String message = String.format("%nEnter the coordinates of the %s (%d cells):%n%n", ship.name, ship.size);
        boolean shipNear = true;

        while (shipNear) {
            String[] coordinates = getString(message).toUpperCase().split(" ");
            int[] positions;

            if (coordinates.length != 2 || (positions = getPositions(coordinates)).length != 4) {
                message = "\nError! Please enter valid coordinates:\n\n";
            } else if (notRange(positions)) {
                message = "\nError! Please enter coordinates within range. Try again:\n\n";
            } else if (positions[0] != positions[2] && positions[1] != positions[3]) {
                message = "\nError! Wrong ship location! Try again:\n\n";
            } else if (Math.abs(positions[0] - positions[2]) + 1 != ship.size
                    && Math.abs(positions[1] - positions[3]) + 1 != ship.size) {
                message = String.format("\nError! Wrong length of the %s! Try again:\n\n", ship.name);
            } else {
                if (positions[0] == positions[2]) {
                    shipNear = anyShipAround(Math.max(positions[0] - 1, 0),
                            Math.min(positions[0] + 1, 9), positions[1], positions[3])
                            || ocean[positions[0]][Math.max(positions[1] - 1, 0)] != '~'
                            || ocean[positions[0]][Math.min(positions[3] + 1, 9)] != '~';

                    if (!shipNear) placeShip(positions[1], positions[3], positions[0], ship, false);
                } else {
                    shipNear = anyShipAround(positions[0], positions[2], Math.max(positions[1] - 1, 0),
                            Math.min(positions[1] + 1, 9))
                            || ocean[Math.max(positions[0] - 1, 0)][positions[1]] != '~'
                            || ocean[Math.min(positions[2] + 1, 9)][positions[1]] != '~';

                    if (!shipNear) placeShip(positions[0], positions[2], positions[1], ship, true);
                }
                if (shipNear) message = "\nError! You placed it too close to another one. Try again:\n\n";
            }
        }
    }

    private String hitShip(int i, int j, boolean vertical) {
        Ship ship = Ship.findShip(i, j, ships, vertical);

        ship.hitShip(i, j, vertical);
        if (ship.isSunk()) {
            ships.remove(ship);
            return ships.isEmpty() ? "\nYou sank the last ship. You won. Congratulations!"
                    : "\nYou sank a ship!";
        } else {
            return "\nYou hit a ship!";
        }
    }

    private boolean anyShipAround(int iMin, int iMax, int jMin, int jMax) {
        for (int i = iMin; i <= iMax; i++) {
            for (int j = jMin; j <= jMax; j++) {
                if (ocean[i][j] != '~') return true;
            }
        }
        return false;
    }

    private void placeShip(int iMin, int iMax, int j, Ship ship, boolean vertical) {
        for (int i = iMin; i <= iMax; i++) {
            ocean[vertical ? i : j][vertical ? j : i] = vertical ? 'V' : 'O';
            ship.addCoordinate(i, j);
        }
    }

    private static int[] getPositions(String[] coordinates) {
        int[] first = testPosition(coordinates[0]);
        int[] second = (first.length == 2) ? testPosition(coordinates[1]) : new int[]{};

        if (second.length == 2) {
            return new int[]{Math.min(first[0], second[0]), Math.min(first[1], second[1]),
                    Math.max(first[0], second[0]), Math.max(first[1], second[1])};
        } else return new int[]{};
    }

    private static int[] testPosition(String coordinate) {
        try {
            int num0 = coordinate.charAt(0) - 65;
            int num1 = Integer.parseInt(coordinate.substring(1)) - 1;

            return new int[]{num0, num1};
        } catch (Exception e) {
            return new int[]{};
        }
    }

    private static boolean notRange(int[] numbers) {
        for (int number : numbers) {
            if (number < 0 || number > 9) return true;
        }
        return false;
    }

    private static void clearScreen() {
        getString("\nPress Enter and pass the move to another player");
        System.out.println("\n".repeat(99));
    }

    private static String getString(String message) {
        System.out.print(message);
        return SCAN.nextLine();
    }
}


 class Main {
    public static void main(String[] args) {
        Battlefield.startGame("Player 1", "Player 2");
    }
}
