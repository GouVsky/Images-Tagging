import ij.*;
import ij.process.*;
import ij.plugin.filter.*;
import java.awt.Color;
import java.util.*;
import java.io.*;


public class ScriptTagging implements PlugInFilter
{
	private List <String> tags;

    // Colors.
    private Map <String, Integer> allColors;

    ImageStatistics stats;


    public ScriptTagging()
    {
    	tags = new ArrayList <String> ();

        allColors = new HashMap <String, Integer> ();
    }

    public void getBrightness(long [] histogram)
    {
    	int darkness = 0;
    	int brightness = 0;

    	for (int i = 0; i < 70; i++)
    		darkness += histogram[i];

    	if (darkness / stats.area >= 0.6)
    		tags.add("dark");

    	else
    	{
			for (int i = 200; i < 255; i++)
    			brightness += histogram[i];

    		if (brightness / stats.area >= 0.6)
    			tags.add("light");
    	}
    }

    public void getMainColors()
    {
        List <String> mainColors = new ArrayList <String> ();

        for (Map.Entry <String, Integer> entry : allColors.entrySet())
        {
            if (Math.ceil(entry.getValue() * 100 / stats.area) >= 10)
                tags.add(entry.getKey());
        }
    }

    public void getColor(float [] hsv)
    {
        float hue = hsv[0];
        float saturation = hsv[1];
        float value = hsv[2];

        String color = "";

        if ((hue >= 0 && hue <= 360) && saturation <= 0.15 && value >= 0.65)
            color = "white";

        else if ((hue >= 0 && hue <= 360) && saturation >= 0 && value <= 0.1)
            return "black";

        else if ((hue >= 0 && hue <= 360) && saturation <= 0.15 && (value >= 0.1 && value <= 0.65))
            color = "gray";

        else if ((hue <= 11 || hue >= 351) && saturation >= 0.7 && value >= 0.1)
            color = "red";

        else if ((hue >= 180 && hue <= 255) && saturation >= 0.15 && value >= 0.1)
            color = "blue";

        else if ((hue >= 64 && hue <= 150) && saturation >= 0.15 && value >= 0.1)
            color = "green";

        else if ((hue >= 45 && hue <= 64) && saturation >= 0.15 && value >= 0.1)
            color = "yellow";

        else if ((hue >= 11 && hue <= 45) && saturation >= 0.15 && value >= 0.75)
            color = "orange";

        else if ((hue >= 11 && hue <= 45) && saturation >= 0.15 && (value >= 0.1 && value <= 0.75))
            color = "brown";


        if (!color.isEmpty())
        {
			if (allColors.containsKey(color))
            	allColors.put(color, allColors.get(color) + 1);

        	else
            	allColors.put(color, 1);
        }
    }

    public void run(ImageProcessor ip)
    {        
    	stats = (IJ.getImage()).getStatistics();

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

                // On convertit la teinte en degrés.
                hsv[0] *= 360;
                
                getColor(hsv);

                ip.set(x, y, hsv);
            }
        }

        getMainColors();
        getBrightness(stats.getHistogram());

        loadInFile();
    }

    public void loadInFile()
    {
        File file = new File("/Users/Greg/Desktop/file.txt"); 
        
        try
        {
            file.createNewFile();
            BufferedWriter out = new BufferedWriter(new FileWriter(file));  

            out.write(tags.get(0));

            for (int i  =1; i < tags.size(); i++)
                out.write(", " + tags.get(i));

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