/**
 *
 */
package mesquite.nexml.InterpretNEXML.AnnotationHandlers;

import java.lang.reflect.Method;
import java.io.*;
import java.util.*;


import mesquite.lib.Associable;
import mesquite.lib.Listable;
import mesquite.nexml.InterpretNEXML.Constants;
import mesquite.nexml.InterpretNEXML.NexmlMesquiteManager;

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
	private File mTSSFile;
	private List<Rule> mTSSList;
	private Hashtable mTSSHash;


	/**
	 * @param subject
	 * @param predicate
	 * @param object
	 */
	public TSSHandler(Annotatable annotatable,Annotation annotation) {
		super(annotatable, annotation);
		mTSSFile = new File(mesquite.lib.MesquiteModule.prefsDirectory + mesquite.lib.MesquiteFile.fileSeparator + "default.tss");
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
				setValue(convertToMesAnnotation(subj, pvs, value));
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

	private String convertToMesAnnotation ( Annotatable subj, List<PropertyValue> pvs, String tssValue ) {
		String formatted_pvs = new String();
		for (PropertyValue pv : pvs) {
			String val = pv.getValue();
			val = val.replaceAll("value|VALUE", tssValue);

			if (pv.getProperty().toString().equals("border")) {
				String[] props = val.split("\\s+");
				for (int i=0; i<props.length; i++) {
					String color = convertToMesColor(props[i]);
					if (color.equals("")) { // this is not a color word
						if (props[i].contains("px")) {
							// we want to set a width
							formatted_pvs = formatted_pvs + ";" + "width:" + props[i].replace("px","");
						}
					} else { // this is a color word
						formatted_pvs = formatted_pvs + ";" + "color:" + color;
					}
				}
			}
			else if (pv.getProperty().toString().equals("color")) {
// 					<color = val > this is actually wrong: converts branch color when it should convert text color
				formatted_pvs = formatted_pvs + ";" + (pv.getProperty() + ":" + convertToMesColor(val));
			}
			else if (pv.getProperty().toString().equals("collapsed")) {
// 	//  			<triangled = on >
				if (val.equals("true")) {
					formatted_pvs = formatted_pvs + ";" + "triangled:on";
				}
				NexmlMesquiteManager.debug("applying the format " +  "triangled : on" + " to " + subj);
			}
		}
		NexmlMesquiteManager.debug("converted to Mes annotation " + formatted_pvs);
		return formatted_pvs;
	}

	private String convertToMesColor ( String val ) {
		String mesColor = "";
		if (val.equals("red") || val.equals("#ff0000")) { mesColor = "5"; }
		else if (val.equals("green") || val.equals("#00ff00")) { mesColor = "11"; }
		else if (val.equals("yellow") || val.equals("#ffff00")) { mesColor = "7"; }
		else if (val.equals("blue") || val.equals("#0000ff")) { mesColor = "14"; }

		return mesColor;
	}

}

