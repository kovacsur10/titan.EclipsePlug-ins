/******************************************************************************
 * Copyright (c) 2000-2018 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST.TTCN3.types;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.titan.designer.compiler.JavaGenData;

/**
 * @author Farkas Izabella Ingrid
 * */
public final class EnumeratedGenerator {

	private static final String UNKNOWN_VALUE = "UNKNOWN_VALUE";
	private static final String UNBOUND_VALUE ="UNBOUND_VALUE";

	public static class Enum_field {
		private final String name;
		private final String displayName;

		private final long value;

		public Enum_field(final String aName, final String aDisplayName, final long aValue) {
			name = aName;
			displayName = aDisplayName;
			value = aValue;
		}
	}

	public static class Enum_Defs {
		private final List<Enum_field> items;
		private final String name;
		private final String displayName;
		private final String templateName;
		private final boolean hasRaw;
		private long firstUnused = -1;  //first unused value for this enum type
		private long secondUnused = -1; //second unused value for this enum type

		public Enum_Defs(final List<Enum_field> aItems, final String aName, final String aDisplayName, final String aTemplateName, final boolean aHasRaw){
			items = aItems;
			name = aName;
			displayName = aDisplayName;
			templateName = aTemplateName;
			hasRaw = aHasRaw;
			calculateFirstAndSecondUnusedValues();
		}

		//This function supposes that the enum class is already checked and error free
		private void calculateFirstAndSecondUnusedValues() {
			if( firstUnused != -1 ) {
				return; //function already have been called
			}
			final Map<Long, Enum_field> valueMap = new HashMap<Long, Enum_field>(items.size());

			for( int i = 0, size = items.size(); i < size; i++) {
				final Enum_field item = items.get(i);
				valueMap.put(item.value, item);
			}

			long firstUnused = Long.valueOf(0);
			while (valueMap.containsKey(firstUnused)) {
				firstUnused++;
			}

			this.firstUnused = firstUnused;
			firstUnused++;
			while (valueMap.containsKey(firstUnused)) {
				firstUnused++;
			}
			secondUnused = firstUnused;
			valueMap.clear();
		}
	}

	private EnumeratedGenerator() {
		// private to disable instantiation
	}

	public static void generateValueClass(final JavaGenData aData, final StringBuilder source, final Enum_Defs e_defs ) {
		aData.addBuiltinTypeImport("TitanInteger");
		aData.addBuiltinTypeImport( "Base_Type" );
		aData.addBuiltinTypeImport( "Base_Template" );
		aData.addBuiltinTypeImport("Param_Types.Module_Parameter");
		aData.addBuiltinTypeImport("RAW");
		aData.addBuiltinTypeImport("RAW.RAW_enc_tr_pos");
		aData.addBuiltinTypeImport("RAW.RAW_enc_tree");
		aData.addBuiltinTypeImport("TTCN_Buffer");
		aData.addBuiltinTypeImport("TTCN_EncDec.coding_type");
		aData.addBuiltinTypeImport("TTCN_EncDec.error_type");
		aData.addBuiltinTypeImport("TTCN_EncDec.raw_order_t");
		aData.addBuiltinTypeImport("TTCN_EncDec_ErrorContext");
		aData.addBuiltinTypeImport("Text_Buf");
		aData.addBuiltinTypeImport("TtcnError");
		aData.addImport( "java.text.MessageFormat" );

		final boolean rawNeeded = e_defs.hasRaw; //TODO can be forced optionally if needed

		//		if(needsAlias()) { ???
		source.append(MessageFormat.format("\tpublic static class {0} extends Base_Type '{'\n", e_defs.name));
		//== enum_type ==
		source.append("\t\tpublic enum enum_type {\n");
		final StringBuilder helper = new StringBuilder();
		final int size = e_defs.items.size();
		Enum_field item = null;
		for ( int i=0; i<size; i++) {
			item = e_defs.items.get(i);
			source.append(MessageFormat.format("\t\t\t{0} (", item.name));
			source.append(item.value);
			source.append("),\n");
			helper.append("\t\t\t\tcase ").append(item.value).append(": ");
			helper.append(" return ").append(MessageFormat.format("{0}", item.name)).append(";\n");
		}

		source.append(MessageFormat.format("\t\t\t{0}({1}),\n", UNKNOWN_VALUE, e_defs.firstUnused));
		source.append(MessageFormat.format("\t\t\t{0}({1});\n", UNBOUND_VALUE, e_defs.secondUnused));
		helper.append("\t\t\t\tcase ").append(MessageFormat.format("{0}", e_defs.firstUnused)).append(": ");
		helper.append(" return ").append("UNKNOWN_VALUE").append(";\n");
		helper.append("\t\t\t\tcase ").append(MessageFormat.format("{0}", e_defs.secondUnused)).append(": ");
		helper.append(" return ").append("UNBOUND_VALUE").append(";\n\n");

		source.append("\t\t\tprivate int enum_num;\n");

		//== constructors for enum_type ==

		source.append("\t\t\tenum_type(final int num) {\n");
		source.append("\t\t\t\tthis.enum_num = num;\n");
		source.append("\t\t\t}\n\n");

		source.append("\t\t\tprivate int getInt() {\n");
		source.append("\t\t\t\treturn enum_num;\n");
		source.append("\t\t\t}\n\n");
		generateValueEnumGetValue(source, helper);
		source.append("\t\t\t}\n\n");
		// end of enum_type

		//== enum_value ==
		source.append("\t\tpublic enum_type enum_value;\n");

		source.append("\t\t//===Constructors===;\n");
		generateValueConstructors(aData, source,e_defs.name);

		//== functions ==
		source.append("\t\t//===Methods===;\n");
		generateValueoperator_assign(aData, source, e_defs.name);
		generateValueoperator_equals(aData, source, e_defs.name, e_defs.displayName);
		generateValueoperator_not_equals(aData, source, e_defs.name);
		generateValueIsLessThan(aData, source, e_defs.name);
		generateValueIsLessThanOrEqual(aData, source, e_defs.name);
		generateValueIsGreaterThan(aData, source, e_defs.name);
		generateValueIsGreaterThanOrEqual(aData, source, e_defs.name);
		generateValueIsPresent(source);
		generateValueIsBound(source);
		generateValueIsValue(source);
		generateValueCleanUp(source);
		generateValueIsValidEnum(source, e_defs.name);
		generateValueIntToEnum(source);
		generateValueEnumToInt(source, e_defs.name);
		generateValueStrToEnum(source, e_defs);
		generateValueEnumToStr(source);
		generateValueAsInt(source);
		generateValueFromInt(source);
		generateValueToString(source);
		generateLog(source);
		generateValueSetParam(source, e_defs.displayName);
		generateValueEncodeDecodeText(source, e_defs.displayName);
		generateValueEncodeDecode(aData, source, e_defs, rawNeeded);
		source.append("\t}\n");
	}

	public static void generateTemplateClass(final JavaGenData aData, final StringBuilder source, final Enum_Defs e_defs){
		aData.addBuiltinTypeImport("TitanInteger");
		aData.addBuiltinTypeImport( "Base_Type" );
		aData.addBuiltinTypeImport( "Base_Template" );
		aData.addBuiltinTypeImport("Text_Buf");
		aData.addBuiltinTypeImport("TtcnError");
		aData.addCommonLibraryImport("TTCN_Logger");
		aData.addImport( "java.text.MessageFormat" );
		aData.addImport("java.util.ArrayList");

		source.append(MessageFormat.format("\tpublic static class {0}_template extends Base_Template '{'\n", e_defs.name, e_defs.templateName));

		generateTemplateDeclaration(source, e_defs.name);
		generatetemplateCopyTemplate(source, e_defs.name);
		generateTemplateConstructors(aData, source, e_defs.name);
		generateTemplateCleanUp(source);
		generateTemplateIsBound(source);
		generateTemplateIsValue(source, e_defs.name);
		generateTemplateoperator_assign(aData, source,e_defs.name);
		generateTemplateMatch(aData, source,  e_defs.name);
		generateTemplateValueOf(source, e_defs.name);
		generateTemplateSetType(source,  e_defs.name);
		generateTemplateListItem(source, e_defs.name);
		generateTemplateMatchOmit(source);
		generateTemplateLog(source, e_defs.name);
		generateTemplateLogMatch(aData, source, e_defs.name, e_defs.displayName);
		generateTemplateSetParam(source, e_defs.name, e_defs.displayName);
		generateTemplateEncodeDecodeText(source, e_defs.name, e_defs.displayName);
		generateTemplateCheckRestriction(source, e_defs.displayName);

		source.append("\t}\n");
	}

	//===

	private static void generateValueToString(final StringBuilder source) {
		source.append("\t\t/** \n");
		source.append("\t\t * Do not use this function!<br>\n");
		source.append("\t\t * It is provided by Java and currently used for debugging.\n");
		source.append("\t\t * But it is not part of the intentionally provided interface,\n");
		source.append("\t\t *   and so can be changed without notice. \n");
		source.append("\t\t * <p>\n");
		source.append("\t\t * JAVA DESCRIPTION:\n");
		source.append("\t\t * <p>\n");
		source.append("\t\t * {@inheritDoc}\n");
		source.append("\t\t *  */\n");
		source.append("\t\tpublic String toString() {\n");
		source.append("\t\t\treturn enum_value.name() + \"(\"+enum_value.enum_num+\")\";\n");
		source.append("\t\t}\n\n");
	}

	private static void generateValueEnumGetValue(final StringBuilder source, final StringBuilder helper) {
		source.append("\t\t\tpublic static enum_type getValue(final int index) {\n");
		source.append("\t\t\t\tswitch (index) {\n");
		source.append(helper);
		source.append("\t\t\t\tdefault:\n");
		source.append("\t\t\t\t\treturn null;\n");
		source.append("\t\t\t\t}\n");
		source.append("\t\t\t}\n\n");
	}

	private static void generateValueConstructors(final JavaGenData aData, final StringBuilder source,final String name) {
		//empty
		if ( aData.isDebug() ) {
			source.append( "\t\t/**\n" );
			source.append( "\t\t * Initializes to unbound value.\n" );
			source.append( "\t\t * */\n" );
		}
		source.append(MessageFormat.format("\t\tpublic {0}() '{'\n", name));
		source.append(MessageFormat.format("\t\t\tenum_value = enum_type.{0};\n", UNBOUND_VALUE));
		source.append("\t\t}\n\n");

		// own type
		if ( aData.isDebug() ) {
			source.append( "\t\t/**\n" );
			source.append( "\t\t * Initializes to a given value.\n" );
			source.append( "\t\t *\n" );
			source.append( "\t\t * @param otherValue\n" );
			source.append( "\t\t *                the value to initialize to.\n" );
			source.append( "\t\t * */\n" );
		}
		source.append(MessageFormat.format("\t\tpublic {0}(final {0} otherValue) '{'\n", name));
		source.append(MessageFormat.format("\t\t\tenum_value = otherValue.enum_value;\n", name));
		source.append("\t\t}\n\n");

		// enum_type
		if ( aData.isDebug() ) {
			source.append( "\t\t/**\n" );
			source.append( "\t\t * Initializes to a given value.\n" );
			source.append( "\t\t *\n" );
			source.append( "\t\t * @param otherValue\n" );
			source.append( "\t\t *                the value to initialize to.\n" );
			source.append( "\t\t * */\n" );
		}
		source.append(MessageFormat.format("\t\tpublic {0}(final {0}.enum_type otherValue ) '{'\n", name));
		source.append("\t\t\tenum_value = otherValue;\n");
		source.append("\t\t}\n\n");

		//arg int
		if ( aData.isDebug() ) {
			source.append( "\t\t/**\n" );
			source.append( "\t\t * Initializes to a given value.\n" );
			source.append( "\t\t *\n" );
			source.append( "\t\t * @param otherValue\n" );
			source.append( "\t\t *                the value to initialize to.\n" );
			source.append( "\t\t * */\n" );
		}
		source.append(MessageFormat.format("\t\tpublic {0}(final int otherValue) '{'\n", name));
		source.append("\t\t\tif (!is_valid_enum(otherValue)) {\n");
		source.append(MessageFormat.format("\t\t\t\tthrow new TtcnError(MessageFormat.format(\"Initializing a variable of enumerated type `{0}'' with invalid numeric value {1} .\", otherValue));\n", name));
		source.append("\t\t\t}\n\n");
		source.append(MessageFormat.format("\t\t\tenum_value =  enum_type.getValue(otherValue);\n", name));
		source.append("\t\t}\n\n");
	}

	private static void generateValueIsValidEnum(final StringBuilder source, final String name) {
		source.append("\t\tpublic static boolean is_valid_enum(final int otherValue) {\n");
		source.append("\t\t\tfinal enum_type helper =  enum_type.getValue(otherValue);\n");
		source.append("\t\t\treturn helper != null && helper != enum_type.UNKNOWN_VALUE && helper != enum_type.UNBOUND_VALUE ;\n");
		source.append("\t\t}\n\n");

		source.append("\t\tpublic static boolean is_valid_enum(final enum_type otherValue) {\n");
		source.append("\t\t\treturn otherValue != enum_type.UNKNOWN_VALUE && otherValue != enum_type.UNBOUND_VALUE ;\n");
		source.append("\t\t}\n\n");
	}

	private static void generateValueEnumToStr(final StringBuilder source) {
		source.append("\t\tpublic static String enum_to_str(final enum_type enumPar) {\n");
		source.append("\t\t\treturn enumPar.name();\n");
		source.append("\t\t}\n\n");
	}

	private static void generateLog(final StringBuilder source) {
		source.append("\t\t@Override\n");
		source.append("\t\tpublic void log() {\n");
		source.append("\t\t\tif (enum_value == enum_type.UNBOUND_VALUE) {\n");
		source.append("\t\t\t\tTTCN_Logger.log_event_unbound();\n");
		source.append("\t\t\t} else {\n");
		source.append("\t\t\t\tTTCN_Logger.log_event_enum(enum_to_str(enum_value), enum2int(enum_value));\n");
		source.append("\t\t\t}\n");
		source.append("\t\t}\n\n");
	}

	private static void generateValueSetParam(final StringBuilder source, final String name) {
		source.append("\t\t@Override\n");
		source.append("\t\tpublic void set_param(final Module_Parameter param) {\n");
		source.append("\t\t\tparam.basic_check(Module_Parameter.basic_check_bits_t.BC_VALUE.getValue(), \"enumerated value\");\n");
		source.append("\t\t\tif (param.get_type() != Module_Parameter.type_t.MP_Enumerated) {\n");
		source.append(MessageFormat.format("\t\t\t\tparam.type_error(\"enumerated_value\", \"{0}\");\n", name));
		source.append("\t\t\t}\n");
		source.append("\t\t\tenum_value = str_to_enum(param.get_enumerated());\n");
		source.append("\t\t\tif (!is_valid_enum(enum_value)) {\n");
		source.append(MessageFormat.format("\t\t\t\tparam.error(\"Invalid enumerated value for type {0}.\");\n", name));
		source.append("\t\t\t}\n");
		source.append("\t\t}\n\n");
	}

	private static void generateValueEncodeDecodeText(final StringBuilder source, final String name) {
		source.append("\t\t@Override\n");
		source.append("\t\tpublic void encode_text(final Text_Buf text_buf) {\n");
		source.append(MessageFormat.format("\t\t\tmust_bound(\"Text encoder: Encoding an unbound value of enumerated type {0}.\");\n", name));
		source.append("\t\t\ttext_buf.push_int(enum_value.enum_num);\n");
		source.append("\t\t}\n\n");

		source.append("\t\t@Override\n");
		source.append("\t\tpublic void decode_text(final Text_Buf text_buf) {\n");
		source.append("\t\t\tfinal int temp = text_buf.pull_int().get_int();\n");
		source.append("\t\t\tif (!is_valid_enum(temp)) {\n");
		source.append(MessageFormat.format("\t\t\t\tthrow new TtcnError(MessageFormat.format(\"Text decoder: Unknown numeric value '{'0'}' was received for enumerated type {0}.\", temp));\n", name));
		source.append("\t\t\t}\n");
		source.append("\t\t\tint2enum(temp);\n");
		source.append("\t\t}\n\n");
	}


	private static void generateValueEncodeDecode(final JavaGenData aData, final StringBuilder source, final Enum_Defs e_defs, final boolean rawNeeded) {
		source.append("\t\t@Override\n");
		source.append("\t\tpublic void encode(final TTCN_Typedescriptor p_td, final TTCN_Buffer p_buf, final coding_type p_coding, final int flavour) {\n");
		source.append("\t\t\tswitch (p_coding) {\n");
		source.append("\t\t\tcase CT_RAW: {\n");
		source.append("\t\t\t\tfinal TTCN_EncDec_ErrorContext errorContext = new TTCN_EncDec_ErrorContext(\"While RAW-encoding type '%s': \", p_td.name);\n");
		source.append("\t\t\t\tif (p_td.raw == null) {\n");
		source.append("\t\t\t\t\tTTCN_EncDec_ErrorContext.error_internal(\"No RAW descriptor available for type '%s'.\", p_td.name);\n");
		source.append("\t\t\t\t}\n");
		source.append("\t\t\t\tfinal RAW_enc_tr_pos tree_position = new RAW_enc_tr_pos(0, null);\n");
		source.append("\t\t\t\tfinal RAW_enc_tree root = new RAW_enc_tree(true, null, tree_position, 1, p_td.raw);\n");
		source.append("\t\t\t\tRAW_encode(p_td, root);\n");
		source.append("\t\t\t\troot.put_to_buf(p_buf);\n");
		source.append("\t\t\t\terrorContext.leave_context();\n");
		source.append("\t\t\t\tbreak;\n");
		source.append("\t\t\t}\n");
		source.append("\t\t\tdefault:\n");
		source.append("\t\t\t\tthrow new TtcnError(MessageFormat.format(\"Unknown coding method requested to encode type `{0}''\", p_td.name));\n");
		source.append("\t\t\t}\n");
		source.append("\t\t}\n\n");

		source.append("\t\t@Override\n");
		source.append("\t\tpublic void decode(final TTCN_Typedescriptor p_td, final TTCN_Buffer p_buf, final coding_type p_coding, final int flavour) {\n");
		source.append("\t\t\tswitch (p_coding) {\n");
		source.append("\t\t\tcase CT_RAW: {\n");
		source.append("\t\t\t\tfinal TTCN_EncDec_ErrorContext errorContext = new TTCN_EncDec_ErrorContext(\"While RAW-decoding type '%s': \", p_td.name);\n");
		source.append("\t\t\t\tif (p_td.raw == null) {\n");
		source.append("\t\t\t\t\tTTCN_EncDec_ErrorContext.error_internal(\"No RAW descriptor available for type '%s'.\", p_td.name);\n");
		source.append("\t\t\t\t}\n");
		source.append("\t\t\t\traw_order_t order;\n");
		source.append("\t\t\t\tswitch (p_td.raw.top_bit_order) {\n");
		source.append("\t\t\t\tcase TOP_BIT_LEFT:\n");
		source.append("\t\t\t\t\torder = raw_order_t.ORDER_LSB;\n");
		source.append("\t\t\t\t\tbreak;\n");
		source.append("\t\t\t\tcase TOP_BIT_RIGHT:\n");
		source.append("\t\t\t\tdefault:\n");
		source.append("\t\t\t\t\torder = raw_order_t.ORDER_MSB;\n");
		source.append("\t\t\t\t\tbreak;\n");
		source.append("\t\t\t\t}\n");
		source.append("\t\t\t\tfinal int rawr = RAW_decode(p_td, p_buf, p_buf.get_len() * 8, order);\n");
		source.append("\t\t\t\tif (rawr < 0) {\n");
		source.append("\t\t\t\t\tfinal error_type temp = error_type.values()[-rawr];\n");
		source.append("\t\t\t\t\tswitch (temp) {\n");
		source.append("\t\t\t\t\tcase ET_INCOMPL_MSG:\n");
		source.append("\t\t\t\t\tcase ET_LEN_ERR:\n");
		source.append("\t\t\t\t\t\tTTCN_EncDec_ErrorContext.error(temp, \"Can not decode type '%s', because invalid or incomplete message was received\", p_td.name);\n");
		source.append("\t\t\t\t\t\tbreak;\n");
		source.append("\t\t\t\t\tcase ET_UNBOUND:\n");
		source.append("\t\t\t\t\tdefault:\n");
		source.append("\t\t\t\t\t\tTTCN_EncDec_ErrorContext.error(error_type.ET_INVAL_MSG, \"Can not decode type '%s', because invalid or incomplete message was received\", p_td.name);\n");
		source.append("\t\t\t\t\t\tbreak;\n");
		source.append("\t\t\t\t\t}\n");
		source.append("\t\t\t\t}\n");
		source.append("\t\t\t\terrorContext.leave_context();\n");
		source.append("\t\t\t\tbreak;\n");
		source.append("\t\t\t}\n");
		source.append("\t\t\tdefault:\n");
		source.append("\t\t\t\tthrow new TtcnError(MessageFormat.format(\"Unknown coding method requested to decode type `{0}''\", p_td.name));\n");
		source.append("\t\t\t}\n");
		source.append("\t\t}\n\n");

		if (rawNeeded) {
			aData.addBuiltinTypeImport("RAW.RAW_Force_Omit");
			aData.addImport("java.util.concurrent.atomic.AtomicInteger");

			int min_bits = 0;
			long max_val = e_defs.firstUnused;
			for (int a = 0; a < e_defs.items.size(); a++) {
				final long val = e_defs.items.get(a).value;
				if (Math.abs(max_val) < Math.abs(val)) {
					max_val = val;
				}
			}
			if (max_val < 0) {
				min_bits = 1;
				max_val = -1 * max_val;
			}
			while (max_val > 0) {
				min_bits++;
				max_val /= 2;
			}

			source.append("\t\t@Override\n");
			source.append("\t\t/** {@inheritDoc} */\n");
			source.append("\t\tpublic int RAW_encode(final TTCN_Typedescriptor p_td, final RAW_enc_tree myleaf) {\n");
			source.append(MessageFormat.format("\t\t\treturn RAW.RAW_encode_enum_type(p_td, myleaf, enum_value.enum_num, {0});\n", min_bits));
			source.append("\t\t}\n");


			source.append("\t\t@Override\n");
			source.append("\t\t/** {@inheritDoc} */\n");
			source.append("\t\tpublic int RAW_decode(final TTCN_Typedescriptor p_td, final TTCN_Buffer buff, int limit, final raw_order_t top_bit_ord, final boolean no_err, final int sel_field, final boolean first_call, final RAW_Force_Omit force_omit) {\n");
			source.append("\t\t\tfinal AtomicInteger decoded_value = new AtomicInteger(0);\n");
			source.append(MessageFormat.format("\t\t\tfinal int decoded_length = RAW.RAW_decode_enum_type(p_td, buff, limit, top_bit_ord, decoded_value, {0}, no_err);\n", min_bits));
			source.append("\t\t\tif (decoded_length < 0) {\n");
			source.append("\t\t\t\treturn decoded_length;\n");
			source.append("\t\t\t}\n");
			source.append("\t\t\tif (is_valid_enum(decoded_value.get())) {\n");
			source.append("\t\t\t\tenum_value = enum_type.getValue(decoded_value.get());\n");
			source.append("\t\t\t} else {\n");
			source.append("\t\t\t\tif (no_err) {\n");
			source.append("\t\t\t\t\treturn -1;\n");
			source.append("\t\t\t\t} else {\n");
			source.append("\t\t\t\t\tTTCN_EncDec_ErrorContext.error(error_type.ET_ENC_ENUM, \"Invalid enum value '%d' for '%s': \", decoded_value.get(), p_td.name);\n");
			source.append("\t\t\t\t\tenum_value = enum_type.UNKNOWN_VALUE;\n");
			source.append("\t\t\t\t}\n");
			source.append("\t\t\t}\n");
			source.append("\t\t\treturn decoded_length;\n");
			source.append("\t\t}\n");
		}
	}

	private static void generateValueStrToEnum(final StringBuilder source, final Enum_Defs e_defs) {
		source.append("\t\tpublic static enum_type str_to_enum(final String strPar) {\n");
		for (int i = 0; i < e_defs.items.size(); i++) {
			final Enum_field field = e_defs.items.get(i);

			source.append("\t\t\tif (");
			boolean first = true;
			if (field.name != null) {
				source.append(MessageFormat.format("\"{0}\".equals(strPar)", field.name));
				first = false;
			}
			//TODO add escaped name support
			if (field.displayName != null && !field.displayName.equals(field.name)) {
				if (!first) {
					source.append(" || ");
				}
				source.append(MessageFormat.format("\"{0}\".equals(strPar)", field.displayName));
			}
			source.append(") {\n");
			source.append(MessageFormat.format("\t\t\t\treturn enum_type.{0};\n", field.name));
			source.append("\t\t\t}\n");
		}

		source.append("\t\t\treturn enum_type.UNKNOWN_VALUE;\n");
		source.append("\t\t}\n\n");
	}

	private static void generateValueAsInt(final StringBuilder source) {
		source.append("\t\tpublic int as_int() {\n");
		source.append("\t\t\treturn enum2int(enum_value);\n");
		source.append("\t\t}\n\n");
	}

	private static void generateValueFromInt(final StringBuilder source) {
		source.append("\t\tpublic void from_int(final int otherValue) {\n");
		source.append("\t\t\tenum_value = enum_type.getValue(otherValue);\n");
		source.append("\t\t}\n\n");
	}

	private static void generateValueIntToEnum(final StringBuilder source) {
		//arg: int
		source.append("\t\tpublic void int2enum(final int intValue) {\n");
		source.append("\t\t\tif (!is_valid_enum(intValue)) {\n");
		source.append("\t\t\t\tthrow new TtcnError(\"Assigning invalid numeric value \"+intValue+\" to a variable of enumerated type {}.\");\n");
		source.append("\t\t\t}\n");
		source.append("\t\t\tenum_value = enum_type.getValue(intValue);\n");
		source.append("\t\t}\n\n");

		//arg: TitanInteger
		source.append("\t\tpublic void int2enum(final TitanInteger intValue) {\n");
		source.append("\t\t\tif (!is_valid_enum(intValue.get_int())) {\n");
		source.append("\t\t\t\tthrow new TtcnError(\"Assigning invalid numeric value \"+intValue.get_int()+\" to a variable of enumerated type {}.\");\n");
		source.append("\t\t\t}\n");
		source.append("\t\t\tenum_value = enum_type.getValue(intValue.get_int());\n");
		source.append("\t\t}\n\n");
	}

	private static void generateValueEnumToInt(final StringBuilder source, final String name) {
		// arg: enum_type
		source.append(MessageFormat.format("\t\tpublic static int enum2int(final {0}.enum_type enumPar) '{'\n", name));
		source.append("\t\t\tif (enumPar == enum_type.UNBOUND_VALUE || enumPar == enum_type.UNKNOWN_VALUE) {\n");
		source.append("\t\t\t\tthrow new TtcnError(\"The argument of function enum2int() is an \"+ (enumPar==enum_type.UNBOUND_VALUE ? \"unbound\":\"invalid\") +\" value of enumerated type {0}.\");\n");
		source.append("\t\t\t}\n");
		source.append("\t\t\treturn enumPar.enum_num;\n");
		source.append("\t\t}\n\n");

		// own type
		source.append(MessageFormat.format("\t\tpublic static int enum2int(final {0} enumPar) '{'\n", name));
		source.append("\t\t\tif (enumPar.enum_value == enum_type.UNBOUND_VALUE || enumPar.enum_value == enum_type.UNKNOWN_VALUE) {\n");
		source.append("\t\t\t\tthrow new TtcnError(\"The argument of function enum2int() is an \"+ (enumPar.enum_value==enum_type.UNBOUND_VALUE ? \"unbound\":\"invalid\") +\" value of enumerated type {0}.\");\n");
		source.append("\t\t\t}\n");
		source.append("\t\t\treturn enumPar.enum_value.enum_num;\n");
		source.append("\t\t}\n\n");

	}

	private static void generateValueIsPresent(final StringBuilder source) {
		source.append("\t\t@Override\n");
		source.append("\t\tpublic boolean is_present() {\n");
		source.append("\t\t\treturn is_bound();\n");
		source.append("\t\t}\n\n");
	}

	private static void generateValueIsBound(final StringBuilder source){
		source.append("\t\t@Override\n");
		source.append("\t\tpublic boolean is_bound() {\n");
		source.append("\t\t\treturn enum_value != enum_type.UNBOUND_VALUE;\n");
		source.append("\t\t}\n\n");
	}

	private static void generateValueIsValue(final StringBuilder source){
		source.append("\t\t@Override\n");
		source.append("\t\tpublic boolean is_value() {\n");
		source.append("\t\t\treturn enum_value != enum_type.UNBOUND_VALUE;\n");
		source.append("\t\t}\n\n");
	}

	private static void generateValueoperator_equals(final JavaGenData aData, final StringBuilder source, final String aName, final String displayName) {
		//Arg type: own type
		if (aData.isDebug()) {
			source.append("\t\t/**\n");
			source.append("\t\t * Checks if the current value is equivalent to the provided one.\n");
			source.append("\t\t *\n");
			source.append("\t\t * operator== in the core\n");
			source.append("\t\t *\n");
			source.append("\t\t * @param otherValue\n");
			source.append("\t\t *                the other value to check against.\n");
			source.append("\t\t * @return true if the values are equivalent.\n");
			source.append("\t\t */\n");
		}
		source.append(MessageFormat.format("\t\tpublic boolean operator_equals(final {0} otherValue)'{'\n", aName));
		source.append(MessageFormat.format("\t\t\treturn enum_value == otherValue.enum_value;\n", aName));
		source.append("\t\t}\n\n");

		//Arg: Base_Type
		if (aData.isDebug()) {
			source.append("\t\t/**\n");
			source.append("\t\t * Checks if the current value is equivalent to the provided one.\n");
			source.append("\t\t *\n");
			source.append("\t\t * operator== in the core\n");
			source.append("\t\t *\n");
			source.append("\t\t * @param otherValue\n");
			source.append("\t\t *                the other value to check against.\n");
			source.append("\t\t * @return true if the values are equivalent.\n");
			source.append("\t\t */\n");
		}
		source.append("\t\tpublic boolean operator_equals(final Base_Type otherValue){\n");
		source.append(MessageFormat.format("\t\t\tif (otherValue instanceof {0}) '{'\n", aName));
		source.append(MessageFormat.format("\t\t\t\treturn operator_equals( ({0}) otherValue);\n", aName));
		source.append("\t\t\t} else {\n");
		source.append(MessageFormat.format("\t\t\t\tthrow new TtcnError(\"Internal Error: value can not be cast to {0}.\");\n", displayName));
		source.append("\t\t\t}\n");
		source.append("\t\t}\n\n");

		//Arg: enum_type
		if (aData.isDebug()) {
			source.append("\t\t/**\n");
			source.append("\t\t * Checks if the current value is equivalent to the provided one.\n");
			source.append("\t\t *\n");
			source.append("\t\t * operator== in the core\n");
			source.append("\t\t *\n");
			source.append("\t\t * @param otherValue\n");
			source.append("\t\t *                the other value to check against.\n");
			source.append("\t\t * @return true if the values are equivalent.\n");
			source.append("\t\t */\n");
		}
		source.append(MessageFormat.format("\t\tpublic boolean operator_equals(final {0}.enum_type otherValue)'{'\n",aName));
		source.append(MessageFormat.format("\t\t\treturn enum_value == otherValue;\n", aName));
		source.append("\t\t}\n\n");
	}

	private static void generateValueoperator_not_equals(final JavaGenData aData, final StringBuilder source,final String aName) {
		//Arg type: own type
		if (aData.isDebug()) {
			source.append("\t\t/**\n");
			source.append("\t\t * Checks if the current value is not equivalent to the provided one.\n");
			source.append("\t\t *\n");
			source.append("\t\t * operator!= in the core\n");
			source.append("\t\t *\n");
			source.append("\t\t * @param otherValue\n");
			source.append("\t\t *                the other value to check against.\n");
			source.append("\t\t * @return {@code true} if the values are not equivalent.\n");
			source.append("\t\t */\n");
		}
		source.append(MessageFormat.format("\t\tpublic boolean operator_not_equals(final {0} otherValue)'{'\n", aName));
		source.append(MessageFormat.format("\t\t\treturn !operator_equals(otherValue);\n", aName));
		source.append("\t\t}\n\n");

		//Arg: Base_Type
		if (aData.isDebug()) {
			source.append("\t\t/**\n");
			source.append("\t\t * Checks if the current value is not equivalent to the provided one.\n");
			source.append("\t\t *\n");
			source.append("\t\t * operator!= in the core\n");
			source.append("\t\t *\n");
			source.append("\t\t * @param otherValue\n");
			source.append("\t\t *                the other value to check against.\n");
			source.append("\t\t * @return {@code true} if the values are not equivalent.\n");
			source.append("\t\t */\n");
		}
		source.append("\t\tpublic boolean operator_not_equals(final Base_Type otherValue){\n");
		source.append(MessageFormat.format("\t\t\treturn !operator_equals(otherValue);\n", aName));
		source.append("\t\t}\n\n");

		//Arg: enum_type
		if (aData.isDebug()) {
			source.append("\t\t/**\n");
			source.append("\t\t * Checks if the current value is not equivalent to the provided one.\n");
			source.append("\t\t *\n");
			source.append("\t\t * operator!= in the core\n");
			source.append("\t\t *\n");
			source.append("\t\t * @param otherValue\n");
			source.append("\t\t *                the other value to check against.\n");
			source.append("\t\t * @return {@code true} if the values are not equivalent.\n");
			source.append("\t\t */\n");
		}
		source.append(MessageFormat.format("\t\tpublic boolean operator_not_equals(final {0}.enum_type otherValue)'{'\n",aName));
		source.append(MessageFormat.format("\t\t\treturn !operator_equals(otherValue);\n", aName));
		source.append("\t\t}\n\n");
	}

	private static void generateValueoperator_assign(final JavaGenData aData, final StringBuilder source, final String name) {
		//Arg type: own type
		if ( aData.isDebug() ) {
			source.append("\t\t/**\n");
			source.append("\t\t * Assigns the other value to this value.\n");
			source.append("\t\t * Overwriting the current content in the process.\n");
			source.append("\t\t *<p>\n");
			source.append("\t\t * operator= in the core.\n");
			source.append("\t\t *\n");
			source.append("\t\t * @param otherValue\n");
			source.append("\t\t *                the other value to assign.\n");
			source.append("\t\t * @return the new value object.\n");
			source.append("\t\t */\n");
		}
		source.append(MessageFormat.format("\t\tpublic {0} operator_assign(final {0} otherValue)'{'\n", name));
		source.append("\t\t\totherValue.must_bound(\"Assignment of an unbound enumerated value\");\n\n");
		source.append( "\t\t\tif (otherValue != this) {\n");
		source.append(MessageFormat.format("\t\t\t\tthis.enum_value = otherValue.enum_value;\n",  name));
		source.append("\t\t\t}\n\n");
		source.append("\t\t\treturn this;\n");
		source.append("\t\t}\n\n");

		//Arg: Base_Type
		source.append("\t\t@Override\n");
		source.append(MessageFormat.format("\t\tpublic {0} operator_assign(final Base_Type otherValue)'{'\n", name));
		source.append(MessageFormat.format("\t\t\tif( otherValue instanceof {0} ) '{'\n", name));
		source.append(MessageFormat.format("\t\t\t\treturn operator_assign(({0}) otherValue);\n", name));
		source.append("\t\t\t}\n\n");
		source.append(MessageFormat.format("\t\t\tthrow new TtcnError(MessageFormat.format(\"Internal Error: value `{0}'' can not be cast to {1}\", otherValue));\n", name));
		source.append("\t\t}\n\n");

		//Arg: enum_type
		if ( aData.isDebug() ) {
			source.append("\t\t/**\n");
			source.append("\t\t * Assigns the other value to this value.\n");
			source.append("\t\t * Overwriting the current content in the process.\n");
			source.append("\t\t *<p>\n");
			source.append("\t\t * operator= in the core.\n");
			source.append("\t\t *\n");
			source.append("\t\t * @param otherValue\n");
			source.append("\t\t *                the other value to assign.\n");
			source.append("\t\t * @return the new value object.\n");
			source.append("\t\t */\n");
		}
		source.append(MessageFormat.format("\t\tpublic {0} operator_assign(final {0}.enum_type otherValue)'{'\n", name));
		source.append(MessageFormat.format("\t\t\treturn operator_assign( new {0}(otherValue) );\n",name));
		source.append("\t\t}\n\n");

		//Arg: int
		if ( aData.isDebug() ) {
			source.append("\t\t/**\n");
			source.append("\t\t * Assigns the other value to this value.\n");
			source.append("\t\t * Overwriting the current content in the process.\n");
			source.append("\t\t *<p>\n");
			source.append("\t\t * operator= in the core.\n");
			source.append("\t\t *\n");
			source.append("\t\t * @param otherValue\n");
			source.append("\t\t *                the other value to assign.\n");
			source.append("\t\t * @return the new value object.\n");
			source.append("\t\t */\n");
		}
		source.append(MessageFormat.format("\t\tpublic {0} operator_assign(final int otherValue)'{'\n", name));
		source.append("\t\t\tif (!is_valid_enum(otherValue)) {\n");
		source.append(MessageFormat.format("\t\t\t\tthrow new TtcnError(MessageFormat.format(\"Assigning unknown numeric value {1} to a variable of enumerated type `{0}''.\", otherValue));\n", name));
		source.append("\t\t\t}\n\n");
		source.append(MessageFormat.format("\t\t\tenum_value =  enum_type.getValue(otherValue);\n", name));
		source.append("\t\t\treturn this;\n");
		source.append("\t\t}\n\n");
	}

	private static void generateValueIsLessThan(final JavaGenData aData, final StringBuilder source, final String name) {
		// arg: enum_type
		if ( aData.isDebug() ) {
			source.append("\t\t/**\n");
			source.append("\t\t * Checks if the current value is less than the provided one.\n");
			source.append("\t\t *\n");
			source.append("\t\t * operator< in the core\n");
			source.append("\t\t *\n");
			source.append("\t\t * @param otherValue\n");
			source.append("\t\t *                the other value to check against.\n");
			source.append("\t\t * @return {@code true} if the value is less than the provided.\n");
			source.append("\t\t */\n");
		}
		source.append(MessageFormat.format("\t\tpublic boolean is_less_than(final {0}.enum_type otherValue)'{'\n", name));
		source.append("\t\t\tmust_bound(\"The left operand of comparison is an unbound value of enumerated type "+ name +". \");\n");
		source.append("\t\t\treturn enum_value.enum_num < otherValue.enum_num;\n");
		source.append("\t\t}\n\n");

		//arg: own type
		if ( aData.isDebug() ) {
			source.append("\t\t/**\n");
			source.append("\t\t * Checks if the current value is less than the provided one.\n");
			source.append("\t\t *\n");
			source.append("\t\t * operator< in the core\n");
			source.append("\t\t *\n");
			source.append("\t\t * @param otherValue\n");
			source.append("\t\t *                the other value to check against.\n");
			source.append("\t\t * @return {@code true} if the value is less than the provided.\n");
			source.append("\t\t */\n");
		}
		source.append(MessageFormat.format("\t\tpublic boolean is_less_than(final {0} otherValue)'{'\n", name));
		source.append("\t\t\tmust_bound(\"The left operand of comparison is an unbound value of enumerated type "+ name +". \");\n");
		source.append("\t\t\totherValue.must_bound(\"The right operand of comparison is an unbound value of enumerated type "+ name +". \");\n");
		source.append("\t\t\treturn  enum_value.enum_num < otherValue.enum_value.enum_num ;\n");
		source.append("\t\t}\n\n");
	}

	private static void generateValueIsLessThanOrEqual(final JavaGenData aData, final StringBuilder source, final String name) {
		// arg: enum_type
		if ( aData.isDebug() ) {
			source.append("\t\t/**\n");
			source.append("\t\t * Checks if the current value is less than or equivalent to the provided one.\n");
			source.append("\t\t *\n");
			source.append("\t\t * operator<= in the core\n");
			source.append("\t\t *\n");
			source.append("\t\t * @param otherValue\n");
			source.append("\t\t *                the other value to check against.\n");
			source.append("\t\t * @return {@code true} if the value is less than or equivalent to the provided.\n");
			source.append("\t\t */\n");
		}
		source.append(MessageFormat.format("\t\tpublic boolean is_less_than_or_equal(final {0}.enum_type otherValue)'{'\n", name));
		source.append("\t\t\tmust_bound(\"The left operand of comparison is an unbound value of enumerated type "+ name +". \");\n");
		source.append("\t\t\treturn enum_value.enum_num <= otherValue.enum_num;\n");
		source.append("\t\t}\n\n");

		// own type
		if ( aData.isDebug() ) {
			source.append("\t\t/**\n");
			source.append("\t\t * Checks if the current value is less than or equivalent to the provided one.\n");
			source.append("\t\t *\n");
			source.append("\t\t * operator<= in the core\n");
			source.append("\t\t *\n");
			source.append("\t\t * @param otherValue\n");
			source.append("\t\t *                the other value to check against.\n");
			source.append("\t\t * @return {@code true} if the value is less than or equivalent to the provided.\n");
			source.append("\t\t */\n");
		}
		source.append(MessageFormat.format("\t\tpublic boolean is_less_than_or_equal(final {0} otherValue)'{'\n", name));
		source.append("\t\t\tmust_bound(\"The left operand of comparison is an unbound value of enumerated type "+ name +". \");\n");
		source.append("\t\t\totherValue.must_bound(\"The right operand of comparison is an unbound value of enumerated type "+ name +". \");\n");
		source.append("\t\t\treturn enum_value.enum_num <= otherValue.enum_value.enum_num ;\n");
		source.append("\t\t}\n\n");
	}

	private static void generateValueIsGreaterThan(final JavaGenData aData, final StringBuilder source, final String name) {
		// arg: enum_type
		if ( aData.isDebug() ) {
			source.append("\t\t/**\n");
			source.append("\t\t * Checks if the current value is greater than the provided one.\n");
			source.append("\t\t *\n");
			source.append("\t\t * operator> in the core\n");
			source.append("\t\t *\n");
			source.append("\t\t * @param otherValue\n");
			source.append("\t\t *                the other value to check against.\n");
			source.append("\t\t * @return {@code true} if the value is greater than the provided.\n");
			source.append("\t\t */\n");
		}
		source.append(MessageFormat.format("\t\tpublic boolean is_greater_than(final {0}.enum_type otherValue)'{'\n", name));
		source.append("\t\t\tmust_bound(\"The left operand of comparison is an unbound value of enumerated type "+ name +". \");\n");
		source.append("\t\t\treturn enum_value.enum_num > otherValue.enum_num;\n");
		source.append("\t\t}\n\n");

		// own type
		if ( aData.isDebug() ) {
			source.append("\t\t/**\n");
			source.append("\t\t * Checks if the current value is greater than the provided one.\n");
			source.append("\t\t *\n");
			source.append("\t\t * operator> in the core\n");
			source.append("\t\t *\n");
			source.append("\t\t * @param otherValue\n");
			source.append("\t\t *                the other value to check against.\n");
			source.append("\t\t * @return {@code true} if the value is greater than the provided.\n");
			source.append("\t\t */\n");
		}
		source.append(MessageFormat.format("\t\tpublic boolean is_greater_than(final {0} otherValue)'{'\n", name));
		source.append("\t\t\tmust_bound(\"The left operand of comparison is an unbound value of enumerated type "+ name +". \");\n");
		source.append("\t\t\totherValue.must_bound(\"The right operand of comparison is an unbound value of enumerated type "+ name +". \");\n");
		source.append("\t\t\treturn enum_value.enum_num > otherValue.enum_value.enum_num ;\n");
		source.append("\t\t}\n\n");
	}

	private static void generateValueIsGreaterThanOrEqual(final JavaGenData aData, final StringBuilder source, final String name) {
		// arg: enum_type
		if ( aData.isDebug() ) {
			source.append("\t\t/**\n");
			source.append("\t\t * Checks if the current value is greater than or equivalent to the provided one.\n");
			source.append("\t\t *\n");
			source.append("\t\t * operator>= in the core\n");
			source.append("\t\t *\n");
			source.append("\t\t * @param otherValue\n");
			source.append("\t\t *                the other value to check against.\n");
			source.append("\t\t * @return {@code true} if the value is greater than or equivalent to the provided.\n");
			source.append("\t\t */\n");
		}
		source.append(MessageFormat.format("\t\tpublic boolean is_greater_than_or_equal(final {0}.enum_type otherValue)'{'\n", name));
		source.append("\t\t\tmust_bound(\"The left operand of comparison is an unbound value of enumerated type "+ name +". \");\n");
		source.append("\t\t\treturn enum_value.enum_num >= otherValue.enum_num;\n");
		source.append("\t\t}\n\n");

		// arg: own type
		if ( aData.isDebug() ) {
			source.append("\t\t/**\n");
			source.append("\t\t * Checks if the current value is greater than or equivalent to the provided one.\n");
			source.append("\t\t *\n");
			source.append("\t\t * operator>= in the core\n");
			source.append("\t\t *\n");
			source.append("\t\t * @param otherValue\n");
			source.append("\t\t *                the other value to check against.\n");
			source.append("\t\t * @return {@code true} if the value is greater than or equivalent to the provided.\n");
			source.append("\t\t */\n");
		}
		source.append(MessageFormat.format("\t\tpublic boolean is_greater_than_or_equal(final {0} otherValue)'{'\n", name));
		source.append("\t\t\tmust_bound(\"The left operand of comparison is an unbound value of enumerated type "+ name +". \");\n");
		source.append("\t\t\totherValue.must_bound(\"The right operand of comparison is an unbound value of enumerated type "+ name +". \");\n");
		source.append("\t\t\treturn enum_value.enum_num >= otherValue.enum_value.enum_num ;\n");
		source.append("\t\t}\n\n");
	}

	private static void generateValueCleanUp(final StringBuilder source) {
		source.append("\t\t@Override\n");
		source.append("\t\tpublic void clean_up() {\n");
		source.append("\t\t\tenum_value = enum_type.UNBOUND_VALUE;\n");
		source.append("\t\t}\n\n");
	}

	private static void generateTemplateDeclaration(final StringBuilder source, final String name) {
		source.append("\t\t// single_value\n");
		source.append(MessageFormat.format("\t\tprivate {0}.enum_type single_value;\n",name));
		source.append("\t\t// value_list part\n");
		source.append(MessageFormat.format("\t\tprivate ArrayList<{0}_template> value_list;\n\n", name));
	}

	private static void generateTemplateConstructors( final JavaGenData aData, final StringBuilder source, final String name){
		// empty
		if (aData.isDebug()) {
			source.append("\t\t/**\n");
			source.append("\t\t * Initializes to unbound/uninitialized template.\n");
			source.append("\t\t * */\n");
		}
		source.append(MessageFormat.format("\t\tpublic {0}_template() '{'\n", name));
		source.append("\t\t\t// do nothing\n");
		source.append("\t\t}\n\n");

		// template_sel
		if (aData.isDebug()) {
			source.append("\t\t/**\n");
			source.append("\t\t * Initializes to a given template kind.\n");
			source.append("\t\t *\n");
			source.append("\t\t * @param otherValue\n");
			source.append("\t\t *                the template kind to initialize to.\n");
			source.append("\t\t * */\n");
		}
		source.append(MessageFormat.format("\t\tpublic {0}_template(final template_sel otherValue) '{'\n", name));
		source.append("\t\t\tsuper(otherValue);\n");
		source.append("\t\t\tcheck_single_selection(otherValue);\n");
		source.append("\t\t}\n\n");

		// int
		if (aData.isDebug()) {
			source.append("\t\t/**\n");
			source.append("\t\t * Initializes to a given value.\n");
			source.append("\t\t * The template becomes a specific template with the provided value.\n");
			source.append("\t\t *\n");
			source.append("\t\t * @param otherValue\n");
			source.append("\t\t *                the value to initialize to.\n");
			source.append("\t\t * */\n");
		}
		source.append(MessageFormat.format("\t\tpublic {0}_template(final int otherValue) '{'\n", name));
		source.append("\t\t\tsuper(template_sel.SPECIFIC_VALUE);\n");
		source.append(MessageFormat.format("\t\t\tif (!{0}.is_valid_enum(otherValue)) '{'\n", name));
		source.append(MessageFormat.format("\t\t\t\tthrow new TtcnError(\"Initializing a template of enumerated type {0} with unknown numeric value \"+ otherValue +\".\");\n", name));
		source.append("\t\t\t}\n");
		source.append(MessageFormat.format("\t\t\tsingle_value = {0}.enum_type.getValue(otherValue);\n", name));
		source.append("\t\t}\n\n");

		// name type
		if (aData.isDebug()) {
			source.append("\t\t/**\n");
			source.append("\t\t * Initializes to a given value.\n");
			source.append("\t\t * The template becomes a specific template with the provided value.\n");
			source.append("\t\t *\n");
			source.append("\t\t * @param otherValue\n");
			source.append("\t\t *                the value to initialize to.\n");
			source.append("\t\t * */\n");
		}
		source.append(MessageFormat.format("\t\tpublic {0}_template(final {0} otherValue) '{'\n", name));
		source.append("\t\t\tsuper(template_sel.SPECIFIC_VALUE);\n");
		source.append("\t\t\totherValue.must_bound(\"Creating a template from an unbound value of enumerated type "+ name +". \");\n");
		source.append("\t\t\tsingle_value = otherValue.enum_value;\n");
		source.append("\t\t}\n\n");

		// own type
		if (aData.isDebug()) {
			source.append("\t\t/**\n");
			source.append("\t\t * Initializes to a given template.\n");
			source.append("\t\t *\n");
			source.append("\t\t * @param otherValue\n");
			source.append("\t\t *                the template to initialize to.\n");
			source.append("\t\t * */\n");
		}
		source.append(MessageFormat.format("\t\tpublic {0}_template(final {0}_template otherValue) '{'\n", name));
		source.append("\t\t\tcopy_template(otherValue);\n");
		source.append("\t\t}\n\n");

		// name.enum_type
		if (aData.isDebug()) {
			source.append("\t\t/**\n");
			source.append("\t\t * Initializes to a given value.\n");
			source.append("\t\t * The template becomes a specific template with the provided value.\n");
			source.append("\t\t *\n");
			source.append("\t\t * @param otherValue\n");
			source.append("\t\t *                the value to initialize to.\n");
			source.append("\t\t * */\n");
		}
		source.append(MessageFormat.format("\t\tpublic {0}_template(final {0}.enum_type otherValue) '{'\n", name));
		source.append("\t\t\tsuper(template_sel.SPECIFIC_VALUE);\n");
		source.append("\t\t\tsingle_value = otherValue;\n");
		source.append("\t\t}\n\n");

		//FIXME implement optional parameter version
	}

	private static void generatetemplateCopyTemplate(final StringBuilder source, final String name) {
		source.append(MessageFormat.format("\t\tprivate void copy_template(final {0}_template otherValue) '{'\n", name));
		source.append("\t\t\tset_selection(otherValue);\n");
		source.append("\t\t\tswitch (otherValue.template_selection) {\n");
		source.append("\t\t\tcase SPECIFIC_VALUE:\n");
		source.append("\t\t\t\tsingle_value = otherValue.single_value;\n");
		source.append("\t\t\t\tbreak;\n");
		source.append("\t\t\tcase OMIT_VALUE:\n");
		source.append("\t\t\tcase ANY_VALUE:\n");
		source.append("\t\t\tcase ANY_OR_OMIT:\n");
		source.append("\t\t\t\tbreak;\n");
		source.append("\t\t\tcase VALUE_LIST:\n");
		source.append("\t\t\tcase COMPLEMENTED_LIST:\n");
		source.append(MessageFormat.format("\t\t\t\tvalue_list = new ArrayList<{0}_template>(otherValue.value_list.size());\n", name));
		source.append("\t\t\t\tfor(int i = 0; i < otherValue.value_list.size(); i++) {\n");
		source.append(MessageFormat.format("\t\t\t\t\tfinal {0}_template temp = new {0}_template(otherValue.value_list.get(i));\n", name));
		source.append("\t\t\t\t\tvalue_list.add(temp);\n");
		source.append("\t\t\t\t}\n");
		source.append("\t\t\t\tbreak;\n");
		source.append("\t\t\tdefault:\n");
		source.append(MessageFormat.format("\t\t\t\tthrow new TtcnError(\"Copying an uninitialized/unsupported template of enumerated type {0}.\");\n", name));
		source.append("\t\t\t}\n");
		source.append("\t\t}\n\n");
	}

	private static void generateTemplateoperator_assign(final JavaGenData aData, final StringBuilder source, final String name) {
		// arg: template_sel
		source.append("\t\t@Override\n");
		source.append(MessageFormat.format("\t\tpublic {0}_template operator_assign(final template_sel otherValue) '{'\n", name));
		source.append("\t\t\tcheck_single_selection(otherValue);\n");
		source.append("\t\t\tclean_up();\n");
		source.append("\t\t\tset_selection(otherValue);\n");
		source.append("\t\t\treturn this;\n");
		source.append("\t\t}\n\n");

		// arg: int
		if ( aData.isDebug() ) {
			source.append("\t\t/**\n");
			source.append("\t\t * Assigns the other value to this template.\n");
			source.append("\t\t * Overwriting the current content in the process.\n");
			source.append("\t\t *<p>\n");
			source.append("\t\t * operator= in the core.\n");
			source.append("\t\t *\n");
			source.append("\t\t * @param otherValue\n");
			source.append("\t\t *                the other value to assign.\n");
			source.append("\t\t * @return the new template object.\n");
			source.append("\t\t */\n");
		}
		source.append(MessageFormat.format("\t\tpublic {0}_template operator_assign(final int otherValue) '{'\n", name));
		source.append(MessageFormat.format("\t\t\tif (!{0}.is_valid_enum(otherValue)) '{'\n", name));
		source.append(MessageFormat.format("\t\t\t\tthrow new TtcnError(\"Assigning unknown numeric value \" + otherValue + \" to a template of enumerated type {0}.\");\n", name));
		source.append("\t\t\t}\n");
		source.append("\t\t\tclean_up();\n");
		source.append("\t\t\tset_selection(template_sel.SPECIFIC_VALUE);\n");
		source.append("\t\t\treturn this;\n");
		source.append("\t\t}\n\n");

		// arg: name.enum_type
		if ( aData.isDebug() ) {
			source.append("\t\t/**\n");
			source.append("\t\t * Assigns the other value to this template.\n");
			source.append("\t\t * Overwriting the current content in the process.\n");
			source.append("\t\t *<p>\n");
			source.append("\t\t * operator= in the core.\n");
			source.append("\t\t *\n");
			source.append("\t\t * @param otherValue\n");
			source.append("\t\t *                the other value to assign.\n");
			source.append("\t\t * @return the new template object.\n");
			source.append("\t\t */\n");
		}
		source.append(MessageFormat.format("\t\tpublic {0}_template operator_assign(final {0}.enum_type otherValue)'{'\n", name));
		source.append("\t\t\tclean_up();\n");
		source.append("\t\t\tset_selection(template_sel.SPECIFIC_VALUE);\n");
		source.append("\t\t\tsingle_value = otherValue;\n");
		source.append("\t\t\treturn this;\n");
		source.append("\t\t}\n\n");

		// arg : own type
		if ( aData.isDebug() ) {
			source.append("\t\t/**\n");
			source.append("\t\t * Assigns the other template to this template.\n");
			source.append("\t\t * Overwriting the current content in the process.\n");
			source.append("\t\t *<p>\n");
			source.append("\t\t * operator= in the core.\n");
			source.append("\t\t *\n");
			source.append("\t\t * @param otherValue\n");
			source.append("\t\t *                the other value to assign.\n");
			source.append("\t\t * @return the new template object.\n");
			source.append("\t\t */\n");
		}
		source.append(MessageFormat.format("\t\tpublic {0}_template operator_assign(final {0}_template otherValue)'{'\n", name));
		source.append("\t\t\t// otherValue.must_bound(\"Assignment of an unbound enumerated value\");\n\n");
		source.append( "\t\t\tif (otherValue != this) {\n");
		source.append("\t\t\t\tclean_up();\n");
		source.append("\t\t\t\tcopy_template(otherValue);\n");
		source.append("\t\t\t}\n");
		source.append("\t\t\treturn this;\n");
		source.append("\t\t}\n\n");

		// arg: name type
		/**
		 * Assigns the other value to this template.
		 * Overwriting the current content in the process.
		 *<p>
		 * operator= in the core.
		 *
		 * @param otherValue
		 *                the other value to assign.
		 * @return the new template object.
		 */
		source.append(MessageFormat.format("\t\tpublic {0}_template operator_assign(final {0} otherValue)'{'\n", name));
		source.append("\t\t\totherValue.must_bound(\"Assignment of an unbound value of enumerated type "+ name +" to a template. \");\n");
		source.append("\t\t\tclean_up();\n");
		source.append("\t\t\tset_selection(template_sel.SPECIFIC_VALUE);\n");
		source.append("\t\t\tsingle_value = otherValue.enum_value;\n");
		source.append("\t\t\treturn this;\n");
		source.append("\t\t}\n\n");

		//Arg: Base_Type
		source.append("\t\t@Override\n");
		source.append(MessageFormat.format("\t\tpublic {0}_template operator_assign(final Base_Type otherValue)'{'\n", name));
		source.append(MessageFormat.format("\t\t\tif( otherValue instanceof {0} ) '{'\n", name));
		source.append(MessageFormat.format("\t\t\t\treturn operator_assign(({0}) otherValue);\n", name));
		source.append("\t\t\t}\n\n");
		source.append(MessageFormat.format("\t\t\tthrow new TtcnError(MessageFormat.format(\"Internal Error: value `{0}'' can not be cast to {1}\", otherValue));\n", name));
		source.append("\t\t}\n\n");

		//Arg: Base_Template
		source.append("\t\t@Override\n");
		source.append(MessageFormat.format("\t\tpublic {0}_template operator_assign(final Base_Template otherValue)'{'\n", name));
		source.append(MessageFormat.format("\t\t\tif( otherValue instanceof {0}_template ) '{'\n", name));
		source.append(MessageFormat.format("\t\t\t\treturn operator_assign(({0}_template) otherValue);\n", name));
		source.append("\t\t\t}\n\n");
		source.append(MessageFormat.format("\t\t\tthrow new TtcnError(MessageFormat.format(\"Internal Error: value `{0}''_template can not be cast to {1}\", otherValue));\n", name));
		source.append("\t\t}\n\n");
		/*@Override
		public myenum1_template operator_assign(Base_Type otherValue) {
			if( otherValue instanceof myenum1 ) {
				return operator_assign((myenum1) otherValue);
			}

			throw new TtcnError(MessageFormat.format("Internal Error: value `myenum1' can not be cast to {1}", otherValue));
		}

		@Override
		public myenum1_template operator_assign(Base_Template otherValue) {
			if( otherValue instanceof myenum1_template ) {
				return operator_assign((myenum1_template) otherValue);
			}

			throw new TtcnError(MessageFormat.format("Internal Error: value `myenum1' can not be cast to {1}", otherValue));
		}*/
		//FIXME implement optional parameter version
	}

	private static void generateTemplateSetType(final StringBuilder source, final String name){
		source.append("\t\t@Override\n");
		source.append("\t\tpublic void set_type(final template_sel templateType, final int list_length) {\n");
		source.append("\t\t\tif (templateType != template_sel.VALUE_LIST && templateType != template_sel.COMPLEMENTED_LIST) {\n");
		source.append(MessageFormat.format("\t\t\t\tthrow new TtcnError(\"Setting an invalid list type for a template of enumerated type {0}.\");\n", name));
		source.append("\t\t\t}\n");
		source.append("\t\t\tclean_up();\n");
		source.append("\t\t\tset_selection(templateType);\n");
		source.append(MessageFormat.format("\t\t\tvalue_list = new ArrayList<{0}_template>();\n", name));
		source.append("\t\t\tfor(int i = 0 ; i < list_length; i++) {\n");
		source.append(MessageFormat.format("\t\t\t\tvalue_list.add(new {0}_template());\n", name));
		source.append("\t\t\t}\n");
		source.append("\t\t}\n\n");
	}

	private static void generateTemplateIsBound(final StringBuilder source) {
		source.append("\t\tpublic boolean is_bound() {\n");
		source.append("\t\t\tif (template_selection == template_sel.UNINITIALIZED_TEMPLATE && !is_ifPresent) {\n");
		source.append("\t\t\t\treturn false;\n");
		source.append("\t\t\t}\n");
		source.append("\t\t\treturn true;\n");
		source.append("\t\t}\n\n");
	}

	private static void generateTemplateIsValue(final StringBuilder source, final String name) {
		source.append("\t\t@Override\n");
		source.append("\t\tpublic boolean is_value() {\n");
		source.append("\t\t\tif (template_selection != template_sel.SPECIFIC_VALUE || is_ifPresent) {\n");
		source.append("\t\t\t\treturn false;\n");
		source.append("\t\t\t}\n");
		source.append(MessageFormat.format("\t\t\treturn single_value != {0}.enum_type.UNBOUND_VALUE;\n", name));
		source.append("\t\t}\n\n");
	}

	private static void generateTemplateCleanUp(final StringBuilder source) {
		source.append("\t\t@Override\n");
		source.append("\t\tpublic void clean_up() {\n");
		source.append("\t\t\tif (template_selection == template_sel.VALUE_LIST || template_selection == template_sel.COMPLEMENTED_LIST) {\n");
		source.append("\t\t\t\tvalue_list.clear();\n");
		source.append("\t\t\t\tvalue_list = null;\n");
		source.append("\t\t\t}\n");
		source.append("\t\t\tif (template_selection == template_sel.SPECIFIC_VALUE) {\n");
		source.append("\t\t\t\tsingle_value = null;\n");
		source.append("\t\t\t}\n");
		source.append("\t\t\ttemplate_selection = template_sel.UNINITIALIZED_TEMPLATE;\n");
		source.append("\t\t}\n\n");
	}

	private static void generateTemplateMatch(final JavaGenData aData, final StringBuilder source, final String name) {
		// name.enum_type
		if (aData.isDebug()) {
			source.append("\t\t/**\n");
			source.append("\t\t * Matches the provided value against this template.\n");
			source.append("\t\t *\n");
			source.append("\t\t * @param otherValue the value to be matched.\n");
			source.append("\t\t * */\n");
		}
		source.append(MessageFormat.format("\t\tpublic boolean match(final {0}.enum_type otherValue) '{'\n", name));
		source.append("\t\t\treturn match(otherValue, false);\n");
		source.append("\t\t}\n\n");

		if (aData.isDebug()) {
			source.append("\t\t/**\n");
			source.append("\t\t * Matches the provided value against this template. In legacy mode\n");
			source.append("\t\t * omitted value fields are not matched against the template field.\n");
			source.append("\t\t *\n");
			source.append("\t\t * @param otherValue\n");
			source.append("\t\t *                the value to be matched.\n");
			source.append("\t\t * @param legacy\n");
			source.append("\t\t *                use legacy mode.\n");
			source.append("\t\t * */\n");
		}
		source.append(MessageFormat.format("\t\tpublic boolean match(final {0}.enum_type otherValue, final boolean legacy) '{'\n", name));
		source.append("\t\t\tswitch (template_selection) {\n");
		source.append("\t\t\tcase SPECIFIC_VALUE:\n");
		source.append("\t\t\t\treturn single_value == otherValue;\n");
		source.append("\t\t\tcase OMIT_VALUE:\n");
		source.append("\t\t\t\treturn false;\n");
		source.append("\t\t\tcase ANY_VALUE:\n");
		source.append("\t\t\tcase ANY_OR_OMIT:\n");
		source.append("\t\t\t\treturn true;\n");
		source.append("\t\t\tcase VALUE_LIST:\n");
		source.append("\t\t\tcase COMPLEMENTED_LIST:\n");
		source.append("\t\t\t\tfor(int i = 0 ; i < value_list.size(); i++) {\n");
		source.append("\t\t\t\t\tif(value_list.get(i).match(otherValue)) {\n");
		source.append("\t\t\t\t\t\treturn template_selection == template_sel.VALUE_LIST;\n");
		source.append("\t\t\t\t\t}\n");
		source.append("\t\t\t\t}\n");
		source.append("\t\t\t\treturn template_selection == template_sel.COMPLEMENTED_LIST;\n");
		source.append("\t\t\tdefault:\n");
		source.append("\t\t\t\tthrow new TtcnError(\"Matching with an uninitialized/unsupported template of enumerated type "+ name +".\");\n");
		source.append("\t\t\t}\n");
		source.append("\t\t}\n\n");

		// name type
		if (aData.isDebug()) {
			source.append("\t\t/**\n");
			source.append("\t\t * Matches the provided value against this template.\n");
			source.append("\t\t *\n");
			source.append("\t\t * @param otherValue the value to be matched.\n");
			source.append("\t\t * */\n");
		}
		source.append(MessageFormat.format("\t\tpublic boolean match(final {0} otherValue) '{'\n", name));
		source.append("\t\t\treturn match(otherValue.enum_value, false);\n");
		source.append("\t\t}\n\n");

		if (aData.isDebug()) {
			source.append("\t\t/**\n");
			source.append("\t\t * Matches the provided value against this template. In legacy mode\n");
			source.append("\t\t * omitted value fields are not matched against the template field.\n");
			source.append("\t\t *\n");
			source.append("\t\t * @param otherValue\n");
			source.append("\t\t *                the value to be matched.\n");
			source.append("\t\t * @param legacy\n");
			source.append("\t\t *                use legacy mode.\n");
			source.append("\t\t * */\n");
		}
		source.append(MessageFormat.format("\t\tpublic boolean match(final {0} otherValue, final boolean legacy) '{'\n", name));
		source.append("\t\t\treturn match(otherValue.enum_value, false);\n");
		source.append("\t\t}\n\n");

		source.append("\t\t@Override\n");
		source.append(MessageFormat.format("\t\tpublic boolean match(final Base_Type otherValue, final boolean legacy)'{'\n", name));
		source.append(MessageFormat.format("\t\t\tif( otherValue instanceof {0} ) '{'\n", name));
		source.append(MessageFormat.format("\t\t\t\treturn match(({0}) otherValue, legacy);\n", name));
		source.append("\t\t\t}\n\n");
		source.append(MessageFormat.format("\t\t\tthrow new TtcnError(MessageFormat.format(\"Internal Error: value `{0}'' can not be cast to {1}\", otherValue));\n", name));
		source.append("\t\t}\n\n");
	}

	private static void generateTemplateValueOf(final StringBuilder source, final String name) {
		source.append("\t\t@Override\n");
		source.append(MessageFormat.format("\t\tpublic {0} valueof() '{'\n", name));
		source.append("\t\t\tif (template_selection != template_sel.SPECIFIC_VALUE || is_ifPresent) {\n");
		source.append(MessageFormat.format("\t\t\t\tthrow new TtcnError(\"Performing a valueof or send operation on a non-specific template of enumerated type {0}.\");\n", name));
		source.append("\t\t\t}\n");
		source.append(MessageFormat.format("\t\t\treturn new {0}(single_value);\n", name));
		source.append("\t\t}\n\n");
	}

	private static void generateTemplateListItem(final StringBuilder source, final String name) {
		source.append("\t\t@Override\n");
		source.append(MessageFormat.format("\t\tpublic {0}_template list_item(final int list_index) '{'\n", name));
		source.append("\t\t\tif (template_selection != template_sel.VALUE_LIST && template_selection != template_sel.COMPLEMENTED_LIST) {\n");
		source.append(MessageFormat.format("\t\t\t\tthrow new TtcnError(\"Accessing a list element of a non-list template of enumerated type {0}.\");\n", name));
		source.append("\t\t\t}\n");

		source.append("\t\t\tif (list_index < 0) {\n");
		source.append(MessageFormat.format("\t\t\t\tthrow new TtcnError(MessageFormat.format(\"Internal error: Accessing a value list template of type {0} using a negative index ('{'0'}').\", list_index));\n", name));
		source.append("\t\t\t} else if(list_index >= value_list.size()) {\n");
		source.append(MessageFormat.format("\t\t\t\tthrow new TtcnError(\"Index overflow in a value list template of enumerated type {0}.\");\n", name));
		source.append("\t\t\t}\n");
		source.append("\t\t\treturn value_list.get(list_index);\n");
		source.append("\t\t}\n\n");
	}

	private static void generateTemplateMatchOmit(final StringBuilder source) {
		source.append("\t\t@Override\n");
		source.append("\t\tpublic boolean match_omit(final boolean legacy) {\n");
		source.append("\t\t\tif (is_ifPresent) {\n");
		source.append("\t\t\t\treturn true;\n");
		source.append("\t\t\t}\n");
		source.append("\t\t\tswitch (template_selection) {\n");
		source.append("\t\t\tcase OMIT_VALUE:\n");
		source.append("\t\t\tcase ANY_OR_OMIT:\n");
		source.append("\t\t\t\treturn true;\n");
		source.append("\t\t\tcase VALUE_LIST:\n");
		source.append("\t\t\tcase COMPLEMENTED_LIST:\n");
		source.append("\t\t\t\tif (legacy) {\n");
		source.append("\t\t\t\t\tfor (int i = 0 ; i < value_list.size(); i++) {\n");
		source.append("\t\t\t\t\t\tif (value_list.get(i).match_omit()) {\n");
		source.append("\t\t\t\t\t\t\treturn template_selection == template_sel.VALUE_LIST;\n");
		source.append("\t\t\t\t\t\t}\n");
		source.append("\t\t\t\t\t}\n");
		source.append("\t\t\t\t\treturn template_selection == template_sel.COMPLEMENTED_LIST;\n");
		source.append("\t\t\t\t}\n");
		source.append("\t\t\tdefault:\n");
		source.append("\t\t\t\treturn false;\n");
		source.append("\t\t\t}\n");
		source.append("\t\t}\n\n");
	}

	private static void generateTemplateLog(final StringBuilder source, final String name) {
		source.append("\t\t@Override\n");
		source.append("\t\tpublic void log() {\n");
		source.append("\t\t\tswitch (template_selection) {\n");
		source.append("\t\t\tcase SPECIFIC_VALUE:\n");
		source.append(MessageFormat.format("\t\t\t\tTTCN_Logger.log_event_enum({0}.enum_to_str(single_value), {0}.enum2int(single_value));\n", name));
		source.append("\t\t\t\tbreak;\n");
		source.append("\t\t\tcase COMPLEMENTED_LIST:\n");
		source.append("\t\t\t\tTTCN_Logger.log_event_str(\"complement\");\n");
		source.append("\t\t\tcase VALUE_LIST:\n");
		source.append("\t\t\t\tTTCN_Logger.log_char('(');\n");
		source.append("\t\t\t\tfor (int list_count = 0; list_count < value_list.size(); list_count++) {\n");
		source.append("\t\t\t\t\tif (list_count > 0) {\n");
		source.append("\t\t\t\t\t\tTTCN_Logger.log_event_str(\", \");\n");
		source.append("\t\t\t\t\t}\n");
		source.append("\t\t\t\t\tvalue_list.get(list_count).log();\n");
		source.append("\t\t\t\t}\n");
		source.append("\t\t\t\tTTCN_Logger.log_char(')');\n");
		source.append("\t\t\t\tbreak;\n");
		source.append("\t\t\tdefault:\n");
		source.append("\t\t\t\tlog_generic();\n");
		source.append("\t\t\t\tbreak;\n");
		source.append("\t\t\t}\n");
		source.append("\t\t\tlog_ifpresent();\n");
		source.append("\t\t}\n");
	}

	private static void generateTemplateLogMatch(final JavaGenData aData, final StringBuilder source, final String name, final String displayName ){
		source.append("\t\t@Override\n");
		source.append("\t\tpublic void log_match(final Base_Type match_value, final boolean legacy) {\n");
		source.append(MessageFormat.format("\t\t\tif (match_value instanceof {0}) '{'\n", name));
		source.append(MessageFormat.format("\t\t\t\tlog_match(({0})match_value, legacy);\n", name));
		source.append("\t\t\t\treturn;\n");
		source.append("\t\t\t}\n\n");
		source.append(MessageFormat.format("\t\t\tthrow new TtcnError(\"Internal Error: value can not be cast to {0}.\");\n", displayName));
		source.append("\t\t}\n");

		if (aData.isDebug()) {
			source.append("\t\t/**\n");
			source.append("\t\t * Logs the matching of the provided value to this template, to help\n");
			source.append("\t\t * identify the reason for mismatch. In legacy mode omitted value fields\n");
			source.append("\t\t * are not matched against the template field.\n");
			source.append("\t\t *\n");
			source.append("\t\t * @param match_value\n");
			source.append("\t\t *                the value to be matched.\n");
			source.append("\t\t * @param legacy\n");
			source.append("\t\t *                use legacy mode.\n");
			source.append("\t\t * */\n");
		}
		source.append(MessageFormat.format("\t\tpublic void log_match(final {0} match_value, final boolean legacy)'{'\n",name));
		source.append("\t\t\tmatch_value.log();\n");
		source.append("\t\t\tTTCN_Logger.log_event_str(\" with \");\n");
		source.append("\t\t\tlog();\n");
		source.append("\t\t\tif (match(match_value, legacy)) {\n");
		source.append("\t\t\t\tTTCN_Logger.log_event_str(\" matched\");\n");
		source.append("\t\t\t} else {\n");
		source.append("\t\t\t\tTTCN_Logger.log_event_str(\" unmatched\");\n");
		source.append("\t\t\t}\n");
		source.append("\t\t}\n\n");
	}

	private static void generateTemplateSetParam(final StringBuilder source, final String name, final String displayName) {
		source.append("\t\t@Override\n");
		source.append("\t\tpublic void set_param(final Module_Parameter param) {\n");
		source.append("\t\t\tparam.basic_check(Module_Parameter.basic_check_bits_t.BC_TEMPLATE.getValue(), \"enumerated template\");\n");
		source.append("\t\t\tswitch (param.get_type()) {\n");
		source.append("\t\t\tcase MP_Omit:\n");
		source.append("\t\t\t\toperator_assign(template_sel.OMIT_VALUE);\n");
		source.append("\t\t\t\tbreak;\n");
		source.append("\t\t\tcase MP_Any:\n");
		source.append("\t\t\t\toperator_assign(template_sel.ANY_VALUE);\n");
		source.append("\t\t\t\tbreak;\n");
		source.append("\t\t\tcase MP_AnyOrNone:\n");
		source.append("\t\t\t\toperator_assign(template_sel.ANY_OR_OMIT);\n");
		source.append("\t\t\t\tbreak;\n");
		source.append("\t\t\tcase MP_List_Template:\n");
		source.append("\t\t\tcase MP_ComplementList_Template: {\n");
		source.append("\t\t\t\tfinal int size = param.get_size();\n");
		source.append("\t\t\t\tset_type(param.get_type() == Module_Parameter.type_t.MP_List_Template ? template_sel.VALUE_LIST : template_sel.COMPLEMENTED_LIST, size);\n");
		source.append("\t\t\t\tfor (int i = 0; i < size; i++) {\n");
		source.append("\t\t\t\t\tlist_item(i).set_param(param.get_elem(i));\n");
		source.append("\t\t\t\t}\n");
		source.append("\t\t\t\tbreak;\n");
		source.append("\t\t\t}\n");
		source.append("\t\t\tcase MP_Enumerated: {\n");
		source.append(MessageFormat.format("\t\t\t\tfinal {0}.enum_type enum_value = {0}.str_to_enum(param.get_enumerated());\n", name));
		source.append(MessageFormat.format("\t\t\t\tif (!{0}.is_valid_enum(enum_value)) '{'\n", name));
		source.append(MessageFormat.format("\t\t\t\t\tparam.error(\"Invalid enumerated value for type {0}.\");\n", displayName));
		source.append("\t\t\t\t}\n");
		source.append("\t\t\t\toperator_assign(enum_value);\n");
		source.append("\t\t\t\tbreak;\n");
		source.append("\t\t\t}\n");
		source.append("\t\t\tdefault:\n");
		source.append(MessageFormat.format("\t\t\t\tparam.type_error(\"enumerated template\", \"{0}\");\n", displayName));
		source.append("\t\t\t}\n");
		source.append("\t\t\tis_ifPresent = param.get_ifpresent();\n");
		source.append("\t\t}\n\n");
	}

	private static void generateTemplateEncodeDecodeText(final StringBuilder source, final String name, final String displayName) {
		source.append("\t\t@Override\n");
		source.append("\t\tpublic void encode_text(final Text_Buf text_buf) {\n");
		source.append("\t\t\tencode_text_base(text_buf);\n");
		source.append("\t\t\tswitch (template_selection) {\n");
		source.append("\t\t\tcase OMIT_VALUE:\n");
		source.append("\t\t\tcase ANY_VALUE:\n");
		source.append("\t\t\tcase ANY_OR_OMIT:\n");
		source.append("\t\t\t\tbreak;\n");
		source.append("\t\t\tcase SPECIFIC_VALUE:\n");
		source.append("\t\t\t\ttext_buf.push_int(single_value.getInt());\n");
		source.append("\t\t\t\tbreak;\n");
		source.append("\t\t\tcase VALUE_LIST:\n");
		source.append("\t\t\tcase COMPLEMENTED_LIST:\n");
		source.append("\t\t\t\ttext_buf.push_int(value_list.size());\n");
		source.append("\t\t\t\tfor (int i = 0; i < value_list.size(); i++) {\n");
		source.append("\t\t\t\t\tvalue_list.get(i).encode_text(text_buf);\n");
		source.append("\t\t\t\t}\n");
		source.append("\t\t\t\tbreak;\n");
		source.append("\t\t\tdefault:\n");
		source.append(MessageFormat.format("\t\t\t\tthrow new TtcnError(\"Text encoder: Encoding an uninitialized/unsupported template of enumerated type {0}.\");\n", displayName));
		source.append("\t\t\t}\n");
		source.append("\t\t}\n\n");

		source.append("\t\t@Override\n");
		source.append("\t\tpublic void decode_text(final Text_Buf text_buf) {\n");
		source.append("\t\t\tclean_up();\n");
		source.append("\t\t\tdecode_text_base(text_buf);\n");
		source.append("\t\t\tswitch (template_selection) {\n");
		source.append("\t\t\tcase OMIT_VALUE:\n");
		source.append("\t\t\tcase ANY_VALUE:\n");
		source.append("\t\t\tcase ANY_OR_OMIT:\n");
		source.append("\t\t\t\tbreak;\n");
		source.append("\t\t\tcase SPECIFIC_VALUE:{\n");
		source.append("\t\t\t\tfinal int temp = text_buf.pull_int().get_int();\n");
		source.append(MessageFormat.format("\t\t\t\tif (!{0}.is_valid_enum(temp)) '{'\n", name));
		source.append(MessageFormat.format("\t\t\t\t\tthrow new TtcnError(MessageFormat.format(\"Text decoder: Unknown numeric value '{'0'}' was received for enumerated type {0}.\", temp));\n", displayName));
		source.append("\t\t\t\t}\n");
		source.append(MessageFormat.format("\t\t\t\tsingle_value = {0}.enum_type.values()[temp];\n", name));
		source.append("\t\t\t\tbreak;\n");
		source.append("\t\t\t}\n");
		source.append("\t\t\tcase VALUE_LIST:\n");
		source.append("\t\t\tcase COMPLEMENTED_LIST: {\n");
		source.append("\t\t\t\tfinal int size = text_buf.pull_int().get_int();\n");
		source.append(MessageFormat.format("\t\t\t\tvalue_list = new ArrayList<{0}_template>(size);\n", name));
		source.append("\t\t\t\tfor (int i = 0; i < size; i++) {\n");
		source.append(MessageFormat.format("\t\t\t\t\tfinal {0}_template temp = new {0}_template();\n", name));
		source.append("\t\t\t\t\ttemp.decode_text(text_buf);\n");
		source.append("\t\t\t\t\tvalue_list.add(temp);\n");
		source.append("\t\t\t\t}\n");
		source.append("\t\t\t\tbreak;\n");
		source.append("\t\t\t}\n");
		source.append("\t\t\tdefault:\n");
		source.append(MessageFormat.format("\t\t\t\tthrow new TtcnError(\"Text decoder: An unknown/unsupported selection was received for a template of enumerated type {0}.\");\n", displayName));
		source.append("\t\t\t}\n");
		source.append("\t\t}\n\n");
	}

	private static void generateTemplateCheckRestriction(final StringBuilder source, final String displayName) {
		source.append("\t\t@Override\n");
		source.append("\t\tpublic void check_restriction(final template_res restriction, final String name, final boolean legacy) {\n");
		source.append("\t\t\tif (template_selection == template_sel.UNINITIALIZED_TEMPLATE) {\n");
		source.append("\t\t\t\treturn;\n");
		source.append("\t\t\t}\n");
		source.append("\t\t\tswitch ((name != null && restriction == template_res.TR_VALUE) ? template_res.TR_OMIT : restriction) {\n");
		source.append("\t\t\tcase TR_VALUE:\n");
		source.append("\t\t\t\tif (!is_ifPresent && template_selection == template_sel.SPECIFIC_VALUE) {\n");
		source.append("\t\t\t\t\treturn;\n");
		source.append("\t\t\t\t}\n");
		source.append("\t\t\t\tbreak;\n");
		source.append("\t\t\tcase TR_OMIT:\n");
		source.append("\t\t\t\tif (!is_ifPresent && (template_selection == template_sel.OMIT_VALUE || template_selection == template_sel.SPECIFIC_VALUE)) {\n");
		source.append("\t\t\t\t\treturn;\n");
		source.append("\t\t\t\t}\n");
		source.append("\t\t\t\tbreak;\n");
		source.append("\t\t\tcase TR_PRESENT:\n");
		source.append("\t\t\t\tif (!match_omit(legacy)) {\n");
		source.append("\t\t\t\t\treturn;\n");
		source.append("\t\t\t\t}\n");
		source.append("\t\t\t\tbreak;\n");
		source.append("\t\t\tdefault:\n");
		source.append("\t\t\t\treturn;\n");
		source.append("\t\t\t}\n");
		source.append(MessageFormat.format("\t\t\tthrow new TtcnError(MessageFormat.format(\"Restriction `'{'0'}''''' on template of type '{'1'}' violated.\", get_res_name(restriction), name == null ? \"{0}\" : name));\n", displayName));
		source.append("\t\t}\n");
	}
}
