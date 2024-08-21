import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.event.*;
import java.text.DecimalFormat;

import javax.swing.*;

class Trainer extends Thread {
    public Trainer(AgentPWQPlusSOM_SOM_Test test) {
        this.test = test;
    }

    private AgentPWQPlusSOM_SOM_Test test;

    @Override
    public void run() {
        for (int iteration = 0; iteration < 32000; iteration++) {
            float[] input = new float[AgentPWQPlusSOM_SOM_Test.DATA_DIM];
            for (int i = 0; i < input.length; i++)
                input[i] = (float)Math.random();
            test.lattice.adapt(input);

            if (iteration % 500 == 0)
				test.renderPanelLeft.render();
        }
    }
}

class LatticeRenderer extends JPanel {

	static final long serialVersionUID = 1;

	private BufferedImage img = null;
	private Font arialFont = new Font("Arial", Font.BOLD, 12);
	private AgentPWQPlusSOM_SelfOrganizingMap lattice;
	// Größe des Panels zur Darstellung
	private final int PANELWIDTH = 400;
	private final int PANELHEIGHT = 400;

	public LatticeRenderer(AgentPWQPlusSOM_SelfOrganizingMap lat) {
		lattice = lat;
		addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
			public void mouseMoved(java.awt.event.MouseEvent evt) {
				panelMouseMoved(evt);
			}
		});
	}

	public void paint(Graphics g) {
		if (img == null)
			super.paint(g);
		else
			g.drawImage(img, 0, 0, this);
	}

	// Neue Daten im Renderer anmelden, z.B. bei neuer Initialisierung
	public void registerLattice(AgentPWQPlusSOM_SelfOrganizingMap lat) {
		lattice = lat;
		this.render();
	}

	@SuppressWarnings("all")
	public void render() {
        if (AgentPWQPlusSOM_SOM_Test.DATA_DIM == 2)
		    this.renderTwoDimensions();
        else if (AgentPWQPlusSOM_SOM_Test.DATA_DIM == 3)
            this.renderRGB();
	}

	private void showIterations(Graphics2D g2) {
		g2.setFont(arialFont);
		g2.setColor(Color.white);
		g2.fillRect(0, getHeight() - 40, getWidth(), 15);
		g2.setColor(Color.black);
		g2.drawString("Iteration: " + lattice.getIteration(), 5, getHeight() - 28);
	}

    // Yeah, it's ugly. But it works.
	// Erzeugt gemäß den Gewichtsvektoren der Knoten mit Farbe befüllte Rechtecke
	public void renderRGB() {
		// System.out.println (getWidth() + " Höhe: " +getHeight());
		// Größe pro Zelle: 10 Pixel (400 / 40 = 10)
		int cellWidth = PANELWIDTH / AgentPWQPlusSOM_SOM_Test.SIZE_PER_DIM;
		int cellHeight = PANELHEIGHT / AgentPWQPlusSOM_SOM_Test.SIZE_PER_DIM;
		// System.out.println (cellWidth + " ZellenHöhe: " +cellHeight);

		double r, g, b;
		Graphics2D g2 = img.createGraphics();
		for (int x = 0; x < AgentPWQPlusSOM_SOM_Test.SIZE_PER_DIM; x++) {
			for (int y = 0; y < AgentPWQPlusSOM_SOM_Test.SIZE_PER_DIM; y++) {
				// Rahmen zeichnen:
				g2.setColor(Color.black);
				g2.drawRect((int) (x * cellWidth), (int) (y * cellHeight), (int) cellWidth, (int) cellHeight);
				// Farbe innerhalb zeichnen:
				r = lattice.getNode(x, y).getWeight()[0];
				g = lattice.getNode(x, y).getWeight()[1];
				b = lattice.getNode(x, y).getWeight()[2];
				g2.setColor(new Color((float) r, (float) g, (float) b));
				g2.fillRect((int) (x * cellWidth) + 1, (int) (y * cellHeight) + 1, (int) cellWidth - 1,
						(int) cellHeight - 1);
			}
		}
		this.showIterations(g2);
		g2.dispose();
		repaint();
	}

	/*
	 * Yeah, it's ugly, too. Erzeugt gemäß den Gewichtsvektoren der Knoten die
	 * Verbindungsgeraden von benachbarten Knoten x/y ---- x+1/y | | | | x/y+1
	 * ---x+1/y+1
	 */
	public void renderTwoDimensions() {
		Graphics2D g2 = img.createGraphics();
		g2.setBackground(Color.WHITE);
		g2.clearRect(0, 0, PANELWIDTH, PANELHEIGHT);

		for (int x = 0; x < AgentPWQPlusSOM_SOM_Test.SIZE_PER_DIM - 1; x++) {
			for (int y = 0; y < AgentPWQPlusSOM_SOM_Test.SIZE_PER_DIM - 1; y++) {
				g2.setColor(Color.black);
				int xpos1 = (int) ((lattice.getNode(x, y).getWeight()[0]) * PANELWIDTH);
				int ypos1 = (int) ((lattice.getNode(x, y).getWeight()[1]) * PANELHEIGHT);
				int xpos2 = (int) ((lattice.getNode(x + 1, y).getWeight()[0]) * PANELWIDTH);
				int ypos2 = (int) ((lattice.getNode(x + 1, y).getWeight()[1]) * PANELHEIGHT);
				int xpos3 = (int) ((lattice.getNode(x, y + 1).getWeight()[0]) * PANELWIDTH);
				int ypos3 = (int) ((lattice.getNode(x, y + 1).getWeight()[1]) * PANELHEIGHT);
				int xpos4 = (int) ((lattice.getNode(x + 1, y + 1).getWeight()[0]) * PANELWIDTH);
				int ypos4 = (int) ((lattice.getNode(x + 1, y + 1).getWeight()[1]) * PANELHEIGHT);
				// System.out.println(xpos1 + "/" + ypos1 + " " + xpos2 + "/" + ypos2);

				// Kreis für den Knoten = Neuron befüllen
				g2.fillOval(xpos1 - 2, ypos1 - 2, 4, 4);
				g2.fillOval(xpos4 - 2, ypos4 - 2, 4, 4);

				// waagerechte Verbindung von x/y zum nächsten Knoten x+1/y zeichnen:
				g2.drawLine(xpos1, ypos1, xpos2, ypos2);

				// senkrechte Verbindung von x/y nach x/y+1:
				g2.drawLine(xpos1, ypos1, xpos3, ypos3);

				// waagerechte Verbindung von x/y+1 nach x+1/y+1
				g2.drawLine(xpos3, ypos3, xpos4, ypos4);

				// senkrechte Verbindung von x+1/y nach x+1/y+1:
				g2.drawLine(xpos2, ypos2, xpos4, ypos4);

			}
		}
		this.showIterations(g2);
		g2.dispose();
		repaint();
	}

	public BufferedImage getImage() {
		if (img == null)
			img = (BufferedImage) createImage(getWidth(), getHeight());

		return img;
	}

	// Falls man mit der Maus über einen Knoten geht, wird der Inhalt des
	// Gewichtsvektors angezeigt
	private void panelMouseMoved(java.awt.event.MouseEvent evt) {
		int x = evt.getX();
		int y = evt.getY();
		// System.out.println (x + " y: " + y);
		int cellWidth = PANELWIDTH / AgentPWQPlusSOM_SOM_Test.SIZE_PER_DIM;
		int cellHeight = PANELHEIGHT / AgentPWQPlusSOM_SOM_Test.SIZE_PER_DIM;
		// Welche Zelle von 0 bis 39 wurde getroffen?
		int cellX = x / cellWidth;
		int cellY = y / cellHeight;
		// System.out.println ("cellY: " + cellX + " cellY: " + cellY);
		Graphics2D g2 = (Graphics2D) img.getGraphics();
		g2.setFont(arialFont);
		StringBuffer sb = new StringBuffer("Gewichtsvektor eines Knoten: ");
		for (int i = 0; i < AgentPWQPlusSOM_SOM_Test.DATA_DIM; i++) {
			double myDouble = 0;
			// hole die Daten aus dem Gitter
			if (cellX < AgentPWQPlusSOM_SOM_Test.SIZE_PER_DIM && cellY < AgentPWQPlusSOM_SOM_Test.SIZE_PER_DIM)
				myDouble = lattice.getNode(cellX, cellY).getWeight()[i];
			// zeige den Inhalt des Gewichtsvektors an
			DecimalFormat df = new DecimalFormat("0.00");
			String s = df.format(myDouble);
			sb.append(s);
			sb.append("  ");
		}
		g2.setColor(Color.white);
		g2.fillRect(0, getHeight() - 20, getWidth(), 15);
		g2.setColor(Color.black);
		g2.drawString(sb.toString(), 5, getHeight() - 8);
		g2.dispose();
		repaint();
	}
}

public class AgentPWQPlusSOM_SOM_Test extends JFrame {
    static final long serialVersionUID =1;
	public AgentPWQPlusSOM_SelfOrganizingMap lattice;
    private Trainer trainer;

	private JSplitPane jSplitPane;
   
	// Anzeige Kohonen Karte links:
    public LatticeRenderer renderPanelLeft;
    
    private JButton btnInitialize;
    private JButton btnStart;
    private JButton btnStop;
    private JPanel ControlsPanelRight;
 
    public static final int DATA_DIM = 2;
    public static final int MAP_DIM = 2;
    public static final int SIZE_PER_DIM = 40;
	
	public AgentPWQPlusSOM_SOM_Test() {
    	super("Kohonen-Map");
		setDefaultCloseOperation (EXIT_ON_CLOSE); 
		
		// Model initialisieren, d.h. die Daten der Karte im Gitter:
		lattice = new AgentPWQPlusSOM_SelfOrganizingMap(DATA_DIM, MAP_DIM, SIZE_PER_DIM);
        lattice.setParameters(2 * SIZE_PER_DIM * SIZE_PER_DIM, 0.1f, 0.025f, SIZE_PER_DIM / 2, 1);
		
		initComponents();
		renderPanelLeft.getImage();
	}

    private void start() {
        trainer = new Trainer(this);
        trainer.start();
    }
    private void stop() {
        //trainer.stop();
    }

	// Initialisierung der Controls
    private void initComponents() {
        jSplitPane = new JSplitPane();
        jSplitPane.setDividerLocation(401);
        jSplitPane.setDividerSize(5);
        jSplitPane.setEnabled(false);
       
        //Fläche für die Karte links:
        renderPanelLeft = new LatticeRenderer(lattice);
        jSplitPane.setLeftComponent(renderPanelLeft);
        
        // Fläche für die Buttons rechts
        ControlsPanelRight = new JPanel();
        ControlsPanelRight.setLayout(new BoxLayout(ControlsPanelRight, BoxLayout.Y_AXIS));
        
        btnInitialize = new JButton();
        btnInitialize.setText("Initialisiere Trainings-Daten");
        btnInitialize.setAlignmentX(CENTER_ALIGNMENT);
        btnInitialize.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        btnInitialize.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
		        lattice = new AgentPWQPlusSOM_SelfOrganizingMap(DATA_DIM, MAP_DIM, SIZE_PER_DIM);
                lattice.setParameters(2 * SIZE_PER_DIM * SIZE_PER_DIM, 0.1f, 0.025f, SIZE_PER_DIM / 2, 1);
                renderPanelLeft.registerLattice(lattice);
             }
        });
        ControlsPanelRight.add(btnInitialize);

        
        btnStart = new JButton();
        btnStart.setText("Start Training");
        btnStart.setAlignmentX(CENTER_ALIGNMENT);
        btnStart.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        btnStart.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                lattice.setIteration(0);
                start();
            }
        });
        ControlsPanelRight.add(btnStart);

        
        btnStop = new JButton();
        btnStop.setText("Stop Training");
        btnStop.setAlignmentX(CENTER_ALIGNMENT);
        btnStop.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        btnStop.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
            	stop();
            }
        });
        ControlsPanelRight.add(btnStop);


        jSplitPane.setRightComponent(ControlsPanelRight);

        getContentPane().add(jSplitPane, BorderLayout.CENTER);

        pack();
        setSize(new Dimension(620, 480));
        setVisible(true);
    }

	public static void main(String args[]) {
		new AgentPWQPlusSOM_SOM_Test();	
	}
}
