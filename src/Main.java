
import javax.swing.SwingUtilities;
//NO AI
public class Main {

    public static void main(String[] args) {
        // Schedule a job for the event-dispatching thread:
        SwingUtilities.invokeLater(()-> GUI.showGUI());
    }
}
