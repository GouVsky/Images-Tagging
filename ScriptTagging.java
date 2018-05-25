import ij.*;
import ij.process.*;
import ij.plugin.filter.*;
import java.awt.Color;
import java.util.*;
import java.io.*;


public class ScriptTagging implements PlugInFilter
{
    // Colors.
    private int colorsNumber;
    private Map <String, Integer> allColors;


    public ScriptTagging()
    {
        allColors = new HashMap <String, Integer> ();
    }

    public List <String> selectMainColors()
    {
        List <String> mainColors = new ArrayList <String> ();

        for (Map.Entry <String, Integer> entry : allColors.entrySet())
        {
            if (!entry.getKey().equals("unknown") && Math.ceil(entry.getValue() * 100f / colorsNumber) >= 15)
                mainColors.add(entry.getKey());
        }

        return mainColors;
    }

    public String getColor(float [] hsv)
    {
        float hue = hsv[0];
        float saturation = hsv[1];
        float value = hsv[2];

        if ((hue >= 0 && hue <= 360) && saturation <= 0.15 && value >= 0.65)
            return "white";

        else if ((hue >= 0 && hue <= 360) && (saturation >= 0) && value <= 0.1)
            return "black";

        else if ((hue >= 0 && hue <= 360) && saturation <= 0.15 && (value >= 0.1 && value <= 0.65))
            return "gray";

        else if ((hue <= 11 || hue >= 351) && saturation >= 0.7 && value >= 0.1)
            return "red";

        else if ((hue >= 180 && hue <= 255) && saturation >= 0.15 && value >= 0.1)
            return "blue";

        else if ((hue >= 64 && hue <= 150) && saturation >= 0.15 && value >= 0.1)
            return "green";

        else if ((hue >= 45 && hue <= 64) && saturation >= 0.15 && value >= 0.1)
            return "yellow";

        else if ((hue >= 11 && hue <= 45) && saturation >= 0.15 && value >= 0.75)
            return "orange";

        else if ((hue >= 11 && hue <= 45) && saturation >= 0.15 && (value >= 0.1 && value <= 0.75))
            return "brown";

        else
            return "unknown";
    }

    public void run(ImageProcessor ip)
    {        
    	boolean grayscale = ip.isGrayscale();

        int width = ip.getWidth();
        int height = ip.getHeight();

        for (int y = 0; y < height; y++)
        {
            for (int x = 0; x < width; x++) 
            {
                // On récupère la couleur de chaque pixel et on tranforme sa valeur RGB en valeur HSV.

                int[] rgb = new int[3];
                float[] hsv = new float[3];
                
                ip.getPixel(x, y, rgb);

				if (grayscale)
				{
                	rgb[1] = rgb[0];
                	rgb[2] = rgb[0];
				}

                Color.RGBtoHSB(rgb[0], rgb[1], rgb[2], hsv);

                // On convertit 'hue' en degrés.
                hsv[0] *= 360;
                
                String color = getColor(hsv);

                colorsNumber++;
 
                if (allColors.containsKey(color))
                    allColors.put(color, allColors.get(color) + 1);

                else
                    allColors.put(color, 1);
            }
        }

        loadInFile();
    }

    public void loadInFile()
    {
        File file = new File("/Users/Greg/Desktop/file.txt"); 
        
        try
        {
            file.createNewFile();
            BufferedWriter out = new BufferedWriter(new FileWriter(file));  
            
            List <String> mainColors = selectMainColors();

            for (String color : mainColors)
                out.write(color + " ");
 
            out.flush(); 
            out.close(); 

        } catch (IOException e)
        {
            e.printStackTrace();
        } 
    }

    public int setup(String arg, ImagePlus imp)
    {
        if (arg.equals("about"))
        {
            IJ.showMessage("Traitement de l'image");

            return DONE;
        }

        return DOES_ALL;
    }
}