// umlrtmessage.hh

/*******************************************************************************
* Copyright (c) 2014-2015 Zeligsoft (2009) Limited  and others.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*******************************************************************************/

#ifndef UMLRTMESSAGE_HH
#define UMLRTMESSAGE_HH

#include "umlrtqueueelement.hh"
#include "umlrtsignal.hh"

struct UMLRTCommsPort;
class UMLRTSignal;
struct UMLRTSlot;

// The RTS library transports signals within 'messages' (UMLRTMessage).

// Messages are small and are fixed in size. The 'message' is the
// entity that is queued in 'message queues' waiting to be consumed by their
// destination capsules.

// Each message contains a reference to a single signal. The signal's payload is the
// (optional) user data added to the signal. (See UMLRTSignal for more info.)

// Signals are separated from messages so that one signal can reside in
// multiple messages (to support broadcast operations).

class UMLRTMessage : public UMLRTQueueElement
{
public:

    UMLRTMessage ( ) : allocated(false), destPort(NULL), destSlot(NULL), isCommand(false), sapIndex0_(0), srcPortIndex(0) { };

    bool allocated;   // For sanity checking of message allocation.
    const UMLRTCommsPort * destPort; // Message destination - capsule contained within.
    const UMLRTSlot * destSlot; // Destination slot.
    bool isCommand;   // true when it's a command and not a signal.
    size_t sapIndex0_; // The port index on the receive side.
    UMLRTSignal signal;
    size_t srcPortIndex; // The associated srcPort of the message is contained within the signal.

    bool defer ( ) const;
    void * getParam ( size_t index ) const;
    UMLRTPriority getPriority ( ) const { return signal.getPriority(); }
    int getSignalId ( ) const { return signal.getId(); }
    const char * getSignalName ( ) const { return signal.getName(); }
    const UMLRTObject_class * getType ( size_t index = 0 ) const { return signal.getType(index); }

    bool isValid ( ) const { return !signal.isInvalid(); }

    size_t sapIndex ( ) const { return sapIndex0_ + 1; }
    size_t sapIndex0 ( ) const { return sapIndex0_; }
    const UMLRTCommsPort * sap ( ) const { return destPort; }
};


#endif // UMLRTMESSAGE_HH
