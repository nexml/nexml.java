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
// 				NexmlMesquiteManager.debug("\tadding "+ s.toString() + " to the hash");
			}
		}
// 		NexmlMesquiteManager.debug("\tthere are now "+ mTSSHash.size() + " elements in the hash");
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

		try {
			pvs = getClass(tssClass, value);
			if (pvs != null) {
				NexmlMesquiteManager.debug("found a corresponding rule, parsing " + value);
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

		List<PropertyValue> pvs = (List) mTSSHash.get(tssClassName + "." + tssValue);
		if (pvs == null) {
			pvs = (List) mTSSHash.get(tssClassName);
		}
		return pvs;
	}

	private String convertToMesAnnotation ( Annotatable subj, List<PropertyValue> pvs, String tssValue ) {
		String formatted_pvs = new String();
		for (PropertyValue pv : pvs) {
			String val = pv.getValue();
			val = val.replaceAll("value|VALUE", tssValue);
			NexmlMesquiteManager.debug("val is now " + val);
			if (pv.getProperty().toString().equals("border")) {
// 				in mesquite: annotate the node as <color = val >
// 				String[] props = val.split("\\s+");

				if (val.contains("red") || val.contains("#ff0000")) { val = "5"; }
				else if (val.contains("green") || val.contains("#00ff00")) { val = "11"; }
				else if (val.contains("yellow") || val.contains("#ffff00")) { val = "7"; }
				else if (val.contains("blue") || val.contains("#0000ff")) { val = "14"; }
				formatted_pvs = formatted_pvs + ";" + "color:" + val;
			}
			else if (pv.getProperty().toString().equals("color")) {
// 					<color = val >
				if (val.equals("red") || val.equals("#ff0000")) { val = "5"; }
				else if (val.equals("green") || val.equals("#00ff00")) { val = "11"; }
				else if (val.equals("yellow") || val.equals("#ffff00")) { val = "7"; }
				else if (val.equals("blue") || val.equals("#0000ff")) { val = "14"; }
				formatted_pvs = formatted_pvs + ";" + (pv.getProperty() + ":" + val);
				NexmlMesquiteManager.debug("applying the format " +  pv.getProperty() + ":" + val + " to " + subj);
			}
// 			else if (pv.getProperty().toString().equals("collapsed")) {
// // 	//  			<triangled = on >
// 				if (val.equals("true")) { val = "on"; }
// 				formatted_pvs = formatted_pvs + ";" + (pv.getProperty() + ":" + val);
// 				NexmlMesquiteManager.debug("applying the format " +  pv.getProperty() + ":" + val + " to " + subj);
// 			}
		}
		return formatted_pvs;
	}

}

