package mesquite.nexml.InterpretNEXML.NexmlWriters;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import mesquite.lib.*;
import org.nexml.model.Annotatable;
import org.nexml.model.Annotation;
import org.nexml.model.Document;
import org.nexml.model.DocumentFactory;
import mesquite.nexml.InterpretNEXML.AnnotationHandlers.AnnotationWrapper;
import mesquite.nexml.InterpretNEXML.Constants;
import mesquite.nexml.InterpretNEXML.NexmlMesquiteManager;
import mesquite.nexml.InterpretNEXML.AnnotationHandlers.PredicateHandler;

public class NexmlWriter extends NexmlMesquiteManager {
	
	/**
	 * 
	 * @param employerEmployee
	 */
	public NexmlWriter (EmployerEmployee employerEmployee) { 
		super(employerEmployee);
	}

	/**
	 * 
	 * @param nr
	 * @param annotatable
	 * @param value
	 */
	private void writeAnnotation(NameReference nr,Annotatable annotatable,Object value) {
		URI namespace = nr.getNamespace();
		String predicate = nr.getName();
        Class<?> handlerClass = null;
        if ( namespace == null ) {
            String[] predParts = predicate.split(":",2);
            if (predParts.length > 1) {
                // look for a namespace that matches the prefix:
                namespace = URI.create(Constants.BaseURIString.replace("#","/"+predParts[0]+"#"));
                handlerClass = findNamespaceHandlerClass(namespace);
                if (handlerClass == null) {
                    // there isn't a known handler namespace for this prefix, so use the default namereference namespace.
                    namespace = Constants.BaseURI;
                    MesquiteMessage.discreetNotifyUser("No known namespace was found for the prefix "+predParts[0]+", using base URI "+namespace+" instead.");
                }
            } else {
                // there wasn't a prefix, so use the default prefix.
                predicate = Constants.NRPrefix + ":" + predicate;
                namespace = URI.create(Constants.NRURIString);
            }
		}
        PredicateHandler handler = getNamespaceHandlerFromURI(namespace);
		if ( null != handler ) {
            handler.write();
		}
        annotatable.addAnnotationValue(predicate,namespace,value);
    }
	
	/**
	 * 
	 * @param associable
	 * @param annotatable
	 * @param segmentCount
	 */
	protected void writeAnnotations(Associable associable, Annotatable annotatable, int segmentCount) {
		int numDoubs = associable.getNumberAssociatedDoubles();
		for ( int i = 0; i < numDoubs; i++ ){  
			DoubleArray array = associable.getAssociatedDoubles(i);
			double value = array.getValue(segmentCount);
			if ( MesquiteDouble.unassigned != value ) {
				writeAnnotation(array.getNameReference(),annotatable,value);
			}
		}	
		
		int numLongs = associable.getNumberAssociatedLongs();
		for ( int i = 0; i < numLongs; i++ ){  
			LongArray array = associable.getAssociatedLongs(i);
			long value = array.getValue(segmentCount);
			if ( MesquiteLong.unassigned != value  ) {
                writeAnnotation(array.getNameReference(),annotatable,value);
			}
		}
		
		int numBits = associable.getNumberAssociatedBits();
		for ( int i = 0; i < numBits; i++ ){  
			Bits array = associable.getAssociatedBits(i);
            writeAnnotation(array.getNameReference(),annotatable,array.isBitOn(segmentCount));
		}	
		
		int numObjs = associable.getNumberAssociatedObjects();
		for ( int i = 0; i < numObjs; i++ ){  
			ObjectArray array = associable.getAssociatedObjects(i);
			Object value = array.getValue(segmentCount);
			if ( null != value ) {
                writeAnnotation(array.getNameReference(),annotatable,value);
			}
		}		
		
	}
	
	/**
	 * 
	 * @param xmlProject
	 * @param mesTaxas
	 */
	private void writeTaxaBlocks(Document xmlProject,ListableVector mesTaxas) {
		NexmlOTUsBlockWriter nobw = new NexmlOTUsBlockWriter(getEmployerEmployee());
		List<FileElement> taxaBlockList = new ArrayList<FileElement>();
		for ( int i = 0; i < mesTaxas.size(); i++ ) {
			taxaBlockList.add((FileElement)mesTaxas.elementAt(i));
		}
		nobw.writeBlocks(xmlProject, taxaBlockList);
	}	
	
	/**
	 * 
	 * @param mesObject
	 * @param xmlObject
	 */
	@SuppressWarnings("rawtypes")
	protected void writeAttributes(Object mesObject,Annotatable xmlObject) {
		Class mesClass = mesObject.getClass();
		Method[] mesMethods = mesClass.getMethods();
		Map<String,Class<?>[]> signatureOf = new HashMap<String,Class<?>[]>();
		Map<String,Method> methodOf = new HashMap<String,Method>();
		for ( int i = 0; i < mesMethods.length; i++ ) {
			signatureOf.put(mesMethods[i].getName(), mesMethods[i].getParameterTypes());
			methodOf.put(mesMethods[i].getName(), mesMethods[i]);
		}
		for ( String mesGetter : signatureOf.keySet() ) {
			if ( mesGetter.equals("getAttachments") ) {
				continue;
			}
			if ( mesGetter.startsWith("get") && signatureOf.get(mesGetter).length == 0 ) {
				String mesSetter = "s" + mesGetter.substring(1);
				if ( signatureOf.containsKey(mesSetter) && signatureOf.get(mesSetter).length == 1 ) {
					Object value = null;
					try {						
						value = methodOf.get(mesGetter).invoke(mesObject);
					} catch (Exception e) {
						e.printStackTrace();
					}
					if ( null != value ) {
						xmlObject.addAnnotationValue(Constants.BeanPrefix+':'+mesGetter.substring(3), Constants.BeanURI, value);
					}
				}
			}
		}
		if ( mesObject instanceof Attachable ) {
			Vector attachmentVector = ((Attachable)mesObject).getAttachments();
			if ( null != attachmentVector ) {
				for ( Object obj : ((Attachable)mesObject).getAttachments() ) {
					if ( obj instanceof AnnotationWrapper ) {
						AnnotationWrapper aw = (AnnotationWrapper)obj;
						xmlObject.addAnnotationValue(aw.getName(), aw.getPredicateNamespace(), aw.getValue());
					}
				}
			}
		}
	}
	
	/**
	 * 
	 * @param xmlProject
	 * @param mesCharacters
	 */
	private void writeCharacterBlocks(Document xmlProject,ListableVector mesCharacters) {
		List<FileElement> mesDatas = new ArrayList<FileElement>();
		for ( int i = 0; i < mesCharacters.size(); i++ ) {
			FileElement mesData = (FileElement)mesCharacters.elementAt(i);
			mesDatas.add(mesData); 		
		}
		NexmlCharactersBlockWriter ncbw = new NexmlCharactersBlockWriter(getEmployerEmployee());
		ncbw.writeBlocks(xmlProject, mesDatas);
	}	
	
	/**
	 * 
	 * @param xmlProject
	 * @param treeVectors
	 */
	private void writeTreeBlocks(Document xmlProject,Listable[] treeVectors) {
		List<FileElement> ltv = new ArrayList<FileElement>();
		NexmlTreeBlockWriter ntbw = new NexmlTreeBlockWriter(getEmployerEmployee());
		for ( int i = 0; i < treeVectors.length; i++ ) {	
			ltv.add((FileElement)treeVectors[i]);
		}	
		ntbw.writeBlocks(xmlProject, ltv);
	}
	
	/**
	 * 
	 * @param mesProject
	 * @return
	 */
	private Document writeProject(MesquiteProject mesProject) {
		Document xmlProject = DocumentFactory.safeCreateDocument();		
		return xmlProject;
	}
	
	/**
	 * 
	 * @param mesProject
	 * @return
	 */
	public Document createDocumentFromProject(MesquiteProject mesProject) {
		ListableVector mesTaxas = mesProject.getTaxas();
		Document xmlProject = writeProject(mesProject);
		try {
			writeTaxaBlocks(xmlProject,mesTaxas);
			writeCharacterBlocks(xmlProject,mesProject.getCharacterMatrices());			
			for ( int i = 0; i < mesTaxas.size(); i++ ) {
				Listable[] treeVectors = mesProject.getCompatibleFileElements(TreeVector.class, mesTaxas.elementAt(i));
				writeTreeBlocks(xmlProject,treeVectors);
			}
		} catch ( Exception e ) {
			e.printStackTrace();
		}
		return xmlProject;
	}	
	
}
