package mesquite.nexml.InterpretNEXML.AnnotationHandlers;

import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;


import mesquite.lib.*;
import mesquite.lib.duties.DrawTree;
import mesquite.minimal.BasicFileCoordinator.BasicFileCoordinator;
import mesquite.nexml.InterpretNEXML.Constants;
import mesquite.nexml.InterpretNEXML.NexmlMesquiteManager;

import mesquite.trees.BasicDrawTaxonNames.BasicDrawTaxonNames;
import mesquite.trees.BasicTreeDrawCoordinator.BasicTreeDrawCoordinator;
import mesquite.trees.BasicTreeWindowCoord.BasicTreeWindowCoord;
import mesquite.trees.BasicTreeWindowMaker.BasicTreeWindowMaker;
import mesquite.trees.NodeLocsStandard.NodeLocsStandard;
import org.nexml.model.Annotatable;
import org.nexml.model.Annotation;

import com.osbcp.cssparser.*;


/**
 * @author rvosa
 *
 */
public class TSSHandler extends NamespaceHandler {
	private List<Rule> mTSSList;
	private Hashtable mTSSHash;
    private File mTSSFile;
    private Vector<PropertyValue> treeProperties;
    private Vector<PropertyValue> canvasProperties;
    private Vector<PropertyValue> scaleProperties;
    private boolean generalSelectorsHaveInitialized = false;
    public static final String TSSPrefix = "tss";
    public static final String TSSURIString = "http://mesquiteproject.org/tss#";


    public TSSHandler() {
        super();
        mTSSHash = new Hashtable();
        try {
            mTSSFile = new File(mesquite.lib.MesquiteModule.mesquiteDirectory + mesquite.lib.MesquiteFile.fileSeparator + "default.tss");
        } catch (Exception e) {
            if (e instanceof FileNotFoundException) {
                MesquiteMessage.discreetNotifyUser("This XML file uses TSS notation, but no TSS file was found.");
            }
        }
        Scanner scanner = null;
        String cssString = "";
        try {
            scanner = new Scanner(mTSSFile);
        } catch (Exception e) {
            NexmlMesquiteManager.debug(e.toString());
        }
        while (scanner.hasNextLine()){
            cssString = cssString + scanner.nextLine();
        }
        try {
            mTSSList = CSSParser.parse(cssString);
        } catch (Exception e) {
            NexmlMesquiteManager.debug(e.toString());
        }
        // store the rules in the hash:
        for ( Rule r : mTSSList) {
            // we want to hash each rule as a value for each selector key separately.
            List<Selector> selectors = r.getSelectors();
            List<PropertyValue> pvs = r.getPropertyValues();
            for (Selector selector : selectors) {
                String selectorName = selector.toString();
                String selectorSubClass = "";

                String[] selectorParts = selector.toString().split("\\.");
                if (selectorParts.length > 1) {  // this selector has subclasses
                    selectorName = selectorParts[0];
                    selectorSubClass = selectorParts[1];
                }

                selectorParts = selector.toString().split("\\[");
                if (selectorParts.length > 1) {  // this selector has ranges
                    selectorName = selectorParts[0];
                    String min = "";
                    String max = "";
                    for (int i=1;i<selectorParts.length;i++) {
                        String str = selectorParts[i].replace("]","");
                        if (str.contains("min")) {
                            min = str.replaceAll("min\\s*=","");
                        } else if (str.contains("max")) {
                            max = str.replaceAll("max\\s*=","");
                        }
                    }
                    selectorSubClass = min+"."+max;
                }
                if (selectorSubClass.isEmpty()) {
                    mTSSHash.put(selectorName, pvs);
                } else {
                    Hashtable subClassHash = (Hashtable) mTSSHash.get(selectorName);
                    if (subClassHash == null) {
                        subClassHash = new Hashtable();
                    }
                    subClassHash.put(selectorSubClass,pvs);
                    mTSSHash.put(selectorName,subClassHash);
                }
            }
        }
        parseGeneralSelectors();
    }

	/* (non-Javadoc)
	 * @see mesquite.nexml.InterpretNEXML.PredicateHandler#getPrefix()
	 */
	@Override
	public
	String getPrefix() {
		return TSSPrefix;
	}

	/* (non-Javadoc)
	 * @see mesquite.nexml.InterpretNEXML.PredicateHandler#getURIString()
	 */
	@Override
	public
	String getURIString() {
		return TSSURIString;
	}
// This parses the actual xml meta tag for TSS
// index is the Mesquite node ID
	@Override
	public
	void read(Associable associable, Listable listable, int index) {
		String[] parts = getPredicate().split(":");
		String tssClass = parts[1];
		String value = getValue().toString();
        String newValue = mesquiteNodeAnnotation(tssClass, value);
        setValue(newValue);

        Object convertedValue = getValue();
        Object pred = getPredicate();
        if (convertedValue.equals(Constants.NO_RULE)) {
            MesquiteMessage.discreetNotifyUser ("couldn't find TSS rule " + pred.toString());
            // no rule specified
        } else {
            String[] mesProps = convertedValue.toString().split(";");
            for (String prop : mesProps) {
                if (prop.contains("tss:")) {
                    String[] propParts = prop.split("=");
                    String convertedProp = propParts[0];
                    String convertedVal = Constants.NO_VALUE;
                    if (propParts.length > 1) {
                        convertedVal = propParts[1];
                    }
                    NameReference mesNr = associable.makeAssociatedObjects(convertedProp);
                    associable.setAssociatedObject(mesNr,index, convertedVal);
                } else {
                    String[] propParts = prop.split(":");
                    String convertedProp = propParts[0];
                    String convertedVal = propParts[1];
                    NameReference mesNr = associable.makeAssociatedObjects(convertedProp);
                    try {
                        associable.setAssociatedLong(mesNr,index,new Long(convertedVal));
                    } catch (Exception e) {
                        associable.setAssociatedBit(mesNr,index,Boolean.TRUE);
                    }
                }
            }
        }

    }

    public void initializeMesquiteProject(MesquiteProject project) {
        if (project == null) {
            return;
        }
        CommandChecker cc = new CommandChecker();
        BasicFileCoordinator projectCoordinator = (BasicFileCoordinator) project.doCommand("getCoordinatorModule", "", cc);
        BasicTreeWindowCoord treeWindowCoord = (BasicTreeWindowCoord) projectCoordinator.doCommand("getEmployee", "#mesquite.trees.BasicTreeWindowCoord.BasicTreeWindowCoord", cc);
        BasicTreeWindowMaker treeWindowMaker = (BasicTreeWindowMaker) treeWindowCoord.doCommand("editingTreeWindow","#mesquite.trees.BasicTreeWindowMaker.BasicTreeWindowMaker",cc);

        // treeWindow is a Commandable because the BasicTreeWindow class isn't public
        Commandable treeWindow = (Commandable) treeWindowMaker.doCommand("getTreeWindow", Integer.toString(1), cc);
        BasicTreeDrawCoordinator treeDrawCoordinator = (BasicTreeDrawCoordinator) treeWindow.doCommand("getTreeDrawCoordinator", "#mesquite.trees.BasicTreeDrawCoordinator.BasicTreeDrawCoordinator", cc);
        BasicDrawTaxonNames taxonNames = (BasicDrawTaxonNames) treeDrawCoordinator.doCommand("getEmployee","#mesquite.trees.BasicDrawTaxonNames.BasicDrawTaxonNames");
        DrawTree treeDrawer = (DrawTree) treeDrawCoordinator.doCommand("setTreeDrawer", "#mesquite.trees.SquareTree.SquareTree", cc);
        NodeLocsStandard nodeLocs = (NodeLocsStandard) treeDrawer.doCommand("setNodeLocs","#mesquite.trees.NodeLocsStandard.NodeLocsStandard",cc);


        treeWindowMaker.doCommand("suppressEPCResponse","",cc);
        treeWindowMaker.doCommand("setTreeSource","#mesquite.trees.StoredTrees.StoredTrees",cc);
        nodeLocs.doCommand("toggleCenter","on",cc);

        // tell the treeDrawCoordinator to set canvas settings:
        if (canvasProperties != null) {
            Dimension dim = (Dimension) treeWindow.doCommand("getTreePaneSize","",cc);
            int width = dim.width;
            int height = dim.height;
            for (PropertyValue pv : canvasProperties) {
                MesquiteMessage.notifyProgrammer("setting canvasProperty "+pv.toString());
                if (pv.getProperty().equalsIgnoreCase("background-color")) {
                    //set the color to the standard color
					treeDrawCoordinator.doCommand("setBackground", pv.getValue(), cc);
                } else if (pv.getProperty().equalsIgnoreCase("font-family")) {
					taxonNames.doCommand("setFont",pv.getValue(),cc);
                } else if (pv.getProperty().equalsIgnoreCase("font-size")) {
					taxonNames.doCommand("setFontSize",pv.getValue(),cc);
                } else if (pv.getProperty().equalsIgnoreCase("width")) {
                    width = Integer.parseInt(pv.getValue());
                } else if (pv.getProperty().equalsIgnoreCase("height")) {
                    height = Integer.parseInt(pv.getValue());
                }
            }
            treeWindow.doCommand("setSize", width+" "+height, cc);
        }

        if (treeProperties != null) {
            for (PropertyValue pv : treeProperties) {
                if (pv.getProperty().equalsIgnoreCase("layout")) {
                    //this isn't implemented yet
                } else if (pv.getProperty().equalsIgnoreCase("border-width")) {
                    String borderWidth = pv.getValue();
					treeDrawer.doCommand("setStemWidth", borderWidth, cc);
					treeDrawer.doCommand("setEdgeWidth", borderWidth, cc);
                } else if (pv.getProperty().equalsIgnoreCase("border-color")) {
					treeDrawer.doCommand("setBranchColor", pv.getValue(), cc);
                } else if (pv.getProperty().equalsIgnoreCase("border-style")) {
                	// this isn't implemented yet
                } else if (pv.getProperty().equalsIgnoreCase("scaled")) {
                	if (pv.getValue().equalsIgnoreCase("true")) {
						nodeLocs.doCommand("branchLengthsToggle", "on", cc);
					} else {
						nodeLocs.doCommand("branchLengthsToggle", "off", cc);
					}
                } else if (pv.getProperty().equalsIgnoreCase("tip-orientation")) {
                    String tipOrientation = pv.getValue();
					if (tipOrientation.equalsIgnoreCase("up")) {
						treeDrawer.doCommand("orientUP","",cc);
					} else if (tipOrientation.equalsIgnoreCase("left")) {
						treeDrawer.doCommand("orientLEFT","",cc);
					} else if (tipOrientation.equalsIgnoreCase("right")) {
						treeDrawer.doCommand("orientRIGHT","",cc);
					} else if (tipOrientation.equalsIgnoreCase("down")) {
						treeDrawer.doCommand("orientDOWN","",cc);
					}
                }
            }
        }

        if (scaleProperties != null) {
            for (PropertyValue pv : scaleProperties) {
                if (pv.getProperty().equalsIgnoreCase("visible")) {
                	if (pv.getValue().equalsIgnoreCase("true")) {
						nodeLocs.doCommand("toggleScale", "on", cc);
					} else {
						nodeLocs.doCommand("toggleScale", "off", cc);
					}
                } else if (pv.getProperty().equalsIgnoreCase("border-color")) {
                	// this isn't implemented yet
                } else if (pv.getProperty().equalsIgnoreCase("border-width")) {
                	// this isn't implemented yet
                } else if (pv.getProperty().equalsIgnoreCase("border-style")) {
                	// this isn't implemented yet
                } else if (pv.getProperty().equalsIgnoreCase("font-family")) {
                    // this isn't implemented yet
                } else if (pv.getProperty().equalsIgnoreCase("font-size")) {
                    // this isn't implemented yet
                } else if (pv.getProperty().equalsIgnoreCase("scale-width")) {
                    //this isn't implemented yet
                } else if (pv.getProperty().equalsIgnoreCase("scale-title")) {
                    //this isn't implemented yet
                }
            }
        }
        treeWindowMaker.doCommand("desuppressEPCResponse","",cc);
    }

    private List<PropertyValue> getClass (String tssClassName, String tssValue) {
		Object hashvalue = mTSSHash.get(tssClassName);
        if (hashvalue == null) {
            MesquiteMessage.discreetNotifyUser("TSS class "+tssClassName+" not found");
            return null;
        }
        List<PropertyValue> pvs;
        if (hashvalue.getClass()==Hashtable.class) {
            // check the tssValue to see if it's a string
            pvs = (List)((Hashtable)hashvalue).get(tssValue);

            // check to see if the value is in a range
            if (pvs == null) {
                for (Enumeration e = ((Hashtable) hashvalue).keys(); e.hasMoreElements();) {
                    String key = (String) e.nextElement();
                    String[] keyParts = key.split("\\.");
                    int min = Integer.parseInt(keyParts[0]);
                    int max = Integer.parseInt(keyParts[1]);
                    int val = Integer.parseInt(tssValue);
                    if ((val>=min) && (val<=max)) {
                        pvs = (List)((Hashtable)hashvalue).get(key);
                    }
                }
            }
        } else {
            pvs = (List) hashvalue;
        }
        if (pvs == null) {
            return null;
        }
        Vector<PropertyValue> new_pvs = new Vector<PropertyValue>();
        // process the compound properties into single properties.
        for (PropertyValue pv : pvs) {
            if (pv.getProperty().equalsIgnoreCase("font")) {
                //three components:
                //font-style font-size font-family
                //font-style is optional, but other two are required
                String fontStyle = "normal";
                String fontSize = String.valueOf(Constants.DEFAULT_FONT_SIZE);
                String fontFamily;
                String[] split_pvs = pv.getValue().split(" ",2);
                if (split_pvs.length != 2) {
                    // something bad happened here, don't add these font properties
                    MesquiteMessage.discreetNotifyUser("Poorly formed font property ("+pv.getValue()+"), should have [font-style] font-size font-family.");
                    continue;
                }
                if (split_pvs[0].matches("^\\D.*")) { // if this value is not a number, then we have a font-style
                    fontStyle = split_pvs[0];
                    split_pvs = split_pvs[1].split(" ",2);
                }
                if (split_pvs.length != 2) {
                    // something bad happened here, don't add these font properties
                    MesquiteMessage.discreetNotifyUser("Poorly formed font property ("+pv.getValue()+"), should have [font-style] font-size font-family.");
                    continue;
                }
                fontSize = split_pvs[0];
                fontFamily = chooseAFont(split_pvs[1]);
                new_pvs.add(new PropertyValue("font-style",fontStyle));
                new_pvs.add(new PropertyValue("font-size",fontSize));
                new_pvs.add(new PropertyValue("font-family",fontFamily));
            } else if (pv.getProperty().equalsIgnoreCase("background")) {
                // this isn't compound now, but it could be later, as per the CSS spec
                new_pvs.add(new PropertyValue("background-color",pv.getValue()));
            } else if (pv.getProperty().equalsIgnoreCase("border")) {
                // three components:
                // border-width border-color border-style
                // all are optional (but border-style is not implemented in Mesquite)
                String[] split_pvs = pv.getValue().split(" ");
                for (String split_pv : split_pvs) {
                    int value = convertToPixels(split_pv,Constants.DEFAULT_BORDER_WIDTH);
                    if (value > 0) {
                        new_pvs.add(new PropertyValue("border-width",String.valueOf(value)));
                        continue;
                    }
                    String col = convertToMesColorName(split_pv);
                    if (col != null) {
                        new_pvs.add(new PropertyValue("border-color",col));
                        continue;
                    }
                }
            } else if (pv.getProperty().equalsIgnoreCase("")) {
            } else {
                new_pvs.add(pv);
            }
        }
        return new_pvs;
	}

    private String mesquiteNodeAnnotation (String tssClass, String tssValue ) {
		String formatted_pvs = "tss:"+tssClass+ "=" +tssValue;
        List<PropertyValue> pvs = getClass(tssClass, tssValue);
        if (pvs == null) {
            return Constants.NO_RULE;
        }
        for (PropertyValue pv : pvs) {
			String val = pv.getValue();
			val = val.replaceAll("value|VALUE", tssValue);
            if (pv.getProperty().equals("border-color")) {
                formatted_pvs = formatted_pvs + ";" + "color:" + convertToMesColorNumber(val);
			} else if (pv.getProperty().equals("border-width")) {
                formatted_pvs = formatted_pvs + ";" + "width:" + val;
            } else if (pv.getProperty().equals("color")) {
				formatted_pvs = formatted_pvs + ";" + ("taxoncolor:" + convertToMesColorNumber(val));
			} else if (pv.getProperty().equals("collapsed")) {
				if (val.equalsIgnoreCase("true")) {
					formatted_pvs = formatted_pvs + ";" + "triangled:on";
				}
			}
		}
		NexmlMesquiteManager.debug("converted to Mesquite annotation " + formatted_pvs);
		return formatted_pvs;
	}

    private int convertToPixels (String stringValue, int defaultValue) {
        //convert this value to a point number
        int value = 0;
        String unit = stringValue.replaceAll("\\d","");
        try {
            value = Integer.parseInt(stringValue.replaceAll("\\D",""));
        } catch (Exception e) {
            return 0;
        }
        if (unit.contains("%")) {
            value = (int) ((double) defaultValue * (value/100));
        } else if (unit.contains("in")) {
            // assume 72 pt per inch
            value = value * 72;
        } else if (unit.contains("cm")) {
            // assume 28.3 pt per cm
            value = (int) (value * 28.3);
        }
        return value;
    }

    private String chooseAFont (String fontString) {
        String fontFamily = Constants.DEFAULT_FONT_FAMILY;
        String[] vals = fontString.split(",");
        for (String val : vals) {
            String value = val.trim().replaceAll("\"","");
            Font f = new Font(value,Font.PLAIN,12); // these are arbitrary values; we just want to see if value is a valid font name
            if (f.getFamily().equalsIgnoreCase("Dialog")) {
                continue;
            }
            fontFamily = value;
            break;
        }
        return fontFamily;
    }
    private String convertToMesColorNumber ( String val ) {
        String mesColor = null;
        if (val.equalsIgnoreCase("VALUE")) {
            return "VALUE";
        }
        for (int i=0;i< ColorDistribution.standardColorNames.getSize();i++) {
            String thisColor = ColorDistribution.standardColorNames.getValue(i);
            if(val.equalsIgnoreCase(thisColor)) {
                mesColor = String.valueOf(i);
            }
        }
        return mesColor;
    }

    private String convertToMesColorName ( String val ) {
        String mesColor = null;
        if (val.equalsIgnoreCase("VALUE")) {
            return "VALUE";
        }
        for (int i=0;i<ColorDistribution.standardColorNames.getSize();i++) {
            String thisColor = ColorDistribution.standardColorNames.getValue(i);
            if(val.equalsIgnoreCase(thisColor)) {
                mesColor = ColorDistribution.standardColorNames.getValue(i);
            }
        }
        return mesColor;
    }

    //    parses the general selectors "canvas," "tree," and "scale"
    private void parseGeneralSelectors () {
        canvasProperties = new Vector<PropertyValue>();
        treeProperties = new Vector<PropertyValue>();
        scaleProperties = new Vector<PropertyValue>();
        List<PropertyValue> pvs = getClass ("canvas", "");
        if (pvs != null) {
            for (PropertyValue pv : pvs) {
                if (pv.getProperty().equalsIgnoreCase("background-color")) {
                    //Mesquite can only handle setting the color of the canvas. Ignore everything else.
                    canvasProperties.add(new PropertyValue("background-color", convertToMesColorName(pv.getValue())));
                } else if (pv.getProperty().equalsIgnoreCase("font-family")) {
                    String fontFamily = chooseAFont(pv.getValue());
                    canvasProperties.add(new PropertyValue("font-family", fontFamily));
                } else if (pv.getProperty().equalsIgnoreCase("font-size")) {
                    int value = convertToPixels(pv.getValue(),Constants.DEFAULT_FONT_SIZE);
                    canvasProperties.add(new PropertyValue("font-size", String.valueOf(value)));
                } else if (pv.getProperty().equalsIgnoreCase("height")) {
                    int value = convertToPixels(pv.getValue(),Constants.DEFAULT_CANVAS_HEIGHT);
                    canvasProperties.add(new PropertyValue("height", String.valueOf(value)));
                } else if (pv.getProperty().equalsIgnoreCase("width")) {
                    int value = convertToPixels(pv.getValue(),Constants.DEFAULT_CANVAS_WIDTH);
                    canvasProperties.add(new PropertyValue("width", String.valueOf(value)));
                }
            }
        }

        pvs = getClass ("tree", "");
        if (pvs != null) {
            for (PropertyValue pv : pvs) {
                if (pv.getProperty().equalsIgnoreCase("layout")) {
                    treeProperties.add(new PropertyValue("layout", pv.getValue().toUpperCase()));
                } else if (pv.getProperty().equalsIgnoreCase("border-width")) {
                    int value = convertToPixels(pv.getValue(),Constants.DEFAULT_BORDER_WIDTH);
                    treeProperties.add(new PropertyValue("border-width", String.valueOf(value)));
                } else if (pv.getProperty().equalsIgnoreCase("border-color")) {
                    treeProperties.add(new PropertyValue("border-color", convertToMesColorNumber(pv.getValue())));
                } else if (pv.getProperty().equalsIgnoreCase("border-style")) {
                    //this isn't implemented yet
                } else if (pv.getProperty().equalsIgnoreCase("tip-orientation")) {
                    treeProperties.add(new PropertyValue("tip-orientation", pv.getValue().toUpperCase()));
                } else if (pv.getProperty().equalsIgnoreCase("scaled")) {
                    treeProperties.add(new PropertyValue("scaled", pv.getValue()));
                }
            }
        }

        pvs = getClass ("scale", "");
        if (pvs != null) {
            for (PropertyValue pv : pvs) {
                if (pv.getProperty().equalsIgnoreCase("visible")) {
                    scaleProperties.add(new PropertyValue("visible", pv.getValue()));
                } else if (pv.getProperty().equalsIgnoreCase("font-family")) {
                    String fontFamily = chooseAFont(pv.getValue());
                    scaleProperties.add(new PropertyValue("font-family", fontFamily));
                } else if (pv.getProperty().equalsIgnoreCase("font-size")) {
                    int value = convertToPixels(pv.getValue(),Constants.DEFAULT_FONT_SIZE);
                    scaleProperties.add(new PropertyValue("font-size", String.valueOf(value)));
                } else if (pv.getProperty().equalsIgnoreCase("border-width")) {
                    int value = convertToPixels(pv.getValue(),Constants.DEFAULT_BORDER_WIDTH);
                    scaleProperties.add(new PropertyValue("border-width", String.valueOf(value)));
                } else if (pv.getProperty().equalsIgnoreCase("border-color")) {
                    scaleProperties.add(new PropertyValue("border-color", convertToMesColorNumber(pv.getValue())));
                } else if (pv.getProperty().equalsIgnoreCase("border-style")) {
                    //this isn't implemented yet
                } else if (pv.getProperty().equalsIgnoreCase("scale-width")) {
                    //this isn't implemented yet
                } else if (pv.getProperty().equalsIgnoreCase("scale-title")) {
                    //this isn't implemented yet
                }
            }
        }
    }
}
