
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.swing.JComponent;

public class CoordinatePanel extends JComponent implements ComponentListener, MouseWheelListener, MouseListener, MouseMotionListener {
	
	private static final long serialVersionUID = -3729805747119272534L;

	private double[] amplitudes;
	private double sigma;
	private int[] portions;
	private int[] cough_portions;

	private BufferedImage bufferedImage;
	private Graphics2D bufferedGraphics;
	
	private double zoom = 1;
	private double zoomFactor = 0.1;
	private boolean changed = false;
	
	private int startX = 0;
	private int startY = 0;
	private int offsetX = 0;
	private int offsetY = 0;
	private int mouseX = 0;
	    
	/*
	 * Erstellt JPanel und aktualisiert bei Änderung die Anzeige
	 */
	public CoordinatePanel(double[] amplitudes, double sigma, int[] portions, int[] cough_portions) {
		bufferedImage = new BufferedImage(640*10,480*10, BufferedImage.TYPE_INT_RGB);
		bufferedGraphics = bufferedImage.createGraphics();
		this.addComponentListener(this);
		this.addMouseWheelListener(this);
		this.addMouseListener(this);
		this.addMouseMotionListener(this);
		this.sigma = sigma * 64;
		this.portions = portions;
		this.cough_portions = cough_portions;
		this.amplitudes = new double[amplitudes.length];
		for (int i = 0; i < amplitudes.length; i++) {
			this.amplitudes[i] = amplitudes[i] * -64;
		}
		
		ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
		exec.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
			if (!changed)
				return;
			
      		bufferedImage = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
      		bufferedGraphics = bufferedImage.createGraphics();
			drawSoundwave();
			
			changed = false;
		  }
		}, 0, 50, TimeUnit.MILLISECONDS);
	}
	
	/*
	 * Malt das Bild auf JPanel
	 */
	public void paintComponent(final Graphics g) {
        g.drawImage(bufferedImage, 0, 0, null);
    }
	
	/*
	 * Transformiert den X-Wert angepasst an die Mausbewegungen
	 */
	public int arrXToDrawX(int index) {
		return (int) (index * getWidth() * zoom / amplitudes.length + offsetX);
	}
	
	/*
	 * Transformiert den Y-Wert angepasst an die Mausbewegungen
	 */
	public int arrYToDrawY(double arrVal) {
		return (int) (arrVal + getHeight() / 2 + offsetY);
	}

	/*
	 * Zeichnet die Inhalte der Anzeige auf ein Bild und aktualisiert Anzeige
	 */
	public void drawSoundwave() {
		bufferedGraphics.setColor(Color.red);
		bufferedGraphics.drawLine(0, (int)(getHeight() / 2 - sigma + offsetY), getWidth(), (int)(getHeight() / 2 - sigma + offsetY));
		bufferedGraphics.drawLine(0, (int)(getHeight() / 2 + sigma + offsetY), getWidth(), (int)(getHeight() / 2 + sigma + offsetY));


		bufferedGraphics.setColor(Color.gray);
		for (int i = 0; i < portions.length; i++) {
			int x = arrXToDrawX(portions[i]);
			bufferedGraphics.drawLine(x, getHeight() / 2 - 100 + offsetY, x, getHeight() / 2 + 100 + offsetY);
			if (i % 2 == 0) {
				int next_x = arrXToDrawX(portions[i + 1]);
				bufferedGraphics.drawLine(x, getHeight() / 2 - 100 + offsetY, next_x, getHeight() / 2 - 100 + offsetY);
				bufferedGraphics.drawLine(x, getHeight() / 2 + 100 + offsetY, next_x, getHeight() / 2 + 100 + offsetY);
			}
		}
		
		bufferedGraphics.setColor(Color.blue);
		bufferedGraphics.setStroke(new BasicStroke(2));
		for (int i = 0; i < cough_portions.length; i++) {
			int x = arrXToDrawX(cough_portions[i]);
			bufferedGraphics.drawLine(x, getHeight() / 2 - 100 + offsetY, x, getHeight() / 2 + 100 + offsetY);
			if (i % 2 == 0) {
				int next_x = arrXToDrawX(cough_portions[i + 1]);
				bufferedGraphics.drawLine(x, getHeight() / 2 - 100 + offsetY, next_x, getHeight() / 2 - 100 + offsetY);
				bufferedGraphics.drawLine(x, getHeight() / 2 + 100 + offsetY, next_x, getHeight() / 2 + 100 + offsetY);
			}
		}
		bufferedGraphics.setStroke(new BasicStroke(1));
		
		bufferedGraphics.setColor(Color.white);
		try {
			bufferedGraphics.drawLine(0, getHeight() / 2 + offsetY, getWidth(), getHeight() / 2 + offsetY);
		} catch (Exception e) {}
		
    	for (int i = 0; i < amplitudes.length - 1; i++) {
    		try {
    			bufferedGraphics.drawLine(arrXToDrawX(i), arrYToDrawY(amplitudes[i]), arrXToDrawX(i+1), arrYToDrawY(amplitudes[i+1]));
    		} catch (Exception e) {}
		}
    	
    	int index = (int)((mouseX - offsetX) * (amplitudes.length / zoom / getWidth()));
    	if (index < 0)
    		index = 0;
    	else if (index >= amplitudes.length)
    		index = amplitudes.length - 1;
    	int mouseAmp = (int)amplitudes[index];
    	bufferedGraphics.drawString("(" + String.valueOf((int)((mouseX - offsetX) * (amplitudes.length / zoom / getWidth()))) + "/" + String.valueOf(mouseAmp) + ")", mouseX, mouseAmp + getHeight() / 2 + offsetY - 30);

		repaint();
	}

	@Override
	public void componentHidden(ComponentEvent e) {		
	}
	
	@Override
	public void componentMoved(ComponentEvent e) {
	}

	@Override
	public void componentResized(ComponentEvent e) {
		changed = true;
	}

	@Override
	public void componentShown(ComponentEvent e) {		
	}

	/*
	 * Zoom-Funktion mit dem Mausrad
	 */
	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {
		zoomFactor = zoom / 10;
        if (e.getWheelRotation() < 0) {
        	if (amplitudes.length / zoom / getWidth() < 0.1)
        		return;
        	zoom += zoomFactor;
        } else if (e.getWheelRotation() > 0) {
            zoom -= zoomFactor;
        	if (zoom < 1)
        		zoom = 1;
        }
        changed = true;
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		
	}

	@Override
	public void mousePressed(MouseEvent e) {
		
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		startX = 0;
		startY = 0;
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		
	}

	@Override
	public void mouseExited(MouseEvent e) {
		
	}

	/*
	 * Bewegung in der Anzeige mit der Maus
	 */
	@Override
	public void mouseDragged(MouseEvent e) {
		if (startX == 0 && startY == 0) {
			startX = e.getX();
			startY = e.getY();
		}
		
		offsetX += e.getX() - startX;
		offsetY += e.getY() - startY;

		startX = e.getX();
		startY = e.getY();
		
		changed = true;
	}

	/*
	 * Für die Amplitudenwert-Anzeige
	 */
	@Override
	public void mouseMoved(MouseEvent e) {
		mouseX = e.getX();
		changed = true;
	}
}
