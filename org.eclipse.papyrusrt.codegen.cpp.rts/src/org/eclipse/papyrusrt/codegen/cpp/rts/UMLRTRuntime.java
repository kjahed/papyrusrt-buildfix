/*******************************************************************************
 * Copyright (c) 2014-2015 Zeligsoft (2009) Limited and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.papyrusrt.codegen.cpp.rts;

import org.eclipse.papyrusrt.codegen.lang.cpp.Element;
import org.eclipse.papyrusrt.codegen.lang.cpp.Expression;
import org.eclipse.papyrusrt.codegen.lang.cpp.Name;
import org.eclipse.papyrusrt.codegen.lang.cpp.Type;
import org.eclipse.papyrusrt.codegen.lang.cpp.Type.CVQualifier;
import org.eclipse.papyrusrt.codegen.lang.cpp.element.CppEnum;
import org.eclipse.papyrusrt.codegen.lang.cpp.element.Function;
import org.eclipse.papyrusrt.codegen.lang.cpp.element.LinkageSpec;
import org.eclipse.papyrusrt.codegen.lang.cpp.element.MemberField;
import org.eclipse.papyrusrt.codegen.lang.cpp.element.MemberFunction;
import org.eclipse.papyrusrt.codegen.lang.cpp.element.Parameter;
import org.eclipse.papyrusrt.codegen.lang.cpp.element.PrimitiveType;
import org.eclipse.papyrusrt.codegen.lang.cpp.element.Variable;
import org.eclipse.papyrusrt.codegen.lang.cpp.expr.AbstractFunctionCall;
import org.eclipse.papyrusrt.codegen.lang.cpp.expr.ElementAccess;
import org.eclipse.papyrusrt.codegen.lang.cpp.expr.MemberAccess;
import org.eclipse.papyrusrt.codegen.lang.cpp.expr.MemberFunctionCall;
import org.eclipse.papyrusrt.codegen.lang.cpp.external.ExternalConstructorCall;
import org.eclipse.papyrusrt.codegen.lang.cpp.external.ExternalElement;
import org.eclipse.papyrusrt.codegen.lang.cpp.external.ExternalFwdDeclarable;
import org.eclipse.papyrusrt.codegen.lang.cpp.external.ExternalHeaderFile;
import org.eclipse.papyrusrt.codegen.lang.cpp.external.ExternalTemplateName;
import org.eclipse.papyrusrt.codegen.lang.cpp.external.StandardLibrary;
import org.eclipse.papyrusrt.xtumlrt.common.NamedElement;
import org.eclipse.papyrusrt.xtumlrt.common.Protocol;
import org.eclipse.papyrusrt.xtumlrt.common.Signal;
import org.eclipse.papyrusrt.xtumlrt.external.predefined.RTSModelLibraryUtils;
import org.eclipse.papyrusrt.xtumlrt.util.XTUMLRTUtil;

/**
 * A utility namespace that defines all elements of the RTS in terms of the C++
 * language model.
 */
public final class UMLRTRuntime {

	/**
	 * This is a utility class, so it's constructor should be private to prevent being used as a normal object.
	 */
	private UMLRTRuntime() {
	}

	private static final ExternalHeaderFile umlrtcapsule_hh = new ExternalHeaderFile("umlrtcapsule.hh");
	private static final ExternalHeaderFile umlrtcapsuleclass_hh = new ExternalHeaderFile("umlrtcapsuleclass.hh");
	private static final ExternalHeaderFile umlrtcapsuleid_hh = new ExternalHeaderFile("umlrtcapsuleid.hh");
	private static final ExternalHeaderFile umlrtcapsulepart_hh = new ExternalHeaderFile("umlrtcapsulepart.hh");
	private static final ExternalHeaderFile umlrtcapsulerole_hh = new ExternalHeaderFile("umlrtcapsulerole.hh");
	private static final ExternalHeaderFile umlrtcommsport_hh = new ExternalHeaderFile("umlrtcommsport.hh");
	private static final ExternalHeaderFile umlrtcommsportfarend_hh = new ExternalHeaderFile("umlrtcommsportfarend.hh");
	private static final ExternalHeaderFile umlrtcommsportmap_hh = new ExternalHeaderFile("umlrtcommsportmap.hh");
	private static final ExternalHeaderFile umlrtcommsportrole_hh = new ExternalHeaderFile("umlrtcommsportrole.hh");
	private static final ExternalHeaderFile umlrtcontroller_hh = new ExternalHeaderFile("umlrtcontroller.hh");
	private static final ExternalHeaderFile umlrtframeprotocol_hh = new ExternalHeaderFile("umlrtframeprotocol.hh");
	private static final ExternalHeaderFile umlrtframeservice_hh = new ExternalHeaderFile("umlrtframeservice.hh");
	private static final ExternalHeaderFile umlrtinsignal_hh = new ExternalHeaderFile("umlrtinsignal.hh");
	private static final ExternalHeaderFile umlrtinoutsignal_hh = new ExternalHeaderFile("umlrtinoutsignal.hh");
	private static final ExternalHeaderFile umlrtlogprotocol_hh = new ExternalHeaderFile("umlrtlogprotocol.hh");
	private static final ExternalHeaderFile umlrtmessage_hh = new ExternalHeaderFile("umlrtmessage.hh");
	private static final ExternalHeaderFile umlrtobjectclass_hh = new ExternalHeaderFile("umlrtobjectclass.hh");
	private static final ExternalHeaderFile umlrtobjectclassgeneric_hh = new ExternalHeaderFile("umlrtobjectclassgeneric.hh");
	private static final ExternalHeaderFile umlrtoutsignal_hh = new ExternalHeaderFile("umlrtoutsignal.hh");
	private static final ExternalHeaderFile umlrtprotocol_hh = new ExternalHeaderFile("umlrtprotocol.hh");
	private static final ExternalHeaderFile umlrtrtsinterface_hh = new ExternalHeaderFile("umlrtrtsinterface.hh");
	private static final ExternalHeaderFile umlrtsignal_hh = new ExternalHeaderFile("umlrtsignal.hh");
	private static final ExternalHeaderFile umlrtslot_hh = new ExternalHeaderFile("umlrtslot.hh");
	private static final ExternalHeaderFile umlrttimerid_hh = new ExternalHeaderFile("umlrttimerid.hh");
	private static final ExternalHeaderFile umlrttimerprotocol_hh = new ExternalHeaderFile("umlrttimerprotocol.hh");
	private static final ExternalHeaderFile umlrttimespec_hh = new ExternalHeaderFile("umlrttimespec.hh");

	public static class UMLRTBaseCommProtocol {
		public static Expression Signal(Signal signal) {
			if ("rtBound".equals(signal.getName()))
				return UMLRTSignal.rtBound();
			if ("rtUnbound".equals(signal.getName()))
				return UMLRTSignal.rtUnbound();
			return null;
		}
	}

	public static class UMLRTCapsule {
		public static final ExternalElement Element = new ExternalFwdDeclarable(umlrtcapsule_hh, "UMLRTCapsule", "class UMLRTCapsule");

		public static Type getType() {
			return Element.getType();
		}

		public static final AbstractFunctionCall Ctor(Expression rtsif, Expression capsuleClass, Expression slot, Expression borderPorts, Expression internalPorts, Expression isStatic) {
			AbstractFunctionCall call = new ExternalConstructorCall(Element);
			call.addArgument(rtsif);
			call.addArgument(capsuleClass);
			call.addArgument(slot);
			call.addArgument(borderPorts);
			call.addArgument(internalPorts);
			call.addArgument(isStatic);
			return call;
		}

		public static final MemberField slotField = new MemberField(UMLRTRuntime.UMLRTSlot.getType().constPtr(), "slot");
		public static final MemberField borderPortsField = new MemberField(UMLRTRuntime.UMLRTCommsPort.getType().const_().ptr().ptr(), "borderPorts");
		public static final MemberField internalPortsField = new MemberField(UMLRTRuntime.UMLRTCommsPort.getType().const_().ptr().ptr(), "internalPorts");
		public static final MemberField msgField = new MemberField(UMLRTMessage.getType().const_().ptr(), "msg");

		public static final MemberFunction bindPortFunction = new MemberFunction(PrimitiveType.VOID, "bindPort");
		public static final MemberFunction unbindPortFunction = new MemberFunction(PrimitiveType.VOID, "unbindPort");
		public static final MemberFunction bindPortInstanceFunction = new MemberFunction(PrimitiveType.VOID, "bindPortInstance");

		public static final MemberFunction unexpectedMessageFunction = new MemberFunction(PrimitiveType.VOID, "unexpectedMessage", CVQualifier.CONST);
		public static final MemberFunction getCurrentStateStringFunction = new MemberFunction(PrimitiveType.CHAR.ptr().const_(), "getCurrentStateString", CVQualifier.CONST);
		public static final MemberFunction getMsgFunction = new MemberFunction(UMLRTMessage.getType().const_().ptr(), "getMsg", CVQualifier.CONST);

		static {
			bindPortFunction.add(new Parameter(PrimitiveType.INT, "portId"));
			unbindPortFunction.add(new Parameter(PrimitiveType.INT, "portId"));
			bindPortInstanceFunction.add(new Parameter(PrimitiveType.INT, "portId"));
			bindPortInstanceFunction.add(new Parameter(PrimitiveType.INT, "instance"));
			unexpectedMessageFunction.setVirtual();
		}

		public static ElementAccess slot() {
			return new ElementAccess(slotField);
		}

		public static ElementAccess borderPorts() {
			return new ElementAccess(borderPortsField);
		}

		public static ElementAccess internalPorts() {
			return new ElementAccess(internalPortsField);
		}

		public static ElementAccess msg() {
			return new ElementAccess(msgField);
		}

		public static AbstractFunctionCall bindPort(Expression self, Expression isBorder, Expression portId) {
			MemberFunctionCall call = new MemberFunctionCall(self, bindPortFunction);
			call.addArgument(isBorder);
			call.addArgument(portId);
			return call;
		}

		public static AbstractFunctionCall unbindPort(Expression self, Expression isBorder, Expression portId) {
			MemberFunctionCall call = new MemberFunctionCall(self, unbindPortFunction);
			call.addArgument(isBorder);
			call.addArgument(portId);
			return call;
		}

		public static AbstractFunctionCall bindPortInstance(Expression self, Expression isBorder, Expression portId, Expression instance) {
			MemberFunctionCall call = new MemberFunctionCall(self, bindPortInstanceFunction);
			call.addArgument(isBorder);
			call.addArgument(portId);
			call.addArgument(instance);
			return call;
		}

		public static AbstractFunctionCall unexpectedMessage(Expression self) {
			return new MemberFunctionCall(self, unexpectedMessageFunction);
		}

		public static AbstractFunctionCall getMsg(Expression self) {
			return new MemberFunctionCall(self, getMsgFunction);
		}
	}

	public static class UMLRTCapsuleClass {
		public static final ExternalElement Element = new ExternalFwdDeclarable(umlrtcapsuleclass_hh, "UMLRTCapsuleClass", "struct UMLRTCapsuleClass");

		public static Type getType() {
			return Element.getType();
		}

		public static final MemberFunction instantiate = new MemberFunction(PrimitiveType.VOID, "instantiate");

		public static final MemberField subcapsuleRoles = new MemberField(UMLRTCapsuleRole.getType().const_().constPtr(), "subcapsuleRoles");
		public static final MemberField portRolesBorder = new MemberField(StandardLibrary.size_t, "portRolesBorder");
		public static final MemberField numPortRolesBorder = new MemberField(UMLRTCommsPortRole.getType().constPtr().const_(), "numPortRolesBorder");
		public static final MemberField numPortRolesInternal = new MemberField(StandardLibrary.size_t, "numPortRolesInternal");
		public static final MemberField portRolesInternal = new MemberField(UMLRTCommsPortRole.getType().constPtr().const_(), "portRolesInternal");

		static {
			instantiate.add(new Parameter(UMLRTSlot.getType().ptr(), "slot"));
			instantiate.add(new Parameter(UMLRTCommsPortMap.getType().ptr(), "portMap"));
		}
	}

	public static class UMLRTCapsuleId {
		public static final ExternalElement Element = new ExternalFwdDeclarable(umlrtcapsuleid_hh, "UMLRTCapsuleId", "class UMLRTCapsuleId");

		public static Type getType() {
			return Element.getType();
		}

		public static final Variable RTType = new Variable(LinkageSpec.EXTERN, UMLRTObject.getType().ptr().constPtr(), "UMLRTType_UMLRTCapsuleId");
		static {
			RTType.setDefinedIn(umlrtcapsuleid_hh);
		}

		public static Expression createRTTypeAccess() {
			return new ElementAccess(RTType);
		}

		public static final AbstractFunctionCall Ctor(Expression capsule) {
			AbstractFunctionCall call = new ExternalConstructorCall(Element);
			call.addArgument(capsule);
			return call;
		}
	}

	public static class UMLRTCapsulePart {
		public static final ExternalElement Element = new ExternalFwdDeclarable(umlrtcapsulepart_hh, "UMLRTCapsulePart", "struct UMLRTCapsulePart");

		public static Type getType() {
			return Element.getType();
		}

		public static final MemberField slots = new MemberField(UMLRTSlot.getType().ptr().ptr(), "slots");
	}

	public static class UMLRTCapsuleRole {
		public static final ExternalElement Element = new ExternalFwdDeclarable(umlrtcapsulerole_hh, "UMLRTCapsuleRole", "struct UMLRTCapsuleRole");

		public static Type getType() {
			return Element.getType();
		}
	}

	public static class UMLRTCommsPort {
		public static final ExternalElement Element = new ExternalFwdDeclarable(umlrtcommsport_hh, "UMLRTCommsPort", "struct UMLRTCommsPort");

		public static Type getType() {
			return Element.getType();
		}

		public static final MemberFunction roleFunction = new MemberFunction(UMLRTCommsPortRole.getType().ptr().const_(), "role");
		public static final MemberFunction getFunction = new MemberFunction(UMLRTCommsPort.getType().ptr().const_(), "get");
		public static final MemberField farEnds = new MemberField(UMLRTCommsPortFarEnd.getType().ptr(), "farEnds");

		// TODO Match the id field to the PortId enum.

		static {
			getFunction.add(new Parameter(PrimitiveType.INT.arrayOf(null), "map"));
			getFunction.add(new Parameter(UMLRTCommsPort.getType().ptr().const_(), "ports"));
			getFunction.add(new Parameter(StandardLibrary.size_t, "index"));
		}

		public static AbstractFunctionCall role(Expression self) {
			return new MemberFunctionCall(self, roleFunction);
		}

		public static AbstractFunctionCall get(Expression portMap, Expression ports, Expression index) {
			AbstractFunctionCall call = new MemberFunctionCall(Element, getFunction);
			call.addArgument(portMap);
			call.addArgument(ports);
			call.addArgument(index);
			return call;
		}
	}

	public static class UMLRTCommsPortFarEnd {
		public static final ExternalElement Element = new ExternalFwdDeclarable(umlrtcommsportfarend_hh, "UMLRTCommsPortFarEnd", "struct UMLRTCommsPortFarEnd");

		public static Type getType() {
			return Element.getType();
		}

		public static final MemberField index = new MemberField(StandardLibrary.size_t, "farEndIndex");
		public static final MemberField port = new MemberField(UMLRTCommsPort.getType().ptr().const_(), "port");
	}

	public static class UMLRTCommsPortMap {
		public static final ExternalElement Element = new ExternalFwdDeclarable(umlrtcommsportmap_hh, "UMLRTCommsPortMap", "struct UMLRTCommsPortMap");

		public static Type getType() {
			return Element.getType();
		}

		public static final MemberField port = new MemberField(PrimitiveType.INT, "port");
	}

	public static class UMLRTCommsPortRole {
		public static final ExternalElement Element = new ExternalFwdDeclarable(umlrtcommsportrole_hh, "UMLRTCommsPortRole", "struct UMLRTCommsPortRole");

		public static Type getType() {
			return Element.getType();
		}

		// TODO Match the roleIndex to InternalPortId/BorderPortId

		public static final MemberField id = new MemberField(PrimitiveType.INT, "id");
		public static final MemberField conjugated = new MemberField(PrimitiveType.BOOL, "conjugated");
	}

	public static class UMLRTController {
		public static final ExternalElement Element = new ExternalFwdDeclarable(umlrtcontroller_hh, "UMLRTController", "class UMLRTController");

		public static Type getType() {
			return Element.getType();
		}

		public static final AbstractFunctionCall Ctor(Expression name) {
			AbstractFunctionCall call = new ExternalConstructorCall(Element);
			call.addArgument(name);
			return call;
		}
	}

	public static class UMLRTLogProtocol {
		public static final ExternalElement Element = new ExternalFwdDeclarable(umlrtlogprotocol_hh, "UMLRTLogProtocol", "class UMLRTLogProtocol");
		public static final ExternalElement BaseRole = new ExternalFwdDeclarable(umlrtlogprotocol_hh, "UMLRTLogProtocol_baserole", "class UMLRTLogProtocol_baserole");

		public static Type getBaseRoleType() {
			return BaseRole.getType();
		}

		public static boolean needsUMLRTCommsPort() {
			return false;
		}
	}

	public static class UMLRTMessage {
		public static final ExternalElement Element = new ExternalFwdDeclarable(umlrtmessage_hh, "UMLRTMessage", "class UMLRTMessage");

		public static Type getType() {
			return Element.getType();
		}

		public static final MemberField destPort = new MemberField(UMLRTCommsPort.Element.getType().ptr().const_(), "destPort");
		public static final MemberField signal = new MemberField(UMLRTSignal.Element.getType(), "signal");
		private static final MemberField desc = new MemberField(UMLRTObject.Element.getType().const_().ptr(), "desc");

		private static final MemberFunction getSignalId_f = new MemberFunction(PrimitiveType.INT, "getSignalId");
		private static final MemberFunction getSignalName_f = new MemberFunction(PrimitiveType.CHAR.ptr().const_(), "getSignalName");
		private static final MemberFunction getParam_f = new MemberFunction(PrimitiveType.VOID, "getParam");

		public static Expression desc(Expression msg) {
			return new MemberAccess(msg, desc);
		}

		public static AbstractFunctionCall getSignalId(Expression msg) {
			return new MemberFunctionCall(msg, getSignalId_f);
		}

		public static AbstractFunctionCall getSignalName(Expression msg) {
			return new MemberFunctionCall(msg, getSignalName_f);
		}

		public static AbstractFunctionCall getParam(Expression msg, Expression index) {
			MemberFunctionCall call = new MemberFunctionCall(msg, getParam_f);
			call.addArgument(index);
			return call;
		}
	}

	public static class UMLRTProtocol {
		public static final ExternalElement Element = new ExternalFwdDeclarable(umlrtprotocol_hh, "UMLRTProtocol", "class UMLRTProtocol");

		public static Type getType() {
			return Element.getType();
		}

		private static final MemberField srcPort = new MemberField(UMLRTCommsPort.Element.getType().constPtr().const_(), "srcPort");

		public static AbstractFunctionCall Ctor() {
			return new ExternalConstructorCall(Element);
		}

		public static Expression srcPort() {
			return new ElementAccess(srcPort);
		}
	}

	public static class UMLRTRtsInterface {
		public static final ExternalElement Element = new ExternalFwdDeclarable(umlrtrtsinterface_hh, "UMLRTRtsInterface", "class UMLRTRtsInterface");

		public static Type getType() {
			return Element.getType();
		}
	}

	public static class UMLRTSignal {
		public static final ExternalElement Element = new ExternalFwdDeclarable(umlrtsignal_hh, "UMLRTSignal", "class UMLRTSignal");

		public static Type getType() {
			return Element.getType();
		}

		public static final MemberFunction getId = new MemberFunction(PrimitiveType.INT, "getId");
		public static final MemberFunction getPayloadFunction = new MemberFunction(PrimitiveType.UCHAR.ptr(), "getPayload");
		private static final MemberFunction initialize_f = new MemberFunction(PrimitiveType.VOID, "initialize");
		public static final MemberFunction isInvalid = new MemberFunction(PrimitiveType.BOOL, "isInvalid");

		public static final MemberField rtBound_field = new MemberField(PrimitiveType.INT, "rtBound");
		public static final MemberField rtUnbound_field = new MemberField(PrimitiveType.INT, "rtUnbound");
		public static final MemberField FIRST_PROTOCOL_SIGNAL_ID_field = new MemberField(PrimitiveType.INT, "FIRST_PROTOCOL_SIGNAL_ID");

		static {
			rtBound_field.getName().setParent(Element);
			rtUnbound_field.getName().setParent(Element);
			FIRST_PROTOCOL_SIGNAL_ID_field.getName().setParent(Element);
		}

		public static AbstractFunctionCall getPayload(Expression signal) {
			return new MemberFunctionCall(signal, getPayloadFunction);
		}

		public static Expression rtBound() {
			return new MemberAccess(Element, rtBound_field);
		}

		public static Expression rtUnbound() {
			return new MemberAccess(Element, rtUnbound_field);
		}

		public static Expression FIRST_PROTOCOL_SIGNAL_ID() {
			return new MemberAccess(Element, FIRST_PROTOCOL_SIGNAL_ID_field);
		}

		public static AbstractFunctionCall initialize(Expression signal, Expression name, Expression id, Expression srcPort, Expression payloadDesc) {
			MemberFunctionCall call = new MemberFunctionCall(signal, UMLRTRuntime.UMLRTSignal.initialize_f);
			call.addArgument(name);
			call.addArgument(id);
			call.addArgument(srcPort);
			call.addArgument(payloadDesc);
			return call;
		}

		public static AbstractFunctionCall initialize(Expression signal, Expression name, Expression id, Expression srcPort, Expression desc, Expression data) {
			MemberFunctionCall call = new MemberFunctionCall(signal, UMLRTRuntime.UMLRTSignal.initialize_f);
			call.addArgument(name);
			call.addArgument(id);
			call.addArgument(srcPort);
			call.addArgument(desc);
			call.addArgument(data);
			return call;
		}

		public static AbstractFunctionCall initialize(Expression signal, Expression name, Expression id) {
			MemberFunctionCall call = new MemberFunctionCall(signal, UMLRTRuntime.UMLRTSignal.initialize_f);
			call.addArgument(name);
			call.addArgument(id);
			return call;
		}
	}

	public static class UMLRTInSignal extends UMLRTSignal {
		public static final ExternalElement Element = new ExternalFwdDeclarable(umlrtinsignal_hh, "UMLRTInSignal", "class UMLRTInSignal");

		public static Type getType() {
			return Element.getType();
		}

		private static final MemberFunction decode_f = new MemberFunction(PrimitiveType.VOID, "decode");

		public static AbstractFunctionCall decode(Expression signal, Expression decodeInfo, Expression data, Expression desc) {
			AbstractFunctionCall call = new MemberFunctionCall(signal, decode_f);
			call.addArgument(decodeInfo);
			call.addArgument(data);
			call.addArgument(desc);
			return call;
		}
	}

	public static class UMLRTInOutSignal extends UMLRTSignal {
		public static final ExternalElement Element = new ExternalFwdDeclarable(umlrtinoutsignal_hh, "UMLRTInOutSignal", "class UMLRTInOutSignal");

		public static Type getType() {
			return Element.getType();
		}

	}

	public static class UMLRTObject {
		public static final ExternalElement Element = new ExternalFwdDeclarable(umlrtobjectclass_hh, "UMLRTObject_class", "struct UMLRTObject_class");

		public static Type getType() {
			return Element.getType();
		}

		public static final ExternalElement Field = new ExternalFwdDeclarable(umlrtobjectclass_hh, "UMLRTObject_field", "struct UMLRTObject_field");

		public static Type getFieldType() {
			return Field.getType();
		}

		public static final ExternalElement Object = new ExternalFwdDeclarable(umlrtobjectclass_hh, "UMLRTObject", "struct UMLRTObject");

		public static Type getObjectType() {
			return Object.getType();
		}

		public static class UMLRTTypedValue {
			public static final ExternalElement Element = new ExternalFwdDeclarable(umlrtobjectclass_hh, "UMLRTTypedValue", "struct UMLRTTypedValue");

			public static Type getType() {
				return Element.getType();
			}

			public static final MemberField type = new MemberField(UMLRTObject.getType().constPtr(), "type");
			public static final MemberField data = new MemberField(PrimitiveType.VOID.ptr(), "data");
		}

		private static final ExternalElement DEFAULT_VERSION_t = new ExternalElement(umlrtobjectclass_hh, "UMLRTOBJECTCLASS_DEFAULT_VERSION");
		private static final ExternalElement DEFAULT_BACKWARDS_t = new ExternalElement(umlrtobjectclass_hh, "UMLRTOBJECTCLASS_DEFAULT_BACKWARDS");

		private static final MemberFunction getSize_f = new MemberFunction(StandardLibrary.size_t, "getSize");
		private static final MemberField sizeDecoded = new MemberField(PrimitiveType.INT, "sizeDecoded");

		private static final ExternalElement UMLRTType_bool_t = new ExternalElement(umlrtobjectclass_hh, getType().const_(), "UMLRTType_bool");
		private static final ExternalElement UMLRTType_char_t = new ExternalElement(umlrtobjectclass_hh, getType().const_(), "UMLRTType_char");
		private static final ExternalElement UMLRTType_double_t = new ExternalElement(umlrtobjectclass_hh, getType().const_(), "UMLRTType_double");
		private static final ExternalElement UMLRTType_float_t = new ExternalElement(umlrtobjectclass_hh, getType().const_(), "UMLRTType_float");
		private static final ExternalElement UMLRTType_int_t = new ExternalElement(umlrtobjectclass_hh, getType().const_(), "UMLRTType_int");
		private static final ExternalElement UMLRTType_long_t = new ExternalElement(umlrtobjectclass_hh, getType().const_(), "UMLRTType_long");
		private static final ExternalElement UMLRTType_longdouble_t = new ExternalElement(umlrtobjectclass_hh, getType().const_(), "UMLRTType_longdouble");
		private static final ExternalElement UMLRTType_longlong_t = new ExternalElement(umlrtobjectclass_hh, getType().const_(), "UMLRTType_longlong");
		private static final ExternalElement UMLRTType_ptr_t = new ExternalElement(umlrtobjectclass_hh, getType().const_(), "UMLRTType_ptr");
		private static final ExternalElement UMLRTType_charptr_t = new ExternalElement(umlrtobjectclass_hh, getType().const_(), "UMLRTType_charptr");
		private static final ExternalElement UMLRTType_short_t = new ExternalElement(umlrtobjectclass_hh, getType().const_(), "UMLRTType_short");
		private static final ExternalElement UMLRTType_uchar_t = new ExternalElement(umlrtobjectclass_hh, getType().const_(), "UMLRTType_uchar");
		private static final ExternalElement UMLRTType_uint_t = new ExternalElement(umlrtobjectclass_hh, getType().const_(), "UMLRTType_uint");
		private static final ExternalElement UMLRTType_ulong_t = new ExternalElement(umlrtobjectclass_hh, getType().const_(), "UMLRTType_ulong");
		private static final ExternalElement UMLRTType_ulonglong_t = new ExternalElement(umlrtobjectclass_hh, getType().const_(), "UMLRTType_ulonglong");
		private static final ExternalElement UMLRTType_ushort_t = new ExternalElement(umlrtobjectclass_hh, getType().const_(), "UMLRTType_ushort");

		private static final Function UMLRTObject_initialize_f = new Function(umlrtobjectclass_hh, LinkageSpec.EXTERN, PrimitiveType.VOID.ptr(), "UMLRTObject_initialize");
		private static final Function UMLRTObject_copy_f = new Function(umlrtobjectclass_hh, LinkageSpec.EXTERN, PrimitiveType.VOID.ptr(), "UMLRTObject_copy");
		private static final Function UMLRTObject_decode_f = new Function(umlrtobjectclass_hh, LinkageSpec.EXTERN, PrimitiveType.VOID.ptr(), "UMLRTObject_decode");
		private static final Function UMLRTObject_encode_f = new Function(umlrtobjectclass_hh, LinkageSpec.EXTERN, PrimitiveType.VOID.ptr(), "UMLRTObject_encode");
		private static final Function UMLRTObject_destroy_f = new Function(umlrtobjectclass_hh, LinkageSpec.EXTERN, PrimitiveType.VOID.ptr(), "UMLRTObject_destroy");
		private static final Function UMLRTObject_getSize_f = new Function(umlrtobjectclass_hh, LinkageSpec.EXTERN, StandardLibrary.size_t, "UMLRTObject_getSize");
		private static final Function UMLRTObject_fprintf_f = new Function(umlrtobjectclass_hh, LinkageSpec.EXTERN, PrimitiveType.INT, "UMLRTObject_fprintf");

		public static Expression DEFAULT_VERSION() {
			return new ElementAccess(DEFAULT_VERSION_t);
		}

		public static Expression DEFAULT_BACKWARDS() {
			return new ElementAccess(DEFAULT_BACKWARDS_t);
		}

		public static Expression UMLRTType_bool() {
			return new ElementAccess(UMLRTType_bool_t);
		}

		public static Expression UMLRTType_char() {
			return new ElementAccess(UMLRTType_char_t);
		}

		public static Expression UMLRTType_double() {
			return new ElementAccess(UMLRTType_double_t);
		}

		public static Expression UMLRTType_float() {
			return new ElementAccess(UMLRTType_float_t);
		}

		public static Expression UMLRTType_int() {
			return new ElementAccess(UMLRTType_int_t);
		}

		public static Expression UMLRTType_long() {
			return new ElementAccess(UMLRTType_long_t);
		}

		public static Expression UMLRTType_longdouble() {
			return new ElementAccess(UMLRTType_longdouble_t);
		}

		public static Expression UMLRTType_longLong() {
			return new ElementAccess(UMLRTType_longlong_t);
		}

		public static Expression UMLRTType_ptr() {
			return new ElementAccess(UMLRTType_ptr_t);
		}

		public static Expression UMLRTType_charptr() {
			return new ElementAccess(UMLRTType_charptr_t);
		}

		public static Expression UMLRTType_short() {
			return new ElementAccess(UMLRTType_short_t);
		}

		public static Expression UMLRTType_uchar() {
			return new ElementAccess(UMLRTType_uchar_t);
		}

		public static Expression UMLRTType_uint() {
			return new ElementAccess(UMLRTType_uint_t);
		}

		public static Expression UMLRTType_ulong() {
			return new ElementAccess(UMLRTType_ulong_t);
		}

		public static Expression UMLRTType_ulongLong() {
			return new ElementAccess(UMLRTType_ulonglong_t);
		}

		public static Expression UMLRTType_ushort() {
			return new ElementAccess(UMLRTType_ushort_t);
		}

		public static Expression UMLRTType(Type type) {
			if (type == null) {
				return null;
			}

			if (type.isIndirect()
					&& !type.isArray()) {
				if (type.getElement() == PrimitiveType.CHAR.getElement()) {
					return UMLRTRuntime.UMLRTObject.UMLRTType_charptr();
				}
				return UMLRTRuntime.UMLRTObject.UMLRTType_ptr();
			}

			if (type.getElement() == PrimitiveType.CHAR.getElement())
				return UMLRTRuntime.UMLRTObject.UMLRTType_char();
			if (type.getElement() == PrimitiveType.SHORT.getElement())
				return UMLRTRuntime.UMLRTObject.UMLRTType_short();
			if (type.getElement() == PrimitiveType.INT.getElement())
				return UMLRTRuntime.UMLRTObject.UMLRTType_int();
			if (type.getElement() == PrimitiveType.LONG.getElement())
				return UMLRTRuntime.UMLRTObject.UMLRTType_long();
			if (type.getElement() == PrimitiveType.LONGLONG.getElement())
				return UMLRTRuntime.UMLRTObject.UMLRTType_longLong();

			if (type.getElement() == PrimitiveType.UCHAR.getElement())
				return UMLRTRuntime.UMLRTObject.UMLRTType_uchar();
			if (type.getElement() == PrimitiveType.USHORT.getElement())
				return UMLRTRuntime.UMLRTObject.UMLRTType_ushort();
			if (type.getElement() == PrimitiveType.UINT.getElement())
				return UMLRTRuntime.UMLRTObject.UMLRTType_uint();
			if (type.getElement() == PrimitiveType.ULONG.getElement())
				return UMLRTRuntime.UMLRTObject.UMLRTType_ulong();
			if (type.getElement() == PrimitiveType.ULONGLONG.getElement())
				return UMLRTRuntime.UMLRTObject.UMLRTType_ulongLong();

			if (type.getElement() == PrimitiveType.BOOL.getElement())
				return UMLRTRuntime.UMLRTObject.UMLRTType_bool();
			if (type.getElement() == PrimitiveType.FLOAT.getElement())
				return UMLRTRuntime.UMLRTObject.UMLRTType_float();
			if (type.getElement() == PrimitiveType.DOUBLE.getElement())
				return UMLRTRuntime.UMLRTObject.UMLRTType_double();
			if (type.getElement() == PrimitiveType.LONGDOUBLE.getElement())
				return UMLRTRuntime.UMLRTObject.UMLRTType_longdouble();

			// Enumerations are serialized as integers.
			if (type.getElement() instanceof CppEnum)
				return UMLRTRuntime.UMLRTObject.UMLRTType_int();

			if (type == UMLRTCapsuleId.getType())
				return UMLRTCapsuleId.createRTTypeAccess();
			if (type == UMLRTTimespec.getType())
				return UMLRTTimespec.createRTTypeAccess();

			return null;
		}

		public static Expression UMLRTObject_initialize() {
			return new ElementAccess(UMLRTObject_initialize_f);
		}

		public static Expression UMLRTObject_copy() {
			return new ElementAccess(UMLRTObject_copy_f);
		}

		public static Expression UMLRTObject_decode() {
			return new ElementAccess(UMLRTObject_decode_f);
		}

		public static Expression UMLRTObject_encode() {
			return new ElementAccess(UMLRTObject_encode_f);
		}

		public static Expression UMLRTObject_destroy() {
			return new ElementAccess(UMLRTObject_destroy_f);
		}

		public static Expression UMLRTObject_getSize() {
			return new ElementAccess(UMLRTObject_getSize_f);
		}

		public static Expression UMLRTObject_fprintf() {
			return new ElementAccess(UMLRTObject_fprintf_f);
		}

		public static Expression getSize(Expression desc) {
			MemberFunctionCall call = new MemberFunctionCall(desc, getSize_f);
			call.addArgument(desc);
			return call;
		}

		public static Expression sizeDecoded(Expression desc) {
			return new MemberAccess(desc, sizeDecoded);
		}

		public static Expression UMLRTObjectGeneric_initialize(Name name) {
			return new ElementAccess(
					new Function(
							umlrtobjectclassgeneric_hh,
							LinkageSpec.EXTERN,
							PrimitiveType.VOID.ptr(),
							new ExternalTemplateName("UMLRTObjectInitialize", name)));
		}

		public static Expression UMLRTObjectGeneric_copy(Name name) {
			return new ElementAccess(
					new Function(
							umlrtobjectclassgeneric_hh,
							LinkageSpec.EXTERN,
							PrimitiveType.VOID.ptr(),
							new ExternalTemplateName("UMLRTObjectCopy", name)));
		}

		public static Expression UMLRTObjectGeneric_destroy(Name name) {
			return new ElementAccess(
					new Function(
							umlrtobjectclassgeneric_hh,
							LinkageSpec.EXTERN,
							PrimitiveType.VOID.ptr(),
							new ExternalTemplateName("UMLRTObjectDestroy", name)));
		}
	}

	public static class UMLRTOutSignal extends UMLRTSignal {
		public static final ExternalElement Element = new ExternalFwdDeclarable(umlrtoutsignal_hh, "UMLRTOutSignal", "class UMLRTOutSignal");

		public static Type getType() {
			return Element.getType();
		}

		private static final MemberFunction encode_f = new MemberFunction(PrimitiveType.VOID, "encode");

		public static AbstractFunctionCall encode(Expression signal, Expression data, Expression desc) {
			AbstractFunctionCall call = new MemberFunctionCall(signal, encode_f);
			call.addArgument(data);
			call.addArgument(desc);
			return call;
		}
	}

	public static class UMLRTSlot {
		public static final ExternalElement Element = new ExternalFwdDeclarable(umlrtslot_hh, "UMLRTSlot", "struct UMLRTSlot");

		public static Type getType() {
			return Element.getType();
		}

		public static final MemberField capsuleClass = new MemberField(UMLRTCapsuleClass.getType().ptr().const_(), "capsuleClass");
		public static final MemberField capsule = new MemberField(UMLRTCapsule.getType().ptr().const_(), "capsule");
		public static final MemberField parts = new MemberField(UMLRTCapsulePart.getType().ptr(), "parts");
		public static final MemberField ports = new MemberField(UMLRTCommsPort.getType().ptr(), "ports");
	}

	public static class UMLRTTimerId {
		public static final ExternalElement Element = new ExternalFwdDeclarable(umlrttimerid_hh, "UMLRTTimerId", "class UMLRTTimerId");

		public static Type getType() {
			return Element.getType();
		}
	}

	public static class UMLRTTimerProtocol {
		public static final ExternalElement Element = new ExternalFwdDeclarable(umlrttimerprotocol_hh, "UMLRTTimerProtocol", "class UMLRTTimerProtocol");
		public static final ExternalElement BaseRole = new ExternalFwdDeclarable(umlrttimerprotocol_hh, "UMLRTTimerProtocol_baserole", "class UMLRTTimerProtocol_baserole");
		public static final ExternalElement TimeoutSignal = new ExternalElement(umlrttimerprotocol_hh, "signal_timeout");

		public static Type getType() {
			return Element.getType();
		}

		public static Type getBaseRoleType() {
			return BaseRole.getType();
		}

		public static boolean needsUMLRTCommsPort() {
			return true;
		}

		static {
			TimeoutSignal.setParent(Element);
		}

		public static Expression Signal(Signal signal) {
			if ("timeout".equals(signal.getName()))
				return new MemberAccess(Element, TimeoutSignal);
			return null;
		}
	}

	public static class UMLRTTimespec {
		public static final ExternalElement Element = new ExternalFwdDeclarable(umlrttimespec_hh, "UMLRTTimespec", "class UMLRTTimespec");

		public static Type getType() {
			return Element.getType();
		}

		public static final Variable RTType = new Variable(LinkageSpec.EXTERN, UMLRTObject.getType().ptr().constPtr(), "UMLRTType_UMLRTTimespec");
		static {
			RTType.setDefinedIn(umlrttimespec_hh);
		}

		public static Expression createRTTypeAccess() {
			return new ElementAccess(RTType);
		}
	}

	public static class UMLRTFrameService {
		public static final ExternalElement Element = new ExternalFwdDeclarable(umlrtframeservice_hh, "UMLRTFrameService", "class UMLRTFrameService");

		public static final MemberFunction automaticServicePortRegistrationFunction = new MemberFunction(PrimitiveType.VOID, "automaticServicePortRegistration");// ( UMLRTCapsule * capsule );
		public static final MemberFunction bindSubcapsulePortFunction = new MemberFunction(PrimitiveType.VOID, "bindSubcapsulePort");// ( bool isBorder, UMLRTCapsule * subcapsule, int portIndex, int farEndIndex );
		public static final MemberFunction unbindSubcapsulePortFunction = new MemberFunction(PrimitiveType.VOID, "unbindSubcapsulePort");// ( bool isBorder, UMLRTCapsule * subcapsule, int portIndex, int farEndIndex );
		public static final MemberFunction bindServicePortFunction = new MemberFunction(PrimitiveType.BOOL, "bindServicePort");// ( const UMLRTCommsPort * sapPort, const UMLRTCommsPort * sppPort );
		public static final MemberFunction unbindServicePortFunction = new MemberFunction(PrimitiveType.BOOL, "unbindServicePort");// ( const UMLRTCommsPort * sapPort, const UMLRTCommsPort * sppPort );
		public static final MemberFunction connectPortsFunction = new MemberFunction(PrimitiveType.VOID, "connectPorts");// ( const UMLRTCommsPort * p1, size_t p1Index, const UMLRTCommsPort * p2, size_t p2Index );
		public static final MemberFunction connectRelayPortFunction = new MemberFunction(PrimitiveType.VOID, "connectRelayPort");
		public static final MemberFunction connectFarEndsFunction = new MemberFunction(PrimitiveType.VOID, "connectFarEnds");

		public static final MemberFunction connectSlotPortFunction = new MemberFunction(PrimitiveType.VOID, "connectSlotPort");// ( const UMLRTSlot * slot, const UMLRTCommsPort * * borderPorts, int portId, int instance );
		public static final MemberFunction controllerDeportFunction = new MemberFunction(PrimitiveType.VOID, "controllerDeport");// ( UMLRTSlot * slot, bool synchronous, bool lockAcquired );
		public static final MemberFunction controllerDestroyFunction = new MemberFunction(PrimitiveType.VOID, "controllerDestroy");// ( UMLRTSlot * slot, bool isTopSlot, bool synchronous, bool lockAcquired );
		public static final MemberFunction controllerImportFunction = new MemberFunction(PrimitiveType.BOOL, "controllerImport");// ( UMLRTSlot * slot, UMLRTCapsule * capsule, bool synchronous, bool lockAcuired );
		public static final MemberFunction controllerIncarnateFunction = new MemberFunction(PrimitiveType.VOID, "controllerIncarnate");// ( UMLRTCapsule * capsule, size_t sizeSerializedData, void * serializedData );
		public static final MemberFunction freeFarEndsCountFunction = new MemberFunction(StandardLibrary.size_t, "freeFarEndsCount");// ( const UMLRTCommsPort * port );
		public static final MemberFunction createInternalPortsFunction = new MemberFunction(UMLRTCommsPort.getType().const_().ptr(), "createInternalPorts");// ( UMLRTSlot * slot, const UMLRTCapsuleClass * capsuleClass );
		public static final MemberFunction createBorderPortsFunction = new MemberFunction(UMLRTCommsPort.getType().const_().ptr().ptr(), "createBorderPorts");// ( UMLRTSlot * slot, size_t numPorts );
		public static final MemberFunction destroyPortsFunction = new MemberFunction(PrimitiveType.VOID, "destroyPorts");// ( size_t numPorts, const UMLRTCommsPort * ports );
		public static final MemberFunction destroyPortListFunction = new MemberFunction(PrimitiveType.VOID, "destroyPortList");// ( size_t numPorts, const UMLRTCommsPort * * ports );
		public static final MemberFunction disconnectPortFunction = new MemberFunction(PrimitiveType.VOID, "disconnectPort");// ( const UMLRTCommsPort * port );
		public static final MemberFunction disconnectPortInstanceFunction = new MemberFunction(PrimitiveType.VOID, "disconnectPort");// ( const UMLRTCommsPort * port, int farEndIndex );
		public static final MemberFunction instantiateFunction = new MemberFunction(PrimitiveType.VOID, "instantiate");// ( UMLRTSlot * slot, const UMLRTCapsuleClass * capsuleClass );
		public static final MemberFunction moveFarEndFunction = new MemberFunction(PrimitiveType.VOID, "moveFarEnd");// ( const UMLRTCommsPort * srcPort, size_t srcIndex, const UMLRTCommsPort * destPort, size_t destIndex );
		public static final MemberFunction importCapsuleFunction = new MemberFunction(PrimitiveType.BOOL, "importCapsule");// ( const UMLRTCommsPort * srcPort, UMLRTCapsule * capsule, const UMLRTCapsulePart * dest, int index );
		public static final MemberFunction incarnateCapsuleFunction = new MemberFunction(UMLRTCapsuleId.getType().const_(), "incarnateCapsule");// ( const UMLRTCommsPort * srcPort, const UMLRTCapsulePart * part, const UMLRTCapsuleClass * capsuleClass, const
																																				// void * userData, const UMLRTObject_class * type, const char * logThread, UMLRTController * controller, int index
																																				// );
		public static final MemberFunction requestControllerDeportFunction = new MemberFunction(PrimitiveType.VOID, "requestControllerDeport");// ( UMLRTSlot * slot, bool lockAcquired );
		public static final MemberFunction requestControllerDestroyFunction = new MemberFunction(PrimitiveType.VOID, "requestControllerDestroy");// ( UMLRTSlot * slotToDestroy, bool isTopSlot, bool lockAcuired );
		public static final MemberFunction rtsLockFunction = new MemberFunction(PrimitiveType.VOID, "rtsLock");// ();
		public static final MemberFunction rtsUnlockFunction = new MemberFunction(PrimitiveType.VOID, "rtsUnlock");// ();
		public static final MemberFunction sendBoundUnboundFunction = new MemberFunction(PrimitiveType.VOID, "sendBoundUnbound");
		public static final MemberFunction sendBoundUnboundFarEndFunction = new MemberFunction(PrimitiveType.VOID, "sendBoundUnboundFarEnd");
		public static final MemberFunction sendBoundUnboundForPortIndexFunction = new MemberFunction(PrimitiveType.VOID, "sendBoundUnboundForPortIndex");

		public static AbstractFunctionCall bindSubcapsulePort(Expression isBorder, Expression subcapsule, Expression portIndex, Expression farEndIndex) {
			AbstractFunctionCall call = new MemberFunctionCall(Element, bindSubcapsulePortFunction);
			call.addArgument(isBorder);
			call.addArgument(subcapsule);
			call.addArgument(portIndex);
			call.addArgument(farEndIndex);
			return call;
		}

		public static AbstractFunctionCall connectPorts(Expression p1, Expression p1Index, Expression p2, Expression p2Index) {
			AbstractFunctionCall call = new MemberFunctionCall(Element, connectPortsFunction);
			call.addArgument(p1);
			call.addArgument(p1Index);
			call.addArgument(p2);
			call.addArgument(p2Index);
			return call;
		}

		public static AbstractFunctionCall connectRelayPort(Expression relayPort, Expression relayIndex, Expression destPort, Expression destIndex) {
			AbstractFunctionCall call = new MemberFunctionCall(Element, connectRelayPortFunction);
			call.addArgument(relayPort);
			call.addArgument(relayIndex);
			call.addArgument(destPort);
			call.addArgument(destIndex);
			return call;
		}

		public static AbstractFunctionCall connectFarEnds(Expression p1, Expression p1Index, Expression p2, Expression p2Index) {
			AbstractFunctionCall call = new MemberFunctionCall(Element, connectFarEndsFunction);
			call.addArgument(p1);
			call.addArgument(p1Index);
			call.addArgument(p2);
			call.addArgument(p2Index);
			return call;
		}

		public static AbstractFunctionCall connectSlotPort(Expression slot, Expression borderPorts, Expression portId, Expression index) {
			AbstractFunctionCall call = new MemberFunctionCall(Element, connectSlotPortFunction);
			call.addArgument(slot);
			call.addArgument(borderPorts);
			call.addArgument(portId);
			call.addArgument(index);
			return call;
		}

		public static AbstractFunctionCall createBorderPorts(Expression slotAccess, Expression numPorts) {
			AbstractFunctionCall call = new MemberFunctionCall(Element, createBorderPortsFunction);
			call.addArgument(slotAccess);
			call.addArgument(numPorts);
			return call;
		}

		public static AbstractFunctionCall createInternalPorts(Expression slotAccess, Expression capsuleClass) {
			AbstractFunctionCall call = new MemberFunctionCall(Element, createInternalPortsFunction);
			call.addArgument(slotAccess);
			call.addArgument(capsuleClass);
			return call;
		}

		public static AbstractFunctionCall disconnectPort(Expression port) {
			AbstractFunctionCall call = new MemberFunctionCall(Element, disconnectPortFunction);
			call.addArgument(port);
			return call;
		}

		public static AbstractFunctionCall disconnectPort(Expression port, Expression instance) {
			AbstractFunctionCall call = new MemberFunctionCall(Element, disconnectPortInstanceFunction);
			call.addArgument(port);
			call.addArgument(instance);
			return call;
		}

		public static AbstractFunctionCall sendBoundUnbound(Expression ports, Expression portId, Expression farEndIndex, Expression isBind) {
			AbstractFunctionCall call = new MemberFunctionCall(Element, sendBoundUnboundFunction);
			call.addArgument(ports);
			call.addArgument(portId);
			call.addArgument(farEndIndex);
			call.addArgument(isBind);
			return call;
		}

		public static AbstractFunctionCall sendBoundUnboundFarEnd(Expression port, Expression index, Expression isBind) {
			AbstractFunctionCall call = new MemberFunctionCall(Element, sendBoundUnboundFarEndFunction);
			call.addArgument(port);
			call.addArgument(index);
			call.addArgument(isBind);
			return call;
		}

		public static AbstractFunctionCall sendBoundUnboundForPortIndex(Expression port, Expression index, Expression isBind) {
			AbstractFunctionCall call = new MemberFunctionCall(Element, sendBoundUnboundForPortIndexFunction);
			call.addArgument(port);
			call.addArgument(index);
			call.addArgument(isBind);
			return call;
		}

		public static AbstractFunctionCall unbindSubcapsulePort(Expression isBorder, Expression subcapsule, Expression portIndex, Expression farEndIndex) {
			AbstractFunctionCall call = new MemberFunctionCall(Element, unbindSubcapsulePortFunction);
			call.addArgument(isBorder);
			call.addArgument(subcapsule);
			call.addArgument(portIndex);
			call.addArgument(farEndIndex);
			return call;
		}
	}

	public static class UMLRTFrameProtocol {
		public static final ExternalElement Element = new ExternalFwdDeclarable(umlrtframeprotocol_hh, "UMLRTFrameProtocol", "class UMLRTFrameProtocol");
		public static final ExternalElement BaseRole = new ExternalFwdDeclarable(umlrtframeprotocol_hh, "UMLRTFrameProtocol_baserole", "class UMLRTFrameProtocol_baserole");
		public static final ExternalElement IncarnateSignal = new ExternalElement(umlrtframeprotocol_hh, "signal_incarnate");

		public static Type getType() {
			return Element.getType();
		}

		public static Type getBaseRoleType() {
			return BaseRole.getType();
		}

		public static boolean needsUMLRTCommsPort() {
			return true;
		}

		public static Expression Signal(Signal signal) {
			if ("incarnate".equals(signal.getName()))
				return new MemberAccess(Element, IncarnateSignal);
			return null;
		}
	}

	/**
	 * Return the system-defined Type of the given uml.Type and null if the type is not recognized.
	 */
	public static Type getSystemType(org.eclipse.papyrusrt.xtumlrt.common.Type type) {
		if (RTSModelLibraryUtils.isCapsuleId(type))
			return UMLRTCapsuleId.getType();
		if (RTSModelLibraryUtils.isTimerId(type))
			return UMLRTTimerId.getType();
		if (RTSModelLibraryUtils.isTimerSpec(type))
			return UMLRTTimespec.getType();
		if (RTSModelLibraryUtils.isMessage(type))
			return UMLRTMessage.getType();
		return null;
	}

	/**
	 * Return the system-defined cpp element of the given element and null if
	 * the type is not recognized.
	 */
	public static Element getSystemElement(NamedElement element) {
		if (RTSModelLibraryUtils.isCapsuleId(element))
			return UMLRTCapsuleId.Element;
		if (RTSModelLibraryUtils.isTimerId(element))
			return UMLRTTimerId.Element;
		if (RTSModelLibraryUtils.isTimerSpec(element))
			return UMLRTTimespec.Element;
		if (RTSModelLibraryUtils.isMessage(element))
			return UMLRTMessage.Element;
		return null;
	}

	/**
	 * Return the Type of the given system-defined protocol and null if the protocol is not recognized.
	 */
	public static Type getSystemProtocolRole(Protocol protocol, boolean baseRole) {
		if (RTSModelLibraryUtils.isFrameProtocol(protocol))
			return baseRole ? UMLRTFrameProtocol.getBaseRoleType() : null;
		if (RTSModelLibraryUtils.isLogProtocol(protocol))
			return baseRole ? UMLRTLogProtocol.getBaseRoleType() : null;
		if (RTSModelLibraryUtils.isTimingProtocol(protocol))
			return baseRole ? UMLRTTimerProtocol.getBaseRoleType() : null;
		return null;
	}

	/**
	 * Return true if the given protocol needs an instance of UMLRTCommsPort during initialization.
	 * For example, the Timer protocol needs a UMLRTCommsPort that is used for communicating with
	 * then central timing service. However, the Log protocol does not need an instance of
	 * UMLRTCommsPort because messages are sent directly to OS-level log target.
	 */
	public static boolean needsUMLRTCommsPort(Protocol protocol) {
		if (RTSModelLibraryUtils.isFrameProtocol(protocol))
			return UMLRTFrameProtocol.needsUMLRTCommsPort();
		if (RTSModelLibraryUtils.isLogProtocol(protocol))
			return UMLRTLogProtocol.needsUMLRTCommsPort();
		if (RTSModelLibraryUtils.isTimingProtocol(protocol))
			return UMLRTTimerProtocol.needsUMLRTCommsPort();

		return true;
	}

	/**
	 * Return an expression to access the signal enumerator for the given system-defined
	 * protocol and null if the protocol is not recognized.
	 */
	public static Expression getSystemProtocolSignalAccess(Signal signal) {
		Protocol protocol = (Protocol) XTUMLRTUtil.getOwner(signal);
		if (RTSModelLibraryUtils.isBaseCommProtocol(protocol))
			return UMLRTBaseCommProtocol.Signal(signal);
		if (RTSModelLibraryUtils.isFrameProtocol(protocol))
			return UMLRTFrameProtocol.Signal(signal);
		if (RTSModelLibraryUtils.isTimingProtocol(protocol))
			return UMLRTTimerProtocol.Signal(signal);
		return null;
	}
}
