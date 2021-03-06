/******************************************************************************
 * Copyright (c) 2000-2018 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 *
 * Contributors:
 *
 *   Meszaros, Mate Robert
 *
 ******************************************************************************/

package org.eclipse.titan.codegenerator.template;

import org.eclipse.titan.codegenerator.Scope;
import org.eclipse.titan.codegenerator.Writable;
import org.eclipse.titan.designer.AST.IVisitableNode;

public class UnknownValueParser extends TemplateValueParser {
	public UnknownValueParser(Scope parent, ValueHolder holder, String type) {
		super(parent, holder);
		value = new Value(type, Writable.NULL);
	}

	@Override
	public Scope process(IVisitableNode node) {
		return this;
	}
}
