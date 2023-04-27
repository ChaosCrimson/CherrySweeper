import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.*;

public class Minesweeper {
  // The value assigned to cells marked as mines. 10 works
  // because no cell will have more than 8 neighbouring mines.
  Dimension screenSize = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds().getSize();
  private final JFrame frame;
  JPanel contentPanel = new JPanel();
  Container grid = new Container();
  private static final int MINE = 10, SIZE = 500;   //Size is frame size in pixels
  int screenHeight = (int) screenSize.getHeight();
  // The number of mines at generated is the grid size * this constant
  private static final double POPULATION_CONSTANT = 1.7;

  // This fixed amount of memory is to avoid repeatedly declaring
  // new arrays every time a cell's neighbours are to be retrieved.
  private static Cell[] reusableStorage = new Cell[8];
  private int insetTop = 30, insetLeft = 20, insetRight = 20, insetBottom = 10, gridSize, buttonPanelHeight;
  Border padding = BorderFactory.createEmptyBorder(insetTop, insetLeft, insetBottom, insetRight);
  private Cell[][] cells;
  private boolean fullscreen = false, firstTurn = true;

  private class Cell extends JButton {
    Image cellTile = ImageIO.read(Objects.requireNonNull(getClass().getResource("tile.png")));
    Image cellTileFlag = ImageIO.read(Objects.requireNonNull(getClass().getResource("TileFlag.png")));
    Image tileFlagScaled;
    private final int row, col;
    private boolean isFlagged = false;
    private int value, buttonWidth, buttonHeight;
    Image tileScaled = cellTile.getScaledInstance(buttonWidth +1, buttonHeight + 1, java.awt.Image.SCALE_SMOOTH);
    private void setUnflagged() {
      isFlagged = false;
    }

    Cell(final int row, final int col) throws IOException {
      JLabel label = new JLabel("");
      label.setFont(new Font("Tahoma",Font.BOLD, 23));
      this.row = row;
      this.col = col;
      addActionListener(e -> {
        Object source = e.getSource();
        handleCell((Cell) source);
        ((Cell) source).setFont(new Font("Tahoma", Font.PLAIN, 1));
      });
      addMouseListener(new MouseAdapter() {   //Handles flagging and right clicks
        @Override
        public void mouseClicked(MouseEvent e) {
          if (e.getButton() == MouseEvent.BUTTON3 && !firstTurn) {
            if(isEnabled() && !isFlagged) {
              isFlagged = true;
              //System.out.println("Flagged at row: " + (row + 1) + "      col: " + (col + 1));
              setIcon(new ImageIcon(tileFlagScaled));
              e.getComponent().repaint();
            } else if(isEnabled() && isFlagged) {
              isFlagged = false;
              //System.out.println("Unflagged at row: " + (row + 1) + "      col: " + (col + 1));
              setIcon(new ImageIcon(tileScaled));
              e.getComponent().repaint();
            }
          }
        }
      });
      addPropertyChangeListener(evt -> {
        Object source = evt.getSource();
        String temp = ((Cell) source).getText();
        if(temp.equals("0")) {
          label.setForeground(new Color(0,0,0,0));
        } else if(temp.equals("1")) {
          label.setForeground(new Color(25,118,210,255));
        } else if(temp.equals("2")) {
          label.setForeground(new Color(70, 146, 68, 255));
        } else if(temp.equals("3")) {
          label.setForeground(new Color(211,55,53,255));
        } else if(temp.equals("4")) {
          label.setForeground(new Color(123,31,162,255));
        } else if(temp.equals("5")) {
          label.setForeground(new Color(229,180,90,255));
        } else if(temp.equals("6")) {
          label.setForeground(new Color(80,172,151,255));
        } else if(temp.equals("7")) {
          label.setForeground(new Color(61,64,69,255));
        } else if(temp.equals("8")) {
          label.setForeground(new Color(163,159,136,255));
        } else if(temp.equals("X")) {
          label.setForeground(new Color(187, 20, 26,255));
        }
        label.setText(temp);

      });

      addComponentListener(new ComponentAdapter() {
        @Override
        public void componentResized(ComponentEvent e) {
          JButton btn = (JButton) e.getComponent();
          btn.setHorizontalTextPosition(JButton.CENTER);
          btn.setVerticalTextPosition(JButton.CENTER);
          btn.setFont(new Font("Tahoma", Font.PLAIN, 1));
          btn.setForeground(new Color(158,158,158,255));
          btn.setLayout(new BorderLayout());
          Dimension size = btn.getSize();
          Insets insets = btn.getInsets();
          size.width -= insets.left + insets.right;
          size.height -= insets.top + insets.bottom;

          if (size.width > size.height) {
            size.width = -1;
          } else {
            size.height = -1;
          }
          buttonWidth = size.width;
          buttonHeight = size.height;

          tileScaled = cellTile.getScaledInstance(buttonWidth, buttonHeight, java.awt.Image.SCALE_SMOOTH);
          tileFlagScaled = cellTileFlag.getScaledInstance(buttonWidth, buttonHeight, java.awt.Image.SCALE_SMOOTH);
          if(!isFlagged) {
            btn.setIcon(new ImageIcon(tileScaled));
          } else if(isFlagged) {
            btn.setIcon(new ImageIcon(tileFlagScaled));
          }
          btn.add(label, BorderLayout.CENTER);
          label.setHorizontalAlignment(btn.CENTER);
          label.setVerticalAlignment(btn.CENTER);
          label.setHorizontalTextPosition(btn.CENTER);
          label.setVerticalTextPosition(btn.CENTER);

          Dimension gSize = grid.getSize();
          Insets gInsets = grid.getInsets();
          gSize.width -= gInsets.left + gInsets.right;
          gSize.height -= gInsets.top + gInsets.bottom;

          /**
           * This is working code, here as a fallback
           * It may be able to be improved upon, idk.
           **/
          if(!fullscreen) {
            insetTop = 30;
            insetBottom = 10;
            insetLeft = 50;
            insetRight = 50;
            if(frame.getHeight() == screenHeight) {
              insetLeft = Math.abs((frame.getWidth() - gSize.width) / 2);
              insetRight = Math.abs((frame.getWidth() - gSize.height) / 2);
            } else if (frame.getWidth() + 25 > frame.getHeight()) {
              insetLeft = Math.abs((frame.getWidth() - gSize.height) / 2);
              insetRight = insetLeft;
            } else if (frame.getHeight() > frame.getWidth() + 25) {
              insetTop = Math.abs((frame.getHeight() - gSize.width) / 2 - 25);
              insetBottom = Math.abs((frame.getHeight() - gSize.width) / 2 - 25);
            }
            padding = BorderFactory.createEmptyBorder(insetTop, insetLeft, insetBottom, insetRight);
          }

          /*
          if(!fullscreen) {   //FIXME: Maybe use a while loop with insetL/R++ until grid is the same size vert and horiz
                              // Don't think this will work. Process froze up
            insetTop = 0;
            insetBottom = 0;
            insetLeft = 0;
            insetRight = 0;
            if(frame.getHeight()>= screenHeight) {
              insetLeft = Math.abs((frame.getWidth() - gSize.width) / 2);
              insetRight = Math.abs((frame.getWidth() - gSize.height) / 2);
            } else if (frame.getWidth() + 25 > frame.getHeight()) {
              while(gSize.width + 10 > gSize.height || gSize.width - 10 > gSize.height) {
                insetLeft++;
              }
              insetLeft = Math.abs((frame.getWidth() - gSize.height) / 2);
              insetRight = insetLeft;
            } else if (frame.getHeight() > frame.getWidth() + 25) {
              while(gSize.height + 10 > gSize.width || gSize.height - 10 > gSize.width) {
                insetRight++;
              }
              insetTop = Math.abs((frame.getHeight() - gSize.width) / 2);
              insetBottom = Math.abs((frame.getHeight() - gSize.width) / 2 - 20);
            }
            padding = BorderFactory.createEmptyBorder(insetTop, insetLeft, insetBottom, insetRight);
          }
           */
          contentPanel.setBorder(padding);
          contentPanel.repaint();
        }
      });
      label.setText("");
    }

    int getValue() {
      return value;
    }

    void setValue(int value) {
      this.value = value;
    }

    boolean isAMine() {
      return value == MINE;
    }

    void reset() {
      setValue(0);
      setEnabled(true);
      setText("");
      setUnflagged();
      setIcon(new ImageIcon(tileScaled));
    }

    void reveal() {
      setEnabled(false);
      setText(isAMine() ? "X" : String.valueOf(value));
      setIcon(new ImageIcon(tileScaled));
      if(value == 0) {
        setText("");
      }
    }

    void updateNeighbourCount() {
      getNeighbours(reusableStorage);
      for (Cell neighbour : reusableStorage) {
        if (neighbour == null) {
          break;
        }
        if (neighbour.isAMine()) {
          value++;
        }
      }
    }

    void getNeighbours(final Cell[] container) {
      // Empty all elements first
      for (int i = 0; i < reusableStorage.length; i++) {
        reusableStorage[i] = null;
      }

      int index = 0;

      for (int rowOffset = -1; rowOffset <= 1; rowOffset++) {
        for (int colOffset = -1; colOffset <= 1; colOffset++) {
          // Make sure that we don't count ourselves
          if (rowOffset == 0 && colOffset == 0) {
            continue;
          }
          int rowValue = row + rowOffset;
          int colValue = col + colOffset;

          if (rowValue < 0 || rowValue >= gridSize
                  || colValue < 0 || colValue >= gridSize) {
            continue;
          }

          container[index++] = cells[rowValue][colValue];
        }
      }
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null || getClass() != obj.getClass())
        return false;
      Cell cell = (Cell) obj;
      return row == cell.row && col == cell.col;
    }

    @Override
    public int hashCode() {
      return Objects.hash(row, col);
    }
  }



  private Minesweeper(final int gridSize) throws IOException {
    frame = new JFrame("Minesweeper");
    contentPanel.setBorder(padding);
    frame.setContentPane(contentPanel);
    frame.setSize(SIZE - 25, SIZE);
    frame.setMinimumSize(new Dimension(500, 500));
    frame.setLayout(new BorderLayout());

    JMenuBar menuBar = new JMenuBar();    //Creates Toolbar above
    JMenu menuGame = new JMenu("Game");
    JMenu menuHelp = new JMenu("Help");
    JMenuItem menuGameItemRestart = new JMenuItem("Restart");
    JMenuItem menuGameItemd = new JMenuItem("Restart");
    JMenuItem menuHint = new JMenuItem("^ This does nothing ^");
    menuGame.add(menuGameItemRestart);
    menuGame.add(menuHint);
    menuBar.add(menuGame);
    menuBar.add(menuHelp);
    frame.setJMenuBar(menuBar);

    this.gridSize = gridSize;
    cells = new Cell[gridSize][gridSize];

    initializeButtonPanel();
    initializeGrid();

    frame.setLocationRelativeTo(null);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setVisible(true);
  }

  private void initializeButtonPanel() {
    JPanel buttonPanel = new JPanel();

    JButton reset = new JButton("Reset");
    JButton giveUp = new JButton("Give Up");

    reset.addActionListener(e -> createMines());
    giveUp.addActionListener(e -> revealBoardAndDisplay("You gave up."));

    buttonPanel.add(reset);
    buttonPanel.add(giveUp);
    frame.add(buttonPanel, BorderLayout.SOUTH);
    buttonPanelHeight = buttonPanel.getHeight();
  }

  private void initializeGrid() throws IOException {
    grid.setLayout(new GridLayout(gridSize, gridSize));
    grid.setBounds(new Rectangle(500, 500));

    for (int row = 0; row < gridSize; row++) {
      for (int col = 0; col < gridSize; col++) {
        cells[row][col] = new Cell(row, col);
        grid.add(cells[row][col]);
        int width = cells[row][col].getWidth() + 10;
        int height = cells[row][col].getHeight() + 10;
        Image img = ImageIO.read(Objects.requireNonNull(getClass().getResource("tile.png")));
        img = img.getScaledInstance(width, height, Image.SCALE_DEFAULT);
        cells[row][col].setBackground(Color.GRAY);
        cells[row][col].setIcon(new ImageIcon(img));
        cells[row][col].setMargin(new Insets(0, 0, 0, 0));
        cells[row][col].setBorder(null);
        cells[row][col].repaint();
      }
    }
    createMines();
    frame.add(grid, BorderLayout.CENTER);
  }

  private void resetAllCells() {
    for (int row = 0; row < gridSize; row++) {
      for (int col = 0; col < gridSize; col++) {
        cells[row][col].reset();
      }
    }
  }

  private void createMines() {
    firstTurn = true;
    resetAllCells();

    final int mineCount = (int) POPULATION_CONSTANT * gridSize * 2;
    final Random random = new Random();

    // Map all (row, col) pairs to unique integers
    Set<Integer> positions = new HashSet<>(gridSize * gridSize);
    for (int row = 0; row < gridSize; row++) {
      for (int col = 0; col < gridSize; col++) {
        positions.add(row * gridSize + col);
      }
    }

    // Initialize mines
    for (int index = 0; index < mineCount; index++) {
      int choice = random.nextInt(positions.size());
      int row = choice / gridSize;
      int col = choice % gridSize;
      cells[row][col].setValue(MINE);
      positions.remove(choice);
    }

    // Initialize neighbour counts
    for (int row = 0; row < gridSize; row++) {
      for (int col = 0; col < gridSize; col++) {
        if (!cells[row][col].isAMine()) {
          cells[row][col].updateNeighbourCount();
        }
      }
    }
  }

  private void handleCell(Cell cell) {
    if(!cell.isFlagged) {
      if (cell.isAMine() && firstTurn) {
        createMines();
        handleCell(cell);
      }
      if (cell.isAMine() && !firstTurn) {
        cell.setForeground(Color.RED);
        cell.reveal();
        revealBoardAndDisplay("You clicked on a mine!");

        return;
      }
      if (cell.getValue() == 0) {
        firstTurn = false;
        Set<Cell> positions = new HashSet<>();
        positions.add(cell);
        cascade(positions);
      } else {
        firstTurn = false;
        cell.reveal();
      }
      checkForWin();
    }
  }

  private void revealBoardAndDisplay(String message) {
    for (int row = 0; row < gridSize; row++) {
      for (int col = 0; col < gridSize; col++) {
        if (!cells[row][col].isEnabled()) {
          cells[row][col].reveal();
          cells[row][col].setUnflagged();
        }
      }
    }

    JOptionPane.showMessageDialog(
            frame, message, "Game Over",
            JOptionPane.ERROR_MESSAGE
    );

    createMines();
  }

  private void cascade(Set<Cell> positionsToClear) {
    while (!positionsToClear.isEmpty()) {
      // Set does not have a clean way for retrieving
      // a single element. This is the best way I could think of.
      Cell cell = positionsToClear.iterator().next();
      positionsToClear.remove(cell);
      if(!cell.isFlagged) {
        cell.reveal();
      }

      cell.getNeighbours(reusableStorage);
      for (Cell neighbour : reusableStorage) {
        if (neighbour == null) {
          break;
        }
        if (neighbour.getValue() == 0 && neighbour.isEnabled()) {
          positionsToClear.add(neighbour);
        } else {
          neighbour.reveal();
        }
      }
    }
  }

  private void checkForWin() {
    boolean won = true;
    outer:
    for (Cell[] cellRow : cells) {
      for (Cell cell : cellRow) {
        if (!cell.isAMine() && cell.isEnabled()) {
          won = false;
          break outer;
        }
      }
    }

    if (won) {
      JOptionPane.showMessageDialog(
        frame, "You have won!", "Congratulations",
        JOptionPane.INFORMATION_MESSAGE
      );
    }
  }

  private static void run(final int gridSize) throws IOException {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } catch (Exception ignore) { }
    // Launch the program
    new Minesweeper(gridSize);
  }

  public static void main(String[] args) {
    final int gridSize = 16;
    SwingUtilities.invokeLater(() -> {
      try {
        Minesweeper.run(gridSize);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });
  }
}
