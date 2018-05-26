import ij.*;
import ij.process.*;
import ij.plugin.filter.*;
import java.awt.Color;
import javafx.util.Pair;
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

    public Pair <Integer, Integer> getEdges(ImageProcessor ip, int [] kernel, boolean vertical)
    {
        ImageProcessor ipClone = ip.duplicate();

        ipClone.convolve3x3(kernel);

        int width = ipClone.getWidth();
        int height = ipClone.getHeight();

        // Le nombre de contours.
        int edgesCounter = 0;

        // La taille total des contours.
        int totalEdgesSize = 0;

        // On ajoute un chaque pixel un booléen afin de savoir s'il on l'a déjà visité ou non.
        // Si c'est le cas, cela signifie que l'on a déjà prix en compte le contour.

        List <List <Boolean>> pixelVisited = new ArrayList <List <Boolean>> (height);

        for (int i = 0; i < height; i++)
        {
        	List <Boolean> subArray = new ArrayList <Boolean> (width);

        	for (int j = 0; j < width; j++)
	        {
	        	subArray.add(Boolean.FALSE);
	        }

        	pixelVisited.add(subArray);
        }

        for (int y = 0; y < height; y++)
        {
            for (int x = 0; x < width; x++) 
            {
            	int edgeSize = 0;

            	int [] rgb = new int[3];

            	ipClone.getPixel(x, y, rgb);

            	if (ipClone.isGrayscale())
				{
                	rgb[1] = rgb[0];
                	rgb[2] = rgb[0];
				}

                // Le contour est constitué de pixels blancs.
                // On évite de compter le pixel plusieurs fois avec le booléen associé.

            	while (rgb[0] == 255 && rgb[1] == 255 && rgb[2] == 255 && x < width && y < height && pixelVisited.get(y).get(x) == false)
            	{
            		pixelVisited.get(y).set(x, true);

            		edgeSize++;

        			if (vertical)
        				y++;

        			else
        				x++;

            		ipClone.getPixel(x, y, rgb);

            		if (ipClone.isGrayscale())
					{
	                	rgb[1] = rgb[0];
	                	rgb[2] = rgb[0];
					}
            	}

            	if (edgeSize > 0)
            	{
            		edgesCounter++;
                    totalEdgesSize += edgeSize;
            		y -= edgeSize;
            	}
            }
        }

        return new Pair <Integer, Integer> (edgesCounter, totalEdgesSize);
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
            color = "black";

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

    public void findSky(ImageProcessor ip)
    {
        int width = ip.getWidth();
        int height = ip.getHeight();
    }

    public void run(ImageProcessor ip)
    {        
    	stats = (IJ.getImage()).getStatistics();

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

				if (ip.isGrayscale())
				{
                	rgb[1] = rgb[0];
                	rgb[2] = rgb[0];
				}

                Color.RGBtoHSB(rgb[0], rgb[1], rgb[2], hsv);

                // On convertit la teinte en degrés.
                hsv[0] *= 360;
                
                getColor(hsv);
            }
        }

        getMainColors();
        getBrightness(stats.getHistogram());
        findSky(ip);




        // Pour des tests.

        /* int [] verticalKernel = {-1, 2, -1, -1, 2, -1, -1, 2, -1};
        int [] horizontalKernel = {-1, -1, -1, 2, 2, 2, -1, -1, -1};

        Pair <Integer, Integer> verticalEdges = getEdges(ip, verticalKernel, true);
        Pair <Integer, Integer> horizontalEdges = getEdges(ip, horizontalKernel, false);

        double proportionVerticale = verticalEdges.getKey() * 1f / (verticalEdges.getKey() + horizontalEdges.getKey());
        double proportionHorizontale = horizontalEdges.getKey() * 1f / (verticalEdges.getKey() + horizontalEdges.getKey());

        double moyenneVerticale = verticalEdges.getValue() * 1f / verticalEdges.getKey();
        double moyennehorizontale = horizontalEdges.getValue() * 1f / horizontalEdges.getKey();

        IJ.write("Proportion verticale : " + Double.toString(proportionVerticale));
        IJ.write("Proportion horizontale : " + Double.toString(proportionHorizontale));
        IJ.write("Moyenne taille verticale : " + Double.toString(moyenneVerticale));
        IJ.write("Moyenne taille horizontale : " + Double.toString(moyennehorizontale));
        IJ.write("Proportion moyenne verticale/horizontale : " + Double.toString(moyenneVerticale / moyennehorizontale));
        IJ.write("Proportion verticale/horizontale : " + Double.toString(proportionVerticale / proportionHorizontale));*/




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