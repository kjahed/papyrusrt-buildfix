// umlrtoutsignal.cc

/*******************************************************************************
* Copyright (c) 2015 Zeligsoft (2009) Limited  and others.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*******************************************************************************/

#include "umlrtcapsule.hh"
#include "umlrtcommsport.hh"
#include "umlrtcommsportfarend.hh"
#include "umlrtcontroller.hh"
#include "umlrtframeservice.hh"
#include "umlrtoutsignal.hh"
#include "umlrtpriority.hh"
#include "umlrtslot.hh"
#include "basefatal.hh"
#include "basedebugtype.hh"
#include "basedebug.hh"

UMLRTOutSignal::UMLRTOutSignal ( )
{

}

UMLRTOutSignal::UMLRTOutSignal ( const UMLRTOutSignal &signal ) : UMLRTSignal(signal)
{

}

UMLRTOutSignal& UMLRTOutSignal::operator= ( const UMLRTOutSignal &signal )
{
    UMLRTSignal::operator=(signal);
    return *this;
}

UMLRTOutSignal::~UMLRTOutSignal ( )
{

}

void UMLRTOutSignal::encode ( const UMLRTObject_class * desc, const void * src, int offset, size_t arraySize )
{
    if (!element)
    {
        FATAL("encode attempted on invalid signal.");
    }
    element->encode(desc, src, offset, arraySize);
}

// Synchronous send out all port instances. Returns the number of replies (0 if fail).
int UMLRTOutSignal::invoke ( UMLRTMessage * replyMsgs )
{
    FATAL("invoke not implemented");
    return 0;
}

// Synchronous send out a specific port instance. Returns the number of replies (0 or 1).
int UMLRTOutSignal::invokeAt ( int index, UMLRTMessage * replyMsg )
{
    FATAL("invokeAt not implemented");
    return 0;
}

// Reply to a synchronous message. Return true if success.
bool UMLRTOutSignal::reply ( )
{
    FATAL("reply not implemented");
    return false;
}

bool UMLRTOutSignal::send ( UMLRTPriority priority ) const
{
    bool ok = false;

    if (!element)
    {
        BDEBUG(BD_SWERR, "WARNING: attempt to send an invalid signal. Capsule and port information are not known.\n");
    }
    else
    {
        const UMLRTCommsPort * srcPort = element->getSrcPort();

        if (!srcPort)
        {
            FATAL("No srcPort when sending a signal. Should be the unbound port at least.");
        }
        else if (srcPort->unbound)
        {
            srcPort->slot->controller->setError(UMLRTController::E_SEND_UNBOUND);
        }
        else
        {
            // Set priority here.
            if ((priority < PRIORITY_SYNCHRONOUS) || (priority >= PRIORITY_MAXPLUS1))
            {
                srcPort->slot->controller->setError(UMLRTController::E_SEND_PRIORITY);
            }
            else
            {
                element->setPriority(priority);

                // Attempt all sends regardless of errors - one successful send will mean overall success.
                for (size_t i = 0; (i < srcPort->numFarEnd); ++i)
                {
                    ok |= signalDeliver(srcPort, i);
                }
                if (ok)
                {
                    // If one succeeds, we'll return success - and remove error code from controller.
                    srcPort->slot->controller->setError(UMLRTController::E_OK);
                }
            }
       }
   }
   return ok;
}

// Send the signal to one far end port. See umlrtsignal.hh for documentation.
bool UMLRTOutSignal::sendAt ( size_t  portIndex, UMLRTPriority priority ) const
{
    bool ok = false;

    if (!element)
    {
        BDEBUG(BD_SEND, "WARNING: attempt to send an invalid signal. Capsule and port information are not known.\n");
    }
    else
    {
        const UMLRTCommsPort * srcPort = element->getSrcPort();

        if (!srcPort)
        {
            FATAL("No srcPort when sending a signal. Should be the unbound port at least.");
        }
        else if (srcPort->unbound)
        {
            srcPort->slot->controller->setError(UMLRTController::E_SEND_UNBOUND);
        }
        else if ((priority < PRIORITY_SYNCHRONOUS) || (priority >= PRIORITY_MAXPLUS1))
        {
            srcPort->slot->controller->setError(UMLRTController::E_SEND_PRIORITY);
        }
        else
        {
            // Set priority here.
            element->setPriority(priority);

            ok = signalDeliver(srcPort, portIndex);
        }
    }
    return ok;
}

// Deliver the signal to a destination.
bool UMLRTOutSignal::signalDeliver ( const UMLRTCommsPort * srcPort, size_t portIndex ) const
{
    bool ok = false;

    const UMLRTCommsPort * destPort = srcPort->farEnds[portIndex].port;

    BDEBUG(BD_SEND, "Controller '%s' capsule '%s' attempt to send signal '%s' from port '%s[%lu]' to capsule '%s' port '%s'\n",
            srcPort->slot->controller->getName(),
            srcPort->slot->capsule->name(),
            getName(),
            srcPort->getName(),
            portIndex,
            (destPort == NULL) ? "(no dest capsule - dest port NULL)" :
                    (destPort->slot->capsule == NULL) ? "(no destination capsule - not instantiated)" : destPort->slot->capsule->getName(),
            (destPort == NULL) ? "(dest port NULL)" : destPort->getName());
    // Lock the RTS
    UMLRTFrameService::rtsLock();

    if (srcPort->slot->condemned)
    {
        srcPort->slot->controller->setError(UMLRTController::E_SEND_FROM_DSTR);
    }
    else if (portIndex >= srcPort->numFarEnd)
    {
        srcPort->slot->controller->setError(UMLRTController::E_SEND_NO_PORT_INST);
    }
    else if (destPort == NULL)
    {
        srcPort->slot->controller->setError(UMLRTController::E_SEND_PRT_NOT_CON);
    }
    else if (destPort->slot->condemned)
    {
        srcPort->slot->controller->setError(UMLRTController::E_SEND_TO_DSTR);
    }
    else if (destPort->slot->capsule == NULL)
    {
        srcPort->slot->controller->setError(UMLRTController::E_SEND_NO_CAP_INST);
    }
    else if (destPort->slot->controller == NULL)
    {
        FATAL("Destination slot %s has no controller when sending from capsule %s via port %s[%d].",
                destPort->slot,
                srcPort->slotName(),
                srcPort->getName(),
                portIndex);
    }
    else
    {
        ok = destPort->slot->controller->deliver( destPort, *this, portIndex );
        // Error code set by 'deliver' if an error occurred.
    }
    // Unlock the RTS
    UMLRTFrameService::rtsUnlock();

    return ok;
}
