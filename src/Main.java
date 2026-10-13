
import java.awt.Color;
import java.util.Arrays;

import  javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.plaf.metal.OceanTheme;

/** Starts the desktop application. */
public class Main {

    /** Creates the GUI on Swing's event-dispatching thread. */
    public static void main(String[] args) {
        applyDarkTheme();
        SwingUtilities.invokeLater(() -> GUI.showGUI());
    }

    private static void applyDarkTheme(){

        //Metal draws small icons (like the combo box arrow) in black, which disappear on the dark buttons,
        //so use the Ocean theme with white icons instead
        MetalLookAndFeel.setCurrentTheme(new OceanTheme(){
            @Override
            public ColorUIResource getControlInfo(){
                return new ColorUIResource(Color.WHITE);
            }
        });
        try{
            UIManager.setLookAndFeel(new MetalLookAndFeel());
        }catch(UnsupportedLookAndFeelException e){
            //Metal is always available, if not we keep the default look
        }

        //background and text to be BLACK and white
        ColorUIResource backG=new ColorUIResource(Color.BLACK);
        ColorUIResource textC=new ColorUIResource(Color.WHITE);

        //Slightly Lighter colors, so the buttons stands out
        ColorUIResource darkGray=new ColorUIResource(new Color(45,45,45));
        ColorUIResource selected=new ColorUIResource(new Color(30,90,160));


        //background and text color for the components
        String[] components={"Panel","OptionPane","List","TextArea","TextField","Label",
                            "ScrollPane","Viewport","SplitPane","ComboBox","ProgressBar"
        };

        for (String name:components){
            UIManager.put(name+".background",backG);
            UIManager.put(name+".foreground",textC);
        }
        UIManager.put("OptionPane.messageForeground",textC);
        
        //Buttons are dark gray 
        UIManager.put("Button.background",darkGray);
        UIManager.put("Button.foreground",textC);

        //The default (Metal Ocean) draws a light gradient on the buttons, which hides the white text.
        //Putting null does not work (Swing falls back to the default gradient), so use a dark one instead:
        //{ratio1, ratio2, top color, middle color, bottom color}
        ColorUIResource lighterGray=new ColorUIResource(new Color(70,70,70));
        UIManager.put("Button.gradient",Arrays.asList(0.3f,0f,lighterGray,darkGray,darkGray));

        //Metal paints pressed buttons light gray and disabled text dark gray,
        //both hide the text on a dark button, so pick colors that stay readable
        UIManager.put("Button.select",new ColorUIResource(new Color(80,80,80)));
        UIManager.put("Button.disabledText",new ColorUIResource(new Color(140,140,140)));
        UIManager.put("Button.focus",selected);

        //Selected rows in the list and the combo box use the blue color
        UIManager.put("List.selectionBackground",selected);
        UIManager.put("List.selectionForeground",textC);
        UIManager.put("ComboBox.selectionBackground",selected);
        UIManager.put("ComboBox.selectionForeground",textC);
        UIManager.put("ComboBox.buttonBackground",darkGray);
        UIManager.put("TextField.selectionBackground",selected);
        UIManager.put("TextField.selectionForeground",textC);
        UIManager.put("TextArea.selectionBackground",selected);
        UIManager.put("TextArea.selectionForeground",textC);

        //blinking the cursor in the text fields must be white or it dissapears
        UIManager.put("TextField.caretForeground",textC);
        UIManager.put("TextArea.caretForeground",textC);
    }
}