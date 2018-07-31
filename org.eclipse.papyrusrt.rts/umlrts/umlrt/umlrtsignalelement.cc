// umlrtsignalelement.cc

/*******************************************************************************
* Copyright (c) 2014-2015 Zeligsoft (2009) Limited  and others.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*******************************************************************************/

#include <stdlib.h>
#include "basedebug.hh"
#include "basedebugtype.hh"
#include "basefatal.hh"
#include "umlrtapi.hh"
#include "umlrtslot.hh"
#include "umlrtcommsportrole.hh"
#include "umlrtguard.hh"
#include "umlrtobjectclass.hh"
#include "umlrtsignalelement.hh"
#include "umlrtuserconfig.hh"

// TODO debugging - to be removed.
extern uint8_t * globalSrc;
extern uint8_t * globalDst;

// See umlrtsignalelement.hh for documentation.

UMLRTSignalElement::UMLRTSignalElement ( ) : id(0), srcPort(0), name(0), defaultPayload(0), payload(0), priority(PRIORITY_NORMAL), appPayloadSize(0),
            maxPayloadSize(USER_CONFIG_SIGNAL_DEFAULT_PAYLOAD_SIZE), nonDefaultPayload(false), allocated(false), refCount(0), object(NULL), desc(NULL)
{
    // DEFAULT PAYLOAD SIZE IS A CONSTANT HERE, BUT HAS TO BE OBTAINED AT RUN-TIME.
    if (!(defaultPayload = payload = (uint8_t*)malloc(USER_CONFIG_SIGNAL_DEFAULT_PAYLOAD_SIZE)))
    {
        FATAL("(%p)qid[%d] payload malloc(%d)",this, qid, USER_CONFIG_SIGNAL_DEFAULT_PAYLOAD_SIZE);
    }
    BDEBUG(BD_SIGNALINIT, "(%p) signal element payload(%p)\n", this, payload);
}

// Free a signal element (return to the pool).
/*static*/ void UMLRTSignalElement::dealloc ( UMLRTSignalElement * element )
{
    if (!element)
    {
        FATAL("element NULL");
    }
    BDEBUG(BD_SIGNAL, "[%p]qid[%d] deallocate id(%d) appPayloadSize(%d) maxPayloadSize(%d)\n",
            element, element->qid, element->id, element->appPayloadSize, element->maxPayloadSize);

    if (element->refCount)
    {
        FATAL("(%p)qid[%d] signal element dealloc refcount non-zero(%d)", element, element->qid, element->refCount);
    }
    element->destroy(); // Destroy any defined parameters;

    if (element->nonDefaultPayload)
    {
        // Return non-default payload.
        free(element->payload);

        // Restore default payload.
        element->payload = element->defaultPayload;
        element->maxPayloadSize = USER_CONFIG_SIGNAL_DEFAULT_PAYLOAD_SIZE;
        element->appPayloadSize = 0; // Should be unused while deallocated.
    }
    umlrt::SignalElementPutToPool(element);
}

void UMLRTSignalElement::decode ( const void * * decodeInfo, const UMLRTObject_class * desc, void * data, int arraySize )
{
    // decodeInfo is initialized to be NULL by a decoder - it is owned by the decoder and not by
    // the signal (contrary to the encodeInfo, which can be owned by the signal, since there is always
    // only ever one encoder).

    if ((*decodeInfo) == NULL)
    {
        *decodeInfo = payload;
    }
    globalDst = (uint8_t *)data; // TODO: Remove this later. It's for debugging.
    globalSrc = (uint8_t *)(*decodeInfo); // TODO: Remove this later. It's for debugging.

    for (int i = 0; i < arraySize; ++i)
    {
        data = desc->copy(desc, *decodeInfo, ((uint8_t *)data + (i * desc->object.sizeOf)));
    }
}

// See documentation in UMLRTSignal.
void UMLRTSignalElement::destroy ( )
{
    if ((object != NULL) && (desc != NULL))
    {
        FATAL("(%p) qid[%d] signal %s has both object and desc non-NULL", this, qid, getName());
    }
    if (object != NULL)
    {
        for (size_t i = 0; i < object->numFields; ++i)
        {
            for (int j = 0; j < object->fields[i].arraySize; ++j)
            {
                object->fields[i].desc->destroy(object->fields[i].desc, payload + object->fields[i].offset + (j * object->fields[i].desc->object.sizeOf));
            }
        }
    }
    else if (desc != NULL)
    {
        desc->destroy(desc, payload);
    }
    object = NULL;
    desc = NULL;
}

void UMLRTSignalElement::decrementRefCount ( ) const
{
    BDEBUG(BD_SIGNALREF, "(%p) qid[%d] signal element dec ref count(%d)\n", this, qid, refCount);

    UMLRTGuard g(refCountMutex);
    if (refCount <= 0)
    {
        FATAL("(%p)qid[%d] signal element dec ref count that is already zero id(%d)", this, qid, id);
    }
    else if (!(--refCount))
    {
        BDEBUG(BD_SIGNALREF, "(%p) qid[%d] signal element dec ref count == 0 dealloc\n", this, qid);

        UMLRTSignalElement::dealloc(const_cast<UMLRTSignalElement *>(this));
    }
}

// See documentation in UMLRTSignal.
void UMLRTSignalElement::encode ( const UMLRTObject_class * desc, const void * src, int offset, int arraySize )
{
    // encodeInfo is intitialized to be 'payload' when the signal is initialized.
    // It is moved forward during successive #encode calls.

    uint8_t * s = (uint8_t *)src;

    globalSrc = (uint8_t *)s; // TODO: Remove this later. It's for debugging.
    globalDst = payload + offset; // TODO: Remove this later. It's for debugging.

    for (int i = 0; i < arraySize; ++i)
    {
        BDEBUG(BD_SIGNAL, "(%p) qid[%d] encode src %p payload %p offset %d\n", this, qid, s, payload, offset + i * (desc->object.sizeOf));
        desc->copy(desc, s, payload + offset + i * (desc->object.sizeOf));
        s += desc->object.sizeOf;
    }
}

void * UMLRTSignalElement::getParam ( size_t index ) const
{
    void * param = NULL;

    if ((object != NULL) && (desc != NULL))
    {
        FATAL("(%p) qid[%d] signal %s has both object and desc non-NULL", this, qid, getName());
    }
    if (object != NULL)
    {
        if (index >= object->numFields)
        {
            BDEBUG(BD_SWERR, "(%p) qid[%d] getParam signal %s index %d too high - numFields %d\n", this, qid, getName(), index, object->numFields);
        }
        else
        {
            param = &getPayload()[object->fields[index].offset];
        }
    }
    else if ((desc != NULL) && (index > 0))
    {
        BDEBUG(BD_SWERR, "(%p) qid[%d] getParam signal %s index %d too high - numFields %d\n", this, qid, getName(), index, object->numFields);
    }
    else if (desc != NULL)
    {
        param = getPayload();
    }
    return param;
}

size_t UMLRTSignalElement::getNumFields ( ) const
{
    if ((object != NULL) && (desc != NULL))
    {
        FATAL("(%p) qid[%d] signal %s has both object and desc non-NULL", this, qid, getName());
    }
    size_t numFields = 0;
    if (object != NULL)
    {
        numFields = object->numFields;
    }
    else if (desc != NULL)
    {
        numFields = 1;
    }
    return numFields;
}

const UMLRTObject_class * UMLRTSignalElement::getType ( size_t index ) const
{
    if ((object != NULL) && (desc != NULL))
    {
        FATAL("(%p) qid[%d] signal %s has both object and desc non-NULL", this, qid, getName());
    }
    const UMLRTObject_class * type = NULL;

    if (object != NULL)
    {
        if (index < object->numFields)
        {
            type = object->fields[index].desc;
        }
    }
    else if (!index && (desc != NULL))
    {
        type = desc;
    }
    return type;
}


void UMLRTSignalElement::incrementRefCount ( ) const
{
    BDEBUG(BD_SIGNALREF, "(%p) qid[%d] signal element inc ref count(%d)\n", this, qid, refCount);

    UMLRTGuard g(refCountMutex);

    ++refCount;
}

// Initialize a signal from the pool and make sure the payload is large enough.
void UMLRTSignalElement::initialize ( const char * name_, Id id_, size_t payloadSize_, UMLRTPriority priority_ )
{
    BDEBUG(BD_SIGNAL, "(%p) qid[%d] initialize signal %s id %d payloadSize %lu refCount %d\n", this, qid, name_, id_, payloadSize_, refCount);

    if (name_ == NULL)
    {
        FATAL("(%p) initialize name is NULL", this);
    }
    if (name != NULL)
    {
        // Free up previous name (if defined).
        free((void*)name);
    }
    name = strdup(name_);
    srcPort = NULL; // No srcPort by default.
    id = id_;
    priority = priority_;
    object = NULL;
    desc = NULL;

    if (refCount)
    {
        FATAL("(%p) qid[%d] signal element alloc refcount non-zero(%d)", this, qid, refCount);
    }
    // If the user is requesting more space than is available in the
    // default payload buffer, allocate it here.

    // THIS CONSTANT MAY HAVE TO BE OBTAINED AT RUN-TIME FROM THE
    // GENERATED USER-CONFIGURATION PARAMETERS, IF THE VALUES FROM umlrtapi.hh ARE NOT USED.
    if (payloadSize_ > USER_CONFIG_SIGNAL_DEFAULT_PAYLOAD_SIZE)
    {
        if (!(payload = (uint8_t *)malloc(payloadSize_)))
        {
            FATAL("(%p) qid[%d] non-default payload malloc(%d)", this, qid, payloadSize_ );
        }
        nonDefaultPayload = true;
        maxPayloadSize = payloadSize_;
    }
    appPayloadSize = payloadSize_;

    incrementRefCount();
}

void UMLRTSignalElement::initialize ( const char * name, const UMLRTCommsPort * srcPort_, Id id, UMLRTPriority priority )
{
    initialize(name, id, 0/*payloadSize*/, priority); // Default initialization.
    srcPort = srcPort_;

    BDEBUG(BD_SIGNAL, "(%p) qid[%d] initialize id(%d) %s(%s) appPayloadSize(%d) maxPayloadSize(%d)\n",
            this, qid, id,
            (srcPort_ != NULL) ? srcPort->slotName() : "(NULL src port)",
            (srcPort_ != NULL) ? srcPort->getName() : "(NULL src port)",
            appPayloadSize,
            maxPayloadSize);
}

void UMLRTSignalElement::initialize ( const char * name, Id id, const UMLRTCommsPort * srcPort_, const UMLRTObject * const object_, va_list vl )
{
    initialize(name, id, object_->sizeOf);
    srcPort = srcPort_;
    object = object_;
    for (size_t i = 0; i < object->numFields; ++i)
    {
        const void * p = va_arg(vl, void *);
        BDEBUG(BD_SIGNAL, "(%p) qid[%d] initialize encode desc(%s) src %p offset %d arraySz %d\n",
                this, qid, object->fields[i].desc->name, p, object->fields[i].offset, object->fields[i].arraySize);
        encode(object->fields[i].desc, p, object->fields[i].offset, object->fields[i].arraySize);
    }
}

void UMLRTSignalElement::initialize ( const char * name, Id id, const UMLRTCommsPort * srcPort_, const UMLRTObject_class * const desc_, const void * const data, UMLRTPriority priority )
{
    initialize(name, id, desc_->object.sizeOf, priority); // Default initialization.
    srcPort = srcPort_;
    desc = desc_;
    encode(desc, data, 0/*offset*/);
}

void UMLRTSignalElement::setAllocated ( bool allocated_ )
{
    if (allocated_)
    {
        if (allocated)
        {
            FATAL("(%p) qid[%d] was already allocated", this, qid);
        }
        allocated = true;
    }
    else
    {
        if (!allocated)
        {
            FATAL("(%p) qid[%d] was already deallocated", this, qid);
        }
        allocated = false;
    }
}

void UMLRTSignalElement::debugOutputPayload ( )
{
    if (base::debugTypeEnabled(BD_MODEL) && ((object != NULL) || (desc != NULL)))
    {
        fprintf(stdout, "        ");
        if (object != NULL)
        {
            for (size_t i = 0; i < object->numFields; ++i)
            {
                object->fields[i].desc->fprintf(stdout, object->fields[i].desc, payload + object->fields[i].offset, 0, object->fields[i].arraySize);
            }
        }
        else if (desc != NULL)
        {
            desc->fprintf(stdout, desc, payload, 0, 1/*arraySize*/);
        }
        fprintf(stdout, "\n");
    }
}
