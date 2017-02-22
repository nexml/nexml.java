/**
 * 
 */
package mesquite.nexml.InterpretNEXML.AnnotationHandlers;

import mesquite.nexml.InterpretNEXML.Constants;
import mesquite.nexml.InterpretNEXML.NexmlMesquiteManager;
import org.nexml.model.Annotatable;

/**
 * @author rvosa
 *
 */
public abstract class NamespaceHandler extends PredicateHandler {
    private Annotatable mSubject;
    private Object mValue;
    private String mPredicate;

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

    public String getPrefix() {
        NexmlMesquiteManager.debug("Fell through to superclass NamespaceHandler, returning base prefix.");
        return Constants.BasePrefix;
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
      * @see mesquite.nexml.InterpretNEXML.PredicateHandler#setURIString(java.lang.String)
      */
    @Override
    public
    void setURIString(String uri) {
    }
}
