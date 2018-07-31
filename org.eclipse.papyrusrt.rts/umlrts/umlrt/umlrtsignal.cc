// umlrtsignal.cc

/*******************************************************************************
* Copyright (c) 2014-2015 Zeligsoft (2009) Limited  and others.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*******************************************************************************/

#include "umlrtapi.hh"
#include "umlrtcommsport.hh"
#include "umlrtcommsportfarend.hh"
#include "umlrtcontroller.hh"
#include "umlrtframeservice.hh"
#include "umlrtsignal.hh"
#include "umlrtsignalelement.hh"
#include "umlrtuserconfig.hh"
#include "basefatal.hh"
#include "basedebugtype.hh"
#include "basedebug.hh"
#include <stdarg.h>

// See umlrtsignal.hh for documentation.

// No signal element by default - equivalent to the 'invalid signal'.
// An element is allocated when signal is 'initialized'.
UMLRTSignal::UMLRTSignal ( ) : element(NULL)
{
}

UMLRTSignal::~UMLRTSignal ( )
{
    if (element)
    {
        element->decrementRefCount();
    }
}

UMLRTSignal::UMLRTSignal ( const UMLRTSignal & signal ) : element(signal.element)
{
    if (element)
    {
        element->incrementRefCount();
    }
}

UMLRTSignal &UMLRTSignal::operator= ( const UMLRTSignal & signal )
{
    if (&signal != this)
    {
        if (element)
        {
            element->decrementRefCount();
        }
        if (signal.element)
        {
            signal.element->incrementRefCount();
        }
        element = signal.element;
    }
    return *this;
}

void UMLRTSignal::initialize ( const char * name, Id id, const UMLRTCommsPort * srcPort, UMLRTPriority priority )
{
    if (element)
    {
        FATAL("initializing signal that already has an element allocated.");
    }
    element = umlrt::SignalElementGetFromPool();

    if (element == NULL)
    {
        FATAL("failed to allocate signal element");
    }
    BDEBUG(BD_SIGNAL, "allocate %s id(%d) no payloadSize refCount(%d)\n", name, id, element->debugGetRefCount());

    element->initialize(name, srcPort, id, priority);
}

void UMLRTSignal::initialize ( const char * name, Id id, const UMLRTCommsPort * srcPort, const UMLRTObject * const object, ... )
{
    if (element)
    {
        FATAL("initializing signal that already has an element allocated.");
    }
    element = umlrt::SignalElementGetFromPool();

    if (element == NULL)
    {
        FATAL("failed to allocate signal element");
    }
    BDEBUG(BD_SIGNAL, "allocate %s id(%d) with object size(%lu) numField(%lu)\n", name, id, object->sizeOf, object->numFields);

    va_list vl;
    va_start(vl, object);
    element->initialize(name, id, srcPort, object, vl);
    va_end(vl);
}

void UMLRTSignal::initialize ( const char * name, Id id, const UMLRTCommsPort * srcPort, const UMLRTObject_class * const desc, const void * const data, UMLRTPriority priority )
{
    if (element)
    {
        FATAL("initializing signal that already has an element allocated.");
    }
    element = umlrt::SignalElementGetFromPool();

    if (element == NULL)
    {
        FATAL("failed to allocate signal element");
    }
    BDEBUG(BD_SIGNAL, "allocate %s id(%d) type(%s)\n", name, id, desc->name);

    element->initialize(name, id, srcPort, desc, data, priority);
}

void UMLRTSignal::initialize ( const char * name, Id id, size_t payloadSize, UMLRTPriority priority )
{
    if (element)
    {
        FATAL("initializing signal that already has an element allocated.");
    }
    element = umlrt::SignalElementGetFromPool();

    if (element == NULL)
    {
        FATAL("failed to allocate signal element");
    }
    BDEBUG(BD_SIGNAL, "allocate %s id(%d) payloadSize(%d)\n", name, id, payloadSize);

    element->initialize(name, id, payloadSize, priority);
}

void UMLRTSignal::initialize ( const char * name, Id id )
{
    if (element)
    {
        FATAL("initializing signal that already has an element allocated.");
    }
    element = umlrt::SignalElementGetFromPool();

    if (element == NULL)
    {
        FATAL("failed to allocate signal element");
    }
    BDEBUG(BD_SIGNAL, "allocate %s id(%d) no payload\n", name, id);

    element->initialize(name, id, 0);
}

void * UMLRTSignal::getParam ( size_t index ) const
{
    if (element == NULL)
    {
        FATAL("getParam element NULL");
    }
    return element->getParam(index);
}


// Get the payload buffer.
uint8_t * UMLRTSignal::getPayload ( ) const
{
    if (element == NULL)
    {
        return NULL;
    }
    return element->getPayload();
}

// Get the payload buffer size.
size_t UMLRTSignal::getPayloadSize ( ) const
{
    if (element == NULL)
    {
        return 0;
    }
    return element->getPayloadSize();
}

const UMLRTCommsPort * UMLRTSignal::getSrcPort ( ) const
{
    if (element == NULL)
    {
        return NULL;
    }
    return element->getSrcPort();
}

UMLRTSignal::Id UMLRTSignal::getId ( ) const
{
    if (element == NULL)
    {
        return invalidSignalId;
    }
    return element->getId();
}
