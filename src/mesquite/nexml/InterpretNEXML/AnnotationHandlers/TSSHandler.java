
package mesquite.nexml.InterpretNEXML.AnnotationHandlers;

import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;


import mesquite.lib.Associable;
import mesquite.lib.ColorDistribution;
import mesquite.lib.Listable;
import mesquite.nexml.InterpretNEXML.Constants;
import mesquite.nexml.InterpretNEXML.NexmlMesquiteManager;
import mesquite.nexml.InterpretNEXML.InterpretNEXML;

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
    private Vector<PropertyValue> mTreeProperties;
    private Vector<PropertyValue> mCanvasProperties;

    public TSSHandler(Annotatable annotatable,Annotation annotation) {
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
			for (Selector s : selectors) {
				mTSSHash.put(s.toString(), pvs);
			}
		}
        mTreeProperties = new Vector<PropertyValue>();
        mCanvasProperties = new Vector<PropertyValue>();
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
    public void parseGeneralSelectors () {
        List<PropertyValue> pvs = getClass ("canvas", "");
        if (pvs != null) {
            for (PropertyValue pv : pvs) {
                NexmlMesquiteManager.debug("parsing canvas pv "+pv.getProperty());
                if (pv.getProperty().equalsIgnoreCase("background-color")) {
                    //Mesquite can only handle setting the color of the canvas. Ignore everything else.
                    //set the color to a standard color
                    mCanvasProperties.add(new PropertyValue("background-color", convertToMesColorName(pv.getValue())));
                } else if (pv.getProperty().equalsIgnoreCase("font-family")) {
                    String fontFamily = chooseAFont(pv.getValue());
                    mCanvasProperties.add(new PropertyValue("font-family",fontFamily));
                } else if (pv.getProperty().equalsIgnoreCase("font-size")) {
                    //convert this value to a point number
                    int value = convertToPixels(pv.getValue(),Constants.DEFAULT_FONT_SIZE);
                    mCanvasProperties.add(new PropertyValue("font-size",String.valueOf(value)));
                } else if (pv.getProperty().equalsIgnoreCase("height")) {
                    //convert this value to a single pixel number
                    int value = convertToPixels(pv.getValue(),Constants.DEFAULT_CANVAS_HEIGHT);
                    mCanvasProperties.add(new PropertyValue("height",String.valueOf(value)));
                } else if (pv.getProperty().equalsIgnoreCase("width")) {
                    //convert this value to a single pixel number
                    int value = convertToPixels(pv.getValue(),Constants.DEFAULT_CANVAS_WIDTH);
                    mCanvasProperties.add(new PropertyValue("width",String.valueOf(value)));
                }
            }
        }

        pvs = getClass ("tree", "");
        if (pvs != null) {
            for (PropertyValue pv : pvs) {
                NexmlMesquiteManager.debug("parsing tree pv "+pv.getProperty()+" with value "+pv.getValue());
                /*
                  border-width: 1px;
                  border-color: black;
                  border-style: solid;
                  layout: rectangular | triangular | radial | polar
                  tip-orientation: left | right | top | bottom
                  scaled: true | false;
                */
                if (pv.getProperty().equalsIgnoreCase("layout")) {
                    mTreeProperties.add(new PropertyValue("layout", "rectangular"));
                } else if (pv.getProperty().equalsIgnoreCase("border-width")) {
                    int value = convertToPixels(pv.getValue(),Constants.DEFAULT_BORDER_WIDTH);
                    mTreeProperties.add(new PropertyValue("border-width",String.valueOf(value)));
                } else if (pv.getProperty().equalsIgnoreCase("border-color")) {
                    mTreeProperties.add(new PropertyValue("border-color",convertToMesColorNumber(pv.getValue())));
                } else if (pv.getProperty().equalsIgnoreCase("border-style")) {
                    //this isn't implemented yet
                } else if (pv.getProperty().equalsIgnoreCase("tip-orientation")) {
                    mTreeProperties.add(new PropertyValue("tip-orientation",pv.getValue().toUpperCase()));
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
		Annotatable subj = getSubject();
		String value = getValue().toString();
		List<PropertyValue> pvs = null;
//		NexmlMesquiteManager.debug("looking for " + tssClass);
		try {
			pvs = getClass(tssClass, value);
			if (pvs != null) {
				NexmlMesquiteManager.debug("found rule " + tssClass + ", parsing " + value);
				setValue(mesquiteNodeAnnotation(pvs, value));
			} else {
				// there is no TSS rule for this
				setValue(Constants.NO_RULE);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public
	void write() {
		// TODO Auto-generated method stub

	}

	private List<PropertyValue> getClass (String tssClassName, String tssValue) {
// here we need to process the tssValue to see if it's a string or range, because those are special cases for selectors
		NexmlMesquiteManager.debug("getting TSS class " + tssClassName + " with value " + tssValue);
		List<PropertyValue> pvs = (List) mTSSHash.get(tssClassName + "." + tssValue);
		if (pvs == null) {
			pvs = (List) mTSSHash.get(tssClassName);
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

    private String mesquiteNodeAnnotation (List<PropertyValue> pvs, String tssValue ) {
		String formatted_pvs = "";
		for (PropertyValue pv : pvs) {
			String val = pv.getValue();
			val = val.replaceAll("value|VALUE", tssValue);
            if (pv.getProperty().equals("border-color")) {
                formatted_pvs = formatted_pvs + ";" + "color:" + convertToMesColorNumber(pv.getValue());
			} else if (pv.getProperty().equals("border-width")) {
                formatted_pvs = formatted_pvs + ";" + "width:" + pv.getValue();
            } else if (pv.getProperty().equals("color")) {
				formatted_pvs = formatted_pvs + ";" + ("taxoncolor:" + convertToMesColorNumber(val));
			} else if (pv.getProperty().equals("collapsed")) {
	//  			<triangled = on >
				if (Boolean.getBoolean(val)) {
					formatted_pvs = formatted_pvs + ";" + "triangled:on";
				}
			}
		}
        if (formatted_pvs.startsWith(";")) {
            formatted_pvs = formatted_pvs.substring(1);
        }
		NexmlMesquiteManager.debug("converted to Mesquite annotation " + formatted_pvs);
		return formatted_pvs;
	}

    public Vector<PropertyValue> getmTreeProperties () {
        return mTreeProperties;
    }
    public Vector<PropertyValue> getmCanvasProperties () {
        return mCanvasProperties;
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
        for(int i=0;i<vals.length;i++) {
            String value = vals[i].trim().replaceAll("\"","");
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

        for (int i=0;i<ColorDistribution.standardColorNames.getSize();i++) {
            String thisColor = ColorDistribution.standardColorNames.getValue(i);
            if(val.equalsIgnoreCase(thisColor)) {
                mesColor = ColorDistribution.standardColorNames.getValue(i);
            }
        }
        return mesColor;
    }


}

