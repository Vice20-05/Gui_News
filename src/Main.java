
import javax.swing.SwingUtilities;

/** Starts the desktop application. */
public class Main {

    /** Creates the GUI on Swing's event-dispatching thread. */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> GUI.showGUI());
    }
}
