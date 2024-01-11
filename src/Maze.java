package src;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;

import javalib.impworld.World;
import javalib.impworld.WorldScene;
import javalib.worldimages.AlignModeX;
import javalib.worldimages.AlignModeY;
import javalib.worldimages.OutlineMode;
import javalib.worldimages.OverlayImage;
import javalib.worldimages.OverlayOffsetAlign;
import javalib.worldimages.RectangleImage;
import javalib.worldimages.TextImage;
import javalib.worldimages.WorldImage;

// To represent a maze
public class Maze extends World {
    // Facilitates the user experience of playing the maze game
    public static void main(String[] args) {
        Maze maze = new Maze(40, 25);

        // Call the makeGame function on a maze instance to play the game
        maze.makeGame();
    }

    // The width of this maze
    int width;
    // The height of this maze
    int height;

    // The random number generator for this maze
    Random rdm;

    // The graph representing the cells in this maze and the connections between
    // them
    Graph cells;

    // The relative size of this maze image
    final int TILE_SIZE = 20;

    // The current node being considered
    int current;

    // The list of nodes that have already been visited
    ArrayList<Integer> visited;

    // The path to the current node
    LinkedList<Integer> path;

    // The list of nodes to be visited next
    LinkedList<Integer> worklist;

    // The list of paths to each corresponding node in the worklist
    LinkedList<LinkedList<Integer>> paths;

    // The end of the maze has been found
    boolean solved;

    // Show the visited nodes and/or the current path
    boolean showVisited;
    boolean showPath;

    // Whether the game is taking user input, using breadth-first search or
    // depth-first search
    boolean manual;
    boolean algorithmic;
    boolean breadthFirst;

    // The player's/computer's current score
    int score;

    // The dimensions of the elements of this game image
    int mazeWidth;
    int mazeHeight;
    int panelWidth;
    int panelHeight;

    // Each of the different parts of this game image
    WorldImage squares;
    WorldImage walls;
    WorldImage panel;

    // Constructor that creates a new random maze of the given width and height
    Maze(int width, int height) {
        this(width, height, new Random());
    }

    // Constructor that creates a new maze of the given width and height
    // from a given random number generator
    Maze(int width, int height, Random rdm) {
        // Throw an exception if the maze dimensions are too small
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException(
                    "Invalid maze dimensions: " + width + "x" + height);
        }

        this.width = width;
        this.height = height;
        this.rdm = rdm;

        this.mazeWidth = this.width * TILE_SIZE + 6;
        this.mazeHeight = this.height * TILE_SIZE + 6;
        this.panelWidth = this.height * TILE_SIZE * 3 / 2 + 6;
        this.panelHeight = this.height * TILE_SIZE / 4 + 6;

        this.makeMaze();
    }

    // Creates the graph of cells that represents this maze
    void makeMaze() {
        Graph mazeGraph = new Graph(width, height, rdm);

        mazeGraph.kruskal();

        this.cells = mazeGraph;

        this.current = 0;
        this.visited = new ArrayList<Integer>();
        this.path = new LinkedList<Integer>();

        this.worklist = new LinkedList<Integer>();
        this.paths = new LinkedList<LinkedList<Integer>>();

        this.solved = false;

        this.manual = false;
        this.algorithmic = false;

        // Allow the user/computer as many moves as the number of cells on the board
        // before recording a negative score
        this.score = this.width * this.height;

        this.squares = this.drawSquares();
        this.walls = this.drawWalls();
        this.panel = this.drawPanel();
    }

    // Resets the current maze to its unsolved state
    void resetMaze() {
        this.current = 0;
        this.visited = new ArrayList<Integer>();
        this.path = new LinkedList<Integer>();

        this.worklist = new LinkedList<Integer>();
        this.paths = new LinkedList<LinkedList<Integer>>();

        this.solved = false;

        this.manual = false;
        this.algorithmic = false;

        this.score = this.width * this.height;

        this.squares = this.drawSquares();
        this.walls = this.drawWalls();
        this.panel = this.drawPanel();
    }

    // Makes the maze game that can be played by the user
    void makeGame() {
        this.bigBang(
                Math.max(this.mazeWidth, this.panelWidth),
                this.mazeHeight + this.panelHeight - 2,
                0.1);
    }

    // Creates the completed scene for this maze
    public WorldScene makeScene() {
        WorldScene scene = new WorldScene(
                Math.max(this.mazeWidth, this.panelWidth),
                this.mazeHeight + this.panelHeight - 3);
        scene.placeImageXY(this.squares,
                Math.max((this.panelWidth - this.mazeWidth) / 2, 0), 0);
        scene.placeImageXY(this.walls,
                Math.max((this.panelWidth - this.mazeWidth) / 2, 0), 0);
        scene.placeImageXY(this.panel,
                Math.max(this.mazeWidth, this.panelWidth) / 2,
                this.mazeHeight + this.panelHeight - 3);
        return scene;
    }

    // Draws the walls of this maze
    WorldImage drawWalls() {
        WorldImage walls = new RectangleImage(
                this.mazeWidth,
                this.mazeHeight,
                OutlineMode.SOLID,
                new Color(0, 0, 0, 0));

        WorldImage vertWall = new RectangleImage(
                2,
                TILE_SIZE,
                OutlineMode.SOLID,
                Color.DARK_GRAY);

        vertWall = vertWall.movePinhole(0, -TILE_SIZE / 2);

        WorldImage horzWall = new RectangleImage(
                TILE_SIZE,
                2,
                OutlineMode.SOLID,
                Color.DARK_GRAY);

        horzWall = horzWall.movePinhole(-TILE_SIZE / 2, 0);

        walls = walls.movePinhole(
                -this.width * TILE_SIZE / 2,
                -this.height * TILE_SIZE / 2);

        for (int i = 0; i < this.width * this.height; i++) {
            // Drawing a vertical wall
            if (i % this.width != 0 && !this.cells.edgeBetween(i - 1, i)) {
                walls = new OverlayImage(vertWall, walls);
            }

            // Drawing a horizontal wall
            if (i / this.width != 0 && !this.cells.edgeBetween(i - this.width, i)) {
                walls = new OverlayImage(horzWall, walls);
            }

            // Moving the pinhole to the next position
            if ((i + 1) % this.width != 0) {
                walls = walls.movePinhole(TILE_SIZE, 0);
            } else {
                walls = walls.movePinhole(-(this.width - 1) * TILE_SIZE, TILE_SIZE);
            }
        }

        walls = walls.movePinhole(-3, -(this.mazeHeight - 3));

        return walls;
    }

    // Draws the different squares present on the game board
    // on top of the board's background image
    WorldImage drawSquares() {
        // Drawing the background
        WorldImage maze = new OverlayImage(
                new RectangleImage(
                        this.mazeWidth - 6,
                        this.mazeHeight - 6,
                        OutlineMode.SOLID,
                        Color.LIGHT_GRAY),
                new RectangleImage(
                        this.mazeWidth,
                        this.mazeHeight,
                        OutlineMode.SOLID,
                        Color.DARK_GRAY));

        maze = maze.movePinhole(
                -(this.mazeWidth - 6) / 2,
                -(this.mazeHeight - 6) / 2);

        for (int i = 0; i < this.width * this.height; i++) {
            // Drawing the red end square
            if (i == this.width * this.height - 1) {
                WorldImage endSquare = new RectangleImage(
                        TILE_SIZE,
                        TILE_SIZE,
                        OutlineMode.SOLID,
                        new Color(255, 0, 0, 200));

                endSquare = endSquare.movePinhole(-TILE_SIZE / 2, -TILE_SIZE / 2);

                maze = new OverlayImage(endSquare, maze);
            }

            // Drawing the visited squares
            if (this.showVisited && this.visited.contains(i)) {
                WorldImage visitedSquare = new RectangleImage(
                        TILE_SIZE,
                        TILE_SIZE,
                        OutlineMode.SOLID,
                        new Color(100, 100, 255, 150));

                visitedSquare = visitedSquare.movePinhole(-TILE_SIZE / 2, -TILE_SIZE / 2);

                maze = new OverlayImage(visitedSquare, maze);
            }

            // Drawing the path to the current square
            if (this.showPath && this.path.contains(i)) {
                WorldImage pathSquare = new RectangleImage(
                        TILE_SIZE,
                        TILE_SIZE,
                        OutlineMode.SOLID,
                        new Color(50, 50, 200, 200));

                pathSquare = pathSquare.movePinhole(-TILE_SIZE / 2, -TILE_SIZE / 2);

                maze = new OverlayImage(pathSquare, maze);
            }

            // Drawing the green start square
            if (i == 0) {
                WorldImage startSquare = new RectangleImage(
                        TILE_SIZE,
                        TILE_SIZE,
                        OutlineMode.SOLID,
                        new Color(50, 175, 50, 255));

                startSquare = startSquare.movePinhole(-TILE_SIZE / 2, -TILE_SIZE / 2);

                maze = new OverlayImage(startSquare, maze);
            }

            // Drawing the current square
            if (i == current && (this.manual || this.algorithmic)) {
                WorldImage currentSquare = new RectangleImage(
                        TILE_SIZE,
                        TILE_SIZE,
                        OutlineMode.SOLID,
                        new Color(150, 0, 150, 200));

                currentSquare = currentSquare.movePinhole(-TILE_SIZE / 2, -TILE_SIZE / 2);

                maze = new OverlayImage(currentSquare, maze);
            }

            // Moving the pinhole forward
            if ((i + 1) % this.width != 0) {
                maze = maze.movePinhole(TILE_SIZE, 0);
            } else {
                maze = maze.movePinhole(-(this.width - 1) * TILE_SIZE, TILE_SIZE);
            }
        }

        maze = maze.movePinhole(-3, -(this.mazeHeight - 3));

        return maze;
    }

    // Drawing the panel with the relevant information for the user to play the game
    WorldImage drawPanel() {
        int size = this.panelHeight - 6;

        WorldImage panel = new OverlayImage(
                new RectangleImage(
                        size * 6,
                        size,
                        OutlineMode.SOLID,
                        Color.LIGHT_GRAY),
                new RectangleImage(
                        size * 6 + 6,
                        size + 6,
                        OutlineMode.SOLID,
                        Color.DARK_GRAY));

        // The panel before you start solving the maze
        if (!this.manual && !this.algorithmic) {
            panel = new OverlayOffsetAlign(
                    AlignModeX.LEFT,
                    AlignModeY.MIDDLE,
                    new TextImage(
                            "1: Manual Solve",
                            size / 4,
                            Color.BLACK),
                    -size / 10,
                    size / 3,
                    panel);

            panel = new OverlayOffsetAlign(
                    AlignModeX.LEFT,
                    AlignModeY.MIDDLE,
                    new TextImage(
                            "2: Breadth-first Algorithmic Solve",
                            size / 4,
                            Color.BLACK),
                    -size / 10,
                    0,
                    panel);

            panel = new OverlayOffsetAlign(
                    AlignModeX.LEFT,
                    AlignModeY.MIDDLE,
                    new TextImage(
                            "3: Depth-first Algorithmic Solve",
                            size / 4,
                            Color.BLACK),
                    -size / 10,
                    -size / 3,
                    panel);

            panel = new OverlayOffsetAlign(
                    AlignModeX.RIGHT,
                    AlignModeY.MIDDLE,
                    new TextImage(
                            "n: New maze",
                            size / 4,
                            Color.BLACK),
                    size / 6,
                    size / 3,
                    panel);
        }
        // The panel for manual user entry
        else if (this.manual && !this.solved) {
            panel = new OverlayOffsetAlign(
                    AlignModeX.LEFT,
                    AlignModeY.MIDDLE,
                    new TextImage(
                            "Arrow Keys: Move",
                            size / 3,
                            Color.BLACK),
                    -size / 8,
                    size / 4,
                    panel);

            panel = new OverlayOffsetAlign(
                    AlignModeX.LEFT,
                    AlignModeY.MIDDLE,
                    new TextImage(
                            "v: Toggle view",
                            size / 3,
                            Color.BLACK),
                    -size / 8,
                    -size / 5,
                    panel);

            panel = new OverlayOffsetAlign(
                    AlignModeX.LEFT,
                    AlignModeY.MIDDLE,
                    new TextImage(
                            "Score: " + this.score,
                            size / 2,
                            Color.BLACK),
                    -size * 13 / 4,
                    -size / 8,
                    panel);

            panel = new OverlayOffsetAlign(
                    AlignModeX.CENTER,
                    AlignModeY.MIDDLE,
                    new TextImage(
                            "r: Reset",
                            size / 4,
                            Color.BLACK),
                    -size * 3 / 4,
                    size / 3,
                    panel);

            panel = new OverlayOffsetAlign(
                    AlignModeX.RIGHT,
                    AlignModeY.MIDDLE,
                    new TextImage(
                            "n: New maze",
                            size / 4,
                            Color.BLACK),
                    size / 6,
                    size / 3,
                    panel);
        }
        // The panel for the algorithmic solving
        else if (this.algorithmic && !this.solved) {
            panel = new OverlayOffsetAlign(
                    AlignModeX.CENTER,
                    AlignModeY.MIDDLE,
                    new TextImage(
                            "Solving...",
                            size * 2 / 3,
                            Color.BLACK),
                    size * 3 / 2,
                    size / 20,
                    panel);

            panel = new OverlayOffsetAlign(
                    AlignModeX.LEFT,
                    AlignModeY.MIDDLE,
                    new TextImage(
                            "Score: " + this.score,
                            size / 2,
                            Color.BLACK),
                    -size * 13 / 4,
                    -size / 8,
                    panel);

            panel = new OverlayOffsetAlign(
                    AlignModeX.CENTER,
                    AlignModeY.MIDDLE,
                    new TextImage(
                            "r: Reset",
                            size / 4,
                            Color.BLACK),
                    -size * 3 / 4,
                    size / 3,
                    panel);

            panel = new OverlayOffsetAlign(
                    AlignModeX.RIGHT,
                    AlignModeY.MIDDLE,
                    new TextImage(
                            "n: New maze",
                            size / 4,
                            Color.BLACK),
                    size / 6,
                    size / 3,
                    panel);
        }
        // The panel for when the maze is solved
        else {
            panel = new OverlayOffsetAlign(
                    AlignModeX.CENTER,
                    AlignModeY.MIDDLE,
                    new TextImage(
                            "Solved!",
                            size * 2 / 3,
                            Color.BLACK),
                    size * 3 / 2,
                    size / 20,
                    panel);

            panel = new OverlayOffsetAlign(
                    AlignModeX.LEFT,
                    AlignModeY.MIDDLE,
                    new TextImage(
                            "Score: " + this.score,
                            size / 2,
                            Color.BLACK),
                    -size * 13 / 4,
                    -size / 8,
                    panel);

            panel = new OverlayOffsetAlign(
                    AlignModeX.CENTER,
                    AlignModeY.MIDDLE,
                    new TextImage(
                            "r: Reset",
                            size / 4,
                            Color.BLACK),
                    -size * 3 / 4,
                    size / 3,
                    panel);

            panel = new OverlayOffsetAlign(
                    AlignModeX.RIGHT,
                    AlignModeY.MIDDLE,
                    new TextImage(
                            "n: New maze",
                            size / 4,
                            Color.BLACK),
                    size / 6,
                    size / 3,
                    panel);
        }

        panel = panel.movePinhole(0, size / 2 + 3);

        return panel;
    }

    // Handles user input keystrokes
    public void onKeyEvent(String key) {
        // Keystrokes for starting the game
        if (!this.manual && !this.algorithmic) {
            // Press 1 to manually solve the maze
            if (key.equals("1")) {
                this.manual = true;
                this.breadthFirst = false;

                this.showVisited = true;
                this.showPath = false;

                this.squares = this.drawSquares();
                this.panel = this.drawPanel();
            }

            // Press 2 to have the computer solve the maze via breadth-first search
            if (key.equals("2")) {
                this.algorithmic = true;
                this.breadthFirst = true;

                this.showVisited = true;
                this.showPath = true;

                this.worklist.add(this.current);
                this.paths.add(new LinkedList<Integer>());

                this.squares = this.drawSquares();
                this.panel = this.drawPanel();
            }

            // Press 3 to have the computer solve the maze via depth-first search
            if (key.equals("3")) {
                this.algorithmic = true;
                this.breadthFirst = false;

                this.showVisited = true;
                this.showPath = true;

                this.worklist.push(this.current);
                this.paths.push(new LinkedList<Integer>());

                this.squares = this.drawSquares();
                this.panel = this.drawPanel();
            }
        }

        // Keystrokes for manual solving
        if (this.manual && !this.solved) {
            // Use the arrow keys to move
            if (key.equals("up")) {
                this.traverse(this.current - this.width);
            }

            if (key.equals("down")) {
                this.traverse(this.current + this.width);
            }

            if (key.equals("left")) {
                this.traverse(this.current - 1);
            }
            if (key.equals("right")) {
                this.traverse(this.current + 1);
            }

            // Press v to toggle viewing the visited squares and current path
            if (key.equals("v")) {
                if (!this.showVisited) {
                    this.showVisited = true;

                    this.squares = this.drawSquares();
                } else if (!this.showPath) {
                    this.showPath = true;

                    this.squares = this.drawSquares();
                } else {
                    this.showVisited = false;
                    this.showPath = false;

                    this.squares = this.drawSquares();
                }
            }
        }

        // Press n to create a new maze
        if (key.equals("n")) {
            this.makeMaze();
        }

        // Press r to reset the current maze
        if (key.equals("r") && (this.manual || this.algorithmic)) {
            this.resetMaze();
        }
    }

    // Animate the algorithmic solving of the maze
    public void onTick() {
        if (this.algorithmic && !this.solved) {
            if (this.breadthFirst) {
                this.traverse(this.worklist.remove());
            } else {
                this.traverse(this.worklist.pop());
            }
        }
    }

    // Traverse the cell of the maze indexed at the given integer
    void traverse(int next) {
        // If the maze is being solved manually
        if (this.manual) {
            if (this.cells.edgeBetween(this.current, next)) {
                if (this.visited.contains(next)) {
                    this.visited.remove(Integer.valueOf(next));
                }

                this.visited.add(this.current);

                if (!this.path.contains(next)) {
                    this.path.push(this.current);
                } else {
                    this.path.pop();
                }

                this.current = next;
                this.score--;

                this.squares = this.drawSquares();
                this.panel = this.drawPanel();
            }
        }
        // If the maze is being solved by breadth-first search
        else if (this.breadthFirst) {
            LinkedList<Integer> currentPath = this.paths.remove();
            this.path = new LinkedList<Integer>(currentPath);
            currentPath.add(next);

            this.visited.add(this.current);

            if (this.cells.edgeBetween(next, next + 1)
                    && !this.visited.contains(next + 1)) {
                this.worklist.add(next + 1);
                this.paths.add(new LinkedList<Integer>(currentPath));
            }
            if (this.cells.edgeBetween(next, next + this.width)
                    && !this.visited.contains(next + this.width)) {
                this.worklist.add(next + this.width);
                this.paths.add(new LinkedList<Integer>(currentPath));
            }
            if (this.cells.edgeBetween(next, next - 1)
                    && !this.visited.contains(next - 1)) {
                this.worklist.add(next - 1);
                this.paths.add(new LinkedList<Integer>(currentPath));
            }
            if (this.cells.edgeBetween(next, next - this.width)
                    && !this.visited.contains(next - this.width)) {
                this.worklist.add(next - this.width);
                this.paths.add(new LinkedList<Integer>(currentPath));
            }

            this.current = next;
            this.score--;

            this.squares = this.drawSquares();
            this.panel = this.drawPanel();
        }
        // If the maze is being solved by depth-first search
        else {
            LinkedList<Integer> currentPath = this.paths.pop();
            this.path = new LinkedList<Integer>(currentPath);
            currentPath.add(next);

            this.visited.add(this.current);

            if (this.cells.edgeBetween(next, next - this.width)
                    && !this.visited.contains(next - this.width)) {
                this.worklist.push(next - this.width);
                this.paths.push(new LinkedList<Integer>(currentPath));
            }
            if (this.cells.edgeBetween(next, next - 1)
                    && !this.visited.contains(next - 1)) {
                this.worklist.push(next - 1);
                this.paths.push(new LinkedList<Integer>(currentPath));
            }
            if (this.cells.edgeBetween(next, next + this.width)
                    && !this.visited.contains(next + this.width)) {
                this.worklist.push(next + this.width);
                this.paths.push(new LinkedList<Integer>(currentPath));
            }
            if (this.cells.edgeBetween(next, next + 1)
                    && !this.visited.contains(next + 1)) {
                this.worklist.push(next + 1);
                this.paths.push(new LinkedList<Integer>(currentPath));
            }

            this.current = next;
            this.score--;

            this.squares = this.drawSquares();
            this.panel = this.drawPanel();
        }

        // Ends the game if the maze is solved
        if (this.current == this.width * this.height - 1) {
            this.solved = true;
            this.showVisited = true;
            this.showPath = true;

            this.squares = this.drawSquares();
            this.panel = this.drawPanel();
        }
    }
}

// To represent an undirected graph, including both the graph from which the
// minimum spanning tree is created, as well as the minimum spanning tree itself
class Graph {
    // The nodes in this graph
    ArrayList<Node> nodes;
    // The edges in this graph
    ArrayList<Edge> edges;

    // Constructor that makes a new random rectangular graph with the given
    // width and height from the given random number generator
    Graph(int width, int height, Random rdm) {
        // Throw an exception if the graph dimensions are too small
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException(
                    "Invalid graph dimensions: " + width + "x" + height);
        }

        nodes = new ArrayList<Node>();
        edges = new ArrayList<Edge>();

        // Filling the graph with nodes
        for (int i = 0; i < width * height; i++) {
            this.nodes.add(new Node());
        }

        // Connecting each node to its adjacent nodes
        for (int i = 0; i < width * height; i++) {
            if ((i + 1) % width != 0) {
                this.connect(i, i + 1, rdm);
            }
        }

        for (int i = 0; i < width * (height - 1); i++) {
            this.connect(i, i + width, rdm);
        }
    }

    // Returns the node at the given index in this graph's list of nodes
    public Node getNode(int index) {
        return this.nodes.get(index);
    }

    // Connects the two nodes in this graph indexed at the given integers with an
    // edge
    void connect(int index1, int index2, Random rdm) {
        if ((index1 >= 0 && index1 < this.nodes.size())
                && (index2 >= 0 && index2 < this.nodes.size())) {
            Node node1 = this.nodes.get(index1);
            Node node2 = this.nodes.get(index2);

            Edge nextEdge = new Edge(node1, node2, rdm.nextInt(100000));

            this.edges.add(nextEdge);

            node1.addEdge(nextEdge);
            node2.addEdge(nextEdge);
        }
    }

    // Determines whether there is an edge between the nodes
    // indexed at the two given integers
    public boolean edgeBetween(int index1, int index2) {
        if ((index1 >= 0 && index1 < this.nodes.size())
                && (index2 >= 0 && index2 < this.nodes.size())) {
            Node node1 = this.nodes.get(index1);
            Node node2 = this.nodes.get(index2);

            return node1.edgeTo(node2);
        }

        return false;
    }

    // Creates a minimum spanning tree from this graph using Kruskal's algorithm
    void kruskal() {
        // Creating a sorted list of the edges in this graph
        ArrayList<Edge> sortedEdges = new ArrayList<Edge>(this.edges);
        sortedEdges.sort(new EdgeComparator());

        // The final list of edges in the MST
        ArrayList<Edge> treeEdges = new ArrayList<Edge>();

        // Creating a HashMap that maps the nodes in this graph to their representative
        HashMap<Integer, Integer> representatives = new HashMap<Integer, Integer>();

        for (int i = 0; i < this.nodes.size(); i++) {
            representatives.put(i, i);
        }

        // The next edge to be considered for the MST
        Edge nextEdge;

        Integer fromRep;
        Integer toRep;

        while (treeEdges.size() < this.nodes.size() - 1) {
            nextEdge = sortedEdges.remove(0);

            // Getting the representatives of the nodes on each side of the edge
            fromRep = representatives.get(this.nodes.indexOf(nextEdge.from));
            while (fromRep != representatives.get(fromRep)) {
                fromRep = representatives.get(fromRep);
            }

            toRep = representatives.get(this.nodes.indexOf(nextEdge.to));
            while (toRep != representatives.get(toRep)) {
                toRep = representatives.get(toRep);
            }

            // Adding the edge to the MST if it does not create a loop
            if (fromRep != toRep) {
                treeEdges.add(nextEdge);
                representatives.replace(fromRep, toRep);
            } else {
                // Removing edges that will not appear in the MST
                nextEdge.from.removeEdge(nextEdge);
                nextEdge.to.removeEdge(nextEdge);
            }
        }

        // Removing the edges that remain in the list
        for (Edge e : sortedEdges) {
            e.from.removeEdge(e);
            e.to.removeEdge(e);
        }

        this.edges = treeEdges;
    }
}

// To represent a node in a graph
class Node {
    // The edges this coming from this node
    ArrayList<Edge> outEdges;

    // Constructor
    Node() {
        outEdges = new ArrayList<Edge>();
    }

    // Adds the given edge to this node's list of edges
    void addEdge(Edge edge) {
        this.outEdges.add(edge);
    }

    // Removes the given edge from this node's list of edges
    void removeEdge(Edge edge) {
        this.outEdges.remove(edge);
    }

    // Determines whether there is an edge connecting this node
    // and the given node
    public boolean edgeTo(Node that) {
        boolean nodeTo = false;

        for (Edge e : this.outEdges) {
            if ((e.to == that && e.from == this) || (e.to == this && e.from == that)) {
                nodeTo = true;
                break;
            }
        }

        return nodeTo;
    }
}

// To represent an edge in a graph
class Edge {
    // The nodes this edge connects
    Node from;
    Node to;

    // The weight of this edge
    int weight;

    // Constructor
    Edge(Node from, Node to, int weight) {
        this.from = from;
        this.to = to;
        this.weight = weight;
    }
}

// A Comparator to compare two graph edges for the purpose of
// sorting a list of edges for Kruskal's algorithm
class EdgeComparator implements Comparator<Edge> {
    // Compares two edges by subtracting their weights
    public int compare(Edge edge1, Edge edge2) {
        return edge1.weight - edge2.weight;
    }
}
