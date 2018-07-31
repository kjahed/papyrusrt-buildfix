// umlrttimerprotocol.cc

/*******************************************************************************
* Copyright (c) 2014-2015 Zeligsoft (2009) Limited  and others.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*******************************************************************************/

#include "umlrtslot.hh"
#include "umlrtcommsport.hh"
#include "umlrtcommsportrole.hh"
#include "umlrtcontroller.hh"
#include "umlrttimerprotocol.hh"
#include "umlrttimer.hh"
#include "umlrttimerid.hh"
#include "umlrtapi.hh"
#include "basefatal.hh"
#include "basedebugtype.hh"
#include "basedebug.hh"
#include <stdlib.h>

// Protocol implementation for timer ports.

// Allocate a timer.
/*static*/ UMLRTTimerId UMLRTTimerProtocol::allocateTimer( const UMLRTCommsPort * srcPort, bool isRelative, bool isInterval, const UMLRTTimespec & due,
        UMLRTPriority priority, const void * userData, const UMLRTObject_class * type )
{
    UMLRTTimer * timer = umlrt::TimerGetFromPool();

    if (!timer)
    {
        srcPort->slot->controller->setError(UMLRTController::E_TIMER_GET);
    }
    else if (!timer->allocated)
    {
        FATAL("allocated timer not deemed allocated.");
    }
    else
    {
        UMLRTTimerQueue::timeAdjustLock();
        if (isRelative)
        {
            UMLRTTimespec now;
            UMLRTTimespec::getclock(&now);
            timer->due = now + due;
        }
        else
        {
            timer->due = due;
        }
        UMLRTTimerQueue::timeAdjustUnlock();
        timer->isInterval = isInterval;
        if (isInterval)
        {
            timer->interval = due;
        }
        timer->destPort = srcPort;
        timer->destSlot = srcPort->slot;

        if (type != NULL)
        {
            timer->signal.initialize("timeout", UMLRTTimerProtocol::signal_timeout, srcPort, type, userData, priority);
        }
        else
        {
            timer->signal.initialize("timeout", UMLRTTimerProtocol::signal_timeout, srcPort, priority);
        }

        char buf[UMLRTTimespec::TIMESPEC_TOSTRING_SZ];

        BDEBUG(BD_TIMER, "allocate timer(%p) destPort(%s) isInterval(%d) priority(%d) payloadSize(%d) due(%s)\n",
                timer, timer->destPort->getName(), timer->isInterval, timer->signal.getPriority(), timer->signal.getPayloadSize(),
                timer->isInterval ? timer->due.toString(buf, sizeof(buf)) : timer->due.toString(buf, sizeof(buf)));
    }
    return UMLRTTimerId(timer);
}

const UMLRTTimerId UMLRTTimerProtocol::OutSignals::informAt( const UMLRTCommsPort * srcPort, const UMLRTTimespec & clockTime, UMLRTPriority priority ) const
{
    UMLRTTimerId id = allocateTimer(srcPort, false, false, clockTime, priority, NULL, NULL);

    if (id.isValid())
    {
        // Pass the timer to the controller to monitor it.
        srcPort->slot->controller->startTimer(id.getTimer());
    }
    return id;
}
const UMLRTTimerId UMLRTTimerProtocol::OutSignals::informAt( const UMLRTCommsPort * srcPort, const UMLRTTimespec & clockTime, const void * userData, const UMLRTObject_class * type, UMLRTPriority priority ) const
{
    UMLRTTimerId id = allocateTimer(srcPort, false, false, clockTime, priority, userData, type);

    if (id.isValid())
    {
        // Pass the timer to the controller to monitor it.
        srcPort->slot->controller->startTimer(id.getTimer());
    }
    return id;
}
const UMLRTTimerId UMLRTTimerProtocol::OutSignals::informAt( const UMLRTCommsPort * srcPort, const UMLRTTimespec & clockTime, const UMLRTTypedValue & typedValue, UMLRTPriority priority ) const
{
    UMLRTTimerId id = allocateTimer(srcPort, false, false, clockTime, priority, typedValue.data, typedValue.type);

    if (id.isValid())
    {
        // Pass the timer to the controller to monitor it.
        srcPort->slot->controller->startTimer(id.getTimer());
    }
    return id;
}

const UMLRTTimerId UMLRTTimerProtocol::OutSignals::informIn( const UMLRTCommsPort * srcPort, const UMLRTTimespec & relativeTime, UMLRTPriority priority ) const
{
    UMLRTTimerId id = allocateTimer(srcPort, true, false, relativeTime, priority, NULL, NULL);

    if (id.isValid())
    {
        // Pass the timer to the controller to monitor it.
        srcPort->slot->controller->startTimer(id.getTimer());
    }
    return id;
}
const UMLRTTimerId UMLRTTimerProtocol::OutSignals::informIn( const UMLRTCommsPort * srcPort, const UMLRTTimespec & relativeTime, void * userData, const UMLRTObject_class * type, UMLRTPriority priority ) const
{
    UMLRTTimerId id = allocateTimer(srcPort, true, false, relativeTime, priority, userData, type);

    if (id.isValid())
    {
        // Pass the timer to the controller to monitor it.
        srcPort->slot->controller->startTimer(id.getTimer());
    }
    return id;
}
const UMLRTTimerId UMLRTTimerProtocol::OutSignals::informIn( const UMLRTCommsPort * srcPort, const UMLRTTimespec & relativeTime, const UMLRTTypedValue & typedValue, UMLRTPriority priority ) const
{
    UMLRTTimerId id = allocateTimer(srcPort, true, false, relativeTime, priority, typedValue.data, typedValue.type);

    if (id.isValid())
    {
        // Pass the timer to the controller to monitor it.
        srcPort->slot->controller->startTimer(id.getTimer());
    }
    return id;
}

const UMLRTTimerId UMLRTTimerProtocol::OutSignals::informEvery( const UMLRTCommsPort * srcPort, const UMLRTTimespec & relativeTime, UMLRTPriority priority ) const
{
    UMLRTTimerId id = allocateTimer(srcPort, true, true, relativeTime, priority, NULL, NULL);

    if (id.isValid())
    {
        // Pass the timer to the controller to monitor it.
        srcPort->slot->controller->startTimer(id.getTimer());
    }
    return id;
}
const UMLRTTimerId UMLRTTimerProtocol::OutSignals::informEvery( const UMLRTCommsPort * srcPort, const UMLRTTimespec & relativeTime, void * userData, const UMLRTObject_class * type, UMLRTPriority priority ) const
{
    UMLRTTimerId id = allocateTimer(srcPort, true, true, relativeTime, priority, userData, type);

    if (id.isValid())
    {
        // Pass the timer to the controller to monitor it.
        srcPort->slot->controller->startTimer(id.getTimer());
    }
    return id;
}
const UMLRTTimerId UMLRTTimerProtocol::OutSignals::informEvery( const UMLRTCommsPort * srcPort, const UMLRTTimespec & relativeTime, const UMLRTTypedValue & typedValue, UMLRTPriority priority ) const
{
    UMLRTTimerId id = allocateTimer(srcPort, true, true, relativeTime, priority, typedValue.data, typedValue.type);

    if (id.isValid())
    {
        // Pass the timer to the controller to monitor it.
        srcPort->slot->controller->startTimer(id.getTimer());
    }
    return id;
}

bool UMLRTTimerProtocol::OutSignals::cancelTimer( const UMLRTCommsPort * srcPort, const UMLRTTimerId id ) const
{
    if (!id.isValid())
    {
        srcPort->slot->controller->setError(UMLRTController::E_TIMER_CANC_INV);
        return false;
    }
    return srcPort->slot->controller->cancelTimer(id);
}

void UMLRTTimerProtocol::OutSignals::timeAdjustStart( const UMLRTCommsPort * srcPort ) const
{
    UMLRTTimerQueue::timeAdjustLock();
}

void UMLRTTimerProtocol::OutSignals::timeAdjustComplete(const UMLRTCommsPort * srcPort, const UMLRTTimespec & delta ) const
{
    UMLRTTimerQueue::timeAdjustAllQueues(delta);
    UMLRTTimerQueue::timeAdjustUnlock();
}
