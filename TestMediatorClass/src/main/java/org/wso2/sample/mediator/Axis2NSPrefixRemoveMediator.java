/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *   * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.wso2.sample.mediator;

import java.util.Iterator;
import javax.xml.namespace.QName;
import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.om.OMNode;
import org.apache.axiom.om.OMText;
import org.apache.axiom.soap.SOAPBody;
import org.apache.axiom.soap.SOAPFault;
import org.apache.synapse.MessageContext;
import org.apache.synapse.mediators.AbstractMediator;

/**
 * 
 * This class removes the additional axis2 namespace prefixes and creates a new
 * OMElement
 * 
 */
public class Axis2NSPrefixRemoveMediator extends AbstractMediator {

	private static final String AXIS2_NAMESPACE_PREFIX = "axis2ns";
	private static final String CLONED_ROOT = "ClonedDetail";

	public boolean mediate(MessageContext synCtx) {
	
		SOAPBody soapBody = synCtx.getEnvelope().getBody();
		
		//Check for a Fault
		if(soapBody.hasFault()){
			SOAPFault  faultNode = soapBody.getFault();
			
			// Retrieves the Detail Element
			OMElement detailNode = faultNode.getDetail();
			
			if (detailNode.getChildElements().hasNext()) {

				Iterator<?> complexElement = detailNode.getChildElements();
				OMFactory fac = OMAbstractFactory.getOMFactory();
				
				// Creates a clone from detail element
				OMElement clonedRoot = fac.createOMElement(new QName(CLONED_ROOT));
				
				//For every child creates a cloned element without axis2ns prefix
				getChildrenRecursively(fac, clonedRoot, complexElement);

				if (clonedRoot.getChildElements().hasNext()) {
					
					// Detach the fault body
					Iterator<?> children = detailNode.getChildElements();
					while (children.hasNext()) {
						((OMNode) children.next()).detach();
					}
					
					// Attach the new fault body
					Iterator<?> clonedChildren = clonedRoot.getChildElements();
					while (clonedChildren.hasNext()) {
						detailNode.addChild((OMElement) clonedChildren.next());
					}
				}
		    }
		}
	
		return true;
	}

	/**
	 * 
	 * @param fac OMFactory
	 * @param root root element of the detail block
	 * @param complexElement children of the detail block
	 * @return final list of elements
	 */
	private OMElement getChildrenRecursively(OMFactory fac, OMElement root, Iterator<?> complexElement) {

		while (complexElement.hasNext()) {

			Object next = complexElement.next();
			if  (next instanceof OMElement) {
				
				OMElement element = (OMElement) next;	
				// Creates the new OMElement excluding additional namespace prefixes
				OMElement duplicateElement = createNewOMElement(fac, element);
	
				if (element.getChildElements().hasNext()) {
					getChildrenRecursively(fac, duplicateElement, element.getChildElements());
					
					//Add to root at the end of populating the child
					root.addChild(duplicateElement);
				} else {
					// If no more children add it to root
					root.addChild(duplicateElement);
				}
			}
		}

		return root;
	}

	/**
	 * 
	 * @param fac OMFactory
	 * @param element complex element
	 * @return duplicated new OMElement
	 */
	private OMElement createNewOMElement(OMFactory fac, OMElement element) {

		OMElement duplicateElement = null;
		String elementText = element.getText();

		// Set Namespace
		duplicateElement = setNameSpaces(fac, element);
		// Set Attributes
		setAttributes(element, duplicateElement);
		// Set text
		setTextValues(fac, duplicateElement, elementText);

		return duplicateElement;

	}

	/**
	 * 
	 * @param fac OMFactory
	 * @param rootElement newly created OMElement
	 * @param elementText Text of the element
	 */
	private void setTextValues(OMFactory fac, OMElement rootElement, String elementText) {
		// Includes the text as a child
		if (elementText != null && !elementText.isEmpty()) {
			OMText omText = fac.createOMText(elementText);
			rootElement.addChild(omText);
		}
	}

	/**
	 * 
	 * @param element complex element
	 * @param tempElement newly created OMElement
	 */
	private void setAttributes(OMElement element, OMElement tempElement) {
		// Get the attributes
		Iterator<?> allAttributes = element.getAllAttributes();

		while (allAttributes.hasNext()) {
			tempElement.addAttribute((OMAttribute) allAttributes.next());
		}
	}

	/**
	 * 
	 * @param fac OMFactory
	 * @param tempElement newly created OMElement
	 * @return OMElement without axis2 namespace prefix
	 */
	private OMElement setNameSpaces(OMFactory fac, OMElement tempElement) {

		String name = tempElement.getLocalName();
		OMNamespace namespace = tempElement.getNamespace();

		if (name != null && namespace != null) {

			String namespaceURI = namespace.getNamespaceURI();
			String namespacePrefix = namespace.getPrefix();
			if (namespacePrefix.startsWith(AXIS2_NAMESPACE_PREFIX)) {
				// Removes additional namespaces
				String emptyNamespacePrefix = namespace.getPrefix().replace(namespacePrefix, "");
				OMNamespace modifiedNamespace = fac.createOMNamespace(namespaceURI, emptyNamespacePrefix);
				// Creates the final OMElement with correct namesapce
				tempElement = fac.createOMElement(name, modifiedNamespace);
			}
		}else{
			tempElement = fac.createOMElement(name, namespace);
		}
		return tempElement;
	}

}
