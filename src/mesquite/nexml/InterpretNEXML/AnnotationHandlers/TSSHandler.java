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
        //process each line in some way
		cssString = cssString + scanner.nextLine();
      }

		try {
			mTSSList = CSSParser.parse(cssString);
			NexmlMesquiteManager.debug("there are " + mTSSList.size() + " in the tss file");
		} catch (Exception e) {
			NexmlMesquiteManager.debug(e.toString());
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

	@Override
	public
	void read(Associable associable, Listable listable, int index) {
		String[] parts = getPredicate().split(":");
		String tssClass = parts[1];
		Method method = null;
		String value = getValue().toString();
		if ( null != value ) {
			try {
				findTSSClass(tssClass, value);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public
	void write() {
		// TODO Auto-generated method stub

	}

	public List<PropertyValue> findTSSClass (String tssClassName, String tssValue) {
		int i = 1;
// here we need to process the tssValue to see if it's a string or range, because those are special cases for selectors
		for (Rule eachClass : mTSSList) {
			List<Selector> selectors = eachClass.getSelectors();
			List<PropertyValue> pvs = eachClass.getPropertyValues();
			i++;
			for (Selector eachSelector : selectors) {
				NexmlMesquiteManager.debug("\tlooking at selector "+ eachSelector.toString());
				if (eachSelector.toString().equals(tssClassName)) {
					NexmlMesquiteManager.debug("FOUND THE SELECTOR");
					return pvs;
				}
			}
		}
		return null;
	}
}
