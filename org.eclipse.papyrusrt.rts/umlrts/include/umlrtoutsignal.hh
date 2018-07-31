// umlrtoutsignal.hh

/*******************************************************************************
* Copyright (c) 2015 Zeligsoft (2009) Limited  and others.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*******************************************************************************/

#ifndef UMLRTOUTSIGNAL_HH
#define UMLRTOUTSIGNAL_HH

#include "umlrtsignal.hh"
#include "umlrtpriority.hh"

class UMLRTMessage;

class UMLRTOutSignal : virtual public UMLRTSignal
{
public:
    UMLRTOutSignal();
    UMLRTOutSignal(const UMLRTOutSignal &signal);
    UMLRTOutSignal& operator=(const UMLRTOutSignal &signal);
    ~UMLRTOutSignal();

    // The encode functions allow the user to serialize data into the payload.
    void encode ( const UMLRTObject_class * desc, const void * src, int offset, size_t arraySize = 1);

    // Synchronous send out all port instances. Returns the number of replies (0 if fail).
    int invoke ( UMLRTMessage * replyMsgs );

    // Synchronous send out a specific port instance. Returns the number of replies (0 or 1).
    int invokeAt ( int index, UMLRTMessage * replyMsg );

    // Send the signal to its far end port (or ports, if it is replicated).
    // Return 0 if there is an error.
    bool send ( UMLRTPriority priority = PRIORITY_NORMAL ) const;

    // Send the signal to an individual port.
    // Return 0 if there is an error.
    bool sendAt ( size_t  portIndex, UMLRTPriority priority = PRIORITY_NORMAL ) const;

    // Deliver the signal to a destination.
    bool signalDeliver ( const UMLRTCommsPort * srcPort, size_t srcPortIndex ) const;

    // Reply to a synchronous message. Return true if success.
    bool reply ( );

};

#endif // UMLRTINSIGNAL_HH
