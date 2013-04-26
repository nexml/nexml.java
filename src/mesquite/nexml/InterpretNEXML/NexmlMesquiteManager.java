package mesquite.nexml.InterpretNEXML;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.URI;
import java.util.Properties;
import java.util.Set;
import java.util.Hashtable;

import org.nexml.model.Annotatable;
import org.nexml.model.Annotation;
import org.nexml.model.Document;
import org.nexml.model.OTU;
import org.nexml.model.OTUs;

import mesquite.lib.EmployerEmployee;
import mesquite.lib.ListableVector;
import mesquite.lib.MesquiteProject;
import mesquite.lib.Taxa;
import mesquite.lib.Taxon;
import mesquite.nexml.InterpretNEXML.AnnotationHandlers.NamespaceHandler;
import mesquite.nexml.InterpretNEXML.AnnotationHandlers.PredicateHandler;
import mesquite.nexml.InterpretNEXML.AnnotationHandlers.PredicateHandlerImpl;

public class NexmlMesquiteManager {
	private static boolean debugging = true;

	private Properties mPredicateHandlerMapping;
	private Properties mNamespaceHandlerMapping;

	private static Hashtable mNamespaceHandlers;

	private EmployerEmployee mEmployerEmployee;

	/**
	 *
	 * @param employerEmployee
	 */
	public NexmlMesquiteManager (EmployerEmployee employerEmployee) {
		mEmployerEmployee = employerEmployee;
		mPredicateHandlerMapping = new Properties();
		mNamespaceHandlerMapping = new Properties();
	    try {
	    	mPredicateHandlerMapping.load(NexmlMesquiteManager.class.getResourceAsStream(Constants.PREDICATES_PROPERTIES));
	    	mNamespaceHandlerMapping.load(NexmlMesquiteManager.class.getResourceAsStream(Constants.NAMESPACE_PROPERTIES));
	    } catch (IOException e) {
	    	e.printStackTrace();
	    }
	}

    public void setNamespaceHandler(URI uriString, NamespaceHandler nh) {
        if (mNamespaceHandlers == null) {
            mNamespaceHandlers = new Hashtable();
        }
        mNamespaceHandlers.put(uriString, nh);
    }

    public void resetNamespaceHandlers() {
        mNamespaceHandlers = new Hashtable();
    }


    public NamespaceHandler getNamespaceHandlerFromURI(URI uriString) {
        if ((mNamespaceHandlers == null) || (uriString == null)) {
            return null;
        } else {
            return (NamespaceHandler) mNamespaceHandlers.get(uriString);
        }
    }

	/**
	 *
	 * @return
	 */
	protected EmployerEmployee getEmployerEmployee() {
		return mEmployerEmployee;
	}

	/**
	 *
	 * @param s
	 */
	public static void debug(String s) {
		if (debugging)
			mesquite.lib.MesquiteMessage.notifyProgrammer(s);
	}

	/**
	 *
	 * @param annotation
	 * @return
	 */
	protected static String getLocalProperty(Annotation annotation) {
		String property = annotation.getProperty();
		if ( property.equals("") ) {
			property = annotation.getRel();
		}
		String[] curie = property.split(":");
		String localProperty = curie[1]; // NameReference;	lookup in properties
		return localProperty;
	}

	/**
	 *
	 * @param annotatable
	 * @param annotation
	 * @return
	 */
	protected PredicateHandler getPredicateHandler(Annotatable annotatable, Annotation annotation) {
		String predicate = getLocalProperty(annotation);
		String handlerClassName = mPredicateHandlerMapping.getProperty(predicate);
		PredicateHandler ph = null;
		if ( handlerClassName != null ) {
			try {
				Class<?> handlerClass = Class.forName(handlerClassName);
				Constructor<?> declaredConstructor = handlerClass.getDeclaredConstructor(Annotatable.class,Annotation.class);
				ph = (PredicateHandler) declaredConstructor.newInstance(annotatable,annotation);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		if ( null == ph ) {
			ph = new PredicateHandlerImpl(annotatable,annotation);
		}
		debug("Using predicateHandler " + ph.toString());
		return ph;
	}

	/**
	 *
     * @param annotatable
     * @param annotation
     * @return
	 */
	protected NamespaceHandler getNamespaceHandler(Annotatable annotatable, Annotation annotation) {
		String handlerClassName = null;
        URI uri = annotation.getPredicateNamespace();
		NamespaceHandler nh = getNamespaceHandlerFromURI(uri);
        debug ("getNamespaceHandler "+annotation.toString()+" with property "+annotation.getProperty()+", value "+annotation.getValue()+", uri "+annotation.getPredicateNamespace());
        if (nh != null) {
			// don't reinstantiate all handlers all the time; if one has already been instantiated,
			// just update the annotation for the existing handler
// 			debug ("using " + nh.getClass().toString());
			nh.setSubject(annotatable);
			nh.setValue(annotation.getValue());
			String property = annotation.getProperty();
			if ( null == property || "".equals(property) ) {
				property = annotation.getRel();
				nh.setPropertyIsRel(true);
			}
			nh.setPredicate(property);
		} else {
            if (uri != null) {
                for ( String name : mNamespaceHandlerMapping.stringPropertyNames() ) {
                    if ( mNamespaceHandlerMapping.getProperty(name).equals(uri.toString()) ) {
                        handlerClassName = name;
                        break;
                    }
                }
            }
			if ( handlerClassName != null ) {
				try {
					Class<?> handlerClass = Class.forName(handlerClassName);
					Constructor<?> declaredConstructor = handlerClass.getDeclaredConstructor(Annotatable.class,Annotation.class);
					nh = (NamespaceHandler) declaredConstructor.newInstance(annotatable,annotation);
					setNamespaceHandler(uri, nh);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		return nh;
	}

	/**
	 *
	 * @param mesTaxa
	 * @param xmlProject
	 * @return
	 */
	protected OTUs findEquivalentTaxa(Taxa mesTaxa,Document xmlProject) {
		for ( OTUs xmlTaxa : xmlProject.getOTUsList() ) {
			Set<Object> msqUIDs = xmlTaxa.getAnnotationValues(Constants.TaxaUID);
			if ( msqUIDs.contains(mesTaxa.getUniqueID()) ) {
				return xmlTaxa;
			}
		}
		return null;
	}

	/**
	 *
	 * @param mesTaxon
	 * @param xmlTaxa
	 * @return
	 */
	protected OTU findEquivalentTaxon(Taxon mesTaxon,OTUs xmlTaxa) {
		Integer mesTaxonIndex = mesTaxon.getNumber();
		for ( OTU xmlTaxon : xmlTaxa.getAllOTUs() ) {
			Set<Object> msqUIDs = xmlTaxon.getAnnotationValues(Constants.TaxonUID);
			if ( msqUIDs.contains(mesTaxonIndex) ) {
				return xmlTaxon;
			}
		}
		return null;
	}

	/**
	 *
	 * @param xmlOTUs
	 * @param mesProject
	 * @return
	 */
	protected Taxa findEquivalentTaxa(OTUs xmlOTUs,MesquiteProject mesProject) {
		ListableVector mesTaxas = mesProject.getTaxas();
		String xmlOTUsId = xmlOTUs.getId();
		for ( int i = 0; i < mesTaxas.size(); i++ ) {
			Taxa mesTaxa = (Taxa)mesTaxas.elementAt(i);
			if ( xmlOTUsId.equals(mesTaxa.getUniqueID()) ) {
				return mesTaxa;
			}
		}
		return null;
	}

	/**
	 *
	 * @param xmlOTU
	 * @param mesTaxa
	 * @return
	 */
	protected Taxon findEquivalentTaxon(OTU xmlOTU, Taxa mesTaxa) {
		String xmlOTUId = xmlOTU.getId();
		for ( int i = 0; i < mesTaxa.getNumTaxa(); i++ ) {
			Taxon mesTaxon = mesTaxa.getTaxon(i);
			if ( xmlOTUId.equals(mesTaxon.getUniqueID()) ) {
				return mesTaxon;
			}
		}
		return null;
	}

}
