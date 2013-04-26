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
	private Annotatable mSubject;
	private Object mValue;
	private String mPredicate;
	private List<Rule> mTSSList;
	private Hashtable mTSSHash;
    private Vector<PropertyValue> treeProperties;
    private Vector<PropertyValue> canvasProperties;
    private Vector<PropertyValue> scaleProperties;
    private boolean generalSelectorsHaveInitialized = false;

    public TSSHandler(Annotatable annotatable, Annotation annotation) {
		super(annotatable, annotation);
        File mTSSFile = new File(mesquite.lib.MesquiteModule.prefsDirectory + mesquite.lib.MesquiteFile.fileSeparator + "default.tss");
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
		mTSSHash = new Hashtable();
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
	 * @see mesquite.nexml.InterpretNEXML.PredicateHandler#getSubject()
	 */
	@Override
	public
	Annotatable getSubject() {
		return mSubject;
	}

	/* (non-Javadoc)
	 * @see mesquite.nexml.InterpretNEXML.PredicateHandler#setSubject(java.lang.Object)
	 */
	@Override
	public
	void setSubject(Annotatable subject) {
		mSubject = subject;
	}

	/* (non-Javadoc)
	 * @see mesquite.nexml.InterpretNEXML.PredicateHandler#getValue()
	 */
	@Override
	public
	Object getValue() {
		return mValue;
	}

	/* (non-Javadoc)
	 * @see mesquite.nexml.InterpretNEXML.PredicateHandler#setValue(java.lang.Object)
	 */
	@Override
	public
	void setValue(Object value) {
		mValue = value;
	}

	/* (non-Javadoc)
	 * @see mesquite.nexml.InterpretNEXML.PredicateHandler#getPrefix()
	 */
	@Override
	public
	String getPrefix() {
		return Constants.TSSPrefix;
	}

	/* (non-Javadoc)
	 * @see mesquite.nexml.InterpretNEXML.PredicateHandler#setPrefix(java.lang.String)
	 */
	@Override
	public
	void setPrefix(String prefix) {
	}

	/* (non-Javadoc)
	 * @see mesquite.nexml.InterpretNEXML.PredicateHandler#getPredicate()
	 */
	@Override
	public
	String getPredicate() {
		return mPredicate;
	}

	/* (non-Javadoc)
	 * @see mesquite.nexml.InterpretNEXML.PredicateHandler#setPredicate(java.lang.String)
	 */
	@Override
	public
	void setPredicate(String predicate) {
		mPredicate = predicate;
	}

	/* (non-Javadoc)
	 * @see mesquite.nexml.InterpretNEXML.PredicateHandler#getPropertyIsRel()
	 */
	@Override
	public
	boolean getPropertyIsRel() {
		return false;
	}

	/* (non-Javadoc)
	 * @see mesquite.nexml.InterpretNEXML.PredicateHandler#setPropertyIsRel(boolean)
	 */
	@Override
	public
	void setPropertyIsRel(boolean propertyIsRel) {

	}

	/* (non-Javadoc)
	 * @see mesquite.nexml.InterpretNEXML.PredicateHandler#getURIString()
	 */
	@Override
	public
	String getURIString() {
		return Constants.TSSURIString;
	}

	/* (non-Javadoc)
	 * @see mesquite.nexml.InterpretNEXML.PredicateHandler#setURIString(java.lang.String)
	 */
	@Override
	public
	void setURIString(String uri) {
	}


    // parses the general selectors "canvas," "tree," and "scale"
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
                    //convert this value to a point number
                    int value = convertToPixels(pv.getValue(),Constants.DEFAULT_FONT_SIZE);
                    canvasProperties.add(new PropertyValue("font-size", String.valueOf(value)));
                } else if (pv.getProperty().equalsIgnoreCase("height")) {
                    //convert this value to a single pixel number
                    int value = convertToPixels(pv.getValue(),Constants.DEFAULT_CANVAS_HEIGHT);
                    canvasProperties.add(new PropertyValue("height", String.valueOf(value)));
                } else if (pv.getProperty().equalsIgnoreCase("width")) {
                    //convert this value to a single pixel number
                    int value = convertToPixels(pv.getValue(),Constants.DEFAULT_CANVAS_WIDTH);
                    canvasProperties.add(new PropertyValue("width", String.valueOf(value)));
                }
            }
        }

        pvs = getClass ("tree", "");
        if (pvs != null) {
            for (PropertyValue pv : pvs) {
                if (pv.getProperty().equalsIgnoreCase("layout")) {
                    treeProperties.add(new PropertyValue("layout", "rectangular"));
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
        /*
            visible: true|false
            font-family, font-size, etc.
            border-color: black;
            border-size: 1px;
            border-style: solid;
            scale-width: value
            scale-title: “text”   */
        if (pvs != null) {
            for (PropertyValue pv : pvs) {
                if (pv.getProperty().equalsIgnoreCase("layout")) {
                    scaleProperties.add(new PropertyValue("layout", "rectangular"));
                } else if (pv.getProperty().equalsIgnoreCase("border-width")) {
                    int value = convertToPixels(pv.getValue(),Constants.DEFAULT_BORDER_WIDTH);
                    scaleProperties.add(new PropertyValue("border-width", String.valueOf(value)));
                } else if (pv.getProperty().equalsIgnoreCase("border-color")) {
                    scaleProperties.add(new PropertyValue("border-color", convertToMesColorNumber(pv.getValue())));
                } else if (pv.getProperty().equalsIgnoreCase("border-style")) {
                    //this isn't implemented yet
                } else if (pv.getProperty().equalsIgnoreCase("tip-orientation")) {
                    scaleProperties.add(new PropertyValue("tip-orientation", pv.getValue().toUpperCase()));
                } else if (pv.getProperty().equalsIgnoreCase("scaled")) {
                    scaleProperties.add(new PropertyValue("scaled", pv.getValue()));
                }
            }
        }
    }
// This parses the actual xml meta tag for TSS
// index is the Mesquite node ID
	@Override
	public
	void read(Associable associable, Listable listable, int index) {
		String[] parts = getPredicate().split(":");
		String tssClass = parts[1];
//		Annotatable subj = getSubject();
		String value = getValue().toString();
        String newValue = mesquiteNodeAnnotation(tssClass, value);
        setValue(newValue);

        Object convertedValue = getValue();
        Object pred = getPredicate();
        if (convertedValue.equals(Constants.NO_RULE)) {
            MesquiteMessage.warnUser ("couldn't find TSS rule " + pred.toString());
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

	@Override
	public
	void write() {
		// TODO Auto-generated method stub

	}

	private List<PropertyValue> getClass (String tssClassName, String tssValue) {
		Object hashvalue = mTSSHash.get(tssClassName);
        if (hashvalue == null) {
            MesquiteMessage.discreetNotifyUser("TSS class "+tssClassName+" not found");
            return null;
        }
        List<PropertyValue> pvs = null;
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
                    NexmlMesquiteManager.debug("poorly formed font property, should have [font-style] font-size font-family: "+pv.getValue());
                    continue;
                }
                if (split_pvs[0].matches("^\\D.*")) { // if this value is not a number, then we have a font-style
                    fontStyle = split_pvs[0];
                    split_pvs = split_pvs[1].split(" ",2);
                }
                if (split_pvs.length != 2) {
                    // something bad happened here, don't add these font properties
                    NexmlMesquiteManager.debug("poorly formed font property, should have [font-style] font-size font-family: "+pv.getValue());
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
				if (Boolean.getBoolean(val)) {
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

    public void initializeGeneralSelectors(MesquiteProject project) {
        if (project != null) {
            if (!generalSelectorsHaveInitialized) {
                generalSelectorsHaveInitialized = true;
                CommandChecker cc = new CommandChecker();
                BasicFileCoordinator projectCoordinator = (BasicFileCoordinator) project.doCommand("getCoordinatorModule", "", cc);
                BasicTreeWindowCoord treeWindowCoord = (BasicTreeWindowCoord) projectCoordinator.doCommand("getEmployee", "#mesquite.trees.BasicTreeWindowCoord.BasicTreeWindowCoord", cc);
                BasicTreeWindowMaker treeWindowMaker = (BasicTreeWindowMaker) treeWindowCoord.doCommand("makeTreeWindow","#mesquite.trees.BasicTreeWindowMaker.BasicTreeWindowMaker",cc);

                // treeWindow is a Commandable because the BasicTreeWindow class isn't public
                Commandable treeWindow = (Commandable) treeWindowMaker.doCommand("getTreeWindow", Integer.toString(1), cc);
                BasicTreeDrawCoordinator treeDrawCoordinator = (BasicTreeDrawCoordinator) treeWindow.doCommand("getTreeDrawCoordinator", "#mesquite.trees.BasicTreeDrawCoordinator.BasicTreeDrawCoordinator", cc);
                BasicDrawTaxonNames taxonNames = (BasicDrawTaxonNames) treeDrawCoordinator.doCommand("getEmployee","#mesquite.trees.BasicDrawTaxonNames.BasicDrawTaxonNames");
                DrawTree treeDrawer = (DrawTree) treeDrawCoordinator.doCommand("setTreeDrawer", "#mesquite.trees.StyledSquareTree.StyledSquareTree", cc);
                NodeLocsStandard nodeLocs = (NodeLocsStandard) treeDrawer.doCommand("setNodeLocs","#mesquite.trees.NodeLocsStandard.NodeLocsStandard",cc);


                treeWindowMaker.doCommand("suppressEPCResponse","",cc);
                treeWindowMaker.doCommand("setTreeSource","#mesquite.trees.StoredTrees.StoredTrees",cc);
                nodeLocs.doCommand("toggleCenter","on",cc);

                // tell the treeDrawCoordinator to set canvas settings:
                if (canvasProperties != null) {
                    Dimension dim = (Dimension) treeWindow.doCommand("getTreePaneSize","",cc);
                    int width = dim.width;
                    int height = dim.height;
                    String backgroundColor = "White";
                    String fontSize = "10";
                    String fontFamily = "Helvetica";
                    for (PropertyValue pv : canvasProperties) {
                        MesquiteMessage.notifyProgrammer("setting canvasProperty "+pv.toString());
                        if (pv.getProperty().equalsIgnoreCase("background-color")) {
                            //set the color to the standard color
                            backgroundColor = pv.getValue();
                        } else if (pv.getProperty().equalsIgnoreCase("width")) {
                            width = Integer.parseInt(pv.getValue());
                        } else if (pv.getProperty().equalsIgnoreCase("height")) {
                            height = Integer.parseInt(pv.getValue());
                        } else if (pv.getProperty().equalsIgnoreCase("font-family")) {
                            fontFamily = pv.getValue();
                        } else if (pv.getProperty().equalsIgnoreCase("font-size")) {
                            fontSize = pv.getValue();
                        }
                    }
                    treeWindow.doCommand("setSize", width+" "+height, cc);
                    treeDrawCoordinator.doCommand("setBackground", backgroundColor, cc);
                    taxonNames.doCommand("setFontSize",fontSize,cc);
                    taxonNames.doCommand("setFont",fontFamily,cc);
                }


                if (treeProperties != null) {
                    String tipOrientation = "RIGHT";
                    String borderWidth = "3";
                    String borderColor = "0";
                    String layout = "rectangular";
                    String fontSize = "10";
                    String fontFamily = "Helvetica";
                    boolean scaled = false;
                    for (PropertyValue pv : treeProperties) {
                        MesquiteMessage.notifyProgrammer("setting treeProperty "+pv.toString());
                        if (pv.getProperty().equalsIgnoreCase("border-width")) {
                            borderWidth = pv.getValue();
                        } else if (pv.getProperty().equalsIgnoreCase("border-color")) {
                            borderColor = pv.getValue();
                        } else if (pv.getProperty().equalsIgnoreCase("tip-orientation")) {
                            tipOrientation = pv.getValue();
                        } else if (pv.getProperty().equalsIgnoreCase("font-family")) {
                            fontFamily = pv.getValue();
                        } else if (pv.getProperty().equalsIgnoreCase("font-size")) {
                            fontSize = pv.getValue();
                        }
                    }
                    treeDrawer.doCommand("setStemWidth", borderWidth, cc);
                    treeDrawer.doCommand("setEdgeWidth", borderWidth, cc);
                    treeDrawer.doCommand("setBranchColor", borderColor, cc);

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

                if (scaleProperties != null) {
        /*          visible: true|false
                    font-family, font-size, etc.
                    border-color: black;
                    border-size: 1px;
                    border-style: solid;
                    scale-width: value
                    scale-title: ‚Äútext‚Äù   */
                    String scaleVisible;
                    String borderWidth;
                    String borderColor;
                    String fontSize = "10";
                    String fontFamily = "Helvetica";
                    for (PropertyValue pv : scaleProperties) {
                        MesquiteMessage.notifyProgrammer("setting scaleProperty "+pv.toString());
                        if (pv.getProperty().equalsIgnoreCase("border-width")) {
                            borderWidth = pv.getValue();
                        } else if (pv.getProperty().equalsIgnoreCase("border-color")) {
                            borderColor = pv.getValue();
                        } else if (pv.getProperty().equalsIgnoreCase("visible")) {
                            scaleVisible = pv.getValue();
                        } else if (pv.getProperty().equalsIgnoreCase("font-family")) {
                            fontFamily = pv.getValue();
                        } else if (pv.getProperty().equalsIgnoreCase("font-size")) {
                            fontSize = pv.getValue();
                        }
                    }
                }
                treeWindowMaker.doCommand("desuppressEPCResponse","",cc);
            }
        }
    }
}
