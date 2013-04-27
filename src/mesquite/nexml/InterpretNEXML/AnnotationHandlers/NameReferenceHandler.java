/**
 * 
 */
package mesquite.nexml.InterpretNEXML.AnnotationHandlers;

import mesquite.lib.Associable;
import mesquite.lib.Listable;
import mesquite.lib.MesquiteProject;
import mesquite.lib.NameReference;
import mesquite.nexml.InterpretNEXML.Constants;

import org.nexml.model.Annotatable;
import org.nexml.model.Annotation;

/**
 * @author rvosa
 *
 */
public class NameReferenceHandler extends NamespaceHandler {
    /* (non-Javadoc)
      * @see mesquite.nexml.InterpretNEXML.PredicateHandler#getPrefix()
      */
    @Override
    public String getURIString() {
        return Constants.NRURIString;
    }
    /* (non-Javadoc)
      * @see mesquite.nexml.InterpretNEXML.PredicateHandler#getPrefix()
      */
    @Override
    public
    String getPrefix() {
        return Constants.NRPrefix;
    }

    public void read(Associable associable, Listable listable, int index) {
		Object value = getValue();
		String predicate = getPredicate();
		String[] parts = predicate.split(":");
		String local = parts[1];
		NameReference nRef = new NameReference(local);
		if ( value instanceof Boolean ) {
			associable.setAssociatedBit(nRef, index, (Boolean)value);
		}
		else if ( value instanceof Double ) {
			associable.setAssociatedDouble(nRef, index, (Double)value);
		}
		else if ( value instanceof Long ) {
			associable.setAssociatedLong(nRef, index, (Long)value);
		}
		else {
			associable.setAssociatedObject(nRef, index, value);
		}	
	}
}
