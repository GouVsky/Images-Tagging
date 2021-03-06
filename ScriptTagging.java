import ij.*;
import ij.process.*;
import ij.plugin.filter.*;
import java.awt.Color;
import javafx.util.Pair;
import java.util.*;
import java.io.*;


public class ScriptTagging implements PlugInFilter
{
    private String name;

	private List <String> tags;

    // Colors.
    private Map <String, Integer> allColors;

    ImageStatistics stats;


    public ScriptTagging()
    {
    	tags = new ArrayList <String> ();

        allColors = new HashMap <String, Integer> ();
    }

    public void findCity(ImageProcessor ip)
    {
        ImageProcessor ipClone = ip.duplicate();

        // Le masque de convolution permettant d'isoler les contours verticaux.
        int [] kernel = {-1, 2, -1, -1, 2, -1, -1, 2, -1};

        ipClone.convolve3x3(kernel);

        int width = ipClone.getWidth();
        int height = ipClone.getHeight();

        // Le nombre de contours.
        int edgesCounter = 0;

        // La taille total des contours.
        int totalEdgesSize = 0;

        // Lorsque l'on détecte un contour, on en récupère l'intégralité
        // (les pixels du dessous, vu que l'on commence en 0x0, on ne peut pas en oublier au dessus).
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

        			y++;

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

        double verticalMean = totalEdgesSize * 1f / edgesCounter;

        if (verticalMean > 4)
            tags.add("city");
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
			for (int i = 190; i < 255; i++)
    			brightness += histogram[i];

    		if (brightness / stats.area >= 0.5)
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

    /*
    Fonction basique permettant de repérer un ciel clair et dégagé grâce à une variable score
    pas aussi efficace qu'un deep/machine learning bien évidemment!
    Consiste en la separation de l'image en plusieurs zones (on ne prend pas de zone particuliere style premier tiers haut
    car l'image peut être prise en biais), puis calculer les edge dans chaque zone (grace au filtre) et les deux
    couleurs principales. Plus une zone a peu de bords (en ayant bleu et blanc comme couleur), plus cela va augmenter le score
    si le score atteind son max, on ajoute le tag "clear sky" aux tags
     */

    public boolean findEnvironnement(ImageProcessor ip)
    {
        // on définit ici une variable qui va s'incrémenter lors du parcours de l'image suivant les résultats obtenus
        int height =ip.getHeight();
        int width =ip.getWidth();
        int heightpixelGrid= height/10;
        int witdhPixelGrid= width/10;
        int scoreSky= 0; // le score de l'image, si elle depasse 100 on considère qu'un ciel est trouvé
        int scoreSea =0;
        int scoreSnow = 0;
        int findEdgeBlackPixel=0; // permet de calculer le nombre de pixels noir dans une section definie
        int findEdgeColoredPixel=0; // permet de calculer le nombre de pixels coloré dans une section definie
        double percentOfBlue=0; // variable pour calculer le poucentage de pixel bleu dans une image
        double percentOfWhiteAndGrey=0; // variable pour calculer le poucentage de pixel blanc et gris dans une image
        double percentOfGrey = 0; // variable pour calculer le poucentage de pixel gris dans une image
        double percentOfWhite = 0; // variable pour calculer le poucentage de pixel blanc dans une image
        float [] ImageGrid;

        ImageProcessor ipClone = ip.duplicate();
        ipClone.findEdges();
        //decoupage de l'iage en grille de 10 par 10 cases (100 au total) avec les deux premieres boucles
        for(int gridWidth=0; gridWidth<10; gridWidth++)
        {
            for(int gridHeight=0; gridHeight<10; gridHeight++)
            {
                // deplacement a l'interieur d'une cellule de la grille pixel par pixel, vidage du tableau;
                ImageGrid = new float[witdhPixelGrid*heightpixelGrid];
                percentOfBlue=0;
                percentOfWhiteAndGrey=0;
                percentOfGrey = 0;
                percentOfWhite = 0;
                findEdgeBlackPixel=0;
                findEdgeColoredPixel=0;
                allColors.clear(); // on efface la map
                for(int widthCounter=gridWidth*witdhPixelGrid; widthCounter< witdhPixelGrid*(gridWidth+1); widthCounter++)
                {
                    for(int heightCounter=gridHeight*heightpixelGrid; heightCounter<heightpixelGrid*(gridHeight+1); heightCounter++)
                    {
                        // on recupere les pixels de ipClone
                        int []rgbClone = new int[3];
                        ipClone.getPixel(widthCounter, heightCounter, rgbClone);

                        //on recupere les pixels de l'image originale
                        float[] hsv = new float[3];
                        int []rgb= new int[3];
                        ip.getPixel(widthCounter, heightCounter, rgb);
                        if (ip.isGrayscale())
                        {
                            rgb[1] = rgb[0];
                            rgb[2] = rgb[0];
                        }
                        Color.RGBtoHSB(rgb[0], rgb[1], rgb[2], hsv);
                        // On convertit la teinte en degrés.
                        hsv[0] *= 360;
                        getColor(hsv);

                        if(rgbClone[0]<=30 && rgbClone[1]<=30 && rgbClone[2]<=30)
                        {
                            findEdgeBlackPixel++;
                        }
                        else
                        {
                            findEdgeColoredPixel++;
                        }
                    }
                }
                //on transforme le nombre de pixel en pourcentage
                double percentOfEdgePixel=(findEdgeColoredPixel*1f/(findEdgeBlackPixel+findEdgeColoredPixel))*100;

                if(allColors.containsKey("blue"))
                {
                    percentOfBlue= (allColors.get("blue")*1f/(witdhPixelGrid*heightpixelGrid))*100;
                }
                if(allColors.containsKey("gray"))
                {
                    percentOfGrey+= (allColors.get("gray")*1f/(witdhPixelGrid*heightpixelGrid))*100;
                }
                if(allColors.containsKey("white"))
                {
                    percentOfWhite+= (allColors.get("white")*1f/(witdhPixelGrid*heightpixelGrid))*100;
                }
                percentOfWhiteAndGrey= percentOfGrey+percentOfWhite;

                 /*
                 partie d'indentation du score, a peu pres ce qu'il se pase dans un reseau de neurone sauf
                 qu'ici on bouge les paramètre du seul neurone créé manuellement pour avoir le plus de
                 reponses juste avec la banque d'image donnée
                */

                 //On indente le score pour le ciel
                if(percentOfBlue==100 && percentOfEdgePixel==0)
                {
                    scoreSky+=20;
                }
                else if(percentOfBlue>90 && percentOfEdgePixel <5)
                {
                    scoreSky+=15;
                }
                else if(percentOfBlue>60 && percentOfWhiteAndGrey>15 && percentOfEdgePixel<30)
                {
                    scoreSky+=10;
                }

                if(percentOfWhite>90 && percentOfEdgePixel<50)
                {
                    scoreSnow+=20;
                }

                if(percentOfWhite>70 && percentOfGrey>10 && percentOfEdgePixel <60)
                {
                    scoreSnow+=15;
                }

                if(percentOfBlue==100 && percentOfEdgePixel>80)
                {
                    scoreSea+=20;
                }

                if(percentOfBlue>90 && percentOfEdgePixel>70)
                {
                    scoreSea+=15;
                }

                if(percentOfBlue>80 && percentOfEdgePixel>60)
                {
                    scoreSea+=10;
                }
            }
        }

        // Si on détecte le ciel, la mer ou la neige, on peut en déduire que l'on se trouve à l'extérieur.
        // En revanche, l'inverse ne veut pas dire que l'on est à en intérieur (des arbres peuvent cacher le ciel par exemple).

        boolean outside = false;

        if(scoreSky>=100)
        {
            outside = true;
            tags.add("clear sky");
        }

        if(scoreSnow>=100 && scoreSky>=30)
        {
            outside = true;
            tags.add("snow");
        }

        if(scoreSea>=100 && scoreSnow<=50)
        {
            outside = true;
            tags.add("sea");
        }

        if (outside)
            tags.add("outside");

        return outside;
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

        if (findEnvironnement(ip)) //Attention, clean la map
        {
            findCity(ip);
        }


        loadInFile();
    }

    public void loadInFile()
    {
        File file = new File(name + ".txt"); 

        try
        {
            file.createNewFile();
            BufferedWriter out = new BufferedWriter(new FileWriter(file));  

            out.write(tags.get(0));

            for (int i = 1; i < tags.size(); i++)
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
        name = imp.getShortTitle();

        if (arg.equals("about"))
        {
            IJ.showMessage("Traitement de l'image");

            return DONE;
        }

        return DOES_ALL;
    }
}