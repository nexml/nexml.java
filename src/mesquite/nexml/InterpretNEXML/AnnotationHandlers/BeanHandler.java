/**
 * 
 */
package mesquite.nexml.InterpretNEXML.AnnotationHandlers;

import java.lang.reflect.Method;

import mesquite.lib.Associable;
import mesquite.lib.Listable;
import mesquite.nexml.InterpretNEXML.Constants;
import mesquite.nexml.InterpretNEXML.NexmlMesquiteManager;

import org.nexml.model.Annotatable;
import org.nexml.model.Annotation;

/**
 * @author rvosa
 *
 */
public class BeanHandler extends NamespaceHandler {
	/* (non-Javadoc)
	 * @see mesquite.nexml.InterpretNEXML.PredicateHandler#getPrefix()
	 */
	@Override
	public
	String getPrefix() {
		return Constants.BeanPrefix;
	}

	/* (non-Javadoc)
	 * @see mesquite.nexml.InterpretNEXML.PredicateHandler#getURIString()
	 */
	@Override
	public
	String getURIString() {
		return Constants.BeanURIString;
	}

	@Override
	public
	void read(Associable associable, Listable listable, int index) {
		String[] parts = getPredicate().split(":");
		String setter = "set" + parts[1];
		String value = (String) getValue();
		NexmlMesquiteManager.debug("BeanHandler setter: "+setter+", value: "+value);
		if ( null != value ) {
            Method method;
            try {
                method = associable.getClass().getMethod(setter, value.getClass());
                NexmlMesquiteManager.debug("should be invoking "+method.toString());
//                method.invoke(associable, value);
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

}
