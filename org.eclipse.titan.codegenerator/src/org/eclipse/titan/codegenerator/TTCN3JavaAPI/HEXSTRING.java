/******************************************************************************
 * Copyright (c) 2000-2018 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 *
 * Contributors:
 *   
 *   Keremi, Andras
 *   Eros, Levente
 *   Kovacs, Gabor
 *   Meszaros, Mate Robert
 *
 ******************************************************************************/

package org.eclipse.titan.codegenerator.TTCN3JavaAPI;

public class HEXSTRING extends BINARY_STRING implements Indexable<HEXSTRING> {

    public HEXSTRING() {
    }

    public HEXSTRING(String value) {
        super(value);
        Integer.parseInt(value, 16); //throws an exception if not legal
    }
    
    public HEXSTRING bitwiseNot(){
    	return new HEXSTRING(Integer.toHexString(generalBitwiseNot(fromHexString(value))));
    }

    public HEXSTRING bitwiseAnd(HEXSTRING b){
    	return new HEXSTRING(Integer.toHexString(generalBitwiseAnd(fromHexString(value), fromHexString(b.value))));
    }
    
    public HEXSTRING bitwiseOr(HEXSTRING b){
    	return new HEXSTRING(Integer.toHexString(generalBitwiseOr(fromHexString(value), fromHexString(b.value))));
    }
    
    public HEXSTRING bitwiseXor(HEXSTRING b){
    	return new HEXSTRING(Integer.toHexString(generalBitwiseXor(fromHexString(value), fromHexString(b.value))));
    }
    
	public String toString() {
		return toString("");
	}
	
	public String toString(String tabs){
		if(anyField) return "?";
		if(omitField) return "omit";
		if(anyOrOmitField) return "*";
		return "'" + new String(value) + "'H";
	}

	@Override
	public HEXSTRING get(int index) {
		return new HEXSTRING(new String(new byte[]{value[index]}));
	}

	@Override
	public void set(int index, HEXSTRING hexstring) {
		value[index] = hexstring.value[0];
	}
}
