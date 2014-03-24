















package ca.uhn.fhir.model.dstu.composite;

import java.util.*;
import ca.uhn.fhir.model.api.*;
import ca.uhn.fhir.model.api.annotation.*;

import ca.uhn.fhir.model.dstu.valueset.AddressUseEnum;
import ca.uhn.fhir.model.dstu.composite.CodingDt;
import ca.uhn.fhir.model.dstu.valueset.ContactSystemEnum;
import ca.uhn.fhir.model.dstu.valueset.ContactUseEnum;
import ca.uhn.fhir.model.dstu.valueset.EventTimingEnum;
import ca.uhn.fhir.model.dstu.valueset.IdentifierUseEnum;
import ca.uhn.fhir.model.dstu.valueset.NameUseEnum;
import ca.uhn.fhir.model.dstu.resource.Organization;
import ca.uhn.fhir.model.dstu.composite.PeriodDt;
import ca.uhn.fhir.model.dstu.valueset.QuantityCompararatorEnum;
import ca.uhn.fhir.model.dstu.composite.QuantityDt;
import ca.uhn.fhir.model.dstu.composite.ResourceReferenceDt;
import ca.uhn.fhir.model.dstu.valueset.UnitsOfTimeEnum;
import ca.uhn.fhir.model.dstu.resource.ValueSet;
import ca.uhn.fhir.model.primitive.Base64BinaryDt;
import ca.uhn.fhir.model.primitive.BooleanDt;
import ca.uhn.fhir.model.primitive.BoundCodeDt;
import ca.uhn.fhir.model.primitive.CodeDt;
import ca.uhn.fhir.model.primitive.DateTimeDt;
import ca.uhn.fhir.model.primitive.DecimalDt;
import ca.uhn.fhir.model.primitive.IntegerDt;
import ca.uhn.fhir.model.primitive.StringDt;
import ca.uhn.fhir.model.primitive.UriDt;

/**
 * HAPI/FHIR <b>Quantity</b> Datatype
 * (A measured or measurable amount)
 *
 * <p>
 * <b>Definition:</b>
 * A measured amount (or an amount that can potentially be measured). Note that measured amounts include amounts that are not precisely quantified, including amounts involving arbitrary units and floating currencies
 * </p> 
 *
 * <p>
 * <b>Requirements:</b>
 * Need to able to capture all sorts of measured values, even if the measured value are not precisely quantified. Values include exact measures such as 3.51g, customary units such as 3 tablets, and currencies such as $100.32USD
 * </p> 
 */
@DatatypeDef(name="Quantity") 
public class QuantityDt 
        extends  BaseElement         implements ICompositeDatatype  {


	@Child(name="value", type=DecimalDt.class, order=0, min=0, max=1)	
	@Description(
		shortDefinition="Numerical value (with implicit precision)",
		formalDefinition="The value of the measured amount. The value includes an implicit precision in the presentation of the value"
	)
	private DecimalDt myValue;
	
	@Child(name="comparator", type=CodeDt.class, order=1, min=0, max=1)	
	@Description(
		shortDefinition="< | <= | >= | > - how to understand the value",
		formalDefinition="How the value should be understood and represented - whether the actual value is greater or less than the stated value due to measurement issues. E.g. if the comparator is \"<\" , then the real value is < stated value"
	)
	private BoundCodeDt<QuantityCompararatorEnum> myComparator;
	
	@Child(name="units", type=StringDt.class, order=2, min=0, max=1)	
	@Description(
		shortDefinition="Unit representation",
		formalDefinition="A human-readable form of the units"
	)
	private StringDt myUnits;
	
	@Child(name="system", type=UriDt.class, order=3, min=0, max=1)	
	@Description(
		shortDefinition="System that defines coded unit form",
		formalDefinition="The identification of the system that provides the coded form of the unit"
	)
	private UriDt mySystem;
	
	@Child(name="code", type=CodeDt.class, order=4, min=0, max=1)	
	@Description(
		shortDefinition="Coded form of the unit",
		formalDefinition="A computer processable form of the units in some unit representation system"
	)
	private CodeDt myCode;
	
	/**
	 * Constructor
	 */
	public QuantityDt() {
		// nothing
	}

	/**
	 * Constructor
	 */
	public QuantityDt(double theValue) {
		setValue(theValue);
	}

	/**
	 * Constructor
	 */
	public QuantityDt(long theValue) {
		setValue(theValue);
	}

	/**
	 * Constructor
	 */
	public QuantityDt(double theValue, String theUnits) {
		setValue(theValue);
		setUnits(theUnits);
	}

	/**
	 * Constructor
	 */
	public QuantityDt(long theValue, String theUnits) {
		setValue(theValue);
		setUnits(theUnits);
	}


	/**
	 * Constructor
	 */
	public QuantityDt(QuantityCompararatorEnum theCompararatorEnum, double theValue, String theUnits) {
		setComparator(theCompararatorEnum);
		setValue(theValue);
		setUnits(theUnits);
	}

	/**
	 * Constructor
	 */
	public QuantityDt(QuantityCompararatorEnum theCompararatorEnum, long theValue, String theUnits) {
		setComparator(theCompararatorEnum);
		setValue(theValue);
		setUnits(theUnits);
	}

	@Override
	public boolean isEmpty() {
		return super.isBaseEmpty() && ca.uhn.fhir.util.ElementUtil.isEmpty(  myValue,  myComparator,  myUnits,  mySystem,  myCode);
	}
	
	@Override
	public java.util.List<IElement> getAllPopulatedChildElements() {
		return getAllPopulatedChildElementsOfType(null);
	}

	@Override
	public <T extends IElement> List<T> getAllPopulatedChildElementsOfType(Class<T> theType) {
		return ca.uhn.fhir.util.ElementUtil.allPopulatedChildElements(theType, myValue, myComparator, myUnits, mySystem, myCode);
	}

	/**
	 * Gets the value(s) for <b>value</b> (Numerical value (with implicit precision)).
	 * creating it if it does
	 * not exist. Will not return <code>null</code>.
	 *
     * <p>
     * <b>Definition:</b>
     * The value of the measured amount. The value includes an implicit precision in the presentation of the value
     * </p> 
	 */
	public DecimalDt getValue() {  
		if (myValue == null) {
			myValue = new DecimalDt();
		}
		return myValue;
	}

	/**
	 * Sets the value(s) for <b>value</b> (Numerical value (with implicit precision))
	 *
     * <p>
     * <b>Definition:</b>
     * The value of the measured amount. The value includes an implicit precision in the presentation of the value
     * </p> 
	 */
	public QuantityDt setValue(DecimalDt theValue) {
		myValue = theValue;
		return this;
	}

 	/**
	 * Sets the value for <b>value</b> (Numerical value (with implicit precision))
	 *
     * <p>
     * <b>Definition:</b>
     * The value of the measured amount. The value includes an implicit precision in the presentation of the value
     * </p> 
	 */
	public QuantityDt setValue( long theValue) {
		myValue = new DecimalDt(theValue); 
		return this; 
	}

	/**
	 * Sets the value for <b>value</b> (Numerical value (with implicit precision))
	 *
     * <p>
     * <b>Definition:</b>
     * The value of the measured amount. The value includes an implicit precision in the presentation of the value
     * </p> 
	 */
	public QuantityDt setValue( double theValue) {
		myValue = new DecimalDt(theValue); 
		return this; 
	}

	/**
	 * Sets the value for <b>value</b> (Numerical value (with implicit precision))
	 *
     * <p>
     * <b>Definition:</b>
     * The value of the measured amount. The value includes an implicit precision in the presentation of the value
     * </p> 
	 */
	public QuantityDt setValue( java.math.BigDecimal theValue) {
		myValue = new DecimalDt(theValue); 
		return this; 
	}

 
	/**
	 * Gets the value(s) for <b>comparator</b> (< | <= | >= | > - how to understand the value).
	 * creating it if it does
	 * not exist. Will not return <code>null</code>.
	 *
     * <p>
     * <b>Definition:</b>
     * How the value should be understood and represented - whether the actual value is greater or less than the stated value due to measurement issues. E.g. if the comparator is \"<\" , then the real value is < stated value
     * </p> 
	 */
	public BoundCodeDt<QuantityCompararatorEnum> getComparator() {  
		if (myComparator == null) {
			myComparator = new BoundCodeDt<QuantityCompararatorEnum>(QuantityCompararatorEnum.VALUESET_BINDER);
		}
		return myComparator;
	}

	/**
	 * Sets the value(s) for <b>comparator</b> (< | <= | >= | > - how to understand the value)
	 *
     * <p>
     * <b>Definition:</b>
     * How the value should be understood and represented - whether the actual value is greater or less than the stated value due to measurement issues. E.g. if the comparator is \"<\" , then the real value is < stated value
     * </p> 
	 */
	public QuantityDt setComparator(BoundCodeDt<QuantityCompararatorEnum> theValue) {
		myComparator = theValue;
		return this;
	}

	/**
	 * Sets the value(s) for <b>comparator</b> (< | <= | >= | > - how to understand the value)
	 *
     * <p>
     * <b>Definition:</b>
     * How the value should be understood and represented - whether the actual value is greater or less than the stated value due to measurement issues. E.g. if the comparator is \"<\" , then the real value is < stated value
     * </p> 
	 */
	public QuantityDt setComparator(QuantityCompararatorEnum theValue) {
		getComparator().setValueAsEnum(theValue);
		return this;
	}

  
	/**
	 * Gets the value(s) for <b>units</b> (Unit representation).
	 * creating it if it does
	 * not exist. Will not return <code>null</code>.
	 *
     * <p>
     * <b>Definition:</b>
     * A human-readable form of the units
     * </p> 
	 */
	public StringDt getUnits() {  
		if (myUnits == null) {
			myUnits = new StringDt();
		}
		return myUnits;
	}

	/**
	 * Sets the value(s) for <b>units</b> (Unit representation)
	 *
     * <p>
     * <b>Definition:</b>
     * A human-readable form of the units
     * </p> 
	 */
	public QuantityDt setUnits(StringDt theValue) {
		myUnits = theValue;
		return this;
	}

 	/**
	 * Sets the value for <b>units</b> (Unit representation)
	 *
     * <p>
     * <b>Definition:</b>
     * A human-readable form of the units
     * </p> 
	 */
	public QuantityDt setUnits( String theString) {
		myUnits = new StringDt(theString); 
		return this; 
	}

 
	/**
	 * Gets the value(s) for <b>system</b> (System that defines coded unit form).
	 * creating it if it does
	 * not exist. Will not return <code>null</code>.
	 *
     * <p>
     * <b>Definition:</b>
     * The identification of the system that provides the coded form of the unit
     * </p> 
	 */
	public UriDt getSystem() {  
		if (mySystem == null) {
			mySystem = new UriDt();
		}
		return mySystem;
	}

	/**
	 * Sets the value(s) for <b>system</b> (System that defines coded unit form)
	 *
     * <p>
     * <b>Definition:</b>
     * The identification of the system that provides the coded form of the unit
     * </p> 
	 */
	public QuantityDt setSystem(UriDt theValue) {
		mySystem = theValue;
		return this;
	}

 	/**
	 * Sets the value for <b>system</b> (System that defines coded unit form)
	 *
     * <p>
     * <b>Definition:</b>
     * The identification of the system that provides the coded form of the unit
     * </p> 
	 */
	public QuantityDt setSystem( String theUri) {
		mySystem = new UriDt(theUri); 
		return this; 
	}

 
	/**
	 * Gets the value(s) for <b>code</b> (Coded form of the unit).
	 * creating it if it does
	 * not exist. Will not return <code>null</code>.
	 *
     * <p>
     * <b>Definition:</b>
     * A computer processable form of the units in some unit representation system
     * </p> 
	 */
	public CodeDt getCode() {  
		if (myCode == null) {
			myCode = new CodeDt();
		}
		return myCode;
	}

	/**
	 * Sets the value(s) for <b>code</b> (Coded form of the unit)
	 *
     * <p>
     * <b>Definition:</b>
     * A computer processable form of the units in some unit representation system
     * </p> 
	 */
	public QuantityDt setCode(CodeDt theValue) {
		myCode = theValue;
		return this;
	}

 	/**
	 * Sets the value for <b>code</b> (Coded form of the unit)
	 *
     * <p>
     * <b>Definition:</b>
     * A computer processable form of the units in some unit representation system
     * </p> 
	 */
	public QuantityDt setCode( String theCode) {
		myCode = new CodeDt(theCode); 
		return this; 
	}

 



}