package ca.uhn.example.base;

import java.util.List;
import java.util.Optional;

import org.apache.ignite.binary.BinaryObjectException;
import org.apache.ignite.binary.BinaryReader;
import org.apache.ignite.binary.BinarySerializer;
import org.apache.ignite.binary.BinaryWriter;
import org.apache.ignite.internal.binary.BinaryClassDescriptor;
import org.apache.ignite.internal.binary.BinaryFieldAccessor;
import org.apache.ignite.internal.binary.BinaryWriterExImpl;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.BaseResource;

import ca.uhn.fhir.context.BaseRuntimeChildDefinition;
import ca.uhn.fhir.context.BaseRuntimeElementDefinition;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.RuntimeResourceDefinition;
import ca.uhn.fhir.model.api.IElement;

public class FhirBinarySerializer implements BinarySerializer {
	public static FhirContext globalFhirContext;
	
	void writePrimitiveType(BaseRuntimeChildDefinition def, IBase obj, BinaryWriter writer) {
		String field = def.getElementName();
		if(obj instanceof IPrimitiveType && def.getValidChildNames().size() <=1 ) {
    		IPrimitiveType o = (IPrimitiveType)obj;
    		if(o.hasValue())
    			writer.writeString(field,o.getValueAsString());
    		else 
    			writer.writeString(field,null);
    	}
		else {
			writer.writeObject(field,obj);
		}
	}
	
	 /** {@inheritDoc} */
    @Override public void writeBinary(Object obj, BinaryWriter writer) throws BinaryObjectException {
    	if(obj instanceof IPrimitiveType) {
    		IPrimitiveType o = (IPrimitiveType)obj;
    		if(o.hasValue())
    			writer.rawWriter().writeString(o.getValueAsString());
    		else 
    			writer.rawWriter().writeString(null);
    	}
    	else if(obj instanceof IBaseResource){
    		IBaseResource resource = (IBaseResource) obj;
    		
    		RuntimeResourceDefinition def = globalFhirContext.getResourceDefinition(resource.fhirType());
    		//IIdType id = resource.getIdElement();    		
    		//writer.writeString("id", id.getValueAsString());    		  		
    		for (BaseRuntimeChildDefinition child : def.getChildren()) {
    			//child.    			
    			if(child.getMax()!=1) { //List
    				writer.writeObject(child.getElementName(),child.getAccessor().getValues(resource));
    			}
    			else {
    				Optional<IBase> v = child.getAccessor().getFirstValueOrNull(resource);
    				if(v.isPresent()) {
    					writePrimitiveType(child,v.get(),writer);
    				}
    				else {
    					writer.writeObject(child.getElementName(),null);
    				}
    			}
    		}
            
    	}
    	else if(obj instanceof IElement) {
    		IElement element = (IElement) obj;
    		BaseRuntimeElementDefinition<?> def = globalFhirContext.getElementDefinition(element.getClass());
    		if(def==null) {    			
    			return;
    		}
    		for (BaseRuntimeChildDefinition child : def.getChildren()) {
    			//child.    			
    			if(child.getMax()!=1) { //List
    				writer.writeObject(child.getElementName(),child.getAccessor().getValues(element));
    			}
    			else {
    				Optional<IBase> v = child.getAccessor().getFirstValueOrNull(element);
    				if(v.isPresent()) {
    					writePrimitiveType(child,v.get(),writer);
    				}
    				else {
    					writer.writeObject(child.getElementName(),null);
    				}
    			}
    		}
    	}
    }    
    
    public void readPrimitiveType(org.hl7.fhir.r4.model.Base obj, BaseRuntimeChildDefinition def, Object readValue,BinaryReader reader) throws BinaryObjectException {
    	if(readValue!=null) {
    		String field = def.getElementName();
    		if(field.equals("id")) {
    			obj.setIdBase(readValue.toString());
    			return;
    		}
    		org.hl7.fhir.r4.model.Base child = obj.makeProperty(field.hashCode(), field);
    		if(child==null) {
    			child = obj.addChild(field);
    		}    		
    		
    		if(child instanceof IPrimitiveType) {
        		IPrimitiveType o = (IPrimitiveType)child;
        		if(readValue instanceof IPrimitiveType) {
        			o.setValue(((IPrimitiveType) readValue).getValue());
        		}
        		else if(readValue instanceof String) {
        			o.setValueAsString(readValue.toString());
        		}
        		else if(readValue instanceof Number) {
        			o.setValue(readValue);
        		}
        		else {
        			o.setValue(readValue);
        		}        		
        	} 
    		else if(child!=null){
    			readBinary(child,reader);    			
    		}
      	}
    	
    }

    /** {@inheritDoc} */
    @Override public void readBinary(Object obj, BinaryReader reader) throws BinaryObjectException {
    	if(obj instanceof IPrimitiveType) {
    		IPrimitiveType o = (IPrimitiveType)obj;
    		o.setValueAsString(reader.rawReader().readString());
    	}
    	else if(obj instanceof IBaseResource){
    		//obj = reader.rawReader().readObject();
    		IBaseResource resource = (IBaseResource) obj;
    		RuntimeResourceDefinition def = globalFhirContext.getResourceDefinition(resource.fhirType());
    		
    		//String id = reader.readString("id");
    		//resource.setId(id);
    		for (BaseRuntimeChildDefinition child : def.getChildren()) {
    			//child. 
    			if(child.getMax()==1) {
    				Object value = reader.readObject(child.getElementName());
    				if(value!=null) {
    					if(value instanceof IBase) {
    						child.getMutator().setValue(resource, (IBase)value);    						
    					}
    					else {
    						readPrimitiveType((org.hl7.fhir.r4.model.Base)resource,child,value,reader);
    					}
    				}
    			}
    			else {
	    			Object value = reader.readObject(child.getElementName());
	    			if(value instanceof IBase[]) {
	    				IBase[] list = (IBase[]) value;
	    				for(IBase item: list) {
	    					child.getMutator().addValue(resource, item);
	    				}
	    			}
	    			else if(value instanceof List<?>) {
	    				List<IBase> list = (List<IBase>) value;
	    				for(IBase item: list) {
	    					child.getMutator().addValue(resource, item);
	    				}
	    			}
    			}
    		}
    	}
    	else if(obj instanceof IElement){
    		//obj = reader.rawReader().readObject();
    		IElement element = (IElement) obj;
    		BaseRuntimeElementDefinition<?> def = globalFhirContext.getElementDefinition(element.getClass());
    		if(def==null) return;    		
    		for (BaseRuntimeChildDefinition child : def.getChildren()) {    			
    			//child. 
    			if(child.getMax()==1) {
    				Object value = reader.readObject(child.getElementName());
    				if(value!=null) {
    					if(value instanceof IBase) {
    						child.getMutator().setValue(element, (IBase)value);    						
    					}
    					else{
    						readPrimitiveType((org.hl7.fhir.r4.model.Base)element,child,value,reader);
    					}
    				}
    			}
    			else {
	    			Object value = reader.readObject(child.getElementName());
	    			if(value instanceof IBase[]) {
	    				IBase[] list = (IBase[]) value;
	    				for(IBase item: list) {
	    					child.getMutator().addValue(element, item);
	    				}
	    			}
	    			else if(value instanceof List<?>) {
	    				List<IBase> list = (List<IBase>) value;
	    				for(IBase item: list) {
	    					child.getMutator().addValue(element, item);
	    				}
	    			}
    			}
    		}
    	}
    }
}
