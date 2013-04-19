
package mesquite.nexml.InterpretNEXML.AnnotationHandlers;

import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;


import mesquite.lib.Associable;
import mesquite.lib.Listable;
import mesquite.lib.ColorDistribution;
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
    public void parseGeneralSelectors (){
        // set defaults
        String backgroundColor = "White";
        String fontFamily = "sans-serif";
        String fontSize = "12";
        String fontStyle = "normal";

        List<PropertyValue> pvs = getClass ("canvas", "");
        if (pvs != null) {
            int j = 0;
            while (j<pvs.size()) {
                PropertyValue pv = pvs.get(j++);
                NexmlMesquiteManager.debug("parsing canvas pv "+pv.getProperty());
                // for each pv, if it is a compound value, break it into simple pvs and add them individually
                if (pv.getProperty().equalsIgnoreCase("font")) {
                    //font-style font-size font-family
                    String[] split_pvs = pv.getValue().split(" ",2);
                    if (split_pvs[0].matches("^\\D.*")) { // if this value is not a number, then we have a font-style
                        pvs.add(new PropertyValue("font-style",split_pvs[0]));
                        split_pvs = split_pvs[1].split(" ",2);
                    }
                    pvs.add(new PropertyValue("font-size",split_pvs[0]));
                    pvs.add(new PropertyValue("font-family",split_pvs[1]));
                } else if (pv.getProperty().contains("background")) {
                    //Mesquite can only handle setting the color of the canvas. Ignore everything else.
                    //set the color to a standard color
                    backgroundColor = InterpretNEXML.convertToMesColorName(pv.getValue());
                } else if (pv.getProperty().equalsIgnoreCase("font-family")) {
                    String[] vals = pv.getValue().split(",");
                    for(int i=0;i<vals.length;i++) {
                        String value = vals[i].trim().replaceAll("\"","");
                        Font f = new Font(value,Font.PLAIN,12); // these are arbitrary values; we just want to see if value is a valid font name
                        if (f.getFamily().equalsIgnoreCase("Dialog")) {
                            continue;
                        }
                        fontFamily = value;
                        break;
                    }
                } else if (pv.getProperty().equalsIgnoreCase("font-size")) {
                    //convert this value to a point number
                    String unit = pv.getValue().replaceAll("\\d","");
                    int value = Integer.parseInt(pv.getValue().replaceAll("\\D",""));
                    if (unit.contains("%")) {
                        // assume percentage is based on a default size of 12pt
                        value = 12 * (value/100);
                    } else if (unit.contains("in")) {
                        // assume 72 pt per inch
                        value = value * 72;
                    } else if (unit.contains("cm")) {
                        // assume 28.3 pt per cm
                        value = (int) (value * 28.3);
                    }
                    fontSize = String.valueOf(value);
                } else if ((pv.getProperty().equalsIgnoreCase("width")) || (pv.getProperty().equalsIgnoreCase("height"))) {
                    //convert this value to a single pixel number
                    String unit = pv.getValue().replaceAll("\\d","");
                    int value = Integer.parseInt(pv.getValue().replaceAll("\\D",""));
                    if (unit.contains("%")) {
                        // assume percentage is based on a window of 800x800px
                        value = 800 * (value/100);
                    } else if (unit.contains("in")) {
                        // assume 72 px per inch
                        value = value * 72;
                    } else if (unit.contains("cm")) {
                        // assume 28.3 px per cm
                        value = (int) (value * 28.3);
                    }
                    mCanvasProperties.add(new PropertyValue(pv.getProperty(),String.valueOf(value)));
                }
            }
        }
        mCanvasProperties.add(new PropertyValue("font-family",fontFamily));
        mCanvasProperties.add(new PropertyValue("font-size",fontSize));
        mCanvasProperties.add(new PropertyValue("background-color",backgroundColor));
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
		NexmlMesquiteManager.debug("looking for " + tssClass);
		try {
			pvs = getClass(tssClass, value);
			if (pvs != null) {
				NexmlMesquiteManager.debug("found rule " + tssClass + ", parsing " + value);
				setValue(mesquiteNodeAnnotation(subj, pvs, value));
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
		if (pvs != null) {
			NexmlMesquiteManager.debug("returning " + pvs.size() + " pvs");
		}
		return pvs;
	}

    private String mesquiteNodeAnnotation ( Annotatable subj, List<PropertyValue> pvs, String tssValue ) {
		String formatted_pvs = "";
		for (PropertyValue pv : pvs) {
			String val = pv.getValue();
			val = val.replaceAll("value|VALUE", tssValue);

			if (pv.getProperty().equals("border")) {
				String[] props = val.split("\\s+");
				for (int i=0; i<props.length; i++) {
					String color = InterpretNEXML.convertToMesColorNumber(props[i]);
					if (color == null) { // this is not a color word
						if (props[i].contains("px")) {
							// we want to set a width
							formatted_pvs = formatted_pvs + ";" + "width:" + props[i].replace("px","");
						}
					} else { // this is a color word
						formatted_pvs = formatted_pvs + ";" + "color:" + color;
					}
				}
			}
			else if (pv.getProperty().equals("color")) {
				formatted_pvs = formatted_pvs + ";" + ("taxoncolor:" + InterpretNEXML.convertToMesColorNumber(val));
			}
			else if (pv.getProperty().equals("collapsed")) {
// 	//  			<triangled = on >
				if (val.equals("true")) {
					formatted_pvs = formatted_pvs + ";" + "triangled:on";
				}
			}
		}
		NexmlMesquiteManager.debug("converted to Mes annotation " + formatted_pvs);
		return formatted_pvs;
	}

    public Vector<PropertyValue> getmTreeProperties () {
        return mTreeProperties;
    }
    public Vector<PropertyValue> getmCanvasProperties () {
        return mCanvasProperties;
    }
}

