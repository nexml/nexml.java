package mesquite.nexml.InterpretNEXML.NexmlWriters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import mesquite.categ.lib.CategoricalData;
import mesquite.categ.lib.CategoricalState;
import mesquite.categ.lib.DNAData;
import mesquite.categ.lib.ProteinData;
import mesquite.categ.lib.RNAData;
import mesquite.cont.lib.ContinuousData;
import mesquite.lib.EmployerEmployee;
import mesquite.lib.FileElement;
import mesquite.lib.MesquiteMessage;
import mesquite.lib.Taxa;
import mesquite.lib.Taxon;
import mesquite.lib.characters.CharacterData;
import mesquite.lib.characters.CharacterState;

import org.nexml.model.Annotatable;
import org.nexml.model.CategoricalMatrix;
import org.nexml.model.Character;
import org.nexml.model.CharacterStateSet;
import org.nexml.model.CompoundCharacterState;
import org.nexml.model.Document;
import org.nexml.model.Matrix;
import org.nexml.model.MatrixCell;
import org.nexml.model.MolecularMatrix;
import org.nexml.model.NexmlWritable;
import org.nexml.model.OTU;
import org.nexml.model.OTUs;
import org.nexml.model.PolymorphicCharacterState;
import org.nexml.model.UncertainCharacterState;

public class NexmlCharactersBlockWriter extends NexmlBlockWriter {

	/**
	 * Generate symbols for uncertainties and polymorphisms that don't conflict with existing state symbols.
	 */
	private int nextMultipleStateSymbol = CategoricalState.getMaxPossibleStateStatic() + 1;

	@SuppressWarnings("serial")
	private static final Map<String , String> xmlMolecularDataTypeFor = new HashMap<String, String>() {{
		put(DNAData.DATATYPENAME, MolecularMatrix.DNA);
		put(RNAData.DATATYPENAME, MolecularMatrix.RNA);
		put(ProteinData.DATATYPENAME, MolecularMatrix.Protein);
	}};	

	/**
	 * 
	 * @param employerEmployee
	 */
	public NexmlCharactersBlockWriter(EmployerEmployee employerEmployee) {
		super(employerEmployee);
	}

	/*
	 * (non-Javadoc)
	 * @see mesquite.nexml.InterpretNEXML.NexmlBlockWriter#writeBlock(org.nexml.model.Document, mesquite.lib.FileElement)
	 */
	@Override
	protected Annotatable writeBlock(Document xmlProject, FileElement mesBlock) {
		CharacterData mesData = (CharacterData)mesBlock;
		Taxa mesTaxa = mesData.getTaxa();
		OTUs xmlTaxa = findEquivalentTaxa(mesTaxa,xmlProject);			
		org.nexml.model.Matrix<?> xmlMatrix = null;		
		String mesDataType = mesData.getDataTypeName();
		if ( xmlMolecularDataTypeFor.containsKey(mesDataType) ) {
			xmlMatrix = xmlProject.createMolecularMatrix(xmlTaxa,xmlMolecularDataTypeFor.get(mesDataType));
		}
		else if ( mesDataType.equalsIgnoreCase(CategoricalData.DATATYPENAME) ) {
			xmlMatrix = xmlProject.createCategoricalMatrix(xmlTaxa);   
		}
		else if ( mesDataType.equalsIgnoreCase(ContinuousData.DATATYPENAME) ) {
			xmlMatrix = xmlProject.createContinuousMatrix(xmlTaxa);      			
		}			
		else {
			MesquiteMessage.warnProgrammer("Can't write data type "+mesDataType);
		} 
		writeCharacterStates(mesData, xmlMatrix);
		return xmlMatrix;
	}

	/**
	 * 
	 * @param mesData
	 * @param xmlMatrix
	 */
	@SuppressWarnings("unchecked")
	private void writeCharacterStates(CharacterData mesData, org.nexml.model.Matrix<?> xmlMatrix) {
		String mesDataType = mesData.getDataTypeName();
		int mesNchar = mesData.getNumChars();
		List<Character> xmlCharacters = new ArrayList<Character>(mesNchar);
		for ( int characterIndex = 0; characterIndex < mesNchar; characterIndex++ ) {
			CharacterStateSet xmlCharacterStateSet = null;
			if ( xmlMolecularDataTypeFor.containsKey(mesDataType) ) {
				xmlCharacterStateSet = ((MolecularMatrix)xmlMatrix).getCharacterStateSet();
			}
			else if ( mesDataType.equalsIgnoreCase(CategoricalData.DATATYPENAME) ) {
				xmlCharacterStateSet = ((CategoricalMatrix)xmlMatrix).createCharacterStateSet();
			}
			Character xmlChar = xmlMatrix.createCharacter(xmlCharacterStateSet);
			String mesCharacterName = mesData.getCharacterName(characterIndex);
			if ( null != mesCharacterName && ! mesCharacterName.equals("") ) {
				xmlChar.setLabel(mesCharacterName);
			}
			if ( mesDataType.equalsIgnoreCase(CategoricalData.DATATYPENAME) ) {
				CategoricalData data = ((CategoricalData)mesData);
				int maxStateIndex = data.maxStateWithName(characterIndex);
				for (int stateIndex = 0; stateIndex <= maxStateIndex; stateIndex++) {
					String symbol = String.valueOf(data.getSymbol(stateIndex));
					org.nexml.model.CharacterState state = xmlChar.getCharacterStateSet().createCharacterState(symbol);
					state.setSymbol(symbol);
					if (data.hasStateName(characterIndex, stateIndex)) {
						String stateLabel = data.getStateName(characterIndex, stateIndex);
						state.setLabel(stateLabel);
					}
				}
			}
			xmlCharacters.add(xmlChar);
		}
		
		// iterate over taxa
		for (int taxonIndex = 0; taxonIndex < mesData.getNumTaxa(); taxonIndex++) {
			CharacterState[] mesCharStates = mesData.getCharacterStateArray(taxonIndex, 0, mesNchar);
			Taxon mesTaxon = mesData.getTaxa().getTaxon(taxonIndex);
			OTU xmlTaxon = findEquivalentTaxon(mesTaxon,xmlMatrix.getOTUs());    			
			
			// iterate over characters
			for ( int characterIndex = 0; characterIndex < mesNchar; characterIndex++ ) {
				Character xmlChar = xmlCharacters.get(characterIndex);
				CharacterState mesState = mesCharStates[characterIndex];
				
				// data are "standard"
				if (mesDataType.equalsIgnoreCase(CategoricalData.DATATYPENAME)) {
					CharacterStateSet xmlStateSet = xmlChar.getCharacterStateSet();
					CategoricalData categoricalData = (CategoricalData)mesData;
					long stateAssignment = categoricalData.getState(characterIndex, taxonIndex);
					org.nexml.model.CharacterState xmlCharacterState = null;
					if (CategoricalState.hasMultipleStates(stateAssignment)) {
						Set<String> symbols = new HashSet<String>();
						for (int mesStateCode : CategoricalState.expand(stateAssignment)) {
							symbols.add(String.valueOf(categoricalData.getSymbol(mesStateCode)));
						}
						if (CategoricalState.isUncertain(stateAssignment)) {
							xmlCharacterState = findOrCreateUncertainStateSet(xmlStateSet, symbols);
						} else { //polymorphic
							xmlCharacterState = findOrCreatePolymorphicStateSet(xmlStateSet, symbols);
						}
					} else { // single state
						if ((!CategoricalState.isUnassigned(stateAssignment)) && (!CategoricalState.isInapplicable(stateAssignment))) {
							String symbol = String.valueOf(categoricalData.getSymbol(CategoricalState.getOnlyElement(stateAssignment)));
							xmlCharacterState = xmlStateSet.lookupCharacterStateBySymbol(symbol);
						} 
					}
					if (xmlCharacterState != null) {
						MatrixCell<org.nexml.model.CharacterState> xmlCell = (MatrixCell<org.nexml.model.CharacterState>) xmlMatrix.getCell(xmlTaxon, xmlChar);
						xmlCell.setValue(xmlCharacterState);
					}									
				} 
				
				// data are continuous
				else if (mesDataType.equalsIgnoreCase(ContinuousData.DATATYPENAME)) {
					MatrixCell<Double> xmlCell = (MatrixCell<Double>) xmlMatrix.getCell(xmlTaxon,xmlChar);
					xmlCell.setValue((Double)xmlMatrix.parseSymbol(mesState.toDisplayString(), xmlChar));
				}
				
				// data are molecular
				else if ( xmlMolecularDataTypeFor.containsKey(mesDataType) ) {
					MatrixCell<org.nexml.model.CharacterState> xmlCell = (MatrixCell<org.nexml.model.CharacterState>) xmlMatrix.getCell(xmlTaxon,xmlChar);
					
					// https://github.com/nexml/nexml.java/issues/14
					String mesSymbol = ((CategoricalState)mesState).toNEXUSString();
					xmlCell.setValue((org.nexml.model.CharacterState)((MolecularMatrix)xmlMatrix).parseSymbol(mesSymbol, xmlMolecularDataTypeFor.get(mesDataType)));
				}   
			}    			
		}
	}

	/*
	 * (non-Javadoc)
	 * @see mesquite.nexml.InterpretNEXML.NexmlBlockWriter#getThingInXmlBlock(org.nexml.model.NexmlWritable, int)
	 */
	@Override
	protected Annotatable getThingInXmlBlock(NexmlWritable xmlBlock, int index) {
		Matrix<?> xmlMatrix = (Matrix<?>)xmlBlock;
		return xmlMatrix.getCharacters().get(index);
	}

	private UncertainCharacterState findOrCreateUncertainStateSet(CharacterStateSet containingStateSet, Set<String> symbols) {
		for (org.nexml.model.CharacterState state : containingStateSet.getCharacterStates()) {
			if (state instanceof UncertainCharacterState) {
				UncertainCharacterState uncertainState = (UncertainCharacterState)state;
				if (containsMatchingStates(uncertainState, symbols)) {
					return uncertainState;
				}
			}
		}
		Set<org.nexml.model.CharacterState> memberStates = collectMatchingStates(containingStateSet, symbols);
		return containingStateSet.createUncertainCharacterState(this.nextMultipleStateSymbol++, memberStates);
	}

	private PolymorphicCharacterState findOrCreatePolymorphicStateSet(CharacterStateSet containingStateSet, Set<String> symbols) {
		for (org.nexml.model.CharacterState state : containingStateSet.getCharacterStates()) {
			if (state instanceof PolymorphicCharacterState) {
				PolymorphicCharacterState polymorphicState = (PolymorphicCharacterState)state;
				if (containsMatchingStates(polymorphicState, symbols)) {
					return polymorphicState;
				}
			}
		}
		Set<org.nexml.model.CharacterState> memberStates = collectMatchingStates(containingStateSet, symbols);
		return containingStateSet.createPolymorphicCharacterState(this.nextMultipleStateSymbol++, memberStates);
	}

	private boolean containsMatchingStates(CompoundCharacterState state, Set<String> symbols) {
		Set<String> containedSymbols = new HashSet<String>();
		for (org.nexml.model.CharacterState containedState : state.getStates()) {
			containedSymbols.add(containedState.getSymbol().toString());
		}
		return containedSymbols.equals(symbols);
	}

	private Set<org.nexml.model.CharacterState> collectMatchingStates(CharacterStateSet containingStateSet, Set<String> symbols) {
		Set<org.nexml.model.CharacterState> memberStates = new HashSet<org.nexml.model.CharacterState>();
		for (String symbol : symbols) {
			org.nexml.model.CharacterState member = containingStateSet.lookupCharacterStateBySymbol(symbol);
			if ( null != member ) {
				memberStates.add(member);
			} else {
				memberStates.add(containingStateSet.createCharacterState(symbol));	
			}
		}
		return memberStates;
	}

}
