// umlrtsignalelement.hh

/*******************************************************************************
* Copyright (c) 2014-2015 Zeligsoft (2009) Limited  and others.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*******************************************************************************/

#ifndef UMLRTSIGNALELEMENT_HH
#define UMLRTSIGNALELEMENT_HH

#include "umlrtcommsport.hh"
#include "umlrtmutex.hh"
#include "umlrtobjectclass.hh"
#include "umlrtqueueelement.hh"
#include "umlrtpriority.hh"
#include <stdint.h>
#include <stdarg.h>

struct UMLRTCommsPort;

// Default-sized payload buffers are created when the signal is first
// used. Thereafter, the default-size payload buffer remains for future use
// (is never deallocated.)

// If the application requires more space than the default size, a temporary
// payload buffer is allocated from the heap. In this case, when the signal
// is 'freed', the temporary buffer is returned to the heap and the default
// payload buffer is restored.

class UMLRTSignalElement : public UMLRTQueueElement
{
public:
    UMLRTSignalElement ( );

    typedef int Id;

    // Deallocate a signal element.
    static void dealloc ( UMLRTSignalElement * element );

    // For debugging reference counter.
    int debugGetRefCount ( ) const { return refCount; }

    // Decode payload into 'data' buffer.
    void decode ( const void * * decodeInfo, const UMLRTObject_class * desc, void * data, int arraySize = 1 );

    // Reference counting
    void decrementRefCount ( ) const;

    // Destroy 'data' previously encoded into the payload.
    void destroy ( );

    // Encode 'data' buffer into payload.
    void encode ( const UMLRTObject_class * desc, const void * src, int offset, int arraySize = 1);

    const UMLRTObject_field * const getFields ( ) const;

    int getId ( ) const { return id; }

    const char * getName ( ) const { return name; }

    void * getParam ( size_t index ) const;

    // Get the payload buffer.
    uint8_t *getPayload ( ) const { return payload; }

    // Size - what the user thinks is the size and what the actual buffer size is (Max).
    size_t getPayloadSize ( ) const { return appPayloadSize; }
    size_t getMaxPayloadSize ( ) const { return maxPayloadSize; }

    UMLRTPriority getPriority ( ) const { return priority; }

    size_t getNumFields ( ) const;

    // Signal source - used to get destination port during sends.
    const UMLRTCommsPort * getSrcPort ( ) const { return srcPort; }

    // Get the type descriptor for the i-th parameter.
    const UMLRTObject_class * getType ( size_t index = 0 ) const;

    // Reference counting
    void incrementRefCount ( ) const;

    void initialize ( const char * name, Id id, const UMLRTCommsPort * srcPort, size_t payloadSize, UMLRTPriority priority = PRIORITY_NORMAL );
    void initialize ( const char * name, Id id, size_t payloadSize, UMLRTPriority priority = PRIORITY_NORMAL );
    void initialize ( const char * name, const UMLRTCommsPort * srcPort, Id id, UMLRTPriority priority = PRIORITY_NORMAL );
    void initialize ( const char * name, Id id, const UMLRTCommsPort * srcPort, const UMLRTObject * const payload, va_list vl );
    void initialize ( const char * name, Id id, const UMLRTCommsPort * srcPort, const UMLRTObject_class * const desc, const void * const data, UMLRTPriority priority = PRIORITY_NORMAL );

    // Pool allocation sanity check.
    void setAllocated ( bool allocated_ );

    void setPriority ( UMLRTPriority priority_) { priority = priority_; }

    // Debug output of signal payload.
    void debugOutputPayload ( );

private:
    // This signal's ID.
    uint32_t id;

    // The src port for this signal.
    const UMLRTCommsPort * srcPort;

    // Signal name defined by signal initialization.
    const char * name;

    // Keep a copy of the default-sized payload buffer.
    uint8_t * defaultPayload;

    // User-data is serialized into payload buffer.
    // This buffer may be temporarily replaced with a larger buffer obtained
    // from the heap.
    uint8_t * payload;

    UMLRTPriority priority;

    size_t appPayloadSize; // What user declared was the payload size.
    size_t maxPayloadSize; // Actual buffer size.

    // Set this true when a buffer larger than the default-size is allocated
    // for a signal and must be deallocated when the signal is returned to the
    // pool.
    bool nonDefaultPayload;

    // Set true when the signal is allocated (obtained) from the free-pool.
    // Set false when the signal is returned to the free-pool.
    // Used for sanity checks to detect access to uninitialized data.
    bool allocated;

    // Number of UMLRTSignal's referring to this element.
    mutable int refCount;

    // Mutex for reference counting.
    mutable UMLRTMutex refCountMutex;

    const UMLRTObject * object;
    const UMLRTObject_class * desc;
};

#endif // UMLRTSIGNALELEMENT_HH
