package mesquite.nexml.InterpretNEXML;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.*;
import java.net.URI;

import mesquite.lib.*;
import org.nexml.model.Annotatable;
import org.nexml.model.Annotation;
import org.nexml.model.Document;
import org.nexml.model.OTU;
import org.nexml.model.OTUs;

import mesquite.nexml.InterpretNEXML.AnnotationHandlers.NamespaceHandler;
import mesquite.nexml.InterpretNEXML.AnnotationHandlers.PredicateHandler;
import mesquite.nexml.InterpretNEXML.AnnotationHandlers.PredicateHandlerImpl;

public class NexmlMesquiteManager {
	private Properties mPredicateHandlerMapping;
	private Properties mNamespaceHandlerMapping;
    protected static LinkedList<String> mStylesheets;
	private static Hashtable mActiveNamespaceHandlers;
	private EmployerEmployee mEmployerEmployee;

	/**
	 *
	 * @param employerEmployee
	 */
	public NexmlMesquiteManager (EmployerEmployee employerEmployee) {
		mEmployerEmployee = employerEmployee;
        mPredicateHandlerMapping = new Properties();
        mNamespaceHandlerMapping = new Properties();
        if (mActiveNamespaceHandlers == null) {
            mActiveNamespaceHandlers = new Hashtable();
        }
        try {
            mPredicateHandlerMapping.load(NexmlMesquiteManager.class.getResourceAsStream(Constants.PREDICATES_PROPERTIES));
            mNamespaceHandlerMapping.load(NexmlMesquiteManager.class.getResourceAsStream(Constants.NAMESPACE_PROPERTIES));
        } catch (IOException e) {
            e.printStackTrace();
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
		if (Constants.DEBUGGING)
			mesquite.lib.MesquiteMessage.notifyProgrammer(s);
	}

    public static void notify(String s) {
            mesquite.lib.MesquiteMessage.notifyProgrammer(s);
    }

    public static LinkedList<String> getStylesheets () {
        return mStylesheets;
    }

    public static void setStylesheets (LinkedList<String> ss) {
        mStylesheets = ss;
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
        if (curie.length > 1) {
            return curie[1]; // NameReference;	lookup in properties
        } else {
            MesquiteMessage.discreetNotifyUser("Malformed local XML property "+property);
            return "";
        }
	}

	/**
	 *
     * @param annotatable
     * @param annotation
     * @return
	 */
	protected PredicateHandler getNamespaceHandler(Annotatable annotatable, Annotation annotation) {
		PredicateHandler ph = getNamespaceHandlerFromURI(annotation.getPredicateNamespace());
        if (ph == null) { // couldn't find a declared namespace, so find a predicate handler
            debug ("XML namespace "+annotation.getPredicateNamespace()+" for annotation "+annotation.getProperty()+" can't be found, using a local predicate handler instead.");
            String predicate = getLocalProperty(annotation);
            String handlerClassName = mPredicateHandlerMapping.getProperty(predicate);
            if ( handlerClassName != null ) { // there is a mapped predicate handler
                try {
                    Class<?> handlerClass = Class.forName(handlerClassName);
                    Constructor<?> declaredConstructor = handlerClass.getDeclaredConstructor(Annotatable.class,Annotation.class);
                    ph = (PredicateHandler) declaredConstructor.newInstance(annotatable,annotation);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else { // couldn't find a mapped predicate handler either, so just make a generic one.
                ph = new PredicateHandlerImpl() ;
            }
        }
        if (ph != null) {
            return ph.initPredicateHandler(annotatable,annotation);
        }
        return null;
	}

    protected NamespaceHandler getNamespaceHandlerFromURI(URI uri) {
        if (uri == null) {
            return null;
        }
        NamespaceHandler nh = getActiveNamespaceHandler(uri); // look for existing NamespaceHandler
        if (nh == null)  {  // if there isn't one yet, see if we can make one from the mappings we know about
            Class<?> handlerClass = findNamespaceHandlerClass(uri);
            if (handlerClass != null) {
                try {
                    Constructor<?> declaredConstructor = handlerClass.getConstructor();
                    nh = (NamespaceHandler) declaredConstructor.newInstance();
                    addActiveNamespaceHandler(uri, nh);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return nh;
    }

    protected Class<?> findNamespaceHandlerClass (URI uri) {
        Class<?> handlerClass = null;
        for ( String name : mNamespaceHandlerMapping.stringPropertyNames() ) {
            if ( mNamespaceHandlerMapping.getProperty(name).equals(uri.toString()) ) {
                try {
                    handlerClass = Class.forName(name);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            }
        }
        return handlerClass;
    }

    protected boolean isNamespaceHandlerActive(URI uri) {
        if (uri == null) {
            return false;
        }
        return mActiveNamespaceHandlers.containsKey(uri);
    }

    protected Enumeration<URI> getActiveNamespaceHandlers() {
        return mActiveNamespaceHandlers.keys();
    }

    protected void addActiveNamespaceHandler(URI uri, NamespaceHandler nh) {
        mActiveNamespaceHandlers.put(uri, nh);
    }

    protected NamespaceHandler getActiveNamespaceHandler (URI uri) {
        if (uri == null) {
            return null;
        }
        return (NamespaceHandler) mActiveNamespaceHandlers.get(uri);
    }

    protected void resetActiveNamespaceHandlers() {
        mActiveNamespaceHandlers = new Hashtable();
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
