// umlrtsignal.hh

/*******************************************************************************
* Copyright (c) 2014-2015 Zeligsoft (2009) Limited  and others.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*******************************************************************************/

#ifndef UMLRTSIGNAL_HH
#define UMLRTSIGNAL_HH

#include <stddef.h>
#include <stdint.h>
#include <stdlib.h>
#include "umlrtqueueelement.hh"
#include "umlrtsignalelement.hh"

// UMLRTSignal is the 'signal' that the application sends via ports.

// This class is used by protocols and user-code to create, define and send
// signals, however the underlying signal contents are maintained within a 'signal element'.

// Multiple UMLRTSignal objects may refer to the same 'signal element'. Allocation
// and deallocation of the underlying 'signal element' is managed by this class.

// A protocol allocates a signal (and its associated 'signal element') and
// user-data is serialized into the 'signal element' 'payload' field (variable-sized) buffer.

// Henceforth, the signal may be copied and used locally to send to multiple destinations.

// A send operation on the signal causes a message to be allocated and a
// reference to the signal is put in the message for enqueuing on the
// destination queue.

// While individual signals may be destroyed, the underlying 'signal element' is
// not deallocated until the last signal referring to the 'signal element' is destroyed.

struct UMLRTCommsPort;
struct UMLRTObject_class;

class UMLRTSignal : public UMLRTQueueElement
{
public:
    UMLRTSignal ( );
    UMLRTSignal ( const UMLRTSignal &signal );
    UMLRTSignal& operator= ( const UMLRTSignal &signal );
    ~UMLRTSignal ( );

    typedef int Id;
    enum { invalidSignalId = -1, rtBound = 0, rtUnbound, FIRST_PROTOCOL_SIGNAL_ID };

    Id getId ( ) const; // Returns -1 if the signal is invalid.

    const char * getName ( ) const { return !element ? "(uninitialized signal)" : element->getName(); }

    void * getParam ( size_t index ) const;

    // Get the payload buffer. Returns NULL if the signal is invalid.
    uint8_t *getPayload ( ) const;

    // Gets what the user set the payload size to be (payload buffer will be >= this).
    size_t getPayloadSize ( ) const;

    UMLRTPriority getPriority ( ) const { return (element == NULL) ? PRIORITY_NORMAL : element->getPriority(); }

    // For debugging signals
    int getQid ( ) const { return (element != NULL) ? element->qid : -1; }

    // Signal source - used to get destination port during sends. Returns NULL if the signal is invalid.
    const UMLRTCommsPort * getSrcPort ( ) const;

    // Get the type descriptor for the i-th parameter.
    const UMLRTObject_class * getType ( size_t index = 0 ) const { return (element == NULL) ? NULL : element->getType(index); }

    // Initialize a signal with the src port and its id. This is used for in-signals, rtBound/rtUnbound and other dataless signals.
    void initialize ( const char * name, Id id, const UMLRTCommsPort * srcPort, UMLRTPriority priority = PRIORITY_NORMAL );
    // Initialize a signal with the src port, id and a single data parameter.
    void initialize ( const char * name, Id id, const UMLRTCommsPort * srcPort, const UMLRTObject_class * const desc, const void * const data, UMLRTPriority priority = PRIORITY_NORMAL );
    // Initialize a signal with the src port, id and one or more data parameter.
    void initialize ( const char * name, Id id, const UMLRTCommsPort * srcPort, const UMLRTObject * const object, ... );

    // Initialize a signal with a payload and no srcPort. Used for internal 'controller commands'.
    void initialize ( const char * name, Id id, size_t payloadSize, UMLRTPriority priority = PRIORITY_NORMAL );
    // Initialize a signal with no payload and no srcPort. Used for internal 'empty initialization signal'.
    void initialize ( const char * name, Id id );

    // Check whether this signal is an 'invalid signal'.
    bool isInvalid ( ) const { return element == NULL; }

    // Debug output of signal payload.
    void debugOutputPayload ( ) { if (element != NULL) element->debugOutputPayload(); }

protected:
    UMLRTSignalElement * element;
};

#endif // UMLRTSIGNAL_HH
